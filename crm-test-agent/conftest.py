"""
Shared pytest configuration & fixtures for both hand-written and generated tests.
"""
from __future__ import annotations

import pytest
from playwright.sync_api import Browser, BrowserContext, Page

from src.config import settings


# ── Playwright fixtures ──────────────────────────────────────────────────────

@pytest.fixture(scope="session")
def browser_context_args():
    """Default context args for Playwright."""
    return {
        "base_url": settings.crm_base_url,
        "viewport": {"width": 1280, "height": 720},
        "ignore_https_errors": True,
    }


@pytest.fixture(scope="session")
def browser_type_launch_args():
    """Launch args for the browser."""
    return {
        "headless": settings.headless,
        "slow_mo": settings.slow_mo,
    }


# ── Auth fixtures ────────────────────────────────────────────────────────────

@pytest.fixture(scope="session")
def auth_token():
    """Obtain a JWT from the auth service."""
    import requests

    resp = requests.post(
        f"{settings.auth_service_url}/api/v1/auth/login",
        json={
            "email": settings.test_admin_email,
            "password": settings.test_admin_password,
            "tenantId": settings.test_tenant_id,
        },
    )
    if resp.status_code == 200:
        data = resp.json()
        return data.get("token") or data.get("accessToken") or ""
    return ""


@pytest.fixture
def api_headers(auth_token):
    """Headers dict with Bearer token."""
    return {
        "Authorization": f"Bearer {auth_token}",
        "Content-Type": "application/json",
    }


@pytest.fixture
def authenticated_page(page: Page, auth_token: str) -> Page:
    """A Playwright page with auth token injected into localStorage."""
    page.goto(settings.crm_base_url)
    page.evaluate(
        f"""() => {{
            localStorage.setItem('token', '{auth_token}');
        }}"""
    )
    page.reload()
    page.wait_for_load_state("networkidle")
    return page
