"""Integration tests for the FastAPI backend (live stack)."""

from __future__ import annotations

import uuid

import httpx
import pytest

pytestmark = pytest.mark.integration


def _unique_student_payload() -> dict:
    suffix = uuid.uuid4().hex[:8]
    return {
        "student_number": f"IT-{suffix}",
        "full_name": f"Integration Test {suffix}",
        "email": f"it_{suffix}@example.com",
        "password": "password123",
    }


def test_health(api_client: httpx.Client):
    r = api_client.get("/health")
    assert r.status_code == 200
    assert r.json() == {"status": "ok"}


def test_openapi_docs_available(api_client: httpx.Client):
    r = api_client.get("/docs")
    assert r.status_code == 200
    assert "swagger" in r.text.lower() or "openapi" in r.text.lower()


def test_openapi_schema_json(api_client: httpx.Client):
    r = api_client.get("/openapi.json")
    assert r.status_code == 200
    schema = r.json()
    assert schema.get("openapi")
    paths = schema.get("paths", {})
    assert "/api/v1/auth/login" in paths
    assert "/api/v1/students/register" in paths


def test_student_register_and_student_login(api_client: httpx.Client):
    body = _unique_student_payload()
    r = api_client.post("/api/v1/students/register", json=body)
    assert r.status_code == 201, r.text
    student = r.json()
    assert student["student_number"] == body["student_number"]

    r2 = api_client.post(
        "/api/v1/auth/student-login",
        json={"email": body["email"], "password": body["password"]},
    )
    assert r2.status_code == 200, r2.text
    tokens = r2.json()
    assert "access_token" in tokens


def test_list_courses_public(api_client: httpx.Client):
    r = api_client.get("/api/v1/courses/")
    assert r.status_code == 200
    assert isinstance(r.json(), list)


def test_admin_login_json_contract(api_client: httpx.Client, admin_credentials):
    email, password = admin_credentials
    r = api_client.post("/api/v1/auth/login", json={"email": email, "password": password})
    if r.status_code != 200:
        pytest.skip("Admin not available or wrong password (expected on fresh DB)")
    data = r.json()
    assert "access_token" in data
    assert "refresh_token" in data


def test_admin_me_and_create_course_flow(api_client: httpx.Client, require_admin_token: str):
    headers = {"Authorization": f"Bearer {require_admin_token}"}
    r = api_client.get("/api/v1/auth/me", headers=headers)
    assert r.status_code == 200
    me = r.json()
    assert me.get("email")
    assert me.get("role") == "admin"

    code = f"IT-C-{uuid.uuid4().hex[:10]}"
    r2 = api_client.post(
        "/api/v1/courses/",
        headers=headers,
        json={
            "name": "Integration Course",
            "code": code,
            "schedule": {"time": "09:00"},
        },
    )
    assert r2.status_code == 201, r2.text
    course = r2.json()
    assert course["code"] == code

    r3 = api_client.get("/api/v1/reports/summary", headers=headers)
    assert r3.status_code == 200
    assert isinstance(r3.json(), list)
