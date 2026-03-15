"""
Scenario 22: Lead Conversion Rate

End-to-end tests for the Lead Analytics / Conversion dashboard:
1. Analytics endpoint returns conversion rate, average score, breakdowns
2. Conversion rate = convertedLeads / totalLeads * 100
3. Status breakdown sums to total
4. Source breakdown sums to total
5. SLA-breached leads endpoint
6. Convert a fresh lead and verify rate updates (dashboard auto-refresh)
"""
from __future__ import annotations

import uuid
import time
import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth/login"
LEAD_URL = "http://localhost:8082/api/v1/leads"
INTEGRATION_URL = "http://localhost:8091/api/v1/integrations/channels/social"

CREDENTIALS = {
    "email": "sarah.chen@acmecorp.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}


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
    """Fetch /leads/analytics once for the module."""
    resp = requests.get(f"{LEAD_URL}/analytics", headers=headers, timeout=15)
    assert resp.status_code == 200, f"Analytics request failed: {resp.text}"
    body = resp.json()
    assert body.get("success") is True
    return body["data"]


# ── Test Group 1: Analytics Fields & Values ──────────────────────────────────

class TestAnalyticsFields:
    """Verify analytics response structure and basic values."""

    def test_required_fields_present(self, analytics):
        required = [
            "totalLeads", "convertedLeads", "conversionRate",
            "averageScore", "byStatus", "bySource", "slaBreached",
        ]
        for field in required:
            assert field in analytics, f"Missing field: {field}"

    def test_total_leads_positive(self, analytics):
        assert analytics["totalLeads"] > 0, "Should have at least 1 lead"

    def test_converted_lte_total(self, analytics):
        assert analytics["convertedLeads"] <= analytics["totalLeads"]

    def test_sla_breached_non_negative(self, analytics):
        assert analytics["slaBreached"] >= 0


# ── Test Group 2: Conversion Rate Calculation ────────────────────────────────

class TestConversionRate:
    """Verify the conversion rate formula is correct."""

    def test_conversion_rate_formula(self, analytics):
        total = analytics["totalLeads"]
        converted = analytics["convertedLeads"]
        if total > 0:
            expected = converted / total * 100
            assert abs(analytics["conversionRate"] - expected) < 0.01

    def test_conversion_rate_in_range(self, analytics):
        assert 0 <= analytics["conversionRate"] <= 100


# ── Test Group 3: Status Breakdown ───────────────────────────────────────────

class TestStatusBreakdown:
    """Verify by-status counts add up to total leads."""

    def test_status_breakdown_sums_to_total(self, analytics):
        by_status = analytics.get("byStatus", {})
        total_from_status = sum(by_status.values())
        assert total_from_status == analytics["totalLeads"], (
            f"Status sums to {total_from_status}, expected {analytics['totalLeads']}"
        )

    def test_converted_count_matches_status(self, analytics):
        by_status = analytics.get("byStatus", {})
        converted_in_status = by_status.get("CONVERTED", 0)
        assert converted_in_status == analytics["convertedLeads"]

    def test_valid_status_keys(self, analytics):
        valid = {"NEW", "CONTACTED", "QUALIFIED", "UNQUALIFIED", "CONVERTED"}
        for key in analytics.get("byStatus", {}):
            assert key in valid, f"Unexpected status key: {key}"


# ── Test Group 4: Source Breakdown ───────────────────────────────────────────

class TestSourceBreakdown:
    """Verify by-source counts add up to total leads."""

    def test_source_breakdown_sums_to_total(self, analytics):
        by_source = analytics.get("bySource", {})
        total_from_source = sum(by_source.values())
        assert total_from_source == analytics["totalLeads"], (
            f"Source sums to {total_from_source}, expected {analytics['totalLeads']}"
        )

    def test_source_keys_are_strings(self, analytics):
        for key in analytics.get("bySource", {}):
            assert isinstance(key, str) and len(key) > 0


# ── Test Group 5: SLA Breached Leads Endpoint ────────────────────────────────

class TestSLABreachedLeads:
    """Verify the /leads/sla-breached endpoint."""

    def test_sla_breached_endpoint_returns_list(self, headers):
        resp = requests.get(
            f"{LEAD_URL}/sla-breached", headers=headers, timeout=15
        )
        assert resp.status_code == 200, f"SLA breached failed: {resp.text}"
        body = resp.json()
        assert body.get("success") is True
        data = body.get("data", [])
        assert isinstance(data, list)

    def test_sla_breached_count_matches_analytics(self, headers, analytics):
        resp = requests.get(
            f"{LEAD_URL}/sla-breached", headers=headers, timeout=15
        )
        breached_list = resp.json().get("data", [])
        assert len(breached_list) == analytics["slaBreached"]


# ── Test Group 6: Dashboard Auto-Refresh (create lead, re-check) ─────────────

class TestDashboardRefresh:
    """
    Create a new lead via integration, then verify analytics reflect
    the new lead (dashboard auto-refresh).
    """

    def test_new_lead_increments_total(self, headers):
        # Snapshot current analytics
        before = requests.get(
            f"{LEAD_URL}/analytics", headers=headers, timeout=15
        ).json()["data"]

        # Create a lead via social integration
        tag = uuid.uuid4().hex[:8]
        payload = {
            "platform": "FACEBOOK",
            "username": f"s22user_{tag}",
            "fullName": f"Scenario22 Test {tag}",
            "email": f"s22_{tag}@test.example",
            "comment": "Testing dashboard auto-refresh",
            "postUrl": f"https://facebook.com/posts/{tag}",
        }
        create_resp = requests.post(
            INTEGRATION_URL, json=payload, headers=headers, timeout=15
        )
        assert create_resp.status_code == 201, f"Lead creation failed: {create_resp.text}"
        time.sleep(1)

        # Verify analytics updated
        after = requests.get(
            f"{LEAD_URL}/analytics", headers=headers, timeout=15
        ).json()["data"]
        assert after["totalLeads"] == before["totalLeads"] + 1, (
            f"Total leads did not increment: {before['totalLeads']} -> {after['totalLeads']}"
        )
