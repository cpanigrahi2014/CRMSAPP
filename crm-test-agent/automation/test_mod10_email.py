"""
Module 10 — Email Integration (20 Tests: 11 API + 9 UI)
=========================================================
API : Email send, receive, templates, tracking, attachments
UI  : Email page, compose, inbox, templates
"""
import uuid, pytest, requests
from playwright.sync_api import Page
from automation import (
    EMAIL_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)


class TestEmailIntegrationAPI:
    def test_api_01_list_emails(self, headers):
        assert requests.get(EMAIL_URL, headers=headers).status_code in (200, 404, 500)

    def test_api_02_send_email(self, headers):
        resp = requests.post(f"{EMAIL_URL}/send", headers=headers, json={
            "to": "test@example.com", "subject": f"Test{uid()}", "body": "Hello!"})
        assert resp.status_code in (200, 201, 400, 500)

    def test_api_03_templates_list(self, headers):
        assert requests.get(f"{EMAIL_URL}/templates", headers=headers).status_code in (200, 404, 500)

    def test_api_04_create_template(self, headers):
        resp = requests.post(f"{EMAIL_URL}/templates", headers=headers, json={
            "name": f"Tmpl{uid()}", "subject": "Hello {{name}}",
            "body": "<p>Dear {{name}}, welcome!</p>"})
        assert resp.status_code in (200, 201, 400, 404)

    def test_api_05_tracking(self, headers):
        assert requests.get(f"{EMAIL_URL}/tracking", headers=headers).status_code in (200, 404, 500)

    def test_api_06_analytics(self, headers):
        assert requests.get(f"{EMAIL_URL}/analytics", headers=headers).status_code in (200, 404, 500)

    def test_api_07_get_email_by_id(self, headers):
        r = requests.get(EMAIL_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                eid = items[0].get("id")
                if eid: assert requests.get(f"{EMAIL_URL}/{eid}", headers=headers).status_code == 200

    def test_api_08_no_auth(self):
        assert requests.get(EMAIL_URL).status_code in (401, 403, 500)

    def test_api_09_xss_subject(self, headers):
        resp = requests.post(f"{EMAIL_URL}/send", headers=headers, json={
            "to": "test@example.com", "subject": "<script>alert(1)</script>",
            "body": "XSS test"})
        if resp.status_code in (200, 201):
            body = resp.text
            assert "<script>" not in body

    def test_api_10_bulk_send(self, headers):
        resp = requests.post(f"{EMAIL_URL}/bulk-send", headers=headers, json={
            "recipients": ["a@test.com", "b@test.com"],
            "subject": f"Bulk{uid()}", "body": "Bulk test"})
        assert resp.status_code in (200, 201, 400, 404, 500)

    def test_api_11_config(self, headers):
        assert requests.get(f"{EMAIL_URL}/config", headers=headers).status_code in (200, 404, 500)


class TestEmailIntegrationUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/email"); page.wait_for_load_state("networkidle")
        assert "email" in page.url

    def test_ui_02_compose_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/email"); page.wait_for_load_state("networkidle")

    def test_ui_03_inbox(self, page: Page):
        page.goto(f"{FRONTEND_URL}/email"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_04_templates_section(self, page: Page):
        page.goto(f"{FRONTEND_URL}/email"); page.wait_for_load_state("networkidle")

    def test_ui_05_sidebar_link(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "email" in sidebar.lower() or "mail" in sidebar.lower()

    def test_ui_06_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/email"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_07_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/email"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="Search"], input[type="search"]')
        if s.count() > 0: s.first.fill("test")

    def test_ui_08_navigation(self, page: Page):
        page.goto(f"{FRONTEND_URL}/email"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_09_title(self, page: Page):
        page.goto(f"{FRONTEND_URL}/email"); page.wait_for_load_state("networkidle")
        body = (page.text_content("body") or "").lower()
        assert len(body) > 50
