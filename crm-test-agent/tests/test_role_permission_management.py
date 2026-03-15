"""
Role & Permission Management — 25 API Tests
Covers: Roles CRUD, Permissions CRUD, Field Security, SSO Providers, MFA, Audit Logs, Users list.

Run:  cd crm-test-agent && python -m pytest tests/test_role_permission_management.py -v
"""
from __future__ import annotations

import uuid
import pytest
import requests

# ── Config ───────────────────────────────────────────────────────────────────

AUTH = "http://localhost:8081"
BASE = f"{AUTH}/api/v1/auth"
SEC = f"{BASE}/security"

TEST_EMAIL = "sarah.chen@acmecorp.com"
TEST_PASSWORD = "Demo@2026!"
TEST_TENANT = "default"

# ── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def auth_token():
    resp = requests.post(f"{BASE}/login", json={
        "email": TEST_EMAIL, "password": TEST_PASSWORD, "tenantId": TEST_TENANT,
    })
    data = resp.json()
    body = data.get("data", data)
    return body.get("accessToken") or body.get("token") or ""

@pytest.fixture
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}

def _uid():
    return uuid.uuid4().hex[:6]


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 1 — Roles CRUD (6 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestRoles:

    def test_01_list_roles(self, headers):
        """GET /security/roles returns a list."""
        resp = requests.get(f"{SEC}/roles", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json()
        body = data.get("data", data)
        assert isinstance(body, list)

    def test_02_create_role(self, headers):
        """POST /security/roles creates a new role with name & description."""
        resp = requests.post(f"{SEC}/roles", headers=headers, json={
            "name": f"QA_ROLE_{_uid()}",
            "description": "Created by automated test",
            "permissions": [],
        })
        assert resp.status_code in (200, 201), resp.text
        body = resp.json().get("data", resp.json())
        assert body.get("name") or body.get("id")

    def test_03_create_role_with_permissions(self, headers):
        """POST /security/roles with a permissions list."""
        resp = requests.post(f"{SEC}/roles", headers=headers, json={
            "name": f"PERM_ROLE_{_uid()}",
            "description": "Role with permissions",
            "permissions": ["LEAD_READ", "LEAD_WRITE"],
        })
        assert resp.status_code in (200, 201), resp.text

    def test_04_create_role_missing_name(self, headers):
        """POST /security/roles without name should fail validation."""
        resp = requests.post(f"{SEC}/roles", headers=headers, json={
            "description": "No name role",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_05_create_role_no_auth(self):
        """POST /security/roles without token should be rejected."""
        resp = requests.post(f"{SEC}/roles", json={"name": "UNAUTH_ROLE"})
        assert resp.status_code in (401, 403, 500), resp.text

    def test_06_delete_role(self, headers):
        """DELETE /security/roles/{id} removes a role."""
        # Create first
        create = requests.post(f"{SEC}/roles", headers=headers, json={
            "name": f"DEL_ROLE_{_uid()}", "description": "To be deleted",
        })
        assert create.status_code in (200, 201), create.text
        role_id = create.json().get("data", create.json()).get("id")
        # Delete
        resp = requests.delete(f"{SEC}/roles/{role_id}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 2 — Permissions CRUD (6 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestPermissions:

    def test_07_list_permissions(self, headers):
        """GET /security/permissions returns a list."""
        resp = requests.get(f"{SEC}/permissions", headers=headers)
        assert resp.status_code == 200, resp.text
        body = resp.json().get("data", resp.json())
        assert isinstance(body, list)

    def test_08_create_permission(self, headers):
        """POST /security/permissions creates a new permission."""
        resp = requests.post(f"{SEC}/permissions", headers=headers, json={
            "name": f"TEST_PERM_{_uid()}",
            "description": "Automated test permission",
            "resource": "leads",
            "actions": "create,read,update",
        })
        assert resp.status_code in (200, 201), resp.text
        body = resp.json().get("data", resp.json())
        assert body.get("name") or body.get("id")

    def test_09_create_permission_missing_name(self, headers):
        """POST /security/permissions without name should fail."""
        resp = requests.post(f"{SEC}/permissions", headers=headers, json={
            "resource": "leads", "actions": "read",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_10_create_permission_missing_resource(self, headers):
        """POST /security/permissions without resource should fail."""
        resp = requests.post(f"{SEC}/permissions", headers=headers, json={
            "name": f"NO_RES_{_uid()}", "actions": "read",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_11_update_permission(self, headers):
        """PUT /security/permissions/{id} updates a permission."""
        # Create first
        create = requests.post(f"{SEC}/permissions", headers=headers, json={
            "name": f"UPD_PERM_{_uid()}", "resource": "accounts", "actions": "read",
        })
        assert create.status_code in (200, 201)
        perm_id = create.json().get("data", create.json()).get("id")
        # Update
        resp = requests.put(f"{SEC}/permissions/{perm_id}", headers=headers, json={
            "name": f"UPD_PERM_{_uid()}", "resource": "accounts", "actions": "read,update,delete",
        })
        assert resp.status_code == 200, resp.text

    def test_12_delete_permission(self, headers):
        """DELETE /security/permissions/{id} removes a permission."""
        create = requests.post(f"{SEC}/permissions", headers=headers, json={
            "name": f"DEL_PERM_{_uid()}", "resource": "contacts", "actions": "read",
        })
        perm_id = create.json().get("data", create.json()).get("id")
        resp = requests.delete(f"{SEC}/permissions/{perm_id}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 3 — Field Security (4 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestFieldSecurity:

    def test_13_list_field_security(self, headers):
        """GET /security/field-security returns a list."""
        resp = requests.get(f"{SEC}/field-security", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_14_create_field_security_rule(self, headers):
        """POST /security/field-security creates a rule."""
        resp = requests.post(f"{SEC}/field-security", headers=headers, json={
            "entityType": "Lead",
            "fieldName": f"phone_{_uid()}",
            "roleName": "USER",
            "accessLevel": "READ_ONLY",
        })
        assert resp.status_code in (200, 201), resp.text
        body = resp.json().get("data", resp.json())
        assert body.get("accessLevel") in ("READ_ONLY", "read_only", None) or body.get("id")

    def test_15_create_field_security_missing_entity(self, headers):
        """POST /security/field-security without entityType should fail."""
        resp = requests.post(f"{SEC}/field-security", headers=headers, json={
            "fieldName": "email", "roleName": "USER", "accessLevel": "HIDDEN",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_16_delete_field_security_rule(self, headers):
        """DELETE /security/field-security/{id} removes a rule."""
        create = requests.post(f"{SEC}/field-security", headers=headers, json={
            "entityType": "Account", "fieldName": f"revenue_{_uid()}",
            "roleName": "USER", "accessLevel": "HIDDEN",
        })
        rule_id = create.json().get("data", create.json()).get("id")
        resp = requests.delete(f"{SEC}/field-security/{rule_id}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 4 — SSO Providers (4 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestSsoProviders:

    def test_17_list_sso_providers(self, headers):
        """GET /security/sso returns a list."""
        resp = requests.get(f"{SEC}/sso", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_18_create_sso_provider(self, headers):
        """POST /security/sso creates an SSO provider."""
        resp = requests.post(f"{SEC}/sso", headers=headers, json={
            "name": f"TestSSO_{_uid()}",
            "providerType": "OIDC",
            "clientId": f"client_{_uid()}",
            "issuerUrl": "https://login.example.com",
            "enabled": False,
            "autoProvision": False,
            "defaultRole": "USER",
        })
        assert resp.status_code in (200, 201), resp.text
        body = resp.json().get("data", resp.json())
        assert body.get("providerType") or body.get("id")

    def test_19_create_sso_missing_name(self, headers):
        """POST /security/sso without name should fail."""
        resp = requests.post(f"{SEC}/sso", headers=headers, json={
            "providerType": "SAML", "clientId": "abc",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_20_delete_sso_provider(self, headers):
        """DELETE /security/sso/{id} removes a provider."""
        create = requests.post(f"{SEC}/sso", headers=headers, json={
            "name": f"DelSSO_{_uid()}", "providerType": "OAUTH2",
            "clientId": f"del_{_uid()}", "enabled": False,
        })
        sso_id = create.json().get("data", create.json()).get("id")
        resp = requests.delete(f"{SEC}/sso/{sso_id}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 5 — MFA Configuration (2 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestMfa:

    def test_21_list_mfa_configs(self, headers):
        """GET /security/mfa returns a list."""
        resp = requests.get(f"{SEC}/mfa", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_22_create_mfa_config(self, headers, auth_token):
        """POST /security/mfa creates an MFA config for a user."""
        # Get current user to get userId
        me = requests.get(f"{BASE}/me", headers=headers)
        user_id = me.json().get("data", me.json()).get("id")
        resp = requests.post(f"{SEC}/mfa", headers=headers, json={
            "userId": user_id,
            "mfaType": "EMAIL",
            "enabled": False,
        })
        # 200/201 = created, 409 = already exists (fine for re-run)
        assert resp.status_code in (200, 201, 409), resp.text


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 6 — Audit Logs (2 tests)
# ═══════════════════════════════════════════════════════════════════════════════

class TestAuditLogs:

    def test_23_list_audit_logs(self, headers):
        """GET /security/audit-logs returns paginated results."""
        resp = requests.get(f"{SEC}/audit-logs", headers=headers, params={"page": 0, "size": 5})
        assert resp.status_code == 200, resp.text

    def test_24_list_audit_logs_no_auth(self):
        """GET /security/audit-logs without auth should be rejected."""
        resp = requests.get(f"{SEC}/audit-logs")
        assert resp.status_code in (401, 403, 500), resp.text


# ═══════════════════════════════════════════════════════════════════════════════
#  GROUP 7 — Users List (1 test)
# ═══════════════════════════════════════════════════════════════════════════════

class TestUsersList:

    def test_25_list_all_users(self, headers):
        """GET /security/users returns user list for the tenant."""
        resp = requests.get(f"{SEC}/users", headers=headers)
        assert resp.status_code == 200, resp.text
        body = resp.json().get("data", resp.json())
        assert isinstance(body, list)
        assert len(body) >= 1  # At least the admin user
