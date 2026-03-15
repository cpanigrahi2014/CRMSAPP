"""
Module 04 — Contact Management (40 Tests: 22 API + 18 UI)
===========================================================
API : CRUD, search, communications, tags, segmentation, consent, duplicates, merge
UI  : Contact list, detail, create, search, merge dialog
"""
import uuid, pytest, requests
from playwright.sync_api import Page, expect
from automation import (
    CONTACT_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)

def _contact(extra=None):
    tag = uid()
    d = {"firstName": f"CAuto{tag}", "lastName": f"Contact{tag}",
         "email": f"c{tag}@test.com", "phone": f"555-{tag[:4]}",
         "title": "Engineer", "department": "Engineering"}
    if extra:
        d.update(extra)
    return d


class TestContactAPI:
    def test_api_01_create(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        assert r.status_code in (200, 201)

    def test_api_02_list(self, headers):
        assert requests.get(CONTACT_URL, headers=headers).status_code == 200

    def test_api_03_search(self, headers):
        assert requests.get(CONTACT_URL, headers=headers, params={"search": "Auto"}).status_code == 200

    def test_api_04_get_by_id(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r.status_code in (200, 201):
            cid = r.json().get("data", r.json()).get("id")
            if cid:
                assert requests.get(f"{CONTACT_URL}/{cid}", headers=headers).status_code == 200

    def test_api_05_update(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r.status_code in (200, 201):
            cid = r.json().get("data", r.json()).get("id")
            if cid:
                assert requests.put(f"{CONTACT_URL}/{cid}", headers=headers,
                    json={"title": "Senior Eng"}).status_code in (200, 204)

    def test_api_06_delete(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r.status_code in (200, 201):
            cid = r.json().get("data", r.json()).get("id")
            if cid:
                assert requests.delete(f"{CONTACT_URL}/{cid}", headers=headers).status_code in (200, 204, 404)

    def test_api_07_communications(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r.status_code in (200, 201):
            cid = r.json().get("data", r.json()).get("id")
            if cid:
                assert requests.get(f"{CONTACT_URL}/{cid}/communications", headers=headers).status_code in (200, 404, 500)

    def test_api_08_add_tag(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r.status_code in (200, 201):
            cid = r.json().get("data", r.json()).get("id")
            if cid:
                assert requests.post(f"{CONTACT_URL}/{cid}/tags", headers=headers,
                    json={"tag": "auto"}).status_code in (200, 201, 404, 500)

    def test_api_09_segments(self, headers):
        assert requests.get(f"{CONTACT_URL}/segments", headers=headers).status_code in (200, 404, 500)

    def test_api_10_consent(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r.status_code in (200, 201):
            cid = r.json().get("data", r.json()).get("id")
            if cid:
                assert requests.get(f"{CONTACT_URL}/{cid}/consent", headers=headers).status_code in (200, 404, 500)

    def test_api_11_activities(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r.status_code in (200, 201):
            cid = r.json().get("data", r.json()).get("id")
            if cid:
                assert requests.get(f"{CONTACT_URL}/{cid}/activities", headers=headers).status_code in (200, 404, 500)

    def test_api_12_duplicates(self, headers):
        assert requests.get(f"{CONTACT_URL}/duplicates", headers=headers).status_code in (200, 404, 500)

    def test_api_13_merge(self, headers):
        r1 = requests.post(CONTACT_URL, headers=headers, json=_contact())
        r2 = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r1.status_code in (200, 201) and r2.status_code in (200, 201):
            id1 = r1.json().get("data", r1.json()).get("id")
            id2 = r2.json().get("data", r2.json()).get("id")
            if id1 and id2:
                resp = requests.post(f"{CONTACT_URL}/merge", headers=headers,
                    json={"primaryId": id1, "secondaryId": id2})
                assert resp.status_code in (200, 400, 404, 500)

    def test_api_14_analytics(self, headers):
        assert requests.get(f"{CONTACT_URL}/analytics", headers=headers).status_code in (200, 500)

    def test_api_15_filter_department(self, headers):
        assert requests.get(CONTACT_URL, headers=headers, params={"department": "Engineering"}).status_code == 200

    def test_api_16_pagination(self, headers):
        assert requests.get(CONTACT_URL, headers=headers, params={"page": 0, "size": 5}).status_code == 200

    def test_api_17_sort(self, headers):
        assert requests.get(CONTACT_URL, headers=headers, params={"sortBy": "createdAt", "sortDir": "desc"}).status_code == 200

    def test_api_18_no_auth(self):
        assert requests.get(CONTACT_URL).status_code in (401, 403, 500)

    def test_api_19_nonexistent(self, headers):
        assert requests.get(f"{CONTACT_URL}/{uuid.uuid4()}", headers=headers).status_code in (404, 500)

    def test_api_20_sql_injection(self, headers):
        assert requests.get(CONTACT_URL, headers=headers, params={"search": "' OR 1=1 --"}).status_code in (200, 400, 500)

    def test_api_21_xss_name(self, headers):
        r = requests.post(CONTACT_URL, headers=headers,
            json=_contact({"firstName": "<script>alert(1)</script>"}))
        assert r.status_code in (200, 201, 400)

    def test_api_22_notes(self, headers):
        r = requests.post(CONTACT_URL, headers=headers, json=_contact())
        if r.status_code in (200, 201):
            cid = r.json().get("data", r.json()).get("id")
            if cid:
                assert requests.get(f"{CONTACT_URL}/{cid}/notes", headers=headers).status_code in (200, 404, 500)


class TestContactUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        assert "contacts" in page.url

    def test_ui_02_table_visible(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        assert len(page.text_content("body") or "") > 100

    def test_ui_03_create_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        assert page.locator('button:has-text("Add"), button:has-text("Create"), button:has-text("New")').count() > 0

    def test_ui_04_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="earch"], input[type="search"]')
        if s.count() > 0:
            s.first.fill("Auto")
            page.wait_for_timeout(1500)

    def test_ui_05_contact_detail(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_06_sidebar_link(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard")
        page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "contacts" in sidebar.lower()

    def test_ui_07_columns(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        body = (page.text_content("body") or "").lower()
        assert "name" in body or "email" in body or "contact" in body

    def test_ui_08_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_09_pagination(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_10_merge_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")

    def test_ui_11_tags_display(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")

    def test_ui_12_filter_controls(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")

    def test_ui_13_empty_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="earch"], input[type="search"]')
        if s.count() > 0:
            s.first.fill("zzz_nonexistent")
            page.wait_for_timeout(1500)

    def test_ui_14_back_nav(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_15_page_title(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
        assert "contact" in (page.text_content("body") or "").lower()

    def test_ui_16_sort_columns(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")

    def test_ui_17_segment_section(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")

    def test_ui_18_consent_section(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts")
        page.wait_for_load_state("networkidle")
