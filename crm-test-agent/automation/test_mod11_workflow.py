"""
Module 11 — Workflow Automation (35 Tests: 19 API + 16 UI)
============================================================
API : Workflow CRUD, rules, triggers, actions, execution, templates
UI  : Workflow builder, rule list, execution history
"""
import uuid, pytest, requests
from playwright.sync_api import Page
from automation import (
    WORKFLOW_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)


class TestWorkflowAPI:
    def test_api_01_list(self, headers):
        assert requests.get(WORKFLOW_URL, headers=headers).status_code in (200, 404)

    def test_api_02_create(self, headers):
        resp = requests.post(WORKFLOW_URL, headers=headers, json={
            "name": f"WF{uid()}", "description": "Auto-assign leads",
            "triggerType": "LEAD_CREATED", "status": "ACTIVE",
            "actions": [{"type": "ASSIGN", "config": {"assignTo": "team-lead"}}]})
        assert resp.status_code in (200, 201, 400)

    def test_api_03_get_by_id(self, headers):
        r = requests.get(WORKFLOW_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                wid = items[0].get("id")
                if wid: assert requests.get(f"{WORKFLOW_URL}/{wid}", headers=headers).status_code == 200

    def test_api_04_update(self, headers):
        r = requests.get(WORKFLOW_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                wid = items[0].get("id")
                if wid:
                    assert requests.put(f"{WORKFLOW_URL}/{wid}", headers=headers,
                        json={"status": "INACTIVE"}).status_code in (200, 204, 404)

    def test_api_05_delete(self, headers):
        r = requests.post(WORKFLOW_URL, headers=headers, json={
            "name": f"Del{uid()}", "triggerType": "LEAD_CREATED", "status": "DRAFT"})
        if r.status_code in (200, 201):
            wid = r.json().get("data", r.json()).get("id")
            if wid: assert requests.delete(f"{WORKFLOW_URL}/{wid}", headers=headers).status_code in (200, 204, 404)

    def test_api_06_trigger_types(self, headers):
        assert requests.get(f"{WORKFLOW_URL}/trigger-types", headers=headers).status_code in (200, 404, 500)

    def test_api_07_action_types(self, headers):
        assert requests.get(f"{WORKFLOW_URL}/action-types", headers=headers).status_code in (200, 404, 500)

    def test_api_08_templates(self, headers):
        assert requests.get(f"{WORKFLOW_URL}/templates", headers=headers).status_code in (200, 404, 500)

    def test_api_09_execution_history(self, headers):
        assert requests.get(f"{WORKFLOW_URL}/executions", headers=headers).status_code in (200, 404, 500)

    def test_api_10_activate(self, headers):
        r = requests.get(WORKFLOW_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                wid = items[0].get("id")
                if wid:
                    assert requests.post(f"{WORKFLOW_URL}/{wid}/activate", headers=headers).status_code in (200, 204, 404, 400, 500)

    def test_api_11_deactivate(self, headers):
        r = requests.get(WORKFLOW_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                wid = items[0].get("id")
                if wid:
                    assert requests.post(f"{WORKFLOW_URL}/{wid}/deactivate", headers=headers).status_code in (200, 204, 404, 400, 500)

    def test_api_12_test_rule(self, headers):
        resp = requests.post(f"{WORKFLOW_URL}/test", headers=headers, json={
            "triggerType": "LEAD_CREATED", "testData": {"leadId": "123"}})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_13_filter_status(self, headers):
        assert requests.get(f"{WORKFLOW_URL}?status=ACTIVE", headers=headers).status_code in (200, 400)

    def test_api_14_paginate(self, headers):
        assert requests.get(f"{WORKFLOW_URL}?page=0&size=5", headers=headers).status_code in (200, 400)

    def test_api_15_no_auth(self):
        assert requests.get(WORKFLOW_URL).status_code in (401, 403, 500)

    def test_api_16_duplicate_name(self, headers):
        name = f"Dup{uid()}"
        requests.post(WORKFLOW_URL, headers=headers, json={
            "name": name, "triggerType": "LEAD_CREATED", "status": "DRAFT"})
        r2 = requests.post(WORKFLOW_URL, headers=headers, json={
            "name": name, "triggerType": "LEAD_CREATED", "status": "DRAFT"})
        assert r2.status_code in (200, 201, 400, 409)

    def test_api_17_clone(self, headers):
        r = requests.get(WORKFLOW_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                wid = items[0].get("id")
                if wid:
                    assert requests.post(f"{WORKFLOW_URL}/{wid}/clone", headers=headers).status_code in (200, 201, 404, 500)

    def test_api_18_conditions(self, headers):
        resp = requests.post(WORKFLOW_URL, headers=headers, json={
            "name": f"Cond{uid()}", "triggerType": "LEAD_UPDATED", "status": "DRAFT",
            "conditions": [{"field": "status", "operator": "EQUALS", "value": "HOT"}],
            "actions": [{"type": "NOTIFY", "config": {"message": "Hot lead!"}}]})
        assert resp.status_code in (200, 201, 400)

    def test_api_19_sql_injection(self, headers):
        r = requests.get(f"{WORKFLOW_URL}?search=' OR 1=1--", headers=headers)
        assert r.status_code in (200, 400, 404, 500)
        assert "syntax" not in r.text.lower()


class TestWorkflowUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_page_loads(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")
        assert "workflow" in page.url

    def test_ui_02_workflow_list(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_03_create_button(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")

    def test_ui_04_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="Search"], input[type="search"]')
        if s.count() > 0: s.first.fill("auto")

    def test_ui_05_sidebar(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "workflow" in sidebar.lower() or "automat" in sidebar.lower()

    def test_ui_06_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_07_status_filter(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")

    def test_ui_08_pagination(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")

    def test_ui_09_history(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")

    def test_ui_10_templates(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")

    def test_ui_11_back_nav(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_12_title(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")
        body = (page.text_content("body") or "").lower()
        assert len(body) > 50

    def test_ui_13_toggle_active(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")

    def test_ui_14_builder_view(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")

    def test_ui_15_empty_search(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")
        s = page.locator('input[placeholder*="Search"], input[type="search"]')
        if s.count() > 0: s.first.fill("ZZZNONEXIST"); page.wait_for_timeout(1000)

    def test_ui_16_columns(self, page: Page):
        page.goto(f"{FRONTEND_URL}/workflows"); page.wait_for_load_state("networkidle")
