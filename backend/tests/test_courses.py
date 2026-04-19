import pytest
from httpx import AsyncClient


@pytest.mark.asyncio
async def test_create_course(auth_client: AsyncClient):
    resp = await auth_client.post(
        "/api/v1/courses/",
        json={"name": "Mathematics 101", "code": "MATH101"},
    )
    assert resp.status_code == 201
    assert resp.json()["code"] == "MATH101"


@pytest.mark.asyncio
async def test_list_courses(auth_client: AsyncClient):
    await auth_client.post(
        "/api/v1/courses/",
        json={"name": "Physics 101", "code": "PHYS101"},
    )
    resp = await auth_client.get("/api/v1/courses/")
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)


@pytest.mark.asyncio
async def test_duplicate_course_code(auth_client: AsyncClient):
    payload = {"name": "Chemistry", "code": "CHEM101"}
    await auth_client.post("/api/v1/courses/", json=payload)
    resp = await auth_client.post("/api/v1/courses/", json=payload)
    assert resp.status_code == 409
