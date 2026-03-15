"""
Scenario 24: Role-based Access Control

End-to-end tests for RBAC enforcement:
1. Sales rep (USER) can list opportunities but cannot delete
2. Sales rep cannot change opportunity stage (MANAGER+ only)
3. Sales rep cannot access security admin endpoints
4. Manager can see all opportunities and change stages
5. Admin can edit sharing rules (roles, permissions)
"""
from __future__ import annotations

import uuid
import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth"
OPP_URL = "http://localhost:8085/api/v1/opportunities"
LEAD_URL = "http://localhost:8082/api/v1/leads"

ADMIN_CREDS = {
    "email": "sarah.chen@acmecorp.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}


# ── Helpers ──────────────────────────────────────────────────────────────────

def _login(email: str, password: str) -> dict:
    resp = requests.post(
        f"{AUTH_URL}/login",
        json={"email": email, "password": password, "tenantId": "default"},
        timeout=15,
    )
    assert resp.status_code == 200, f"Login failed for {email}: {resp.text}"
    data = resp.json()["data"]
    return {
        "token": data["accessToken"],
        "userId": data["userId"],
        "roles": data["roles"],
    }


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}", "X-Tenant-ID": "default"}


# ── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def admin():
    """Login as ADMIN."""
    info = _login(ADMIN_CREDS["email"], ADMIN_CREDS["password"])
    assert "ADMIN" in info["roles"]
    return info


@pytest.fixture(scope="module")
def sales_rep(admin):
    """Register a fresh USER-role sales rep."""
    tag = uuid.uuid4().hex[:8]
    email = f"rep_{tag}@test.example"
    password = "RepPass@123!"
    body = {
        "email": email,
        "password": password,
        "firstName": "Rep",
        "lastName": tag,
        "tenantId": "default",
    }
    resp = requests.post(f"{AUTH_URL}/register", json=body, timeout=15)
    assert resp.status_code in (200, 201), f"Register failed: {resp.text}"
    data = resp.json()["data"]
    assert "USER" in data["roles"], f"Expected USER role, got {data['roles']}"
    return {
        "token": data["accessToken"],
        "userId": data["userId"],
        "email": email,
        "roles": data["roles"],
    }


@pytest.fixture(scope="module")
def manager(admin):
    """Register a user and promote to MANAGER."""
    tag = uuid.uuid4().hex[:8]
    email = f"mgr_{tag}@test.example"
    password = "MgrPass@123!"
    body = {
        "email": email,
        "password": password,
        "firstName": "Manager",
        "lastName": tag,
        "tenantId": "default",
    }
    resp = requests.post(f"{AUTH_URL}/register", json=body, timeout=15)
    assert resp.status_code in (200, 201), f"Register failed: {resp.text}"
    user_id = resp.json()["data"]["userId"]

    # Promote to MANAGER using admin token
    promote = requests.post(
        f"{AUTH_URL}/users/{user_id}/roles",
        params={"roleName": "MANAGER"},
        headers=_headers(admin["token"]),
        timeout=15,
    )
    assert promote.status_code == 200, f"Promote failed: {promote.text}"

    # Re-login to get updated JWT with MANAGER role
    info = _login(email, password)
    assert "MANAGER" in info["roles"], f"Expected MANAGER role, got {info['roles']}"
    return {**info, "email": email}


@pytest.fixture(scope="module")
def sample_opp_id(admin):
    """Get an existing opportunity ID for testing."""
    resp = requests.get(
        OPP_URL,
        params={"page": 0, "size": 1},
        headers=_headers(admin["token"]),
        timeout=15,
    )
    assert resp.status_code == 200
    content = resp.json()["data"]["content"]
    assert len(content) > 0, "No opportunities exist for testing"
    return content[0]["id"]


# ── Test Group 1: USER Role Restrictions ─────────────────────────────────────

class TestUserRestrictions:
    """Sales rep (USER) should have limited access."""

    def test_user_can_list_opportunities(self, sales_rep):
        resp = requests.get(
            OPP_URL,
            params={"page": 0, "size": 5},
            headers=_headers(sales_rep["token"]),
            timeout=15,
        )
        assert resp.status_code == 200
        assert resp.json()["success"] is True

    def test_user_can_read_single_opportunity(self, sales_rep, sample_opp_id):
        resp = requests.get(
            f"{OPP_URL}/{sample_opp_id}",
            headers=_headers(sales_rep["token"]),
            timeout=15,
        )
        assert resp.status_code == 200

    def test_user_cannot_delete_opportunity(self, sales_rep, sample_opp_id):
        resp = requests.delete(
            f"{OPP_URL}/{sample_opp_id}",
            headers=_headers(sales_rep["token"]),
            timeout=15,
        )
        assert resp.status_code == 403, f"Expected 403, got {resp.status_code}"

    def test_user_cannot_change_stage(self, sales_rep, sample_opp_id):
        resp = requests.patch(
            f"{OPP_URL}/{sample_opp_id}/stage",
            params={"stage": "QUALIFICATION"},
            headers=_headers(sales_rep["token"]),
            timeout=15,
        )
        assert resp.status_code == 403, f"Expected 403, got {resp.status_code}"

    def test_user_cannot_access_security_admin(self, sales_rep):
        endpoints = [
            f"{AUTH_URL}/security/roles",
            f"{AUTH_URL}/security/permissions",
            f"{AUTH_URL}/security/field-security",
            f"{AUTH_URL}/security/audit-logs",
        ]
        for url in endpoints:
            resp = requests.get(
                url, headers=_headers(sales_rep["token"]), timeout=15
            )
            assert resp.status_code == 403, (
                f"USER should not access {url}, got {resp.status_code}"
            )

    def test_user_cannot_access_lead_analytics(self, sales_rep):
        resp = requests.get(
            f"{LEAD_URL}/analytics",
            headers=_headers(sales_rep["token"]),
            timeout=15,
        )
        assert resp.status_code == 403


# ── Test Group 2: MANAGER Role Capabilities ──────────────────────────────────

class TestManagerCapabilities:
    """Manager can see all and manage stages."""

    def test_manager_can_list_all_opportunities(self, manager):
        resp = requests.get(
            OPP_URL,
            params={"page": 0, "size": 5},
            headers=_headers(manager["token"]),
            timeout=15,
        )
        assert resp.status_code == 200
        assert resp.json()["data"]["totalElements"] > 0

    def test_manager_can_access_lead_analytics(self, manager):
        resp = requests.get(
            f"{LEAD_URL}/analytics",
            headers=_headers(manager["token"]),
            timeout=15,
        )
        assert resp.status_code == 200
        assert resp.json()["success"] is True

    def test_manager_cannot_access_security_admin(self, manager):
        resp = requests.get(
            f"{AUTH_URL}/security/roles",
            headers=_headers(manager["token"]),
            timeout=15,
        )
        assert resp.status_code == 403


# ── Test Group 3: ADMIN Role Capabilities ────────────────────────────────────

class TestAdminCapabilities:
    """Admin can manage roles, permissions, and sharing rules."""

    def test_admin_can_list_roles(self, admin):
        resp = requests.get(
            f"{AUTH_URL}/security/roles",
            headers=_headers(admin["token"]),
            timeout=15,
        )
        assert resp.status_code == 200
        roles = resp.json()["data"]
        role_names = [r["name"] for r in roles]
        assert "ADMIN" in role_names
        assert "MANAGER" in role_names
        assert "USER" in role_names

    def test_admin_can_create_permission(self, admin):
        tag = uuid.uuid4().hex[:6]
        body = {
            "name": f"S24_PERM_{tag}",
            "description": "Scenario 24 test permission",
            "resource": "opportunities",
            "actions": "read,update",
        }
        resp = requests.post(
            f"{AUTH_URL}/security/permissions",
            json=body,
            headers={**_headers(admin["token"]), "Content-Type": "application/json"},
            timeout=15,
        )
        assert resp.status_code in (200, 201), f"Create permission failed: {resp.text}"
        data = resp.json()["data"]
        assert data["name"] == body["name"]
        assert data["resource"] == "opportunities"

    def test_admin_can_create_field_security_rule(self, admin):
        tag = uuid.uuid4().hex[:6]
        body = {
            "entityType": "Opportunity",
            "fieldName": f"test_field_{tag}",
            "roleName": "USER",
            "accessLevel": "READ_ONLY",
        }
        resp = requests.post(
            f"{AUTH_URL}/security/field-security",
            json=body,
            headers={**_headers(admin["token"]), "Content-Type": "application/json"},
            timeout=15,
        )
        assert resp.status_code in (200, 201), f"Create field rule failed: {resp.text}"
        data = resp.json()["data"]
        assert data["entityType"] == "Opportunity"
        assert data["accessLevel"] == "READ_ONLY"

    def test_admin_can_list_users(self, admin):
        resp = requests.get(
            f"{AUTH_URL}/security/users",
            headers=_headers(admin["token"]),
            timeout=15,
        )
        assert resp.status_code == 200
        users = resp.json()["data"]
        assert len(users) > 0


# ── Test Group 4: Unauthenticated Access ─────────────────────────────────────

class TestUnauthenticated:
    """Requests without token should be rejected."""

    def test_no_token_returns_401(self):
        resp = requests.get(OPP_URL, timeout=15)
        assert resp.status_code == 401

    def test_invalid_token_returns_401(self):
        resp = requests.get(
            OPP_URL,
            headers={"Authorization": "Bearer invalid.token.here"},
            timeout=15,
        )
        assert resp.status_code == 401
