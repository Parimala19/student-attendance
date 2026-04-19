import uuid
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.schemas import AttendanceResponse, CheckInRequest
from app.core.database import get_db
from app.models.attendance import Attendance
from app.models.course import Course

router = APIRouter()

CONFIDENCE_THRESHOLD = 0.6


@router.post("/check-in", response_model=AttendanceResponse, status_code=status.HTTP_201_CREATED)
async def check_in(body: CheckInRequest, db: AsyncSession = Depends(get_db)):
    if body.confidence_score < CONFIDENCE_THRESHOLD:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Confidence score {body.confidence_score} below threshold {CONFIDENCE_THRESHOLD}",
        )

    course_result = await db.execute(select(Course).where(Course.id == body.course_id))
    course = course_result.scalar_one_or_none()
    if not course:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Course not found")

    now = datetime.utcnow()  # naive UTC — matches TIMESTAMP WITHOUT TIME ZONE column

    # Determine late status based on course schedule
    attendance_status = "present"
    if course.schedule and "time" in course.schedule:
        scheduled_time = course.schedule["time"]
        hour, minute = map(int, scheduled_time.split(":"))
        if now.hour > hour or (now.hour == hour and now.minute > minute + 15):
            attendance_status = "late"

    attendance = Attendance(
        student_id=body.student_id,
        course_id=body.course_id,
        checked_in_at=now,
        status=attendance_status,
        confidence_score=body.confidence_score,
        latitude=body.latitude,
        longitude=body.longitude,
        device_info=body.device_info,
    )
    db.add(attendance)
    await db.commit()
    await db.refresh(attendance)
    return attendance


@router.get("/course/{course_id}", response_model=list[AttendanceResponse])
async def get_course_attendance(
    course_id: uuid.UUID,
    date: str | None = Query(None, description="Filter by date (YYYY-MM-DD)"),
    db: AsyncSession = Depends(get_db),
):
    query = select(Attendance).where(Attendance.course_id == course_id)

    if date:
        filter_date = datetime.strptime(date, "%Y-%m-%d")
        next_day = filter_date.replace(hour=23, minute=59, second=59)
        query = query.where(
            Attendance.checked_in_at >= filter_date,
            Attendance.checked_in_at <= next_day,
        )

    query = query.order_by(Attendance.checked_in_at.desc())
    result = await db.execute(query)
    return result.scalars().all()


@router.get("/student/{student_id}", response_model=list[AttendanceResponse])
async def get_student_attendance(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Attendance)
        .where(Attendance.student_id == student_id)
        .order_by(Attendance.checked_in_at.desc())
    )
    return result.scalars().all()
