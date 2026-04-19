import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_register_student(client: AsyncClient):
    resp = await client.post(
        "/api/v1/students/register",
        json={
            "student_number": "STU001",
            "full_name": "John Doe",
            "email": "john@test.com",
            "password": "password123",
        },
    )
    assert resp.status_code == 201
    data = resp.json()
    assert data["student_number"] == "STU001"
    assert data["full_name"] == "John Doe"


@pytest.mark.asyncio
async def test_register_duplicate_student(client: AsyncClient):
    payload = {
        "student_number": "STU002",
        "full_name": "Jane Doe",
        "email": "jane@test.com",
        "password": "password123",
    }
    await client.post("/api/v1/students/register", json=payload)
    resp = await client.post("/api/v1/students/register", json=payload)
    assert resp.status_code == 409


@pytest.mark.asyncio
async def test_list_students(auth_client: AsyncClient):
    await auth_client.post(
        "/api/v1/students/register",
        json={
            "student_number": "STU003",
            "full_name": "Test Student",
            "password": "password123",
        },
    )
    # Need to use the base client URL for list (auth required)
    resp = await auth_client.get("/api/v1/students/")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)


@pytest.mark.asyncio
async def test_upload_embedding_wrong_dim(client: AsyncClient):
    # Register a student first
    reg = await client.post(
        "/api/v1/students/register",
        json={
            "student_number": "STU004",
            "full_name": "Embed Student",
            "password": "password123",
        },
    )
    student_id = reg.json()["id"]

    resp = await client.post(
        f"/api/v1/students/{student_id}/embeddings",
        json={"embedding": [0.1] * 128, "capture_angle": "front"},
    )
    assert resp.status_code == 400
    assert "512" in resp.json()["detail"]
