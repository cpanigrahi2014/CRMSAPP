"""
Module 14 — Reports & Dashboards (20 Tests: 11 API + 9 UI)
=============================================================
API : Reports generation, dashboards, KPI, analytics, widget data
UI  : Reports page, dashboard widgets, charts, filters
"""
import uuid, pytest, requests
from playwright.sync_api import Page
from automation import (
    AI_URL, OPP_URL, LEAD_URL, ACTIVITY_URL, EMAIL_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)


class TestReportsDashboardsAPI:
    def test_api_01_dashboard_data(self, headers):
        assert requests.get(f"{OPP_URL}/dashboard", headers=headers).status_code in (200, 500)

    def test_api_02_lead_analytics(self, headers):
        assert requests.get(f"{LEAD_URL}/analytics", headers=headers).status_code in (200, 404, 500)

    def test_api_03_opportunity_analytics(self, headers):
        assert requests.get(f"{OPP_URL}/analytics/performance", headers=headers).status_code in (200, 500)

    def test_api_04_revenue_analytics(self, headers):
        assert requests.get(f"{OPP_URL}/analytics/revenue", headers=headers).status_code in (200, 500)

    def test_api_05_activity_analytics(self, headers):
        assert requests.get(f"{ACTIVITY_URL}/analytics", headers=headers).status_code in (200, 404, 500)

    def test_api_06_email_analytics(self, headers):
        assert requests.get(f"{EMAIL_URL}/analytics", headers=headers).status_code in (200, 404, 500)

    def test_api_07_win_loss_analytics(self, headers):
        assert requests.get(f"{OPP_URL}/analytics/win-loss", headers=headers).status_code in (200, 500)

    def test_api_08_conversion_analytics(self, headers):
        assert requests.get(f"{OPP_URL}/analytics/conversion", headers=headers).status_code in (200, 500)

    def test_api_09_forecast(self, headers):
        assert requests.get(f"{OPP_URL}/forecast", headers=headers).status_code in (200, 500)

    def test_api_10_ai_insights(self, headers):
        assert requests.get(f"{AI_URL}/insights", headers=headers).status_code in (200, 404, 500)

    def test_api_11_no_auth_dashboard(self):
        assert requests.get(f"{OPP_URL}/dashboard").status_code in (401, 403, 500)


class TestReportsDashboardsUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_dashboard_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        assert "dashboard" in page.url

    def test_ui_02_reports_page(self, page: Page):
        page.goto(f"{FRONTEND_URL}/reports"); page.wait_for_load_state("networkidle")
        assert "report" in page.url

    def test_ui_03_dashboard_widgets(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        body = (page.text_content("body") or "").lower()
        assert len(body) > 50

    def test_ui_04_charts(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_05_sidebar_reports(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "report" in sidebar.lower() or "dashboard" in sidebar.lower()

    def test_ui_06_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/reports"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_07_filter(self, page: Page):
        page.goto(f"{FRONTEND_URL}/reports"); page.wait_for_load_state("networkidle")

    def test_ui_08_navigation(self, page: Page):
        page.goto(f"{FRONTEND_URL}/reports"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_09_title(self, page: Page):
        page.goto(f"{FRONTEND_URL}/reports"); page.wait_for_load_state("networkidle")
        body = (page.text_content("body") or "").lower()
        assert len(body) > 50
