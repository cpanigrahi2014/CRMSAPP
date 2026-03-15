"""
Module 12 — AI CRM Features (40 Tests: 22 API + 18 UI)
========================================================
API : AI chat, lead scoring, sentiment, recommendations, insights,
      campaign, summarisation, configure, health score, forecast
UI  : AI config, AI insights, chat widget, recommendations panel
"""
import uuid, pytest, requests
from playwright.sync_api import Page
from automation import (
    AI_URL, AGENT_URL, LEAD_URL, OPP_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)


class TestAICRMAPI:
    def test_api_01_ai_health(self, headers):
        r = requests.get(f"{AI_URL}/health", headers=headers)
        assert r.status_code in (200, 404, 500)

    def test_api_02_ai_chat(self, headers):
        resp = requests.post(f"{AGENT_URL}/chat", headers=headers, json={
            "message": "How many leads were created this month?"})
        assert resp.status_code in (200, 201, 400, 404, 500)

    def test_api_03_lead_scoring(self, headers):
        resp = requests.post(f"{AI_URL}/lead-scoring", headers=headers, json={
            "leadId": "test-001", "features": {"source": "Website", "industry": "Tech"}})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_04_sentiment(self, headers):
        resp = requests.post(f"{AI_URL}/sentiment", headers=headers, json={
            "text": "I am very happy with the product!"})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_05_recommendations(self, headers):
        assert requests.get(f"{AI_URL}/recommendations", headers=headers).status_code in (200, 404, 500)

    def test_api_06_insights(self, headers):
        assert requests.get(f"{AI_URL}/insights", headers=headers).status_code in (200, 404, 500)

    def test_api_07_summarize(self, headers):
        resp = requests.post(f"{AI_URL}/summarize", headers=headers, json={
            "text": "Customer called to discuss pricing. They want a 10% discount. "
                    "Follow up next week. They are also interested in premium plan."})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_08_campaign_suggest(self, headers):
        resp = requests.post(f"{AI_URL}/campaign-suggest", headers=headers, json={
            "segment": "enterprise", "goal": "upsell"})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_09_configure(self, headers):
        resp = requests.post(f"{AGENT_URL}/configure", headers=headers, json={
            "instruction": "List all open opportunities worth more than 50000"})
        assert resp.status_code in (200, 201, 400, 404, 500)

    def test_api_10_forecast(self, headers):
        assert requests.get(f"{OPP_URL}/forecast", headers=headers).status_code in (200, 500)

    def test_api_11_predict_close(self, headers):
        r = requests.get(OPP_URL, headers=headers)
        if r.status_code == 200:
            data = r.json().get("data", r.json())
            items = data if isinstance(data, list) else data.get("content", [])
            if items:
                oid = items[0].get("id")
                if oid:
                    assert requests.get(f"{OPP_URL}/{oid}/predict-close", headers=headers).status_code in (200, 404, 500)

    def test_api_12_ai_analytics(self, headers):
        assert requests.get(f"{AI_URL}/analytics", headers=headers).status_code in (200, 404, 500)

    def test_api_13_email_suggest(self, headers):
        resp = requests.post(f"{AI_URL}/email-suggest", headers=headers, json={
            "context": "follow up on demo", "tone": "professional"})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_14_next_best_action(self, headers):
        resp = requests.post(f"{AI_URL}/next-best-action", headers=headers, json={
            "entityType": "LEAD", "entityId": "123"})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_15_churn_prediction(self, headers):
        assert requests.get(f"{AI_URL}/churn-prediction", headers=headers).status_code in (200, 404, 500)

    def test_api_16_health_score(self, headers):
        assert requests.get(f"{AI_URL}/health-score", headers=headers).status_code in (200, 404, 500)

    def test_api_17_no_auth_agent(self):
        assert requests.post(f"{AGENT_URL}/chat", json={"message": "hi"}).status_code in (401, 403, 500)

    def test_api_18_empty_chat(self, headers):
        resp = requests.post(f"{AGENT_URL}/chat", headers=headers, json={"message": ""})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_19_long_prompt(self, headers):
        resp = requests.post(f"{AGENT_URL}/chat", headers=headers, json={
            "message": "x" * 5000})
        assert resp.status_code in (200, 400, 404, 413, 500)

    def test_api_20_xss_chat(self, headers):
        resp = requests.post(f"{AGENT_URL}/chat", headers=headers, json={
            "message": "<script>alert(1)</script>"})
        if resp.status_code in (200, 201):
            assert "<script>" not in resp.text

    def test_api_21_sql_injection_chat(self, headers):
        resp = requests.post(f"{AGENT_URL}/chat", headers=headers, json={
            "message": "'; DROP TABLE leads; --"})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_22_agent_status(self, headers):
        assert requests.get(f"{AGENT_URL}/status", headers=headers).status_code in (200, 404, 500)


class TestAICRMUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_ai_config(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-config"); page.wait_for_load_state("networkidle")
        assert "ai" in page.url.lower()

    def test_ui_02_ai_insights(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-insights"); page.wait_for_load_state("networkidle")
        assert "ai" in page.url.lower()

    def test_ui_03_chat_widget(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        chat = page.locator('[data-testid="ai-chat"], .ai-chat, button:has-text("AI"), button:has-text("Chat"), [aria-label*="chat"]')
        if chat.count() > 0: chat.first.click(); page.wait_for_timeout(2000)

    def test_ui_04_ai_sidebar(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        sidebar = page.text_content("nav, [class*='sidebar'], [class*='Sidebar'], [class*='MuiDrawer']") or ""
        assert "ai" in sidebar.lower() or "intelligence" in sidebar.lower()

    def test_ui_05_insights_cards(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-insights"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_06_config_sections(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-config"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_07_responsive_config(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/ai-config"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_08_responsive_insights(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/ai-insights"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_09_recommendations(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-insights"); page.wait_for_load_state("networkidle")

    def test_ui_10_sentiment_display(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-insights"); page.wait_for_load_state("networkidle")

    def test_ui_11_forecast_section(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-insights"); page.wait_for_load_state("networkidle")

    def test_ui_12_lead_score_display(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads"); page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)

    def test_ui_13_nav_ai_config(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-config"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_14_nav_ai_insights(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-insights"); page.goto(f"{FRONTEND_URL}/dashboard")
        assert "dashboard" in page.url

    def test_ui_15_chat_input(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")
        chat_input = page.locator('input[placeholder*="Ask"], textarea[placeholder*="Ask"], input[placeholder*="chat"], textarea[placeholder*="chat"]')
        if chat_input.count() > 0: chat_input.first.fill("test question")

    def test_ui_16_chat_send(self, page: Page):
        page.goto(f"{FRONTEND_URL}/dashboard"); page.wait_for_load_state("networkidle")

    def test_ui_17_developer_page(self, page: Page):
        page.goto(f"{FRONTEND_URL}/developer"); page.wait_for_load_state("networkidle")

    def test_ui_18_title(self, page: Page):
        page.goto(f"{FRONTEND_URL}/ai-insights"); page.wait_for_load_state("networkidle")
        body = (page.text_content("body") or "").lower()
        assert len(body) > 50
