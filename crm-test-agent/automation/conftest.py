"""
Shared pytest fixtures for the automation agent (UI + API).
"""
import pytest, requests
from playwright.sync_api import Page
from automation import (
    FRONTEND_URL, CREDS, AUTH_URL,
    get_auth_token, api_headers, auth_only_headers,
    ui_inject_token,
)


# ── Session-scoped auth token ────────────────────────────────────────────────
@pytest.fixture(scope="session")
def token():
    return get_auth_token()


@pytest.fixture(scope="session")
def headers(token):
    return api_headers(token)


@pytest.fixture(scope="session")
def headers_no_ct(token):
    return auth_only_headers(token)


# ── Playwright fixtures ──────────────────────────────────────────────────────
@pytest.fixture(scope="session")
def browser_context_args():
    return {
        "base_url": FRONTEND_URL,
        "viewport": {"width": 1280, "height": 720},
        "ignore_https_errors": True,
    }


@pytest.fixture(scope="session")
def browser_type_launch_args():
    return {"headless": True, "slow_mo": 0}


@pytest.fixture
def auth_page(page: Page, token: str) -> Page:
    """Playwright page pre-authenticated via localStorage token injection."""
    ui_inject_token(page, token)
    return page
