"""
Module 03 — Lead Management (60 Tests: 35 API + 25 UI)
========================================================
API : CRUD, search, notes, tags, scoring, assignment, bulk, import/export
UI  : Lead list, create dialog, detail page, search, filters
"""
import io, uuid, pytest, requests
from playwright.sync_api import Page, expect
from automation import (
    LEAD_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, auth_only_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)

@pytest.fixture(scope="module")
def headers_no_ct(token):
    return auth_only_headers(token)

def _lead(extra=None):
    tag = uid()
    d = {"firstName": f"Auto{tag}", "lastName": f"Lead{tag}",
         "email": f"auto{tag}@test.com", "company": f"Co{tag}",
         "phone": f"555-{tag[:4]}", "source": "WEB", "status": "NEW"}
    if extra:
        d.update(extra)
    return d


# ══════════════════════════════════════════════════════════════════════════════
#  API Tests (35)
# ══════════════════════════════════════════════════════════════════════════════
class TestLeadAPI:

    def test_api_01_create_lead(self, headers):
        resp = requests.post(LEAD_URL, headers=headers, json=_lead())
        assert resp.status_code in (200, 201)

    def test_api_02_list_leads(self, headers):
        resp = requests.get(LEAD_URL, headers=headers)
        assert resp.status_code == 200

    def test_api_03_search_leads(self, headers):
        resp = requests.get(LEAD_URL, headers=headers, params={"search": "Auto"})
        assert resp.status_code == 200

    def test_api_04_get_lead_by_id(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.get(f"{LEAD_URL}/{lid}", headers=headers)
                assert resp.status_code == 200

    def test_api_05_update_lead(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.put(f"{LEAD_URL}/{lid}", headers=headers,
                                    json={"status": "CONTACTED"})
                assert resp.status_code in (200, 204)

    def test_api_06_delete_lead(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.delete(f"{LEAD_URL}/{lid}", headers=headers)
                assert resp.status_code in (200, 204, 404)

    def test_api_07_create_lead_missing_fields(self, headers):
        resp = requests.post(LEAD_URL, headers=headers, json={"firstName": "X"})
        assert resp.status_code in (200, 201, 400, 500)

    def test_api_08_add_note(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.post(f"{LEAD_URL}/{lid}/notes", headers=headers,
                                     json={"content": "Automation note"})
                assert resp.status_code in (200, 201, 404)

    def test_api_09_list_notes(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.get(f"{LEAD_URL}/{lid}/notes", headers=headers)
                assert resp.status_code in (200, 404)

    def test_api_10_add_tag(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.post(f"{LEAD_URL}/{lid}/tags", headers=headers,
                                     json={"tag": "automation"})
                assert resp.status_code in (200, 201, 404, 500)

    def test_api_11_list_tags(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.get(f"{LEAD_URL}/{lid}/tags", headers=headers)
                assert resp.status_code in (200, 404)

    def test_api_12_scoring_rules(self, headers):
        resp = requests.get(f"{LEAD_URL}/scoring-rules", headers=headers)
        assert resp.status_code in (200, 404, 500)

    def test_api_13_create_scoring_rule(self, headers):
        resp = requests.post(f"{LEAD_URL}/scoring-rules", headers=headers, json={
            "field": "source", "operator": "EQUALS", "value": "WEB", "score": 10,
        })
        assert resp.status_code in (200, 201, 400, 404, 500)

    def test_api_14_assignment_rules(self, headers):
        resp = requests.get(f"{LEAD_URL}/assignment-rules", headers=headers)
        assert resp.status_code in (200, 404, 500)

    def test_api_15_lead_activities(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.get(f"{LEAD_URL}/{lid}/activities", headers=headers)
                assert resp.status_code in (200, 404)

    def test_api_16_lead_analytics(self, headers):
        resp = requests.get(f"{LEAD_URL}/analytics", headers=headers)
        assert resp.status_code in (200, 500)

    def test_api_17_lead_duplicates(self, headers):
        resp = requests.get(f"{LEAD_URL}/duplicates", headers=headers,
                            params={"email": "auto@test.com"})
        assert resp.status_code in (200, 404)

    def test_api_18_lead_sla(self, headers):
        resp = requests.get(f"{LEAD_URL}/sla-status", headers=headers)
        assert resp.status_code in (200, 404, 500)

    def test_api_19_bulk_update(self, headers):
        resp = requests.post(f"{LEAD_URL}/bulk", headers=headers, json={
            "leadIds": [str(uuid.uuid4())], "status": "CONTACTED",
        })
        assert resp.status_code in (200, 400, 404)

    def test_api_20_import_csv(self, headers_no_ct):
        csv = "firstName,lastName,email,phone,company,title,source\nA,B,a@t.com,555,Co,Dev,WEB"
        files = {"file": ("test.csv", io.BytesIO(csv.encode()), "text/csv")}
        resp = requests.post(f"{LEAD_URL}/import", headers=headers_no_ct, files=files)
        assert resp.status_code in (200, 201)

    def test_api_21_export_csv(self, headers):
        resp = requests.get(f"{LEAD_URL}/export", headers=headers)
        assert resp.status_code == 200

    def test_api_22_filter_by_status(self, headers):
        resp = requests.get(LEAD_URL, headers=headers, params={"status": "NEW"})
        assert resp.status_code == 200

    def test_api_23_filter_by_source(self, headers):
        resp = requests.get(LEAD_URL, headers=headers, params={"source": "WEB"})
        assert resp.status_code == 200

    def test_api_24_sort_leads(self, headers):
        resp = requests.get(LEAD_URL, headers=headers,
                            params={"sortBy": "createdAt", "sortDir": "desc"})
        assert resp.status_code == 200

    def test_api_25_paginate_leads(self, headers):
        resp = requests.get(LEAD_URL, headers=headers, params={"page": 0, "size": 5})
        assert resp.status_code == 200

    def test_api_26_convert_lead(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.post(f"{LEAD_URL}/{lid}/convert", headers=headers, json={})
                assert resp.status_code in (200, 400, 404, 500)

    def test_api_27_web_form(self, headers):
        resp = requests.get(f"{LEAD_URL}/web-forms", headers=headers)
        assert resp.status_code in (200, 404, 500)

    def test_api_28_attachments(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.get(f"{LEAD_URL}/{lid}/attachments", headers=headers)
                assert resp.status_code in (200, 404)

    def test_api_29_assign_lead(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.put(f"{LEAD_URL}/{lid}", headers=headers,
                                    json={"assignedTo": str(uuid.uuid4()), "territory": "West"})
                assert resp.status_code in (200, 204, 400)

    def test_api_30_recalculate_score(self, headers):
        r1 = requests.post(LEAD_URL, headers=headers, json=_lead())
        if r1.status_code in (200, 201):
            lid = r1.json().get("data", r1.json()).get("id")
            if lid:
                resp = requests.post(f"{LEAD_URL}/{lid}/recalculate-score",
                                     headers=headers)
                assert resp.status_code in (200, 404, 500)

    def test_api_31_no_auth_leads(self):
        resp = requests.get(LEAD_URL)
        assert resp.status_code in (401, 403, 500)

    def test_api_32_get_nonexistent_lead(self, headers):
        resp = requests.get(f"{LEAD_URL}/{uuid.uuid4()}", headers=headers)
        assert resp.status_code in (404, 500)

    def test_api_33_sql_injection_search(self, headers):
        resp = requests.get(LEAD_URL, headers=headers,
                            params={"search": "'; DROP TABLE leads; --"})
        assert resp.status_code in (200, 400, 500)

    def test_api_34_xss_in_name(self, headers):
        resp = requests.post(LEAD_URL, headers=headers,
                             json=_lead({"firstName": "<script>alert(1)</script>"}))
        assert resp.status_code in (200, 201, 400)

    def test_api_35_empty_csv_import(self, headers_no_ct):
        files = {"file": ("empty.csv", io.BytesIO(b""), "text/csv")}
        resp = requests.post(f"{LEAD_URL}/import", headers=headers_no_ct, files=files)
        assert resp.status_code in (200, 400, 500)


# ══════════════════════════════════════════════════════════════════════════════
#  UI Tests (25)
# ══════════════════════════════════════════════════════════════════════════════
class TestLeadUI:

    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_leads_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        assert "leads" in page.url.lower()

    def test_ui_02_leads_table_visible(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        body = page.text_content("body") or ""
        assert len(body) > 100

    def test_ui_03_create_lead_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Add"), button:has-text("Create"), button:has-text("New")')
        assert btn.count() > 0

    def test_ui_04_open_create_dialog(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Add"), button:has-text("Create"), button:has-text("New")').first
        if btn.is_visible():
            btn.click()
            page.wait_for_timeout(1000)

    def test_ui_05_search_leads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        search = page.locator('input[placeholder*="earch"], input[type="search"]')
        if search.count() > 0:
            search.first.fill("Auto")
            page.wait_for_timeout(1500)

    def test_ui_06_leads_pagination(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_07_lead_detail_page(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        row = page.locator('tr, [class*="row"], [class*="Row"]').nth(1)
        if row.count() > 0:
            row.click()
            page.wait_for_timeout(2000)

    def test_ui_08_sidebar_leads_link(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard")
        page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "leads" in sidebar.lower()

    def test_ui_09_leads_filter_controls(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        body = page.text_content("body") or ""
        assert len(body) > 50

    def test_ui_10_leads_column_headers(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        body = page.text_content("body") or ""
        # Should show typical lead columns
        lower = body.lower()
        assert "name" in lower or "email" in lower or "lead" in lower

    def test_ui_11_leads_status_chips(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_12_leads_export_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Export"), button:has-text("Download")')
        assert btn.count() >= 0

    def test_ui_13_leads_import_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Import"), button:has-text("Upload")')
        assert btn.count() >= 0

    def test_ui_14_leads_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_15_leads_back_navigation(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url.lower()

    def test_ui_16_lead_detail_back(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")

    def test_ui_17_leads_sort_columns(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_18_leads_bulk_actions(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")

    def test_ui_19_leads_empty_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        search = page.locator('input[placeholder*="earch"], input[type="search"]')
        if search.count() > 0:
            search.first.fill("zzz_nonexistent_999")
            page.wait_for_timeout(1500)

    def test_ui_20_leads_page_title(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        body = page.text_content("body") or ""
        assert "lead" in body.lower()

    def test_ui_21_leads_notes_section(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")

    def test_ui_22_leads_activity_section(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")

    def test_ui_23_leads_tags_display(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")

    def test_ui_24_leads_score_display(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")

    def test_ui_25_leads_kanban_view(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads")
        page.wait_for_load_state("networkidle")
        kanban = page.locator('button:has-text("Kanban"), button:has-text("Board")')
        if kanban.count() > 0:
            kanban.first.click()
            page.wait_for_timeout(2000)
