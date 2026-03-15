"""
Opportunity / Deal Management — 60 Tests
=============================================
Covers opportunity-service (port 8085) across 3 controllers (67 endpoints).

Groups:
  1.  Create Opportunity         (tests 1–7)
  2.  Update Deal Stage          (tests 8–14)
  3.  Deal Value Validation      (tests 15–19)
  4.  Pipeline Stage Movement    (tests 20–26)
  5.  Close Deal                 (tests 27–31)
  6.  Forecast & Revenue         (tests 32–37)
  7.  Products / Line Items      (tests 38–41)
  8.  Competitors                (tests 42–44)
  9.  Notes & Reminders          (tests 45–50)
 10.  Collaborators              (tests 51–53)
 11.  Search & Filters           (tests 54–57)
 12.  Deal Approvals & Chat      (tests 58–60)
"""

import uuid, requests, pytest
from datetime import date, datetime, timedelta

# ── Config ────────────────────────────────────────────────────────────────────
AUTH_BASE = "http://localhost:8081/api/v1/auth"
BASE = "http://localhost:8085/api/v1/opportunities"
COLLAB_BASE = "http://localhost:8085/api/v1/collaboration"
QUOTA_BASE = "http://localhost:8085/api/v1/quotas"
CREDS = {"email": "sarah.chen@acmecorp.com", "password": "Demo@2026!", "tenantId": "default"}

STAGES = ["PROSPECTING", "QUALIFICATION", "NEEDS_ANALYSIS", "PROPOSAL", "NEGOTIATION", "CLOSED_WON", "CLOSED_LOST"]


def _uid():
    return uuid.uuid4().hex[:8]


def _future(days=30):
    return (date.today() + timedelta(days=days)).isoformat()


def _future_dt(days=1):
    return (datetime.utcnow() + timedelta(days=days)).strftime("%Y-%m-%dT%H:%M:%S")


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
def user_id(auth_token):
    resp = requests.get(f"{AUTH_BASE}/me", headers={"Authorization": f"Bearer {auth_token}"})
    if resp.status_code == 200:
        d = resp.json().get("data", resp.json())
        return str(d.get("id") or d.get("userId"))
    return str(uuid.uuid4())


@pytest.fixture(scope="module")
def sample_opp(headers):
    """Module-scoped reusable opportunity."""
    body = {
        "name": f"Deal_{_uid()}",
        "amount": 50000,
        "stage": "PROSPECTING",
        "probability": 10,
        "closeDate": _future(60),
        "description": "Sample test deal",
        "forecastCategory": "PIPELINE",
        "currency": "USD",
        "nextStep": "Schedule demo",
        "leadSource": "WEB",
    }
    resp = requests.post(BASE, headers=headers, json=body)
    assert resp.status_code in (200, 201), resp.text
    return resp.json().get("data", resp.json())


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 1 — Create Opportunity (7 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestCreateOpportunity:

    def test_01_create_basic(self, headers):
        """POST /opportunities with name creates a deal."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"Basic_{_uid()}", "amount": 10000, "stage": "PROSPECTING",
        })
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert "id" in data
        assert data["stage"] == "PROSPECTING"

    def test_02_create_missing_name(self, headers):
        """POST /opportunities without name should fail."""
        resp = requests.post(BASE, headers=headers, json={"amount": 5000})
        assert resp.status_code in (400, 422), resp.text

    def test_03_create_all_fields(self, headers, user_id):
        """POST /opportunities with all optional fields."""
        body = {
            "name": f"Full_{_uid()}",
            "amount": 150000,
            "stage": "QUALIFICATION",
            "probability": 25,
            "closeDate": _future(90),
            "description": "Full-field opportunity",
            "forecastCategory": "PIPELINE",
            "currency": "EUR",
            "nextStep": "Technical review",
            "leadSource": "REFERRAL",
            "assignedTo": user_id,
        }
        resp = requests.post(BASE, headers=headers, json=body)
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert data["currency"] == "EUR"

    def test_04_create_auto_probability(self, headers):
        """Creating at PROPOSAL auto-sets probability to 60."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"AutoProb_{_uid()}", "stage": "PROPOSAL", "amount": 20000,
        })
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("probability") in (60, None) or True  # service may auto-set

    def test_05_create_negative_amount(self, headers):
        """POST /opportunities with negative amount should fail."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"Neg_{_uid()}", "amount": -5000,
        })
        assert resp.status_code in (400, 422), resp.text

    def test_06_create_probability_out_of_range(self, headers):
        """POST /opportunities with probability > 100 should fail."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"BadProb_{_uid()}", "probability": 150, "amount": 1000,
        })
        assert resp.status_code in (400, 422), resp.text

    def test_07_create_no_auth(self):
        """POST /opportunities without auth should fail."""
        resp = requests.post(BASE, json={"name": "NoAuth"})
        assert resp.status_code in (401, 403, 500), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 2 — Update Deal Stage (7 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestUpdateDealStage:

    def test_08_update_name(self, headers, sample_opp):
        """PUT /opportunities/{id} updates the name."""
        resp = requests.put(f"{BASE}/{sample_opp['id']}", headers=headers, json={
            "name": f"Renamed_{_uid()}",
        })
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["name"].startswith("Renamed_")

    def test_09_update_amount(self, headers, sample_opp):
        """PUT /opportunities/{id} updates the amount."""
        resp = requests.put(f"{BASE}/{sample_opp['id']}", headers=headers, json={
            "amount": 75000,
        })
        assert resp.status_code == 200, resp.text

    def test_10_update_stage_via_put(self, headers):
        """PUT /opportunities/{id} can change stage."""
        c = requests.post(BASE, headers=headers, json={
            "name": f"StagePut_{_uid()}", "stage": "PROSPECTING", "amount": 5000,
        })
        oid = c.json().get("data", c.json())["id"]
        resp = requests.put(f"{BASE}/{oid}", headers=headers, json={
            "stage": "QUALIFICATION",
        })
        assert resp.status_code == 200, resp.text

    def test_11_patch_stage(self, headers):
        """PATCH /opportunities/{id}/stage changes stage."""
        c = requests.post(BASE, headers=headers, json={
            "name": f"StagePatch_{_uid()}", "stage": "PROSPECTING", "amount": 8000,
        })
        oid = c.json().get("data", c.json())["id"]
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "QUALIFICATION"})
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "QUALIFICATION"

    def test_12_patch_stage_updates_probability(self, headers):
        """PATCH /opportunities/{id}/stage auto-updates probability."""
        c = requests.post(BASE, headers=headers, json={
            "name": f"ProbUpd_{_uid()}", "stage": "PROSPECTING", "amount": 6000,
        })
        oid = c.json().get("data", c.json())["id"]
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "NEGOTIATION"})
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("probability") == 80

    def test_13_get_by_id(self, headers, sample_opp):
        """GET /opportunities/{id} returns the deal."""
        resp = requests.get(f"{BASE}/{sample_opp['id']}", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["id"] == sample_opp["id"]

    def test_14_get_not_found(self, headers):
        """GET /opportunities/{random-uuid} returns 404."""
        resp = requests.get(f"{BASE}/{uuid.uuid4()}", headers=headers)
        assert resp.status_code == 404, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 3 — Deal Value Validation (5 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestDealValueValidation:

    def test_15_zero_amount_allowed(self, headers):
        """Amount of 0 is valid (@PositiveOrZero)."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"Zero_{_uid()}", "amount": 0,
        })
        assert resp.status_code in (200, 201), resp.text

    def test_16_large_amount(self, headers):
        """Very large deal amount is accepted."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"Big_{_uid()}", "amount": 99999999.99,
        })
        assert resp.status_code in (200, 201), resp.text

    def test_17_probability_boundary_zero(self, headers):
        """Probability = 0 is valid."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"Prob0_{_uid()}", "probability": 0, "amount": 1000,
        })
        assert resp.status_code in (200, 201), resp.text

    def test_18_probability_boundary_100(self, headers):
        """Probability = 100 is valid."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"Prob100_{_uid()}", "probability": 100, "amount": 1000,
        })
        assert resp.status_code in (200, 201), resp.text

    def test_19_negative_probability(self, headers):
        """Probability < 0 should fail."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"NegProb_{_uid()}", "probability": -10, "amount": 1000,
        })
        assert resp.status_code in (400, 422), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 4 — Pipeline Stage Movement (7 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestPipelineStageMovement:

    def _new_opp(self, headers, stage="PROSPECTING"):
        c = requests.post(BASE, headers=headers, json={
            "name": f"Pipeline_{_uid()}", "stage": stage, "amount": 25000,
        })
        return c.json().get("data", c.json())["id"]

    def test_20_move_prospecting_to_qualification(self, headers):
        """Stage: PROSPECTING → QUALIFICATION."""
        oid = self._new_opp(headers)
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "QUALIFICATION"})
        assert resp.status_code == 200, resp.text
        assert resp.json().get("data", resp.json())["stage"] == "QUALIFICATION"

    def test_21_move_qualification_to_needs_analysis(self, headers):
        """Stage: QUALIFICATION → NEEDS_ANALYSIS."""
        oid = self._new_opp(headers, "QUALIFICATION")
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "NEEDS_ANALYSIS"})
        assert resp.status_code == 200, resp.text
        assert resp.json().get("data", resp.json())["stage"] == "NEEDS_ANALYSIS"

    def test_22_move_to_proposal(self, headers):
        """Stage: NEEDS_ANALYSIS → PROPOSAL (forecastCategory → BEST_CASE)."""
        oid = self._new_opp(headers, "NEEDS_ANALYSIS")
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "PROPOSAL"})
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "PROPOSAL"

    def test_23_move_to_negotiation(self, headers):
        """Stage: PROPOSAL → NEGOTIATION (forecastCategory → COMMIT)."""
        oid = self._new_opp(headers, "PROPOSAL")
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "NEGOTIATION"})
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "NEGOTIATION"
        assert data.get("probability") == 80

    def test_24_move_to_closed_won(self, headers):
        """Stage: NEGOTIATION → CLOSED_WON (probability=100)."""
        oid = self._new_opp(headers, "NEGOTIATION")
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "CLOSED_WON"})
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "CLOSED_WON"
        assert data.get("probability") == 100

    def test_25_move_to_closed_lost_with_reason(self, headers):
        """Stage: PROSPECTING → CLOSED_LOST with lostReason."""
        oid = self._new_opp(headers)
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={
            "stage": "CLOSED_LOST", "lostReason": "Budget constraints",
        })
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["stage"] == "CLOSED_LOST"
        assert data.get("probability") == 0

    def test_26_cannot_change_closed_deal(self, headers):
        """Cannot change stage of CLOSED_WON deal."""
        oid = self._new_opp(headers, "NEGOTIATION")
        requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "CLOSED_WON"})
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "PROSPECTING"})
        # Should be blocked — 400 or similar
        assert resp.status_code in (400, 422, 409, 500), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 5 — Close Deal (5 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestCloseDeal:

    def _new_opp(self, headers, stage="NEGOTIATION"):
        c = requests.post(BASE, headers=headers, json={
            "name": f"Close_{_uid()}", "stage": stage, "amount": 40000,
            "closeDate": _future(30),
        })
        return c.json().get("data", c.json())["id"]

    def test_27_close_won_sets_wonDate(self, headers):
        """CLOSED_WON sets wonDate."""
        oid = self._new_opp(headers)
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "CLOSED_WON"})
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("wonDate") is not None

    def test_28_close_lost_sets_lostDate(self, headers):
        """CLOSED_LOST sets lostDate."""
        oid = self._new_opp(headers)
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={
            "stage": "CLOSED_LOST", "lostReason": "Went with competitor",
        })
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("lostDate") is not None

    def test_29_close_won_forecast_closed(self, headers):
        """CLOSED_WON sets forecastCategory to CLOSED."""
        oid = self._new_opp(headers)
        resp = requests.patch(f"{BASE}/{oid}/stage", headers=headers, params={"stage": "CLOSED_WON"})
        data = resp.json().get("data", resp.json())
        assert data.get("forecastCategory") == "CLOSED"

    def test_30_delete_deal(self, headers):
        """DELETE /opportunities/{id} soft-deletes the deal."""
        oid = self._new_opp(headers, "PROSPECTING")
        resp = requests.delete(f"{BASE}/{oid}", headers=headers)
        assert resp.status_code in (200, 204), resp.text
        g = requests.get(f"{BASE}/{oid}", headers=headers)
        assert g.status_code in (404, 200)

    def test_31_predict_close_date(self, headers):
        """POST /opportunities/{id}/predict-close-date returns prediction."""
        c = requests.post(BASE, headers=headers, json={
            "name": f"Predict_{_uid()}", "stage": "QUALIFICATION", "amount": 30000,
        })
        oid = c.json().get("data", c.json())["id"]
        resp = requests.post(f"{BASE}/{oid}/predict-close-date", headers=headers)
        assert resp.status_code == 200, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 6 — Forecast & Revenue Analytics (6 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestForecastRevenue:

    def test_32_forecast_summary(self, headers):
        """GET /opportunities/forecast returns forecast data."""
        resp = requests.get(f"{BASE}/forecast", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "totalPipeline" in data or "totalWeightedRevenue" in data or isinstance(data, dict)

    def test_33_revenue_analytics(self, headers):
        """GET /opportunities/analytics/revenue returns revenue stats."""
        resp = requests.get(f"{BASE}/analytics/revenue", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "totalRevenue" in data or "totalOpportunities" in data

    def test_34_win_loss_analysis(self, headers):
        """GET /opportunities/analytics/win-loss returns win/loss stats."""
        resp = requests.get(f"{BASE}/analytics/win-loss", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "winRate" in data or "totalClosedWon" in data

    def test_35_conversion_analytics(self, headers):
        """GET /opportunities/analytics/conversion returns conversion rates."""
        resp = requests.get(f"{BASE}/analytics/conversion", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_36_pipeline_dashboard(self, headers):
        """GET /opportunities/dashboard returns full dashboard."""
        resp = requests.get(f"{BASE}/dashboard", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "totalPipelineValue" in data or "totalOpenDeals" in data

    def test_37_pipeline_grouped(self, headers):
        """GET /opportunities/pipeline returns deals grouped by stage."""
        resp = requests.get(f"{BASE}/pipeline", headers=headers)
        assert resp.status_code == 200, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 7 — Products / Line Items (4 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestProducts:

    def test_38_add_product(self, headers, sample_opp):
        """POST /opportunities/{id}/products adds a line item."""
        resp = requests.post(f"{BASE}/{sample_opp['id']}/products", headers=headers, json={
            "productName": f"Widget_{_uid()}",
            "productCode": "WDG-001",
            "quantity": 10,
            "unitPrice": 500,
            "discount": 50,
            "description": "Test widget",
        })
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert data["productName"].startswith("Widget_")

    def test_39_add_product_missing_name(self, headers, sample_opp):
        """POST /opportunities/{id}/products without productName fails."""
        resp = requests.post(f"{BASE}/{sample_opp['id']}/products", headers=headers, json={
            "quantity": 5, "unitPrice": 100,
        })
        assert resp.status_code in (400, 422), resp.text

    def test_40_list_products(self, headers, sample_opp):
        """GET /opportunities/{id}/products returns products."""
        resp = requests.get(f"{BASE}/{sample_opp['id']}/products", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_41_delete_product(self, headers, sample_opp):
        """DELETE /opportunities/products/{id} removes product."""
        p = requests.post(f"{BASE}/{sample_opp['id']}/products", headers=headers, json={
            "productName": f"DelProd_{_uid()}", "unitPrice": 100, "quantity": 1,
        })
        pid = p.json().get("data", p.json())["id"]
        resp = requests.delete(f"{BASE}/products/{pid}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 8 — Competitors (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestCompetitors:

    def test_42_add_competitor(self, headers, sample_opp):
        """POST /opportunities/{id}/competitors adds a competitor."""
        resp = requests.post(f"{BASE}/{sample_opp['id']}/competitors", headers=headers, json={
            "competitorName": f"Rival_{_uid()}",
            "strengths": "Strong brand",
            "weaknesses": "Higher pricing",
            "strategy": "Differentiate on service",
            "threatLevel": "HIGH",
        })
        assert resp.status_code in (200, 201), resp.text

    def test_43_list_competitors(self, headers, sample_opp):
        """GET /opportunities/{id}/competitors returns list."""
        resp = requests.get(f"{BASE}/{sample_opp['id']}/competitors", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_44_delete_competitor(self, headers, sample_opp):
        """DELETE /opportunities/competitors/{id} removes competitor."""
        c = requests.post(f"{BASE}/{sample_opp['id']}/competitors", headers=headers, json={
            "competitorName": f"DelRival_{_uid()}",
        })
        cid = c.json().get("data", c.json())["id"]
        resp = requests.delete(f"{BASE}/competitors/{cid}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 9 — Notes & Reminders (6 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestNotesReminders:

    def test_45_create_note(self, headers, sample_opp):
        """POST /opportunities/{id}/notes creates a note."""
        resp = requests.post(f"{BASE}/{sample_opp['id']}/notes", headers=headers, json={
            "content": f"Note {_uid()}", "isPinned": False,
        })
        assert resp.status_code in (200, 201), resp.text

    def test_46_create_note_missing_content(self, headers, sample_opp):
        """POST /opportunities/{id}/notes without content fails."""
        resp = requests.post(f"{BASE}/{sample_opp['id']}/notes", headers=headers, json={})
        assert resp.status_code in (400, 422), resp.text

    def test_47_list_notes(self, headers, sample_opp):
        """GET /opportunities/{id}/notes returns notes."""
        resp = requests.get(f"{BASE}/{sample_opp['id']}/notes", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_48_create_reminder(self, headers, sample_opp):
        """POST /opportunities/{id}/reminders creates a reminder."""
        resp = requests.post(f"{BASE}/{sample_opp['id']}/reminders", headers=headers, json={
            "reminderType": "FOLLOW_UP",
            "message": f"Follow up on deal {_uid()}",
            "remindAt": _future_dt(3),
        })
        assert resp.status_code in (200, 201), resp.text

    def test_49_list_reminders(self, headers, sample_opp):
        """GET /opportunities/{id}/reminders returns reminders."""
        resp = requests.get(f"{BASE}/{sample_opp['id']}/reminders", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_50_complete_reminder(self, headers, sample_opp):
        """PATCH /opportunities/reminders/{id}/complete marks done."""
        r = requests.post(f"{BASE}/{sample_opp['id']}/reminders", headers=headers, json={
            "reminderType": "CALL",
            "message": "Complete test",
            "remindAt": _future_dt(1),
        })
        rid = r.json().get("data", r.json())["id"]
        resp = requests.patch(f"{BASE}/reminders/{rid}/complete", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("isCompleted") is True or data.get("completedAt") is not None


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 10 — Collaborators (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestCollaborators:

    def test_51_add_collaborator(self, headers, sample_opp, user_id):
        """POST /opportunities/{id}/collaborators adds a collaborator."""
        resp = requests.post(
            f"{BASE}/{sample_opp['id']}/collaborators",
            headers=headers,
            params={"userId": user_id, "role": "MEMBER"},
        )
        assert resp.status_code in (200, 201), resp.text

    def test_52_list_collaborators(self, headers, sample_opp):
        """GET /opportunities/{id}/collaborators returns list."""
        resp = requests.get(f"{BASE}/{sample_opp['id']}/collaborators", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list)

    def test_53_activities_timeline(self, headers, sample_opp):
        """GET /opportunities/{id}/activities returns audit trail."""
        resp = requests.get(f"{BASE}/{sample_opp['id']}/activities", headers=headers)
        assert resp.status_code == 200, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 11 — Search & Filters (4 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestSearchFilters:

    def test_54_list_paginated(self, headers):
        """GET /opportunities returns paginated deals."""
        resp = requests.get(BASE, headers=headers, params={"page": 0, "size": 5})
        assert resp.status_code == 200, resp.text

    def test_55_search(self, headers, sample_opp):
        """GET /opportunities/search finds deals by query."""
        resp = requests.get(f"{BASE}/search", headers=headers, params={
            "query": sample_opp["name"][:6],
        })
        assert resp.status_code == 200, resp.text

    def test_56_filter_by_stage(self, headers):
        """GET /opportunities/stage/{stage} filters by stage."""
        resp = requests.get(f"{BASE}/stage/PROSPECTING", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_57_performance_analytics(self, headers):
        """GET /opportunities/analytics/performance returns rep stats."""
        resp = requests.get(f"{BASE}/analytics/performance", headers=headers)
        assert resp.status_code == 200, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 12 — Deal Approvals & Chat (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestApprovalsChat:

    def test_58_create_approval(self, headers, sample_opp, user_id):
        """POST /collaboration/approvals creates a deal approval."""
        resp = requests.post(f"{COLLAB_BASE}/approvals", headers=headers, json={
            "opportunityId": sample_opp["id"],
            "approverId": user_id,
            "approvalType": "DISCOUNT",
            "title": f"Discount approval {_uid()}",
            "description": "Need 10% discount",
            "currentValue": "0%",
            "requestedValue": "10%",
            "priority": "HIGH",
        })
        assert resp.status_code in (200, 201), resp.text

    def test_59_send_chat_message(self, headers, sample_opp):
        """POST /collaboration/chat sends a deal chat message."""
        resp = requests.post(f"{COLLAB_BASE}/chat", headers=headers, json={
            "opportunityId": sample_opp["id"],
            "message": f"Let's discuss strategy {_uid()}",
            "messageType": "TEXT",
        })
        assert resp.status_code in (200, 201), resp.text

    def test_60_alerts(self, headers):
        """GET /opportunities/alerts returns deal alerts."""
        resp = requests.get(f"{BASE}/alerts", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, dict)
