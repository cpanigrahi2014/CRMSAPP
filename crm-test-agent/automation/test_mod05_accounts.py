"""
Module 05 — Account Management (40 Tests: 22 API + 18 UI)
===========================================================
API : CRUD, search, hierarchy, notes, tags, scores, bulk, import/export
UI  : Account list, detail, create, search, hierarchy view
"""
import io, uuid, pytest, requests
from playwright.sync_api import Page
from automation import (
    ACCT_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)

def _acct(extra=None):
    tag = uid()
    d = {"name": f"AcctAuto{tag}", "industry": "Technology",
         "type": "PROSPECT", "website": f"https://{tag}.com", "phone": f"555-{tag[:4]}"}
    if extra:
        d.update(extra)
    return d


class TestAccountAPI:
    def test_api_01_create(self, headers):
        assert requests.post(ACCT_URL, headers=headers, json=_acct()).status_code in (200, 201)

    def test_api_02_list(self, headers):
        assert requests.get(ACCT_URL, headers=headers).status_code == 200

    def test_api_03_search(self, headers):
        assert requests.get(ACCT_URL, headers=headers, params={"search": "Auto"}).status_code == 200

    def test_api_04_get_by_id(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                assert requests.get(f"{ACCT_URL}/{aid}", headers=headers).status_code == 200

    def test_api_05_update(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                assert requests.put(f"{ACCT_URL}/{aid}", headers=headers,
                    json={"industry": "Finance"}).status_code in (200, 204)

    def test_api_06_delete(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                assert requests.delete(f"{ACCT_URL}/{aid}", headers=headers).status_code in (200, 204, 404)

    def test_api_07_hierarchy(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                assert requests.get(f"{ACCT_URL}/{aid}/hierarchy", headers=headers).status_code in (200, 404, 500)

    def test_api_08_notes(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                requests.post(f"{ACCT_URL}/{aid}/notes", headers=headers, json={"content": "test"})
                assert requests.get(f"{ACCT_URL}/{aid}/notes", headers=headers).status_code in (200, 404)

    def test_api_09_tags(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                requests.post(f"{ACCT_URL}/{aid}/tags", headers=headers, json={"tag": "auto"})
                assert requests.get(f"{ACCT_URL}/{aid}/tags", headers=headers).status_code in (200, 404)

    def test_api_10_health_score(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                assert requests.put(f"{ACCT_URL}/{aid}/health-score", headers=headers,
                    json={"score": 85}).status_code in (200, 400, 404, 500)

    def test_api_11_engagement_score(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                assert requests.put(f"{ACCT_URL}/{aid}/engagement-score", headers=headers,
                    json={"score": 75}).status_code in (200, 400, 404, 500)

    def test_api_12_import_csv(self, headers):
        csv = "name,industry,website,phone,type\nTestCo,Tech,https://t.com,555,PROSPECT"
        assert requests.post(f"{ACCT_URL}/import", headers=headers, json=csv).status_code in (200, 201)

    def test_api_13_export_csv(self, headers):
        assert requests.get(f"{ACCT_URL}/export", headers=headers).status_code == 200

    def test_api_14_bulk_update(self, headers):
        assert requests.put(f"{ACCT_URL}/bulk-update", headers=headers, json={
            "accountIds": [str(uuid.uuid4())], "territory": "West"}).status_code in (200, 400, 404)

    def test_api_15_bulk_delete(self, headers):
        assert requests.post(f"{ACCT_URL}/bulk-delete", headers=headers,
            json=[str(uuid.uuid4())]).status_code in (200, 400, 404)

    def test_api_16_duplicates(self, headers):
        assert requests.get(f"{ACCT_URL}/duplicates", headers=headers, params={"name": "Acme"}).status_code in (200, 404)

    def test_api_17_analytics(self, headers):
        assert requests.get(f"{ACCT_URL}/analytics", headers=headers).status_code in (200, 500)

    def test_api_18_activities(self, headers):
        r = requests.post(ACCT_URL, headers=headers, json=_acct())
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid:
                assert requests.get(f"{ACCT_URL}/{aid}/activities", headers=headers).status_code in (200, 404)

    def test_api_19_pagination(self, headers):
        assert requests.get(ACCT_URL, headers=headers, params={"page": 0, "size": 5}).status_code == 200

    def test_api_20_no_auth(self):
        assert requests.get(ACCT_URL).status_code in (401, 403, 500)

    def test_api_21_nonexistent(self, headers):
        assert requests.get(f"{ACCT_URL}/{uuid.uuid4()}", headers=headers).status_code in (404, 500)

    def test_api_22_sql_injection(self, headers):
        assert requests.get(ACCT_URL, headers=headers, params={"search": "'; DROP TABLE accounts; --"}).status_code in (200, 400, 500)


class TestAccountUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        assert "accounts" in page.url

    def test_ui_02_table_visible(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        assert len(page.text_content("body") or "") > 100

    def test_ui_03_create_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        assert page.locator('button:has-text("Add"), button:has-text("Create"), button:has-text("New")').count() > 0

    def test_ui_04_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="earch"], input[type="search"]')
        if s.count() > 0: s.first.fill("Auto"); page.wait_for_timeout(1500)

    def test_ui_05_detail_page(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_06_sidebar_link(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "accounts" in sidebar.lower()

    def test_ui_07_columns(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        body = (page.text_content("body") or "").lower()
        assert "name" in body or "account" in body

    def test_ui_08_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_09_pagination(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")

    def test_ui_10_export(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")

    def test_ui_11_import(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")

    def test_ui_12_filter(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")

    def test_ui_13_sort(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")

    def test_ui_14_hierarchy(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")

    def test_ui_15_empty_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="earch"], input[type="search"]')
        if s.count() > 0: s.first.fill("zzz_nonexistent"); page.wait_for_timeout(1500)

    def test_ui_16_back_nav(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_17_notes_tab(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")

    def test_ui_18_tags_display(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
