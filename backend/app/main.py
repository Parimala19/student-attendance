import logging
import os
import uuid

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import select

from app.api.attendance import router as attendance_router
from app.api.auth import router as auth_router
from app.api.courses import router as courses_router
from app.api.reports import router as reports_router
from app.api.students import router as students_router
from app.core.config import settings
from app.core.database import async_session
from app.core.security import hash_password
from app.models.user import User

logger = logging.getLogger(__name__)

_DEFAULT_ADMIN_EMAIL = os.environ.get("ADMIN_EMAIL", "admin@test.com")
_DEFAULT_ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD", "password123")

app = FastAPI(title=settings.app_name, version="0.1.0")


@app.on_event("startup")
async def seed_admin() -> None:
    """Create a default admin user on first startup if none exists."""
    async with async_session() as session:
        result = await session.execute(select(User).where(User.role == "admin").limit(1))
        if result.scalar_one_or_none() is None:
            admin = User(
                id=uuid.uuid4(),
                email=_DEFAULT_ADMIN_EMAIL,
                password_hash=hash_password(_DEFAULT_ADMIN_PASSWORD),
                full_name="Admin",
                role="admin",
            )
            session.add(admin)
            await session.commit()
            logger.info("Default admin user created: %s", _DEFAULT_ADMIN_EMAIL)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router, prefix="/api/v1/auth", tags=["auth"])
app.include_router(students_router, prefix="/api/v1/students", tags=["students"])
app.include_router(courses_router, prefix="/api/v1/courses", tags=["courses"])
app.include_router(attendance_router, prefix="/api/v1/attendance", tags=["attendance"])
app.include_router(reports_router, prefix="/api/v1/reports", tags=["reports"])


@app.get("/health")
async def health_check():
    return {"status": "ok"}
