from datetime import UTC, datetime

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.schemas import AttendanceSummary
from app.core.database import get_db
from app.core.deps import get_current_user
from app.models.attendance import Attendance
from app.models.course import Course, Enrollment
from app.models.user import User

router = APIRouter()


@router.get("/summary", response_model=list[AttendanceSummary])
async def attendance_summary(
    date: str | None = Query(None, description="Filter by date (YYYY-MM-DD)"),
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(get_current_user),
):
    courses_result = await db.execute(select(Course))
    courses = courses_result.scalars().all()

    summaries = []
    for course in courses:
        enrolled_result = await db.execute(
            select(func.count()).where(Enrollment.course_id == course.id)
        )
        total_students = enrolled_result.scalar() or 0

        attendance_query = select(Attendance).where(Attendance.course_id == course.id)
        if date:
            filter_date = datetime.strptime(date, "%Y-%m-%d").replace(tzinfo=UTC)
            next_day = filter_date.replace(hour=23, minute=59, second=59)
            attendance_query = attendance_query.where(
                Attendance.checked_in_at >= filter_date,
                Attendance.checked_in_at <= next_day,
            )

        attendance_result = await db.execute(attendance_query)
        records = attendance_result.scalars().all()

        present = sum(1 for r in records if r.status == "present")
        late = sum(1 for r in records if r.status == "late")
        absent = max(0, total_students - present - late)
        rate = ((present + late) / total_students * 100) if total_students > 0 else 0.0

        summaries.append(
            AttendanceSummary(
                course_id=course.id,
                course_name=course.name,
                total_students=total_students,
                present=present,
                late=late,
                absent=absent,
                attendance_rate=round(rate, 1),
            )
        )

    return summaries
