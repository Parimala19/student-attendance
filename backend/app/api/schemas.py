import uuid
from datetime import datetime

from pydantic import BaseModel, EmailStr


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class UserCreate(BaseModel):
    email: EmailStr
    password: str
    full_name: str
    role: str = "teacher"


class UserResponse(BaseModel):
    id: uuid.UUID
    email: str
    full_name: str
    role: str

    model_config = {"from_attributes": True}


class StudentCreate(BaseModel):
    student_number: str
    full_name: str
    email: EmailStr | None = None
    password: str


class StudentResponse(BaseModel):
    id: uuid.UUID
    student_number: str
    full_name: str
    email: str | None
    created_at: datetime

    model_config = {"from_attributes": True}


class EmbeddingUpload(BaseModel):
    embedding: list[float]
    capture_angle: str | None = None


class EmbeddingResponse(BaseModel):
    id: uuid.UUID
    student_id: uuid.UUID
    embedding: list[float]
    capture_angle: str | None
    created_at: datetime


class CourseCreate(BaseModel):
    name: str
    code: str
    schedule: dict | None = None


class CourseResponse(BaseModel):
    id: uuid.UUID
    name: str
    code: str
    teacher_id: uuid.UUID | None
    schedule: dict | None

    model_config = {"from_attributes": True}


class EnrollmentRequest(BaseModel):
    student_id: uuid.UUID
    course_id: uuid.UUID


class CheckInRequest(BaseModel):
    student_id: uuid.UUID
    course_id: uuid.UUID
    confidence_score: float
    latitude: float | None = None
    longitude: float | None = None
    device_info: dict | None = None


class AttendanceResponse(BaseModel):
    id: uuid.UUID
    student_id: uuid.UUID
    course_id: uuid.UUID
    checked_in_at: datetime
    status: str
    confidence_score: float | None

    model_config = {"from_attributes": True}


class AttendanceSummary(BaseModel):
    course_id: uuid.UUID
    course_name: str
    total_students: int
    present: int
    late: int
    absent: int
    attendance_rate: float
