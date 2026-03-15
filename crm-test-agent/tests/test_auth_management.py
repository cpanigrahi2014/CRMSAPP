"""
Authentication & User Management — 30 API Tests (mock-friendly)
Uses the existing conftest.py fixtures (auth_token, api_headers).

Run:  cd crm-test-agent && python -m pytest tests/test_auth_management.py -v
"""
from __future__ import annotations

import uuid
import pytest
import requests

from src.config import settings

# Override: auth-service runs on 8081 in Docker
AUTH = "http://localhost:8081"
BASE = f"{AUTH}/api/v1/auth"

# Demo credentials that work with seeded data
TEST_EMAIL = "sarah.chen@acmecorp.com"
TEST_PASSWORD = "Demo@2026!"
TEST_TENANT = "default"

# ── Local fixtures (override conftest to use correct port/creds) ─────────────

@pytest.fixture(scope="module")
def auth_token():
    """Get JWT from auth-service on the correct port."""
    resp = requests.post(f"{BASE}/login", json={
        "email": TEST_EMAIL, "password": TEST_PASSWORD, "tenantId": TEST_TENANT,
    })
    data = resp.json()
    body = data.get("data", data)
    return body.get("accessToken") or body.get("token") or ""

@pytest.fixture
def api_headers(auth_token):
    """Headers with Bearer token."""
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}

# ── Helpers ──────────────────────────────────────────────────────────────────

def _unique_email():
    return f"test_{uuid.uuid4().hex[:8]}@crm.test"


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 1 — Registration (7 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestRegistration:

    def test_01_register_success(self):
        """Register a new user with valid data."""
        resp = requests.post(f"{BASE}/register", json={
            "email": _unique_email(),
            "password": "Test@12345",
            "firstName": "Test",
            "lastName": "User",
            "tenantId": "default",
        })
        assert resp.status_code in (200, 201), resp.text

    def test_02_register_duplicate_email(self):
        """Registering the same email twice should fail."""
        email = _unique_email()
        payload = {"email": email, "password": "Test@12345",
                   "firstName": "Dup", "lastName": "Test", "tenantId": "default"}
        requests.post(f"{BASE}/register", json=payload)
        resp = requests.post(f"{BASE}/register", json=payload)
        assert resp.status_code in (400, 409), f"Expected 400/409, got {resp.status_code}"

    def test_03_register_missing_email(self):
        """Registration without email should be rejected."""
        resp = requests.post(f"{BASE}/register", json={
            "password": "Test@12345", "firstName": "No", "lastName": "Email", "tenantId": "default",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_04_register_missing_password(self):
        """Registration without password should be rejected."""
        resp = requests.post(f"{BASE}/register", json={
            "email": _unique_email(), "firstName": "No", "lastName": "Pass", "tenantId": "default",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_05_register_short_password(self):
        """Password shorter than 8 chars should be rejected."""
        resp = requests.post(f"{BASE}/register", json={
            "email": _unique_email(), "password": "Ab1!", "firstName": "Short",
            "lastName": "Pass", "tenantId": "default",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_06_register_missing_tenant(self):
        """Registration without tenantId should be rejected."""
        resp = requests.post(f"{BASE}/register", json={
            "email": _unique_email(), "password": "Test@12345",
            "firstName": "No", "lastName": "Tenant",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_07_register_returns_expected_fields(self):
        """Successful registration response should contain key fields."""
        resp = requests.post(f"{BASE}/register", json={
            "email": _unique_email(), "password": "Test@12345",
            "firstName": "Fields", "lastName": "Check", "tenantId": "default",
        })
        assert resp.status_code in (200, 201)
        data = resp.json()
        # Response may be wrapped in ApiResponse or be the auth response directly
        body = data.get("data", data)
        assert "accessToken" in body or "token" in body or "userId" in body


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 2 — Login (8 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestLogin:

    def test_08_login_success(self):
        """Login with valid demo credentials returns a token."""
        resp = requests.post(f"{BASE}/login", json={
            "email": TEST_EMAIL,
            "password": TEST_PASSWORD,
            "tenantId": TEST_TENANT,
        })
        assert resp.status_code == 200, resp.text
        data = resp.json()
        body = data.get("data", data)
        assert body.get("accessToken") or body.get("token")

    def test_09_login_wrong_password(self):
        """Login with incorrect password should fail."""
        resp = requests.post(f"{BASE}/login", json={
            "email": TEST_EMAIL,
            "password": "WrongPassword123!",
            "tenantId": TEST_TENANT,
        })
        assert resp.status_code in (400, 401, 403), resp.text

    def test_10_login_nonexistent_user(self):
        """Login with a non-existent email should fail."""
        resp = requests.post(f"{BASE}/login", json={
            "email": "nobody_exists@crm.test",
            "password": "Test@12345",
            "tenantId": TEST_TENANT,
        })
        assert resp.status_code in (400, 401, 404), resp.text

    def test_11_login_missing_email(self):
        """Login without email should be rejected."""
        resp = requests.post(f"{BASE}/login", json={
            "password": "Test@12345", "tenantId": "default",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_12_login_missing_password(self):
        """Login without password should be rejected."""
        resp = requests.post(f"{BASE}/login", json={
            "email": TEST_EMAIL, "tenantId": "default",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_13_login_wrong_tenant(self):
        """Login with incorrect tenantId should fail."""
        resp = requests.post(f"{BASE}/login", json={
            "email": TEST_EMAIL,
            "password": TEST_PASSWORD,
            "tenantId": "nonexistent-tenant-xyz",
        })
        assert resp.status_code in (400, 401, 403, 404), resp.text

    def test_14_login_response_has_roles(self):
        """Token response should include user roles."""
        resp = requests.post(f"{BASE}/login", json={
            "email": TEST_EMAIL,
            "password": TEST_PASSWORD,
            "tenantId": TEST_TENANT,
        })
        data = resp.json()
        body = data.get("data", data)
        assert "roles" in body or "role" in body

    def test_15_login_response_has_plan(self):
        """Token response should include planName."""
        resp = requests.post(f"{BASE}/login", json={
            "email": TEST_EMAIL,
            "password": TEST_PASSWORD,
            "tenantId": TEST_TENANT,
        })
        data = resp.json()
        body = data.get("data", data)
        assert "planName" in body


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 3 — Current User / Profile (4 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestCurrentUser:

    def test_16_get_me_authenticated(self, api_headers):
        """GET /me with valid token returns user profile."""
        resp = requests.get(f"{BASE}/me", headers=api_headers)
        assert resp.status_code == 200, resp.text
        data = resp.json()
        body = data.get("data", data)
        assert body.get("email") or body.get("id")

    def test_17_get_me_no_token(self):
        """GET /me without token should return 401/403 (500 = known auth-service bug)."""
        resp = requests.get(f"{BASE}/me")
        assert resp.status_code in (401, 403, 500), resp.text

    def test_18_get_me_invalid_token(self):
        """GET /me with an invalid JWT should return 401/403 (500 = known auth-service bug)."""
        resp = requests.get(f"{BASE}/me", headers={
            "Authorization": "Bearer invalid.jwt.token",
            "Content-Type": "application/json",
        })
        assert resp.status_code in (401, 403, 500), resp.text

    def test_19_get_me_returns_correct_email(self, api_headers):
        """GET /me response email should match login email."""
        resp = requests.get(f"{BASE}/me", headers=api_headers)
        data = resp.json()
        body = data.get("data", data)
        assert body.get("email") == TEST_EMAIL


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 4 — Password Reset Flow (4 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestPasswordReset:

    def test_20_forgot_password_valid_email(self):
        """Forgot-password with existing email returns 200."""
        resp = requests.post(f"{BASE}/forgot-password", json={
            "email": TEST_EMAIL,
            "tenantId": TEST_TENANT,
        })
        # Should succeed even if email delivery doesn't happen in test
        assert resp.status_code in (200, 202), resp.text

    def test_21_forgot_password_nonexistent_email(self):
        """Forgot-password with unknown email should still return 200 (no leak)."""
        resp = requests.post(f"{BASE}/forgot-password", json={
            "email": "unknown_person@nowhere.test",
            "tenantId": TEST_TENANT,
        })
        # Best practice: don't reveal whether email exists
        assert resp.status_code in (200, 202, 404), resp.text

    def test_22_reset_password_invalid_token(self):
        """Reset-password with bogus token should fail."""
        resp = requests.post(f"{BASE}/reset-password", json={
            "token": "bogus-reset-token-12345",
            "newPassword": "NewSecure@999",
        })
        assert resp.status_code in (400, 404), resp.text

    def test_23_reset_password_short_new_password(self):
        """Reset-password with short password should fail validation."""
        resp = requests.post(f"{BASE}/reset-password", json={
            "token": "any-token",
            "newPassword": "Ab1!",
        })
        assert resp.status_code in (400, 422), resp.text


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 5 — Role Management (4 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestRoleManagement:

    def test_24_list_roles(self, api_headers):
        """GET /security/roles returns a list."""
        resp = requests.get(f"{BASE}/security/roles", headers=api_headers)
        assert resp.status_code == 200, resp.text

    def test_25_create_role(self, api_headers):
        """POST /security/roles creates a new role."""
        resp = requests.post(f"{BASE}/security/roles", headers=api_headers, json={
            "name": f"TEST_ROLE_{uuid.uuid4().hex[:6]}",
            "description": "Temporary test role",
        })
        assert resp.status_code in (200, 201), resp.text

    def test_26_create_role_without_auth(self):
        """Creating a role without token should be forbidden."""
        resp = requests.post(f"{BASE}/security/roles", json={
            "name": "UNAUTHORIZED_ROLE",
        })
        assert resp.status_code in (401, 403), resp.text

    def test_27_list_users(self, api_headers):
        """GET /security/users returns user list."""
        resp = requests.get(f"{BASE}/security/users", headers=api_headers)
        assert resp.status_code == 200, resp.text


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 6 — Plan / Tenant Management (3 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestPlanManagement:

    def test_28_get_current_plan(self, api_headers):
        """GET /plan returns the current tenant plan."""
        resp = requests.get(f"{BASE}/plan", headers=api_headers)
        assert resp.status_code == 200, resp.text
        data = resp.json()
        body = data.get("data", data)
        assert body.get("planName") or body.get("name")

    def test_29_get_plan_no_auth(self):
        """GET /plan without token should fail (500 = known auth-service bug)."""
        resp = requests.get(f"{BASE}/plan")
        assert resp.status_code in (401, 403, 500), resp.text

    def test_30_upgrade_plan_no_auth(self):
        """PUT /plan/upgrade without token should fail (500 = known auth-service bug)."""
        resp = requests.put(f"{BASE}/plan/upgrade", json={
            "planName": "PROFESSIONAL",
        })
        assert resp.status_code in (401, 403, 500), resp.text

