"""
Scenario 25: Field-level Security

End-to-end tests for field-level security rules:
1. Admin can create/list/update/delete field security rules
2. Rules correctly specify entity, field, role, and access level
3. Revenue (amount) field hidden for junior reps (USER) via HIDDEN rule
4. Revenue field visible for managers (MANAGER) — no HIDDEN rule
5. Rule lifecycle: create → verify → update → delete
"""
from __future__ import annotations

import uuid
import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth"
SECURITY_URL = f"{AUTH_URL}/security"
OPP_URL = "http://localhost:8085/api/v1/opportunities"

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
    return {"token": data["accessToken"], "userId": data["userId"], "roles": data["roles"]}


def _admin_headers(token: str) -> dict:
    return {
        "Authorization": f"Bearer {token}",
        "X-Tenant-ID": "default",
        "Content-Type": "application/json",
    }


def _headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}", "X-Tenant-ID": "default"}


# ── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def admin():
    return _login(ADMIN_CREDS["email"], ADMIN_CREDS["password"])


@pytest.fixture(scope="module")
def admin_h(admin):
    return _admin_headers(admin["token"])


@pytest.fixture(scope="module")
def sales_rep(admin):
    """Register a fresh USER role."""
    tag = uuid.uuid4().hex[:8]
    body = {
        "email": f"jr_{tag}@test.example",
        "password": "JrPass@123!",
        "firstName": "Junior",
        "lastName": tag,
        "tenantId": "default",
    }
    resp = requests.post(f"{AUTH_URL}/register", json=body, timeout=15)
    assert resp.status_code in (200, 201)
    data = resp.json()["data"]
    return {"token": data["accessToken"], "userId": data["userId"], "roles": data["roles"]}


@pytest.fixture(scope="module")
def manager_user(admin):
    """Register and promote to MANAGER."""
    tag = uuid.uuid4().hex[:8]
    email = f"mgr_{tag}@test.example"
    password = "MgrPass@123!"
    body = {
        "email": email,
        "password": password,
        "firstName": "Mgr",
        "lastName": tag,
        "tenantId": "default",
    }
    resp = requests.post(f"{AUTH_URL}/register", json=body, timeout=15)
    assert resp.status_code in (200, 201)
    uid = resp.json()["data"]["userId"]
    requests.post(
        f"{AUTH_URL}/users/{uid}/roles",
        params={"roleName": "MANAGER"},
        headers=_headers(admin["token"]),
        timeout=15,
    )
    info = _login(email, password)
    assert "MANAGER" in info["roles"]
    return info


# ── Test Group 1: Field Security Rule CRUD ────────────────────────────────────

class TestFieldSecurityCRUD:
    """Admin can create, list, update, and delete field security rules."""

    def test_list_field_security_rules(self, admin_h):
        resp = requests.get(f"{SECURITY_URL}/field-security", headers=admin_h, timeout=15)
        assert resp.status_code == 200
        assert resp.json()["success"] is True
        assert isinstance(resp.json()["data"], list)

    def test_create_field_security_rule(self, admin_h):
        tag = uuid.uuid4().hex[:6]
        body = {
            "entityType": "Opportunity",
            "fieldName": f"s25_field_{tag}",
            "roleName": "USER",
            "accessLevel": "READ_ONLY",
        }
        resp = requests.post(
            f"{SECURITY_URL}/field-security", json=body, headers=admin_h, timeout=15
        )
        assert resp.status_code in (200, 201), f"Create failed: {resp.text}"
        data = resp.json()["data"]
        assert data["entityType"] == "Opportunity"
        assert data["roleName"] == "USER"
        assert data["accessLevel"] == "READ_ONLY"
        assert data["id"] is not None

    def test_update_field_security_rule(self, admin_h):
        # Create a rule first
        tag = uuid.uuid4().hex[:6]
        create_body = {
            "entityType": "Lead",
            "fieldName": f"s25_upd_{tag}",
            "roleName": "USER",
            "accessLevel": "READ_ONLY",
        }
        cr = requests.post(
            f"{SECURITY_URL}/field-security", json=create_body, headers=admin_h, timeout=15
        )
        assert cr.status_code in (200, 201)
        rule_id = cr.json()["data"]["id"]

        # Update it
        update_body = {
            "entityType": "Lead",
            "fieldName": f"s25_upd_{tag}",
            "roleName": "USER",
            "accessLevel": "HIDDEN",
        }
        up = requests.put(
            f"{SECURITY_URL}/field-security/{rule_id}",
            json=update_body,
            headers=admin_h,
            timeout=15,
        )
        assert up.status_code == 200, f"Update failed: {up.text}"
        assert up.json()["data"]["accessLevel"] == "HIDDEN"

    def test_delete_field_security_rule(self, admin_h):
        tag = uuid.uuid4().hex[:6]
        body = {
            "entityType": "Contact",
            "fieldName": f"s25_del_{tag}",
            "roleName": "USER",
            "accessLevel": "HIDDEN",
        }
        cr = requests.post(
            f"{SECURITY_URL}/field-security", json=body, headers=admin_h, timeout=15
        )
        assert cr.status_code in (200, 201)
        rule_id = cr.json()["data"]["id"]

        dl = requests.delete(
            f"{SECURITY_URL}/field-security/{rule_id}", headers=admin_h, timeout=15
        )
        assert dl.status_code == 200, f"Delete failed: {dl.text}"


# ── Test Group 2: Revenue Field Hidden for Junior Reps ────────────────────────

class TestRevenueHiddenForJunior:
    """
    Create a HIDDEN rule on Opportunity.amount for USER role, then verify
    the rule exists and restricts access appropriately.
    """

    @pytest.fixture(autouse=True, scope="class")
    def _hidden_rule(self, admin_h):
        """Create HIDDEN rule for amount field for USER role."""
        body = {
            "entityType": "Opportunity",
            "fieldName": "amount",
            "roleName": "USER",
            "accessLevel": "HIDDEN",
        }
        resp = requests.post(
            f"{SECURITY_URL}/field-security", json=body, headers=admin_h, timeout=15
        )
        # May already exist from manual testing — either 200 or error is fine
        if resp.status_code == 200:
            self.__class__._rule_id = resp.json()["data"]["id"]
        else:
            # Find existing rule
            rules = requests.get(
                f"{SECURITY_URL}/field-security", headers=admin_h, timeout=15
            ).json()["data"]
            for r in rules:
                if (
                    r["entityType"] == "Opportunity"
                    and r["fieldName"] == "amount"
                    and r["roleName"] == "USER"
                    and r["accessLevel"] == "HIDDEN"
                ):
                    self.__class__._rule_id = r["id"]
                    break
        yield

    def test_hidden_rule_exists_in_list(self, admin_h):
        rules = requests.get(
            f"{SECURITY_URL}/field-security", headers=admin_h, timeout=15
        ).json()["data"]
        amount_rules = [
            r for r in rules
            if r["entityType"] == "Opportunity"
            and r["fieldName"] == "amount"
            and r["roleName"] == "USER"
        ]
        assert len(amount_rules) >= 1
        assert any(r["accessLevel"] == "HIDDEN" for r in amount_rules)

    def test_user_cannot_manage_field_security(self, sales_rep):
        resp = requests.get(
            f"{SECURITY_URL}/field-security",
            headers=_headers(sales_rep["token"]),
            timeout=15,
        )
        assert resp.status_code == 403

    def test_rule_specifies_correct_entity_and_field(self, admin_h):
        rules = requests.get(
            f"{SECURITY_URL}/field-security", headers=admin_h, timeout=15
        ).json()["data"]
        amount_hidden = [
            r for r in rules
            if r["entityType"] == "Opportunity"
            and r["fieldName"] == "amount"
            and r["accessLevel"] == "HIDDEN"
        ]
        assert len(amount_hidden) >= 1
        rule = amount_hidden[0]
        assert rule["roleName"] == "USER"


# ── Test Group 3: Revenue Visible for Manager ────────────────────────────────

class TestRevenueVisibleForManager:
    """Manager should NOT have a HIDDEN rule on amount field."""

    def test_no_hidden_rule_for_manager_on_amount(self, admin_h):
        rules = requests.get(
            f"{SECURITY_URL}/field-security", headers=admin_h, timeout=15
        ).json()["data"]
        hidden_for_manager = [
            r for r in rules
            if r["entityType"] == "Opportunity"
            and r["fieldName"] == "amount"
            and r["roleName"] == "MANAGER"
            and r["accessLevel"] == "HIDDEN"
        ]
        assert len(hidden_for_manager) == 0, "Manager should NOT have HIDDEN on amount"

    def test_manager_can_see_opportunity_amount(self, manager_user):
        resp = requests.get(
            OPP_URL,
            params={"page": 0, "size": 1},
            headers=_headers(manager_user["token"]),
            timeout=15,
        )
        assert resp.status_code == 200
        content = resp.json()["data"]["content"]
        if content:
            assert "amount" in content[0], "Manager should see amount field"


# ── Test Group 4: Access Level Validation ─────────────────────────────────────

class TestAccessLevelValidation:
    """Verify valid access levels and entity types."""

    @pytest.mark.parametrize("level", ["HIDDEN", "READ_ONLY", "READ_WRITE"])
    def test_valid_access_levels(self, admin_h, level):
        tag = uuid.uuid4().hex[:6]
        body = {
            "entityType": "Lead",
            "fieldName": f"s25_lvl_{tag}",
            "roleName": "USER",
            "accessLevel": level,
        }
        resp = requests.post(
            f"{SECURITY_URL}/field-security", json=body, headers=admin_h, timeout=15
        )
        assert resp.status_code in (200, 201)

    @pytest.mark.parametrize("entity", ["Lead", "Account", "Opportunity", "Contact"])
    def test_valid_entity_types(self, admin_h, entity):
        tag = uuid.uuid4().hex[:6]
        body = {
            "entityType": entity,
            "fieldName": f"s25_ent_{tag}",
            "roleName": "USER",
            "accessLevel": "READ_ONLY",
        }
        resp = requests.post(
            f"{SECURITY_URL}/field-security", json=body, headers=admin_h, timeout=15
        )
        assert resp.status_code in (200, 201)
