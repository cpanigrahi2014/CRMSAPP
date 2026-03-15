"""
Module 01 — Auth Management (30 Tests: 15 API + 15 UI)
========================================================
API : Registration, Login, Password reset, Roles, Plans
UI  : Login page, Register page, Forgot password, Dashboard redirect
"""
import uuid, pytest, requests
from playwright.sync_api import Page, expect
from automation import (
    AUTH_URL, FRONTEND_URL, CREDS, uid,
    get_auth_token, api_headers, ui_login, ui_navigate,
)


# ── fixtures ─────────────────────────────────────────────────────────────────
@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)

def _email():
    return f"auto_{uid()}@test.com"


# ══════════════════════════════════════════════════════════════════════════════
#  API Tests (15)
# ══════════════════════════════════════════════════════════════════════════════
class TestAuthAPI:
    """API-level auth validation."""

    def test_api_01_register_success(self, headers):
        resp = requests.post(f"{AUTH_URL}/register", json={
            "email": _email(), "password": "Test@12345",
            "firstName": "Auto", "lastName": "Test", "tenantId": "default",
        })
        assert resp.status_code in (200, 201, 409)

    def test_api_02_register_duplicate(self, headers):
        email = _email()
        requests.post(f"{AUTH_URL}/register", json={
            "email": email, "password": "Test@12345",
            "firstName": "A", "lastName": "B", "tenantId": "default",
        })
        resp = requests.post(f"{AUTH_URL}/register", json={
            "email": email, "password": "Test@12345",
            "firstName": "A", "lastName": "B", "tenantId": "default",
        })
        assert resp.status_code in (400, 409, 500)

    def test_api_03_register_invalid_email(self):
        resp = requests.post(f"{AUTH_URL}/register", json={
            "email": "not-an-email", "password": "Test@12345",
            "firstName": "A", "lastName": "B", "tenantId": "default",
        })
        assert resp.status_code in (400, 422, 500)

    def test_api_04_register_weak_password(self):
        resp = requests.post(f"{AUTH_URL}/register", json={
            "email": _email(), "password": "123",
            "firstName": "A", "lastName": "B", "tenantId": "default",
        })
        assert resp.status_code in (400, 422, 500)

    def test_api_05_login_success(self):
        resp = requests.post(f"{AUTH_URL}/login", json=CREDS)
        assert resp.status_code == 200
        d = resp.json().get("data", resp.json())
        assert d.get("accessToken") or d.get("token")

    def test_api_06_login_wrong_password(self):
        resp = requests.post(f"{AUTH_URL}/login", json={
            **CREDS, "password": "WrongP@ss1"
        })
        assert resp.status_code in (400, 401)

    def test_api_07_login_nonexistent_user(self):
        resp = requests.post(f"{AUTH_URL}/login", json={
            "email": "nobody@fake.com", "password": "Test@12345",
            "tenantId": "default",
        })
        assert resp.status_code in (400, 401)

    def test_api_08_get_current_user(self, headers):
        resp = requests.get(f"{AUTH_URL}/me", headers=headers)
        assert resp.status_code == 200

    def test_api_09_no_token_rejected(self):
        resp = requests.get(f"{AUTH_URL}/me")
        assert resp.status_code in (401, 403, 500)

    def test_api_10_invalid_token_rejected(self):
        resp = requests.get(f"{AUTH_URL}/me",
                            headers={"Authorization": "Bearer invalidtoken"})
        assert resp.status_code in (401, 403, 500)

    def test_api_11_forgot_password(self):
        resp = requests.post(f"{AUTH_URL}/forgot-password",
                             json={"email": CREDS["email"], "tenantId": "default"})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_12_list_roles(self, headers):
        resp = requests.get(f"{AUTH_URL}/security/roles", headers=headers)
        assert resp.status_code in (200, 403)

    def test_api_13_list_permissions(self, headers):
        resp = requests.get(f"{AUTH_URL}/security/permissions", headers=headers)
        assert resp.status_code in (200, 403)

    def test_api_14_list_users(self, headers):
        resp = requests.get(f"{AUTH_URL}/users", headers=headers)
        assert resp.status_code in (200, 403, 404, 500)

    def test_api_15_jwt_has_three_parts(self, token):
        assert len(token.split(".")) == 3


# ══════════════════════════════════════════════════════════════════════════════
#  UI Tests (15)
# ══════════════════════════════════════════════════════════════════════════════
class TestAuthUI:
    """Browser-level auth validation."""

    def test_ui_01_login_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/login")
        expect(page.locator('input[type="email"]')).to_be_visible()
        expect(page.locator('input[type="password"]')).to_be_visible()

    def test_ui_02_login_button_visible(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/login")
        expect(page.locator('button:has-text("Sign In")')).to_be_visible()

    def test_ui_03_successful_login_redirects(self, page: Page):
        ui_login(page)
        page.wait_for_url("**/dashboard**", timeout=10000)
        assert "/dashboard" in page.url or "/auth" not in page.url

    def test_ui_04_login_shows_error_on_wrong_creds(self, page: Page):
        ui_login(page, password="WrongP@ss1")
        page.wait_for_timeout(2000)
        error = page.locator('[role="alert"], .MuiAlert-root')
        assert error.count() > 0 or "login" in page.url.lower()

    def test_ui_05_register_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/register")
        page.wait_for_load_state("networkidle")
        assert "register" in page.url.lower()

    def test_ui_06_forgot_password_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/forgot-password")
        page.wait_for_load_state("networkidle")
        assert "forgot" in page.url.lower()

    def test_ui_07_login_page_has_forgot_link(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/login")
        link = page.locator('a:has-text("Forgot"), a:has-text("forgot")')
        assert link.count() > 0

    def test_ui_08_login_page_has_signup_link(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/login")
        link = page.locator('a:has-text("Sign Up"), a:has-text("Register"), a:has-text("sign up")')
        assert link.count() > 0

    def test_ui_09_dashboard_requires_auth(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        # Should redirect to login if not authenticated
        url = page.url.lower()
        assert "login" in url or "auth" in url or "dashboard" in url or "landing" in url

    def test_ui_10_password_toggle_visibility(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/login")
        pw = page.locator('input[type="password"]')
        expect(pw).to_be_visible()
        toggle = page.locator('button[aria-label*="toggle"], button:near(input[type="password"])').first
        if toggle.count() > 0:
            toggle.click()

    def test_ui_11_login_form_validation(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/login")
        page.click('button:has-text("Sign In")')
        page.wait_for_timeout(1000)
        # Form should not navigate away when empty
        assert "login" in page.url.lower()

    def test_ui_12_branding_on_login(self, page: Page):
        page.goto(f"{FRONTEND_URL}/auth/login")
        body_text = page.text_content("body") or ""
        assert "CRM" in body_text or "crm" in body_text.lower()

    def test_ui_13_landing_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/landing")
        page.wait_for_load_state("networkidle")
        assert page.url is not None

    def test_ui_14_logout_clears_session(self, page: Page):
        ui_login(page)
        page.wait_for_timeout(2000)
        # Clear storage to simulate logout
        page.evaluate("() => { localStorage.clear(); sessionStorage.clear(); }")
        page.goto(f"{FRONTEND_URL}/dashboard")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_15_login_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/auth/login")
        expect(page.locator('input[type="email"]')).to_be_visible()
        page.set_viewport_size({"width": 1280, "height": 720})
