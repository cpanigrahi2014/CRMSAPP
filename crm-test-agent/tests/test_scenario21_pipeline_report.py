"""
Scenario 21: Pipeline Report – Opportunities across stages

End-to-end tests for the Pipeline Report & Analytics:
1. Dashboard summary (KPIs, stage breakdown, revenue sources)
2. Revenue analytics (by stage, by source, weighted pipeline)
3. Pipeline view grouped by stage
4. Stage filter / pagination
5. Totals cross-verified against pipeline view counts
"""
from __future__ import annotations

import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth/login"
OPP_URL = "http://localhost:8085/api/v1/opportunities"

CREDENTIALS = {
    "email": "sarah.chen@acmecorp.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}

VALID_STAGES = [
    "PROSPECTING", "QUALIFICATION", "NEEDS_ANALYSIS",
    "PROPOSAL", "NEGOTIATION", "CLOSED_WON", "CLOSED_LOST",
]

STAGE_PROBABILITIES = {
    "PROSPECTING": 10,
    "QUALIFICATION": 25,
    "NEEDS_ANALYSIS": 40,
    "PROPOSAL": 60,
    "NEGOTIATION": 80,
    "CLOSED_WON": 100,
    "CLOSED_LOST": 0,
}

FORECAST_CATEGORIES = {
    "PROSPECTING": "PIPELINE",
    "QUALIFICATION": "PIPELINE",
    "NEEDS_ANALYSIS": "BEST_CASE",
    "PROPOSAL": "BEST_CASE",
    "NEGOTIATION": "COMMIT",
    "CLOSED_WON": "CLOSED",
    "CLOSED_LOST": "OMITTED",
}


# ── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def auth_token():
    """Obtain a JWT token for API calls."""
    resp = requests.post(AUTH_URL, json=CREDENTIALS, timeout=15)
    assert resp.status_code == 200, f"Auth failed: {resp.text}"
    data = resp.json()
    token = data.get("data", {}).get("accessToken") or data.get("token", "")
    assert token, "No token returned from auth"
    return token


@pytest.fixture(scope="module")
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "X-Tenant-ID": "default"}


@pytest.fixture(scope="module")
def dashboard(headers):
    """Fetch /opportunities/dashboard once for the test module."""
    resp = requests.get(f"{OPP_URL}/dashboard", headers=headers, timeout=15)
    assert resp.status_code == 200, f"Dashboard request failed: {resp.text}"
    body = resp.json()
    assert body.get("success") is True
    return body["data"]


@pytest.fixture(scope="module")
def revenue_analytics(headers):
    """Fetch /opportunities/analytics/revenue once."""
    resp = requests.get(
        f"{OPP_URL}/analytics/revenue", headers=headers, timeout=15
    )
    assert resp.status_code == 200, f"Revenue analytics failed: {resp.text}"
    body = resp.json()
    assert body.get("success") is True
    return body["data"]


@pytest.fixture(scope="module")
def pipeline_view(headers):
    """Fetch /opportunities/pipeline – map of stage -> list."""
    resp = requests.get(f"{OPP_URL}/pipeline", headers=headers, timeout=15)
    assert resp.status_code == 200, f"Pipeline view failed: {resp.text}"
    body = resp.json()
    assert body.get("success") is True
    return body["data"]


# ── Test Group 1: Dashboard KPIs ─────────────────────────────────────────────

class TestDashboardKPIs:
    """Verify top-level dashboard metrics are present and sane."""

    def test_dashboard_has_required_kpi_fields(self, dashboard):
        required = [
            "totalPipelineValue", "totalOpenDeals", "totalClosedWon",
            "totalClosedLost", "totalRevenue", "avgDealSize", "winRate",
            "weightedPipeline",
        ]
        for field in required:
            assert field in dashboard, f"Missing dashboard field: {field}"

    def test_total_open_deals_positive(self, dashboard):
        assert dashboard["totalOpenDeals"] >= 0

    def test_win_rate_in_valid_range(self, dashboard):
        assert 0 <= dashboard["winRate"] <= 100

    def test_total_revenue_equals_closed_won_sum(self, dashboard):
        """totalRevenue should equal the totalAmount of CLOSED_WON stage."""
        won_stage = next(
            (s for s in dashboard["stageBreakdown"] if s["stage"] == "CLOSED_WON"),
            None,
        )
        if won_stage:
            assert abs(dashboard["totalRevenue"] - won_stage["totalAmount"]) < 0.01

    def test_stage_breakdown_covers_all_stages(self, dashboard):
        stages_present = {s["stage"] for s in dashboard["stageBreakdown"]}
        for stage in VALID_STAGES:
            assert stage in stages_present, f"Stage {stage} missing from breakdown"


# ── Test Group 2: Revenue Analytics ──────────────────────────────────────────

class TestRevenueAnalytics:
    """Verify revenue analytics breakdown and mathematical consistency."""

    def test_revenue_analytics_has_required_fields(self, revenue_analytics):
        required = [
            "totalOpportunities", "openOpportunities", "closedWon",
            "closedLost", "totalRevenue", "avgDealSize", "winRate",
            "revenueByStage", "countByStage",
        ]
        for field in required:
            assert field in revenue_analytics, f"Missing field: {field}"

    def test_win_rate_calculation(self, revenue_analytics):
        won = revenue_analytics["closedWon"]
        lost = revenue_analytics["closedLost"]
        if won + lost > 0:
            expected = round(won / (won + lost) * 100, 2)
            assert abs(revenue_analytics["winRate"] - expected) < 0.1

    def test_stage_counts_sum_to_total(self, revenue_analytics):
        count_by_stage = revenue_analytics.get("countByStage", {})
        if count_by_stage:
            total_from_stages = sum(count_by_stage.values())
            assert total_from_stages == revenue_analytics["totalOpportunities"]

    def test_revenue_by_stage_present(self, revenue_analytics):
        rbs = revenue_analytics.get("revenueByStage", {})
        assert isinstance(rbs, dict)
        assert len(rbs) > 0, "revenueByStage should not be empty"


# ── Test Group 3: Pipeline View ──────────────────────────────────────────────

class TestPipelineView:
    """Verify stage-grouped pipeline view."""

    def test_pipeline_returns_map_of_stages(self, pipeline_view):
        assert isinstance(pipeline_view, dict)
        for stage in pipeline_view:
            assert stage in VALID_STAGES, f"Unexpected stage: {stage}"

    def test_pipeline_counts_match_dashboard(self, pipeline_view, dashboard):
        """Pipeline view counts should match dashboard stage breakdown."""
        dashboard_counts = {
            s["stage"]: s["count"] for s in dashboard["stageBreakdown"]
        }
        for stage, opps in pipeline_view.items():
            expected = dashboard_counts.get(stage, 0)
            actual = len(opps)
            assert actual == expected, (
                f"Stage {stage}: pipeline has {actual}, dashboard says {expected}"
            )


# ── Test Group 4: Stage Filter ───────────────────────────────────────────────

class TestStageFilter:
    """Verify per-stage endpoint returns paginated results."""

    @pytest.mark.parametrize("stage", VALID_STAGES)
    def test_stage_filter_returns_correct_stage(self, headers, stage):
        resp = requests.get(
            f"{OPP_URL}/stage/{stage}",
            params={"page": 0, "size": 5},
            headers=headers,
            timeout=15,
        )
        assert resp.status_code == 200, f"Stage filter {stage} failed: {resp.text}"
        body = resp.json()
        assert body.get("success") is True
        content = body["data"].get("content", [])
        for opp in content:
            assert opp["stage"] == stage, f"Got stage {opp['stage']} expected {stage}"

    def test_stage_filter_pagination_fields(self, headers):
        resp = requests.get(
            f"{OPP_URL}/stage/PROSPECTING",
            params={"page": 0, "size": 3},
            headers=headers,
            timeout=15,
        )
        assert resp.status_code == 200
        page = resp.json()["data"]
        assert "pageNumber" in page
        assert "totalElements" in page
        assert "totalPages" in page
        assert page["pageSize"] <= 3


# ── Test Group 5: Forecast & Alerts ──────────────────────────────────────────

class TestForecastAndAlerts:
    """Verify forecast categories and deal alerts in dashboard."""

    def test_forecast_categories_present(self, dashboard):
        for key in ["forecastPipeline", "forecastBestCase", "forecastCommit", "forecastClosed"]:
            assert key in dashboard, f"Missing forecast field: {key}"

    def test_alert_counts_non_negative(self, dashboard):
        for key in ["overdueDeals", "closingSoonDeals", "staleDeals", "activeReminders"]:
            assert dashboard.get(key, 0) >= 0, f"{key} should be >= 0"

    def test_revenue_by_lead_source(self, dashboard):
        rbs = dashboard.get("revenueByLeadSource", {})
        assert isinstance(rbs, dict)
        assert len(rbs) > 0, "revenueByLeadSource should not be empty"
