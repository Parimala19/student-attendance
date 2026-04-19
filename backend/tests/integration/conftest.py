"""Fixtures for HTTP integration tests against a running Docker stack.

Set INTEGRATION_API_BASE_URL (default http://127.0.0.1:8000) and optionally
INTEGRATION_DASHBOARD_URL (default http://127.0.0.1:8501).

Admin-only tests use INTEGRATION_ADMIN_EMAIL / INTEGRATION_ADMIN_PASSWORD
(defaults match README: admin@test.com / password123). If login fails, those
tests are skipped (empty or wrong credentials on a fresh DB).

Set INTEGRATION_SKIP=1 to skip the whole module without probing the network.
"""

from __future__ import annotations

import os

import httpx
import pytest

pytestmark = pytest.mark.integration

_DEFAULT_API = "http://127.0.0.1:8000"
_DEFAULT_DASHBOARD = "http://127.0.0.1:8501"


def _env(name: str, default: str) -> str:
    v = os.environ.get(name)
    return v.strip() if v else default


@pytest.fixture(scope="session")
def integration_skip() -> None:
    if os.environ.get("INTEGRATION_SKIP", "").lower() in ("1", "true", "yes"):
        pytest.skip("INTEGRATION_SKIP is set")


@pytest.fixture(scope="session")
def api_base_url(integration_skip) -> str:
    return _env("INTEGRATION_API_BASE_URL", _DEFAULT_API).rstrip("/")


@pytest.fixture(scope="session")
def dashboard_base_url(integration_skip) -> str:
    return _env("INTEGRATION_DASHBOARD_URL", _DEFAULT_DASHBOARD).rstrip("/")


@pytest.fixture(scope="session")
def stack_reachable(api_base_url: str) -> None:
    try:
        r = httpx.get(f"{api_base_url}/health", timeout=3.0)
        r.raise_for_status()
    except (httpx.HTTPError, OSError) as e:
        pytest.skip(f"Backend not reachable at {api_base_url}: {e}")


@pytest.fixture(scope="session")
def dashboard_reachable(dashboard_base_url: str, stack_reachable) -> None:
    try:
        r = httpx.get(f"{dashboard_base_url}/_stcore/health", timeout=3.0)
        r.raise_for_status()
    except (httpx.HTTPError, OSError) as e:
        pytest.skip(f"Dashboard not reachable at {dashboard_base_url}: {e}")


@pytest.fixture
def api_client(api_base_url: str, stack_reachable) -> httpx.Client:
    with httpx.Client(base_url=api_base_url, timeout=15.0) as client:
        yield client


@pytest.fixture
def dashboard_client(dashboard_base_url: str, dashboard_reachable) -> httpx.Client:
    with httpx.Client(base_url=dashboard_base_url, timeout=15.0) as client:
        yield client


@pytest.fixture(scope="session")
def admin_credentials() -> tuple[str, str]:
    email = _env("INTEGRATION_ADMIN_EMAIL", "admin@test.com")
    password = _env("INTEGRATION_ADMIN_PASSWORD", "password123")
    return email, password


@pytest.fixture(scope="session")
def admin_token(api_base_url: str, stack_reachable, admin_credentials) -> str | None:
    email, password = admin_credentials
    try:
        r = httpx.post(
            f"{api_base_url}/api/v1/auth/login",
            json={"email": email, "password": password},
            timeout=10.0,
        )
    except (httpx.HTTPError, OSError):
        return None
    if r.status_code != 200:
        return None
    data = r.json()
    return data.get("access_token")


@pytest.fixture
def require_admin_token(admin_token: str | None) -> str:
    if not admin_token:
        pytest.skip(
            "Admin login failed; create an admin user or set "
            "INTEGRATION_ADMIN_EMAIL / INTEGRATION_ADMIN_PASSWORD"
        )
    return admin_token
