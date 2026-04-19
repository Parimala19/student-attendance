import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_check_in_low_confidence(client: AsyncClient, auth_client: AsyncClient):
    # Create a course and student
    student_resp = await client.post(
        "/api/v1/students/register",
        json={
            "student_number": "STU010",
            "full_name": "Attend Student",
            "password": "password123",
        },
    )
    student_id = student_resp.json()["id"]

    course_resp = await auth_client.post(
        "/api/v1/courses/",
        json={"name": "Art 101", "code": "ART101"},
    )
    course_id = course_resp.json()["id"]

    resp = await client.post(
        "/api/v1/attendance/check-in",
        json={
            "student_id": student_id,
            "course_id": course_id,
            "confidence_score": 0.3,
        },
    )
    assert resp.status_code == 400
    assert "threshold" in resp.json()["detail"]


@pytest.mark.asyncio
async def test_check_in_success(client: AsyncClient, auth_client: AsyncClient):
    student_resp = await client.post(
        "/api/v1/students/register",
        json={
            "student_number": "STU011",
            "full_name": "Good Student",
            "password": "password123",
        },
    )
    student_id = student_resp.json()["id"]

    course_resp = await auth_client.post(
        "/api/v1/courses/",
        json={"name": "Music 101", "code": "MUS101"},
    )
    course_id = course_resp.json()["id"]

    resp = await client.post(
        "/api/v1/attendance/check-in",
        json={
            "student_id": student_id,
            "course_id": course_id,
            "confidence_score": 0.85,
            "latitude": 40.7128,
            "longitude": -74.006,
        },
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["status"] in ("present", "late")
    assert data["confidence_score"] == 0.85


@pytest.mark.asyncio
async def test_get_course_attendance(client: AsyncClient, auth_client: AsyncClient):
    course_resp = await auth_client.post(
        "/api/v1/courses/",
        json={"name": "History 101", "code": "HIST101"},
    )
    course_id = course_resp.json()["id"]

    resp = await client.get(f"/api/v1/attendance/course/{course_id}")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)
