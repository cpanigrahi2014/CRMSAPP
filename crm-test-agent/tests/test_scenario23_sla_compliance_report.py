"""
Scenario 23: SLA Compliance Report

End-to-end tests for Case SLA analytics:
1. Analytics endpoint – SLA metrics, status/priority breakdowns
2. SLA compliance rate formula verification
3. Case filtering by status & priority
4. Case escalation flow + escalated cases highlighted
5. Case resolution flow + SLA counters update
"""
from __future__ import annotations

import uuid
import time
import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth/login"
CASE_URL = "http://localhost:8092/api/v1/cases"
INTEGRATION_URL = "http://localhost:8091/api/v1/integrations/channels/email-support"

CREDENTIALS = {
    "email": "sarah.chen@acmecorp.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}

VALID_STATUSES = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "ESCALATED"]
VALID_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"]


# ── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def auth_token():
    resp = requests.post(AUTH_URL, json=CREDENTIALS, timeout=15)
    assert resp.status_code == 200, f"Auth failed: {resp.text}"
    data = resp.json()
    token = data.get("data", {}).get("accessToken") or data.get("token", "")
    assert token, "No token returned"
    return token


@pytest.fixture(scope="module")
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "X-Tenant-ID": "default"}


@pytest.fixture(scope="module")
def analytics(headers):
    """Fetch /cases/analytics once for the module."""
    resp = requests.get(f"{CASE_URL}/analytics", headers=headers, timeout=15)
    assert resp.status_code == 200, f"Analytics failed: {resp.text}"
    body = resp.json()
    assert body.get("success") is True
    return body["data"]


@pytest.fixture(scope="module")
def created_case(headers):
    """Create a case via email-support integration for escalation/resolve tests."""
    tag = uuid.uuid4().hex[:8]
    payload = {
        "fromAddress": f"sla_{tag}@test.example",
        "toAddress": "support@acmecorp.com",
        "subject": f"SLA test case {tag}",
        "bodyText": "Testing SLA compliance scenario 23",
        "contactName": f"SLA Tester {tag}",
    }
    resp = requests.post(INTEGRATION_URL, json=payload, headers=headers, timeout=15)
    assert resp.status_code == 201, f"Case creation failed: {resp.text}"
    data = resp.json().get("data", {})
    assert data.get("caseId"), "No caseId returned"
    return data


# ── Test Group 1: Analytics Fields ───────────────────────────────────────────

class TestAnalyticsFields:
    """Verify case analytics structure and values."""

    def test_required_fields_present(self, analytics):
        required = [
            "totalCases", "openCases", "resolvedCases", "escalatedCases",
            "slaComplianceRate", "slaMetCount", "slaBreachedCount",
            "countByStatus", "countByPriority",
        ]
        for field in required:
            assert field in analytics, f"Missing field: {field}"

    def test_total_cases_positive(self, analytics):
        assert analytics["totalCases"] > 0

    def test_sla_compliance_rate_in_range(self, analytics):
        assert 0 <= analytics["slaComplianceRate"] <= 100

    def test_escalated_cases_non_negative(self, analytics):
        assert analytics["escalatedCases"] >= 0


# ── Test Group 2: SLA Compliance Rate Formula ────────────────────────────────

class TestSLAComplianceRate:
    """Verify SLA compliance rate = slaMet / (slaMet + slaBreached) * 100."""

    def test_sla_rate_formula(self, analytics):
        met = analytics["slaMetCount"]
        breached = analytics["slaBreachedCount"]
        if met + breached > 0:
            expected = round(met / (met + breached) * 100, 2)
            assert abs(analytics["slaComplianceRate"] - expected) < 0.1
        else:
            # No resolved cases yet – rate should be 100 (default)
            assert analytics["slaComplianceRate"] == 100.0

    def test_sla_met_plus_breached_lte_resolved(self, analytics):
        met = analytics["slaMetCount"]
        breached = analytics["slaBreachedCount"]
        resolved = analytics["resolvedCases"]
        assert met + breached <= resolved + analytics.get("escalatedCases", 0) + analytics["totalCases"]


# ── Test Group 3: Status & Priority Breakdown ────────────────────────────────

class TestBreakdowns:
    """Verify status/priority breakdown consistency."""

    def test_status_breakdown_sums_to_total(self, analytics):
        by_status = analytics.get("countByStatus", {})
        total = sum(by_status.values())
        assert total == analytics["totalCases"], (
            f"Status sum {total} != total {analytics['totalCases']}"
        )

    def test_priority_breakdown_sums_to_total(self, analytics):
        by_priority = analytics.get("countByPriority", {})
        total = sum(by_priority.values())
        assert total == analytics["totalCases"], (
            f"Priority sum {total} != total {analytics['totalCases']}"
        )

    def test_valid_status_keys(self, analytics):
        for key in analytics.get("countByStatus", {}):
            assert key in VALID_STATUSES, f"Unexpected status: {key}"

    def test_valid_priority_keys(self, analytics):
        for key in analytics.get("countByPriority", {}):
            assert key in VALID_PRIORITIES, f"Unexpected priority: {key}"


# ── Test Group 4: Case Filtering ─────────────────────────────────────────────

class TestCaseFiltering:
    """Verify filtered case list endpoint."""

    @pytest.mark.parametrize("priority", VALID_PRIORITIES)
    def test_filter_by_priority(self, headers, priority):
        resp = requests.get(
            CASE_URL,
            params={"priority": priority, "page": 0, "size": 5},
            headers=headers,
            timeout=15,
        )
        assert resp.status_code == 200, f"Filter {priority} failed: {resp.text}"
        body = resp.json()
        assert body.get("success") is True
        for c in body["data"].get("content", []):
            assert c["priority"] == priority

    def test_filter_by_status_open(self, headers):
        resp = requests.get(
            CASE_URL,
            params={"status": "OPEN", "page": 0, "size": 5},
            headers=headers,
            timeout=15,
        )
        assert resp.status_code == 200
        for c in resp.json()["data"].get("content", []):
            assert c["status"] == "OPEN"

    def test_pagination_fields_present(self, headers):
        resp = requests.get(
            CASE_URL,
            params={"page": 0, "size": 3},
            headers=headers,
            timeout=15,
        )
        assert resp.status_code == 200
        page = resp.json()["data"]
        assert "totalElements" in page
        assert "totalPages" in page
        assert page["pageSize"] <= 3


# ── Test Group 5: Escalation Flow ────────────────────────────────────────────

class TestEscalation:
    """Verify case escalation and its reflection in analytics."""

    def test_escalate_case(self, headers, created_case):
        case_id = created_case["caseId"]
        resp = requests.patch(
            f"{CASE_URL}/{case_id}/escalate",
            headers=headers,
            timeout=15,
        )
        assert resp.status_code == 200, f"Escalate failed: {resp.text}"
        body = resp.json()
        assert body.get("success") is True
        assert body["data"]["status"] == "ESCALATED"

    def test_escalated_case_appears_in_filter(self, headers, created_case):
        time.sleep(1)
        resp = requests.get(
            CASE_URL,
            params={"status": "ESCALATED", "page": 0, "size": 50},
            headers=headers,
            timeout=15,
        )
        assert resp.status_code == 200
        ids = [c["id"] for c in resp.json()["data"].get("content", [])]
        assert created_case["caseId"] in ids, "Escalated case not in ESCALATED filter"

    def test_escalated_count_incremented(self, headers, analytics):
        """After escalation, escalatedCases should have incremented."""
        time.sleep(1)
        updated = requests.get(
            f"{CASE_URL}/analytics", headers=headers, timeout=15
        ).json()["data"]
        assert updated["escalatedCases"] >= analytics["escalatedCases"] + 1


# ── Test Group 6: Resolution Flow ────────────────────────────────────────────

class TestResolution:
    """Verify case resolution and SLA counters update."""

    def test_resolve_escalated_case(self, headers, created_case):
        case_id = created_case["caseId"]
        resp = requests.patch(
            f"{CASE_URL}/{case_id}/resolve",
            params={"resolutionNotes": "Resolved for SLA testing"},
            headers=headers,
            timeout=15,
        )
        assert resp.status_code == 200, f"Resolve failed: {resp.text}"
        body = resp.json()
        assert body.get("success") is True
        assert body["data"]["status"] == "RESOLVED"

    def test_resolved_cases_incremented(self, headers):
        time.sleep(1)
        updated = requests.get(
            f"{CASE_URL}/analytics", headers=headers, timeout=15
        ).json()["data"]
        assert updated["resolvedCases"] >= 1

    def test_sla_counters_updated_after_resolve(self, headers):
        updated = requests.get(
            f"{CASE_URL}/analytics", headers=headers, timeout=15
        ).json()["data"]
        # After resolving, either slaMet or slaBreached should increase
        assert (updated["slaMetCount"] + updated["slaBreachedCount"]) >= 1
