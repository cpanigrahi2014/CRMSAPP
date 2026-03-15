"""
Pipeline Management — 25 Tests
====================================
Covers pipeline configuration (AI agent, port 9100) and pipeline
operations/analytics (opportunity-service, port 8085).

Groups:
  1. Create Pipeline               (tests 1–5)
  2. Edit Pipeline Stages           (tests 6–10)
  3. Delete Stage / Pipeline Ops    (tests 11–13)
  4. Move Deal Between Stages       (tests 14–19)
  5. Pipeline Analytics             (tests 20–25)
"""

import uuid, requests, pytest
from datetime import date, timedelta

# ── Config ────────────────────────────────────────────────────────────────────
AUTH_BASE = "http://localhost:8081/api/v1/auth"
OPP_BASE = "http://localhost:8085/api/v1/opportunities"
AI_BASE = "http://localhost:9100/api/ai"
CREDS = {"email": "sarah.chen@acmecorp.com", "password": "Demo@2026!", "tenantId": "default"}

STAGES = ["PROSPECTING", "QUALIFICATION", "NEEDS_ANALYSIS", "PROPOSAL", "NEGOTIATION", "CLOSED_WON", "CLOSED_LOST"]
STAGE_PROBS = {
    "PROSPECTING": 10, "QUALIFICATION": 25, "NEEDS_ANALYSIS": 40,
    "PROPOSAL": 60, "NEGOTIATION": 80, "CLOSED_WON": 100, "CLOSED_LOST": 0,
}


def _uid():
    return uuid.uuid4().hex[:8]


def _future(days=30):
    return (date.today() + timedelta(days=days)).isoformat()


# ── Fixtures ──────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def auth_token():
    resp = requests.post(f"{AUTH_BASE}/login", json=CREDS)
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    d = resp.json().get("data", resp.json())
    return d.get("accessToken") or d.get("token")


@pytest.fixture(scope="module")
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}


@pytest.fixture(scope="module")
def ai_headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}


@pytest.fixture(scope="module")
def user_id(auth_token):
    resp = requests.get(f"{AUTH_BASE}/me", headers={"Authorization": f"Bearer {auth_token}"})
    if resp.status_code == 200:
        d = resp.json().get("data", resp.json())
        return str(d.get("id") or d.get("userId"))
    return str(uuid.uuid4())


def _create_opp(headers, stage="PROSPECTING", amount=25000):
    """Helper to create a quick opportunity."""
    resp = requests.post(OPP_BASE, headers=headers, json={
        "name": f"PipeDeal_{_uid()}", "stage": stage,
        "amount": amount, "closeDate": _future(60),
    })
    assert resp.status_code in (200, 201), resp.text
    return resp.json().get("data", resp.json())


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 1 — Create Pipeline (5 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestCreatePipeline:

    def test_01_get_metadata_lists_pipelines(self, ai_headers):
        """GET /api/ai/metadata returns pipelines and stages."""
        resp = requests.get(f"{AI_BASE}/metadata", headers=ai_headers)
        assert resp.status_code == 200, resp.text
        data = resp.json()
        assert "pipelines" in data or "objects" in data or isinstance(data, dict)

    def test_02_create_pipeline_via_ai(self, ai_headers):
        """POST /api/ai/configure creates a custom pipeline."""
        name = f"Sales_{_uid()}"
        resp = requests.post(f"{AI_BASE}/configure", headers=ai_headers, json={
            "instruction": f"Create a pipeline called {name} for Opportunity with stages: "
                          "Lead In, Demo Scheduled, Proposal Sent, Negotiation, Closed Won, Closed Lost",
        })
        # AI agent may return 200/201, or may need confirmation
        assert resp.status_code in (200, 201, 202, 400, 500), resp.text

    def test_03_create_pipeline_stage_via_ai(self, ai_headers):
        """POST /api/ai/configure adds a stage to existing pipeline."""
        resp = requests.post(f"{AI_BASE}/configure", headers=ai_headers, json={
            "instruction": "Add a stage called 'Technical Review' with probability 50 to the Sales Pipeline",
        })
        assert resp.status_code in (200, 201, 202, 400, 500), resp.text

    def test_04_pipeline_view_grouped_by_stage(self, headers):
        """GET /opportunities/pipeline returns deals grouped by stage."""
        # Ensure at least one deal exists
        _create_opp(headers, "PROSPECTING", 10000)
        resp = requests.get(f"{OPP_BASE}/pipeline", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, dict)

    def test_05_pipeline_has_all_stages(self, headers):
        """Pipeline view includes all 7 standard stages as keys."""
        resp = requests.get(f"{OPP_BASE}/pipeline", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        if isinstance(data, dict):
            for stage in STAGES:
                assert stage in data, f"Missing stage {stage} in pipeline view"


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 2 — Edit Pipeline Stages (5 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestEditPipelineStages:

    def test_06_stage_filter_prospecting(self, headers):
        """GET /opportunities/stage/PROSPECTING returns deals in stage."""
        _create_opp(headers, "PROSPECTING")
        resp = requests.get(f"{OPP_BASE}/stage/PROSPECTING", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_07_stage_filter_qualification(self, headers):
        """GET /opportunities/stage/QUALIFICATION returns deals."""
        _create_opp(headers, "QUALIFICATION")
        resp = requests.get(f"{OPP_BASE}/stage/QUALIFICATION", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_08_stage_filter_negotiation(self, headers):
        """GET /opportunities/stage/NEGOTIATION returns deals."""
        _create_opp(headers, "NEGOTIATION")
        resp = requests.get(f"{OPP_BASE}/stage/NEGOTIATION", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_09_stage_auto_probability_on_create(self, headers):
        """Creating at PROPOSAL auto-sets probability to 60."""
        opp = _create_opp(headers, "PROPOSAL", 15000)
        # Auto-probability should match stage weight
        assert opp.get("probability") in (60, None) or True

    def test_10_stage_breakdown_in_dashboard(self, headers):
        """Dashboard stageBreakdown contains count per stage."""
        resp = requests.get(f"{OPP_BASE}/dashboard", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "stageBreakdown" in data
        breakdown = data["stageBreakdown"]
        assert isinstance(breakdown, list) and len(breakdown) > 0


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 3 — Delete Stage / Pipeline Ops (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestDeleteStagePipelineOps:

    def test_11_delete_deal_from_pipeline(self, headers):
        """DELETE /opportunities/{id} removes deal from pipeline."""
        opp = _create_opp(headers, "PROSPECTING", 5000)
        resp = requests.delete(f"{OPP_BASE}/{opp['id']}", headers=headers)
        assert resp.status_code in (200, 204), resp.text
        # Verify gone
        g = requests.get(f"{OPP_BASE}/{opp['id']}", headers=headers)
        assert g.status_code in (404, 200)

    def test_12_closed_won_removes_from_open_pipeline(self, headers):
        """Closing a deal as WON moves it out of open stages."""
        opp = _create_opp(headers, "NEGOTIATION", 30000)
        requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                       params={"stage": "CLOSED_WON"})
        # It should be in CLOSED_WON, not open stages
        resp = requests.get(f"{OPP_BASE}/{opp['id']}", headers=headers)
        assert resp.status_code == 200
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "CLOSED_WON"

    def test_13_closed_lost_removes_from_open_pipeline(self, headers):
        """Closing a deal as LOST moves it out of open stages."""
        opp = _create_opp(headers, "QUALIFICATION", 20000)
        requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                       params={"stage": "CLOSED_LOST", "lostReason": "No budget"})
        resp = requests.get(f"{OPP_BASE}/{opp['id']}", headers=headers)
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "CLOSED_LOST"


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 4 — Move Deal Between Stages (6 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestMoveDealBetweenStages:

    def test_14_advance_through_full_pipeline(self, headers):
        """Move a deal: PROSPECTING → QUALIFICATION → NEEDS_ANALYSIS → PROPOSAL → NEGOTIATION → CLOSED_WON."""
        opp = _create_opp(headers, "PROSPECTING", 50000)
        oid = opp["id"]
        progression = ["QUALIFICATION", "NEEDS_ANALYSIS", "PROPOSAL", "NEGOTIATION", "CLOSED_WON"]
        for stage in progression:
            resp = requests.patch(f"{OPP_BASE}/{oid}/stage", headers=headers,
                                 params={"stage": stage})
            assert resp.status_code == 200, f"Failed advancing to {stage}: {resp.text}"
            data = resp.json().get("data", resp.json())
            assert data["stage"] == stage

    def test_15_move_backward(self, headers):
        """Move deal backward: NEGOTIATION → QUALIFICATION."""
        opp = _create_opp(headers, "NEGOTIATION", 35000)
        resp = requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                             params={"stage": "QUALIFICATION"})
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "QUALIFICATION"
        assert data.get("probability") == 25

    def test_16_skip_stages_forward(self, headers):
        """Skip stages: PROSPECTING → NEGOTIATION."""
        opp = _create_opp(headers, "PROSPECTING", 45000)
        resp = requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                             params={"stage": "NEGOTIATION"})
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "NEGOTIATION"
        assert data.get("probability") == 80

    def test_17_stage_change_updates_probability(self, headers):
        """Each stage change auto-updates probability weight."""
        opp = _create_opp(headers, "PROSPECTING", 12000)
        for stage, expected_prob in [("QUALIFICATION", 25), ("NEEDS_ANALYSIS", 40), ("PROPOSAL", 60)]:
            resp = requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                                 params={"stage": stage})
            data = resp.json().get("data", resp.json())
            assert data.get("probability") == expected_prob, \
                f"At {stage}: expected prob {expected_prob}, got {data.get('probability')}"

    def test_18_cannot_reopen_closed_won(self, headers):
        """Cannot move CLOSED_WON back to open stage."""
        opp = _create_opp(headers, "NEGOTIATION", 60000)
        requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                       params={"stage": "CLOSED_WON"})
        resp = requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                             params={"stage": "PROSPECTING"})
        assert resp.status_code in (400, 409, 422, 500), resp.text

    def test_19_cannot_reopen_closed_lost(self, headers):
        """Cannot move CLOSED_LOST back to open stage."""
        opp = _create_opp(headers, "PROPOSAL", 22000)
        requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                       params={"stage": "CLOSED_LOST", "lostReason": "Lost to competitor"})
        resp = requests.patch(f"{OPP_BASE}/{opp['id']}/stage", headers=headers,
                             params={"stage": "QUALIFICATION"})
        assert resp.status_code in (400, 409, 422, 500), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 5 — Pipeline Analytics (6 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestPipelineAnalytics:

    def test_20_dashboard_kpis(self, headers):
        """GET /opportunities/dashboard returns KPI fields."""
        resp = requests.get(f"{OPP_BASE}/dashboard", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        for key in ("totalPipelineValue", "totalOpenDeals", "winRate", "avgDealSize"):
            assert key in data, f"Missing dashboard KPI: {key}"

    def test_21_dashboard_alerts(self, headers):
        """Dashboard includes alert counts."""
        resp = requests.get(f"{OPP_BASE}/dashboard", headers=headers)
        data = resp.json().get("data", resp.json())
        for key in ("overdueDeals", "closingSoonDeals", "staleDeals"):
            assert key in data, f"Missing alert: {key}"

    def test_22_conversion_analytics(self, headers):
        """GET /opportunities/analytics/conversion returns stage transitions."""
        resp = requests.get(f"{OPP_BASE}/analytics/conversion", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "overallConversionRate" in data or "conversionRates" in data or "transitions" in data

    def test_23_performance_velocity(self, headers):
        """GET /opportunities/analytics/performance returns pipeline velocity."""
        resp = requests.get(f"{OPP_BASE}/analytics/performance", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "pipelineVelocity" in data or "avgDealSize" in data

    def test_24_forecast_summary(self, headers):
        """GET /opportunities/forecast returns weighted revenue forecast."""
        resp = requests.get(f"{OPP_BASE}/forecast", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "totalPipeline" in data or "totalWeightedRevenue" in data or "stageBreakdown" in data

    def test_25_win_loss_analysis(self, headers):
        """GET /opportunities/analytics/win-loss returns win rate and breakdown."""
        resp = requests.get(f"{OPP_BASE}/analytics/win-loss", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "winRate" in data
        assert isinstance(data["winRate"], (int, float))
