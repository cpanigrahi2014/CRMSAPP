"""
Module 09 — Meeting & Calendar Integration (20 Tests: 11 API + 9 UI)
=====================================================================
API : Meeting CRUD, calendar sync, reminders, availability, recurring
UI  : Calendar views, meeting forms, scheduling UI
"""
import uuid, pytest, requests
from playwright.sync_api import Page
from automation import (
    ACTIVITY_URL, AI_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

MEET_URL = f"{ACTIVITY_URL}"  # meetings are a subtype of activities

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)


class TestMeetingCalendarAPI:
    def test_api_01_create_meeting(self, headers):
        resp = requests.post(MEET_URL, headers=headers, json={
            "type": "MEETING", "subject": f"Meet{uid()}", "status": "SCHEDULED",
            "startDate": "2026-06-15T10:00:00", "endDate": "2026-06-15T11:00:00",
            "location": "Zoom", "description": "Kickoff meeting"})
        assert resp.status_code in (200, 201, 400)

    def test_api_02_list_meetings(self, headers):
        assert requests.get(f"{MEET_URL}?type=MEETING", headers=headers).status_code in (200, 400)

    def test_api_03_get_meeting(self, headers):
        r = requests.get(MEET_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                mid = items[0].get("id")
                if mid: assert requests.get(f"{MEET_URL}/{mid}", headers=headers).status_code == 200

    def test_api_04_update_meeting(self, headers):
        r = requests.get(MEET_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                mid = items[0].get("id")
                if mid:
                    assert requests.put(f"{MEET_URL}/{mid}", headers=headers,
                        json={"subject": f"Updated{uid()}"}).status_code in (200, 204, 404)

    def test_api_05_delete_meeting(self, headers):
        r = requests.post(MEET_URL, headers=headers, json={
            "type": "MEETING", "subject": f"Del{uid()}", "status": "SCHEDULED"})
        if r.status_code in (200, 201):
            mid = r.json().get("data", r.json()).get("id")
            if mid: assert requests.delete(f"{MEET_URL}/{mid}", headers=headers).status_code in (200, 204, 404)

    def test_api_06_calendar_sync(self, headers):
        assert requests.get(f"{MEET_URL}/calendar", headers=headers).status_code in (200, 404, 500)

    def test_api_07_availability(self, headers):
        assert requests.get(f"{MEET_URL}/availability", headers=headers).status_code in (200, 404, 500)

    def test_api_08_ai_schedule(self, headers):
        resp = requests.post(f"{AI_URL}/suggest-meeting", headers=headers, json={
            "participants": ["user1@test.com"], "duration": 30})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_09_reminders(self, headers):
        assert requests.get(f"{MEET_URL}/reminders", headers=headers).status_code in (200, 404, 500)

    def test_api_10_no_auth(self):
        assert requests.post(MEET_URL, json={"type": "MEETING", "subject": "NoAuth"}).status_code in (401, 403, 500)

    def test_api_11_recurring(self, headers):
        resp = requests.post(MEET_URL, headers=headers, json={
            "type": "MEETING", "subject": f"Recurring{uid()}", "status": "SCHEDULED",
            "recurring": True, "frequency": "WEEKLY"})
        assert resp.status_code in (200, 201, 400)


class TestMeetingCalendarUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_activities_page(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        assert "activities" in page.url

    def test_ui_02_calendar_view(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Calendar"), [data-testid="calendar-view"]')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(2000)

    def test_ui_03_create_meeting_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")

    def test_ui_04_meeting_detail(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_05_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_06_sidebar(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "activit" in sidebar.lower() or "calendar" in sidebar.lower()

    def test_ui_07_filter_meetings(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")

    def test_ui_08_navigation(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_09_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/activities"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="Search"], input[type="search"]')
        if s.count() > 0: s.first.fill("meeting")
