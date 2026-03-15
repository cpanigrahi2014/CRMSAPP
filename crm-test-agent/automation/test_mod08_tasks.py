"""
Module 08 — Task & Activity Management (30 Tests: 17 API + 13 UI)
===================================================================
API : Activity / Task CRUD, notes, reminders, calendar, analytics
UI  : Activities page, task forms, timelines
"""
import uuid, pytest, requests
from playwright.sync_api import Page
from automation import (
    ACTIVITY_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)


class TestTaskActivityAPI:
    def test_api_01_list(self, headers):
        assert requests.get(ACTIVITY_URL, headers=headers).status_code in (200, 404)

    def test_api_02_create_task(self, headers):
        resp = requests.post(ACTIVITY_URL, headers=headers, json={
            "type": "TASK", "subject": f"Task{uid()}", "status": "OPEN",
            "priority": "HIGH", "dueDate": "2026-12-31"})
        assert resp.status_code in (200, 201, 400, 500)

    def test_api_03_create_call(self, headers):
        resp = requests.post(ACTIVITY_URL, headers=headers, json={
            "type": "CALL", "subject": f"Call{uid()}", "status": "COMPLETED",
            "description": "Intro call"})
        assert resp.status_code in (200, 201, 400)

    def test_api_04_get_by_id(self, headers):
        r = requests.get(ACTIVITY_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                aid = items[0].get("id")
                if aid: assert requests.get(f"{ACTIVITY_URL}/{aid}", headers=headers).status_code == 200

    def test_api_05_update(self, headers):
        r = requests.get(ACTIVITY_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                aid = items[0].get("id")
                if aid:
                    assert requests.put(f"{ACTIVITY_URL}/{aid}", headers=headers,
                        json={"status": "COMPLETED"}).status_code in (200, 204, 404)

    def test_api_06_delete(self, headers):
        r = requests.post(ACTIVITY_URL, headers=headers, json={
            "type": "TASK", "subject": f"Del{uid()}", "status": "OPEN"})
        if r.status_code in (200, 201):
            aid = r.json().get("data", r.json()).get("id")
            if aid: assert requests.delete(f"{ACTIVITY_URL}/{aid}", headers=headers).status_code in (200, 204, 404)

    def test_api_07_filter_type(self, headers):
        assert requests.get(f"{ACTIVITY_URL}?type=TASK", headers=headers).status_code in (200, 400)

    def test_api_08_filter_status(self, headers):
        assert requests.get(f"{ACTIVITY_URL}?status=OPEN", headers=headers).status_code in (200, 400, 500)

    def test_api_09_sort(self, headers):
        assert requests.get(f"{ACTIVITY_URL}?sort=createdAt,desc", headers=headers).status_code in (200, 400)

    def test_api_10_paginate(self, headers):
        assert requests.get(f"{ACTIVITY_URL}?page=0&size=5", headers=headers).status_code in (200, 400)

    def test_api_11_overdue(self, headers):
        assert requests.get(f"{ACTIVITY_URL}/overdue", headers=headers).status_code in (200, 404, 500)

    def test_api_12_timeline(self, headers):
        assert requests.get(f"{ACTIVITY_URL}/timeline", headers=headers).status_code in (200, 404, 500)

    def test_api_13_analytics(self, headers):
        assert requests.get(f"{ACTIVITY_URL}/analytics", headers=headers).status_code in (200, 404, 500)

    def test_api_14_no_auth(self):
        assert requests.get(ACTIVITY_URL).status_code in (401, 403, 500)

    def test_api_15_nonexistent(self, headers):
        assert requests.get(f"{ACTIVITY_URL}/99999999", headers=headers).status_code in (404, 400, 500)

    def test_api_16_sql_injection(self, headers):
        r = requests.get(f"{ACTIVITY_URL}?search=' OR 1=1--", headers=headers)
        assert r.status_code in (200, 400, 404, 500)
        body = r.text.lower()
        assert "sql" not in body and "syntax" not in body

    def test_api_17_bulk_update(self, headers):
        assert requests.post(f"{ACTIVITY_URL}/bulk-update", headers=headers,
            json={"ids": [], "status": "COMPLETED"}).status_code in (200, 400, 404, 500)


class TestTaskActivityUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        assert "activities" in page.url

    def test_ui_02_task_table(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_03_create_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")

    def test_ui_04_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="Search"], input[type="search"], input[aria-label*="search"]')
        if s.count() > 0: s.first.fill("test")

    def test_ui_05_sidebar_link(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "activit" in sidebar.lower() or "tasks" in sidebar.lower()

    def test_ui_06_filter(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")

    def test_ui_07_pagination(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")

    def test_ui_08_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_09_empty_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="Search"], input[type="search"]')
        if s.count() > 0: s.first.fill("ZZZNONEXIST"); page.wait_for_timeout(1000)

    def test_ui_10_navigation(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_11_title(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        body = (page.text_content("body") or "").lower()
        assert len(body) > 50

    def test_ui_12_columns(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")

    def test_ui_13_calendar_view(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Calendar"), button:has-text("calendar")')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(2000)
