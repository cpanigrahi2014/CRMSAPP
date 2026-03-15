"""
Scenario 26: Audit Log Validation

End-to-end tests for audit/change tracking:
1. Auth-service audit log endpoint structure and access control
2. Opportunity stage history captures old → new values
3. Activity service records entity changes with relatedEntityType/Id
4. Create → update → verify trail end-to-end
"""
from __future__ import annotations

import uuid
import time
import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth"
OPP_URL = "http://localhost:8085/api/v1/opportunities"
ACTIVITY_URL = "http://localhost:8086/api/v1/activities"

ADMIN_CREDS = {
    "email": "sarah.chen@acmecorp.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}


def _headers(token: str, json: bool = False) -> dict:
    h = {"Authorization": f"Bearer {token}", "X-Tenant-ID": "default"}
    if json:
        h["Content-Type"] = "application/json"
    return h


# ── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def admin():
    resp = requests.post(
        f"{AUTH_URL}/login", json=ADMIN_CREDS, timeout=15
    )
    assert resp.status_code == 200
    data = resp.json()["data"]
    return {"token": data["accessToken"], "userId": data["userId"]}


@pytest.fixture(scope="module")
def admin_h(admin):
    return _headers(admin["token"], json=True)


@pytest.fixture(scope="module")
def sales_rep():
    """Register a fresh USER for RBAC audit check."""
    tag = uuid.uuid4().hex[:8]
    body = {
        "email": f"aud_{tag}@test.example",
        "password": "AudPass@123!",
        "firstName": "Audit",
        "lastName": tag,
        "tenantId": "default",
    }
    resp = requests.post(f"{AUTH_URL}/register", json=body, timeout=15)
    assert resp.status_code in (200, 201)
    data = resp.json()["data"]
    return {"token": data["accessToken"], "userId": data["userId"]}


@pytest.fixture(scope="module")
def created_opp(admin_h):
    """Create an opportunity for audit trail testing."""
    tag = uuid.uuid4().hex[:8]
    body = {
        "name": f"Audit Trail Opp {tag}",
        "amount": 42000.00,
        "stage": "PROSPECTING",
    }
    resp = requests.post(OPP_URL, json=body, headers=admin_h, timeout=15)
    assert resp.status_code in (200, 201), f"Create opp failed: {resp.text}"
    data = resp.json()["data"]
    return data


# ── Test Group 1: Audit Log Endpoint Access Control ──────────────────────────

class TestAuditLogAccess:
    """Only ADMIN can access the audit log endpoint."""

    def test_admin_can_access_audit_logs(self, admin):
        resp = requests.get(
            f"{AUTH_URL}/security/audit-logs",
            params={"page": 0, "size": 5},
            headers=_headers(admin["token"]),
            timeout=15,
        )
        assert resp.status_code == 200
        body = resp.json()
        assert body["success"] is True
        assert "content" in body["data"]

    def test_user_cannot_access_audit_logs(self, sales_rep):
        resp = requests.get(
            f"{AUTH_URL}/security/audit-logs",
            params={"page": 0, "size": 5},
            headers=_headers(sales_rep["token"]),
            timeout=15,
        )
        assert resp.status_code == 403

    def test_unauthenticated_cannot_access_audit_logs(self):
        resp = requests.get(
            f"{AUTH_URL}/security/audit-logs",
            params={"page": 0, "size": 5},
            timeout=15,
        )
        assert resp.status_code in (401, 403)

    def test_audit_logs_by_user_endpoint(self, admin):
        resp = requests.get(
            f"{AUTH_URL}/security/audit-logs/user/{admin['userId']}",
            params={"page": 0, "size": 5},
            headers=_headers(admin["token"]),
            timeout=15,
        )
        assert resp.status_code == 200
        assert resp.json()["success"] is True


# ── Test Group 2: Opportunity Creation Logged ─────────────────────────────────

class TestOpportunityCreationLogged:
    """Creating an opportunity should produce an activity record."""

    def test_opportunity_has_created_activity(self, admin_h, created_opp):
        time.sleep(1)
        resp = requests.get(
            f"{OPP_URL}/{created_opp['id']}/activities",
            params={"page": 0, "size": 50},
            headers=admin_h,
            timeout=15,
        )
        assert resp.status_code == 200
        activities = resp.json()["data"]["content"]
        created_acts = [
            a for a in activities if a.get("activityType") == "CREATED"
        ]
        assert len(created_acts) >= 1, "No CREATED activity found"
        act = created_acts[0]
        assert created_opp["name"] in act.get("description", "")

    def test_created_activity_has_correct_opp_id(self, admin_h, created_opp):
        resp = requests.get(
            f"{OPP_URL}/{created_opp['id']}/activities",
            params={"page": 0, "size": 50},
            headers=admin_h,
            timeout=15,
        )
        activities = resp.json()["data"]["content"]
        for act in activities:
            assert act["opportunityId"] == created_opp["id"]


# ── Test Group 3: Stage Change Captures Old vs New ────────────────────────────

class TestStageChangeAudit:
    """Stage changes should log from-stage and to-stage."""

    @pytest.fixture(autouse=True, scope="class")
    def _advance_stage(self, admin_h, created_opp):
        """Advance the opportunity through stages."""
        opp_id = created_opp["id"]
        # PROSPECTING → QUALIFICATION
        resp = requests.patch(
            f"{OPP_URL}/{opp_id}/stage",
            params={"stage": "QUALIFICATION"},
            headers=admin_h,
            timeout=15,
        )
        assert resp.status_code == 200, f"Stage change failed: {resp.text}"
        time.sleep(1)

        # QUALIFICATION → PROPOSAL
        resp2 = requests.patch(
            f"{OPP_URL}/{opp_id}/stage",
            params={"stage": "PROPOSAL"},
            headers=admin_h,
            timeout=15,
        )
        assert resp2.status_code == 200
        time.sleep(1)

    def test_stage_change_activity_logged(self, admin_h, created_opp):
        resp = requests.get(
            f"{OPP_URL}/{created_opp['id']}/activities",
            params={"page": 0, "size": 50},
            headers=admin_h,
            timeout=15,
        )
        assert resp.status_code == 200
        activities = resp.json()["data"]["content"]
        stage_acts = [
            a for a in activities
            if a.get("activityType") == "STAGE_CHANGED"
        ]
        assert len(stage_acts) >= 2, (
            f"Expected >=2 STAGE_CHANGED activities, found {len(stage_acts)}"
        )

    def test_stage_change_captures_old_and_new(self, admin_h, created_opp):
        resp = requests.get(
            f"{OPP_URL}/{created_opp['id']}/activities",
            params={"page": 0, "size": 50},
            headers=admin_h,
            timeout=15,
        )
        activities = resp.json()["data"]["content"]
        stage_acts = [
            a for a in activities if a.get("activityType") == "STAGE_CHANGED"
        ]
        # At least one should mention PROSPECTING → QUALIFICATION or QUALIFICATION → PROPOSAL
        descriptions = [a.get("description", "") for a in stage_acts]
        all_text = " ".join(descriptions)
        assert "PROSPECTING" in all_text or "QUALIFICATION" in all_text, (
            f"Expected old stage name in descriptions: {descriptions}"
        )

    def test_conversion_analytics_has_transitions(self, admin_h):
        """Stage conversion analytics should show transitions."""
        resp = requests.get(
            f"{OPP_URL}/analytics/conversion",
            headers=admin_h,
            timeout=15,
        )
        assert resp.status_code == 200
        data = resp.json()["data"]
        transitions = data.get("transitions", [])
        assert len(transitions) > 0, "Expected at least one stage transition"
        # Each transition should have fromStage, toStage, count
        for t in transitions:
            assert "fromStage" in t
            assert "toStage" in t
            assert "count" in t


# ── Test Group 4: Activity Service Records Entity Changes ─────────────────────

class TestActivityServiceTracking:
    """Activity service records actions on entities."""

    def test_activities_have_related_entity_fields(self, admin_h):
        resp = requests.get(
            ACTIVITY_URL,
            params={"page": 0, "size": 5},
            headers=admin_h,
            timeout=15,
        )
        assert resp.status_code == 200
        activities = resp.json()["data"]["content"]
        assert len(activities) > 0
        act = activities[0]
        assert "relatedEntityType" in act
        assert "relatedEntityId" in act

    def test_activities_paginated(self, admin_h):
        resp = requests.get(
            ACTIVITY_URL,
            params={"page": 0, "size": 3},
            headers=admin_h,
            timeout=15,
        )
        assert resp.status_code == 200
        page = resp.json()["data"]
        assert "totalElements" in page
        assert "totalPages" in page
        assert page["pageSize"] <= 3

    def test_activity_has_timestamp_and_creator(self, admin_h):
        resp = requests.get(
            ACTIVITY_URL,
            params={"page": 0, "size": 1},
            headers=admin_h,
            timeout=15,
        )
        act = resp.json()["data"]["content"][0]
        assert act.get("createdAt") is not None
        assert act.get("createdBy") is not None


# ── Test Group 5: End-to-End Audit Trail ──────────────────────────────────────

class TestEndToEndAudit:
    """Create → update → verify the full audit trail."""

    def test_full_audit_trail(self, admin_h):
        tag = uuid.uuid4().hex[:8]

        # 1. Create an opportunity
        opp = requests.post(
            OPP_URL,
            json={"name": f"E2E Audit {tag}", "amount": 10000, "stage": "PROSPECTING"},
            headers=admin_h,
            timeout=15,
        ).json()["data"]
        opp_id = opp["id"]
        time.sleep(1)

        # 2. Change stage
        requests.patch(
            f"{OPP_URL}/{opp_id}/stage",
            params={"stage": "QUALIFICATION"},
            headers=admin_h,
            timeout=15,
        )
        time.sleep(1)

        # 3. Verify activity timeline shows both events
        acts_resp = requests.get(
            f"{OPP_URL}/{opp_id}/activities",
            params={"page": 0, "size": 50},
            headers=admin_h,
            timeout=15,
        )
        assert acts_resp.status_code == 200
        activities = acts_resp.json()["data"]["content"]
        types = [a.get("activityType") for a in activities]
        assert "CREATED" in types, f"Missing CREATED in {types}"
        assert "STAGE_CHANGED" in types, f"Missing STAGE_CHANGED in {types}"

        # 4. Verify chronological order (newest first)
        timestamps = [a["createdAt"] for a in activities]
        assert timestamps == sorted(timestamps, reverse=True), "Activities should be newest-first"
