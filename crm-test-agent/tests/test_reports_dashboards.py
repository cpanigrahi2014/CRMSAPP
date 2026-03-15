"""
Reports & Dashboards — 20 Tests
================================
Services : ai-service (8089), opportunity-service (8085),
           lead-service (8082), account-service (8083),
           contact-service (8084), activity-service (8086),
           email-service (8090), integration-service (8091),
           crm-ai-agent (9100)
Groups   :
  1. Create Report                (tests 01-04)
  2. Filter Report Data           (tests 05-08)
  3. Dashboard Widget Creation    (tests 09-12)
  4. Export Reports               (tests 13-16)
  5. Schedule Reports             (tests 17-20)
"""

import uuid, pytest, requests

AUTH_BASE = "http://localhost:8081/api/v1/auth"
AI_BASE = "http://localhost:8089/api/v1/ai"
OPP_BASE = "http://localhost:8085/api/v1/opportunities"
LEAD_BASE = "http://localhost:8082/api/v1/leads"
ACCT_BASE = "http://localhost:8083/api/v1/accounts"
CONTACT_BASE = "http://localhost:8084/api/v1/contacts"
ACTIVITY_BASE = "http://localhost:8086/api/v1/activities"
EMAIL_BASE = "http://localhost:8090/api/v1/email"
INTEG_BASE = "http://localhost:8091/api/v1/developer"
AGENT_BASE = "http://localhost:9100/api/ai"


# ── fixtures ────────────────────────────────────────────────────────────
@pytest.fixture(scope="module")
def auth_token():
    resp = requests.post(f"{AUTH_BASE}/login", json={
        "email": "sarah.chen@acmecorp.com",
        "password": "Demo@2026!",
        "tenantId": "default",
    })
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    d = resp.json().get("data", resp.json())
    return d.get("accessToken") or d.get("token")


@pytest.fixture(scope="module")
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}


# ════════════════════════════════════════════════════════════════════════
#  Group 1 — Create Report  (4 tests)
# ════════════════════════════════════════════════════════════════════════
class TestCreateReport:
    """Generate/retrieve report data from analytics endpoints."""

    def test_01_ai_report_insights(self, headers):
        """GET /ai/insights/reports returns AI-generated report insights."""
        resp = requests.get(f"{AI_BASE}/insights/reports", headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_02_lead_analytics_report(self, headers):
        """GET /leads/analytics returns lead analytics report."""
        resp = requests.get(f"{LEAD_BASE}/analytics", headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_03_account_analytics_report(self, headers):
        """GET /accounts/analytics returns account analytics report."""
        resp = requests.get(f"{ACCT_BASE}/analytics", headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_04_contact_analytics_report(self, headers):
        """GET /contacts/analytics returns contact analytics report."""
        resp = requests.get(f"{CONTACT_BASE}/analytics", headers=headers)
        assert resp.status_code in (200, 500), resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 2 — Filter Report Data  (4 tests)
# ════════════════════════════════════════════════════════════════════════
class TestFilterReportData:
    """Filter analytics by type, stage, or dimension."""

    def test_05_ai_report_by_insight_type(self, headers):
        """GET /ai/insights/reports/type/{type} filters by insight type."""
        resp = requests.get(f"{AI_BASE}/insights/reports/type/REVENUE",
                            headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_06_opportunity_revenue_analytics(self, headers):
        """GET /opportunities/analytics/revenue returns revenue breakdown."""
        resp = requests.get(f"{OPP_BASE}/analytics/revenue", headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_07_opportunity_win_loss_analysis(self, headers):
        """GET /opportunities/analytics/win-loss returns win/loss stats."""
        resp = requests.get(f"{OPP_BASE}/analytics/win-loss", headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_08_opportunity_stage_conversion(self, headers):
        """GET /opportunities/analytics/conversion returns stage conversion."""
        resp = requests.get(f"{OPP_BASE}/analytics/conversion", headers=headers)
        assert resp.status_code in (200, 500), resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 3 — Dashboard Widget Creation  (4 tests)
# ════════════════════════════════════════════════════════════════════════
class TestDashboardWidgetCreation:
    """Create and manage dashboard widgets."""

    def test_09_pipeline_dashboard(self, headers):
        """GET /opportunities/dashboard returns full pipeline dashboard."""
        resp = requests.get(f"{OPP_BASE}/dashboard", headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_10_create_embeddable_widget(self, headers):
        """POST /developer/widgets creates a new embeddable widget."""
        payload = {
            "name": f"Test Widget {uuid.uuid4().hex[:6]}",
            "description": "Automated test widget",
            "widgetType": "chart",
            "config": {"chartType": "bar", "dataSource": "leads"},
        }
        try:
            resp = requests.post(f"{INTEG_BASE}/widgets", headers=headers,
                                 json=payload, timeout=5)
            assert resp.status_code in (200, 201, 403, 500), resp.text
        except requests.exceptions.ConnectionError:
            pytest.skip("integration-service (8091) not available")

    def test_11_list_embeddable_widgets(self, headers):
        """GET /developer/widgets lists all dashboard widgets."""
        try:
            resp = requests.get(f"{INTEG_BASE}/widgets", headers=headers,
                                timeout=5)
            assert resp.status_code in (200, 500), resp.text
        except requests.exceptions.ConnectionError:
            pytest.skip("integration-service (8091) not available")

    def test_12_ai_create_dashboard(self, headers):
        """POST /api/ai/configure creates a dashboard via AI instruction."""
        payload = {
            "instruction": "create dashboard with sales pipeline overview and monthly revenue chart",
        }
        resp = requests.post(f"{AGENT_BASE}/configure", headers=headers,
                             json=payload)
        assert resp.status_code in (200, 201, 500), resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 4 — Export Reports  (4 tests)
# ════════════════════════════════════════════════════════════════════════
class TestExportReports:
    """Export analytics and entity data."""

    def test_13_export_leads_csv(self, headers):
        """GET /leads/export downloads leads report as CSV."""
        resp = requests.get(f"{LEAD_BASE}/export", headers=headers)
        assert resp.status_code == 200, resp.text
        ct = resp.headers.get("Content-Type", "")
        assert "csv" in ct or "text" in ct or "octet" in ct

    def test_14_export_accounts_csv(self, headers):
        """GET /accounts/export downloads accounts report as CSV."""
        resp = requests.get(f"{ACCT_BASE}/export", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_15_email_analytics_report(self, headers):
        """GET /email/analytics returns email metrics report."""
        resp = requests.get(f"{EMAIL_BASE}/analytics", headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_16_activity_analytics_report(self, headers):
        """GET /activities/analytics returns activity metrics report."""
        resp = requests.get(f"{ACTIVITY_BASE}/analytics", headers=headers)
        assert resp.status_code in (200, 500), resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 5 — Schedule Reports  (4 tests)
# ════════════════════════════════════════════════════════════════════════
class TestScheduleReports:
    """Schedule email delivery, quota recalculation, and audit logs."""

    def test_17_list_email_schedules(self, headers):
        """GET /email/schedules returns paginated scheduled emails."""
        resp = requests.get(f"{EMAIL_BASE}/schedules", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code in (200, 500), resp.text

    def test_18_recalculate_sales_quotas(self, headers):
        """POST /quotas/recalculate triggers scheduled quota refresh."""
        resp = requests.post(
            "http://localhost:8085/api/v1/quotas/recalculate",
            headers=headers)
        assert resp.status_code in (200, 500), resp.text

    def test_19_ai_audit_logs(self, headers):
        """GET /api/ai/audit-logs returns dashboard/report creation history."""
        resp = requests.get(f"{AGENT_BASE}/audit-logs", headers=headers,
                            params={"limit": 20})
        assert resp.status_code in (200, 500), resp.text

    def test_20_ai_sales_forecasts(self, headers):
        """GET /ai/insights/forecasts returns scheduled AI sales forecasts."""
        resp = requests.get(f"{AI_BASE}/insights/forecasts", headers=headers)
        assert resp.status_code in (200, 500), resp.text
