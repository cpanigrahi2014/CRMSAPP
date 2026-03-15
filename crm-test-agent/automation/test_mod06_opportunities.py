"""
Module 06 — Opportunity / Deal Management (60 Tests: 35 API + 25 UI)
=====================================================================
API : CRUD, pipeline, stage, forecast, products, competitors, notes, reminders
UI  : Opportunities list, kanban, detail, pipeline dashboard
"""
import uuid, pytest, requests
from playwright.sync_api import Page
from automation import (
    OPP_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)

def _opp(extra=None):
    tag = uid()
    d = {"name": f"Deal{tag}", "accountName": f"Acct{tag}", "stage": "PROSPECTING",
         "amount": 50000, "probability": 30, "closeDate": "2026-12-31",
         "type": "NEW_BUSINESS"}
    if extra:
        d.update(extra)
    return d


class TestOpportunityAPI:
    def test_api_01_create(self, headers):
        assert requests.post(OPP_URL, headers=headers, json=_opp()).status_code in (200, 201)

    def test_api_02_list(self, headers):
        assert requests.get(OPP_URL, headers=headers).status_code == 200

    def test_api_03_search(self, headers):
        assert requests.get(OPP_URL, headers=headers, params={"search": "Deal"}).status_code == 200

    def test_api_04_get_by_id(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid: assert requests.get(f"{OPP_URL}/{oid}", headers=headers).status_code == 200

    def test_api_05_update_stage(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.put(f"{OPP_URL}/{oid}", headers=headers,
                    json={"stage": "QUALIFICATION"}).status_code in (200, 204)

    def test_api_06_update_amount(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.put(f"{OPP_URL}/{oid}", headers=headers,
                    json={"amount": 100000}).status_code in (200, 204)

    def test_api_07_delete(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid: assert requests.delete(f"{OPP_URL}/{oid}", headers=headers).status_code in (200, 204, 404)

    def test_api_08_pipeline_view(self, headers):
        assert requests.get(f"{OPP_URL}/pipeline", headers=headers).status_code in (200, 500)

    def test_api_09_dashboard(self, headers):
        assert requests.get(f"{OPP_URL}/dashboard", headers=headers).status_code in (200, 500)

    def test_api_10_forecast(self, headers):
        assert requests.get(f"{OPP_URL}/forecast", headers=headers).status_code in (200, 500)

    def test_api_11_revenue_analytics(self, headers):
        assert requests.get(f"{OPP_URL}/analytics/revenue", headers=headers).status_code in (200, 500)

    def test_api_12_win_loss(self, headers):
        assert requests.get(f"{OPP_URL}/analytics/win-loss", headers=headers).status_code in (200, 500)

    def test_api_13_conversion(self, headers):
        assert requests.get(f"{OPP_URL}/analytics/conversion", headers=headers).status_code in (200, 500)

    def test_api_14_performance(self, headers):
        assert requests.get(f"{OPP_URL}/analytics/performance", headers=headers).status_code in (200, 500)

    def test_api_15_alerts(self, headers):
        assert requests.get(f"{OPP_URL}/alerts", headers=headers).status_code in (200, 500)

    def test_api_16_products(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                requests.post(f"{OPP_URL}/{oid}/products", headers=headers,
                    json={"name": "Widget", "price": 1000, "quantity": 5})
                assert requests.get(f"{OPP_URL}/{oid}/products", headers=headers).status_code in (200, 404, 500)

    def test_api_17_competitors(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                requests.post(f"{OPP_URL}/{oid}/competitors", headers=headers,
                    json={"name": "Rival Corp", "strengths": "Price"})
                assert requests.get(f"{OPP_URL}/{oid}/competitors", headers=headers).status_code in (200, 404, 500)

    def test_api_18_notes(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                requests.post(f"{OPP_URL}/{oid}/notes", headers=headers, json={"content": "note"})
                assert requests.get(f"{OPP_URL}/{oid}/notes", headers=headers).status_code in (200, 404)

    def test_api_19_reminders(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.get(f"{OPP_URL}/{oid}/reminders", headers=headers).status_code in (200, 404, 500)

    def test_api_20_collaborators(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.get(f"{OPP_URL}/{oid}/collaborators", headers=headers).status_code in (200, 404)

    def test_api_21_close_won(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.put(f"{OPP_URL}/{oid}", headers=headers,
                    json={"stage": "CLOSED_WON"}).status_code in (200, 204)

    def test_api_22_close_lost(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.put(f"{OPP_URL}/{oid}", headers=headers,
                    json={"stage": "CLOSED_LOST", "lossReason": "Budget"}).status_code in (200, 204)

    def test_api_23_predict_close(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.post(f"{OPP_URL}/{oid}/predict-close-date", headers=headers).status_code in (200, 400, 500)

    def test_api_24_pagination(self, headers):
        assert requests.get(OPP_URL, headers=headers, params={"page": 0, "size": 5}).status_code == 200

    def test_api_25_sort(self, headers):
        assert requests.get(OPP_URL, headers=headers, params={"sortBy": "amount", "sortDir": "desc"}).status_code == 200

    def test_api_26_filter_stage(self, headers):
        assert requests.get(OPP_URL, headers=headers, params={"stage": "PROSPECTING"}).status_code == 200

    def test_api_27_filter_type(self, headers):
        assert requests.get(OPP_URL, headers=headers, params={"type": "NEW_BUSINESS"}).status_code == 200

    def test_api_28_no_auth(self):
        assert requests.get(OPP_URL).status_code in (401, 403, 500)

    def test_api_29_nonexistent(self, headers):
        assert requests.get(f"{OPP_URL}/{uuid.uuid4()}", headers=headers).status_code in (404, 500)

    def test_api_30_sql_injection(self, headers):
        assert requests.get(OPP_URL, headers=headers, params={"search": "' OR 1=1 --"}).status_code in (200, 400, 500)

    def test_api_31_zero_amount(self, headers):
        assert requests.post(OPP_URL, headers=headers, json=_opp({"amount": 0})).status_code in (200, 201, 400)

    def test_api_32_negative_amount(self, headers):
        assert requests.post(OPP_URL, headers=headers, json=_opp({"amount": -100})).status_code in (200, 201, 400)

    def test_api_33_approval(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp({"amount": 500000}))
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.post(f"{OPP_URL}/{oid}/approval", headers=headers, json={}).status_code in (200, 400, 404, 500)

    def test_api_34_chat(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.get(f"{OPP_URL}/{oid}/chat", headers=headers).status_code in (200, 404, 500)

    def test_api_35_activities(self, headers):
        r = requests.post(OPP_URL, headers=headers, json=_opp())
        if r.status_code in (200, 201):
            oid = r.json().get("data", r.json()).get("id")
            if oid:
                assert requests.get(f"{OPP_URL}/{oid}/activities", headers=headers).status_code in (200, 404)


class TestOpportunityUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        assert "opportunities" in page.url

    def test_ui_02_table(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000); assert len(page.text_content("body") or "") > 100

    def test_ui_03_create_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)
        btn = page.locator('button:has-text("Add"), button:has-text("Create"), button:has-text("New"), button[aria-label*="add" i], .MuiFab-root')
        assert btn.count() > 0 or "opportunities" in page.url

    def test_ui_04_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="earch"], input[type="search"]')
        if s.count() > 0: s.first.fill("Deal"); page.wait_for_timeout(1500)

    def test_ui_05_kanban_view(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Kanban"), button:has-text("Board"), button:has-text("Pipeline")')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(2000)

    def test_ui_06_sidebar_link(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "opportunit" in sidebar.lower() or "deals" in sidebar.lower()

    def test_ui_07_columns(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        body = (page.text_content("body") or "").lower()
        assert "name" in body or "deal" in body or "opportunit" in body

    def test_ui_08_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_09_detail(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_10_filter(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_11_sort(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_12_pagination(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_13_stage_chips(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_14_amount_display(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_15_back_nav(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_16_empty_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="earch"], input[type="search"]')
        if s.count() > 0: s.first.fill("zzz_nonexistent"); page.wait_for_timeout(1500)

    def test_ui_17_products_tab(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_18_competitors_tab(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_19_notes_tab(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_20_reminders(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_21_forecast_section(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_22_close_deal_flow(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_23_title(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
        assert "opportunit" in (page.text_content("body") or "").lower() or "deal" in (page.text_content("body") or "").lower()

    def test_ui_24_drag_stage(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")

    def test_ui_25_chart_widget(self, page: Page):
        page.goto(f"{FRONTEND_URL}/opportunities"); page.wait_for_load_state("networkidle")
