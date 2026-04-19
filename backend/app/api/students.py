import uuid

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.api.schemas import (
    EmbeddingResponse,
    EmbeddingUpload,
    StudentCreate,
    StudentResponse,
)
from app.core.database import get_db
from app.core.deps import get_current_user
from app.core.security import hash_password
from app.models.face_embedding import FaceEmbedding
from app.models.student import Student
from app.models.user import User

router = APIRouter()

EMBEDDING_DIM = 512


@router.post("/register", response_model=StudentResponse, status_code=status.HTTP_201_CREATED)
async def register_student(body: StudentCreate, db: AsyncSession = Depends(get_db)):
    existing = await db.execute(
        select(Student).where(Student.student_number == body.student_number)
    )
    if existing.scalar_one_or_none():
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT, detail="Student number already exists"
        )

    student = Student(
        student_number=body.student_number,
        full_name=body.full_name,
        email=body.email,
        password_hash=hash_password(body.password),
    )
    db.add(student)
    await db.commit()
    await db.refresh(student)
    return student


@router.get("/", response_model=list[StudentResponse])
async def list_students(
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(get_current_user),
):
    result = await db.execute(select(Student).order_by(Student.full_name))
    return result.scalars().all()


@router.get("/{student_id}", response_model=StudentResponse)
async def get_student(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(get_current_user),
):
    result = await db.execute(select(Student).where(Student.id == student_id))
    student = result.scalar_one_or_none()
    if not student:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Student not found")
    return student


@router.delete("/{student_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_student(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(get_current_user),
):
    result = await db.execute(select(Student).where(Student.id == student_id))
    student = result.scalar_one_or_none()
    if not student:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Student not found")
    await db.delete(student)
    await db.commit()


@router.post(
    "/{student_id}/embeddings",
    response_model=EmbeddingResponse,
    status_code=status.HTTP_201_CREATED,
)
async def upload_embedding(
    student_id: uuid.UUID,
    body: EmbeddingUpload,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(Student).where(Student.id == student_id))
    if not result.scalar_one_or_none():
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Student not found")

    if len(body.embedding) != EMBEDDING_DIM:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Embedding must be {EMBEDDING_DIM}-dimensional",
        )

    embedding = FaceEmbedding(
        student_id=student_id,
        embedding=body.embedding,
        capture_angle=body.capture_angle,
    )
    db.add(embedding)
    await db.commit()
    await db.refresh(embedding)
    return EmbeddingResponse(
        id=embedding.id,
        student_id=embedding.student_id,
        embedding=list(embedding.embedding),
        capture_angle=embedding.capture_angle,
        created_at=embedding.created_at,
    )


@router.get("/{student_id}/embeddings", response_model=list[EmbeddingResponse])
async def get_embeddings(
    student_id: uuid.UUID,
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(FaceEmbedding).where(FaceEmbedding.student_id == student_id)
    )
    rows = result.scalars().all()
    return [
        EmbeddingResponse(
            id=row.id,
            student_id=row.student_id,
            embedding=list(row.embedding),
            capture_angle=row.capture_angle,
            created_at=row.created_at,
        )
        for row in rows
    ]
