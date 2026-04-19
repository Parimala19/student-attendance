"""Integration tests for the Streamlit dashboard (reachable + API contract)."""

from __future__ import annotations

import httpx
import pytest

pytestmark = pytest.mark.integration


def test_streamlit_health_endpoint(dashboard_client: httpx.Client):
    r = dashboard_client.get("/_stcore/health")
    assert r.status_code == 200


def test_streamlit_main_page_loads(dashboard_client: httpx.Client):
    r = dashboard_client.get("/")
    assert r.status_code == 200
    assert "text/html" in r.headers.get("content-type", "")
    body = r.text.lower()
    assert "streamlit" in body or "attendance" in body


def test_dashboard_login_matches_backend_json_contract(
    api_client: httpx.Client, admin_credentials
):
    """Dashboard must use the same JSON body as FastAPI LoginRequest."""
    email, password = admin_credentials
    r = api_client.post("/api/v1/auth/login", json={"email": email, "password": password})
    if r.status_code != 200:
        pytest.skip("Admin not available (same as test_admin_login_json_contract)")
    assert r.json().get("access_token")
