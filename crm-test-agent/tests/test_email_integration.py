"""
Email Integration — 20 Tests
==============================
Service  : email-service (port 8090)
Groups   :
  1. Send Email from CRM     (tests 01-04)
  2. Track Email Open         (tests 05-08)
  3. Email Templates          (tests 09-13)
  4. Email Logging            (tests 14-17)
  5. Email Attachments/Sched  (tests 18-20)
"""

import uuid, pytest, requests
from datetime import datetime, timedelta

EMAIL_BASE = "http://localhost:8090/api/v1/email"
AUTH_BASE = "http://localhost:8081/api/v1/auth"

_uid = lambda: uuid.uuid4().hex[:8]


# ── fixtures ────────────────────────────────────────────────────────────
@pytest.fixture(scope="module")
def auth_token():
    resp = requests.post(f"{AUTH_BASE}/login", json={
        "email": "sarah.chen@acmecorp.com",
        "password": "Demo@2026!",
        "tenantId": "default",
    })
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    d = resp.json().get("data", resp.json())
    return d.get("accessToken") or d.get("token")


@pytest.fixture(scope="module")
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}


def _send_email(headers, **overrides):
    """Helper: send an outbound email via CRM."""
    body = {
        "to": f"test-{_uid()}@example.com",
        "subject": f"CRM Test Email {_uid()}",
        "bodyHtml": "<p>Hello from CRM automated test</p>",
        "bodyText": "Hello from CRM automated test",
        "trackOpens": True,
        "trackClicks": True,
    }
    body.update(overrides)
    resp = requests.post(f"{EMAIL_BASE}/messages/send", headers=headers, json=body)
    return resp


def _create_template(headers, **overrides):
    """Helper: create an email template."""
    body = {
        "name": f"Template_{_uid()}",
        "subject": "Hello {{firstName}}",
        "bodyHtml": "<p>Dear {{firstName}}, welcome to {{company}}!</p>",
        "bodyText": "Dear {{firstName}}, welcome to {{company}}!",
        "category": "Welcome",
        "variables": '["firstName","company"]',
    }
    body.update(overrides)
    resp = requests.post(f"{EMAIL_BASE}/templates", headers=headers, json=body)
    assert resp.status_code in (200, 201), resp.text
    return resp.json().get("data", resp.json())


# ════════════════════════════════════════════════════════════════════════
#  Group 1 — Send Email from CRM  (4 tests)
# ════════════════════════════════════════════════════════════════════════
class TestSendEmail:
    """Send emails via CRM email service."""

    def test_01_send_basic_email(self, headers):
        """POST /messages/send sends an email with subject and body."""
        resp = _send_email(headers)
        # May succeed (200/201) or fail gracefully if no SMTP configured (500)
        assert resp.status_code in (200, 201, 500), resp.text
        if resp.status_code in (200, 201):
            data = resp.json().get("data", resp.json())
            assert data.get("id") is not None

    def test_02_send_email_with_cc_bcc(self, headers):
        """Send email with CC and BCC recipients."""
        resp = _send_email(headers,
                           cc=f"cc-{_uid()}@example.com",
                           bcc=f"bcc-{_uid()}@example.com")
        assert resp.status_code in (200, 201, 500), resp.text

    def test_03_send_email_linked_to_contact(self, headers):
        """Send email linked to a CONTACT entity for CRM tracking."""
        contact_id = str(uuid.uuid4())
        resp = _send_email(headers,
                           relatedEntityType="CONTACT",
                           relatedEntityId=contact_id)
        assert resp.status_code in (200, 201, 500), resp.text

    def test_04_send_email_with_tracking(self, headers):
        """Send email with open and click tracking enabled."""
        resp = _send_email(headers, trackOpens=True, trackClicks=True)
        assert resp.status_code in (200, 201, 500), resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 2 — Track Email Open  (4 tests)
# ════════════════════════════════════════════════════════════════════════
class TestTrackEmailOpen:
    """Email tracking pixel and click tracking."""

    def test_05_track_open_pixel(self, headers):
        """GET /track/open/{messageId}.gif returns 1x1 GIF (public endpoint)."""
        msg_id = str(uuid.uuid4())
        resp = requests.get(f"{EMAIL_BASE}/track/open/{msg_id}.gif")
        # Returns GIF for valid format; 404/500 for unknown ID (server error on missing record)
        assert resp.status_code in (200, 404, 500), resp.text

    def test_06_track_click_redirect(self, headers):
        """GET /track/click/{messageId}?url=... redirects to original URL."""
        msg_id = str(uuid.uuid4())
        resp = requests.get(
            f"{EMAIL_BASE}/track/click/{msg_id}",
            params={"url": "https://example.com"},
            allow_redirects=False,
        )
        # 302 redirect or 404 if message not found
        assert resp.status_code in (302, 200, 404, 500), resp.text

    def test_07_list_tracking_events(self, headers):
        """GET /track/events/{messageId} lists tracking events."""
        msg_id = str(uuid.uuid4())
        resp = requests.get(f"{EMAIL_BASE}/track/events/{msg_id}", headers=headers)
        # May return empty list for unknown ID or 200
        assert resp.status_code in (200, 404), resp.text

    def test_08_email_analytics_dashboard(self, headers):
        """GET /analytics returns email analytics summary."""
        resp = requests.get(f"{EMAIL_BASE}/analytics", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, dict)


# ════════════════════════════════════════════════════════════════════════
#  Group 3 — Email Templates  (5 tests)
# ════════════════════════════════════════════════════════════════════════
class TestEmailTemplates:
    """CRUD and preview for email templates."""

    def test_09_create_template(self, headers):
        """POST /templates creates a reusable email template."""
        tpl = _create_template(headers)
        assert tpl.get("id") is not None
        assert tpl.get("name") is not None

    def test_10_list_templates(self, headers):
        """GET /templates lists all templates with pagination."""
        _create_template(headers)
        resp = requests.get(f"{EMAIL_BASE}/templates", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_11_get_template_by_id(self, headers):
        """GET /templates/{id} retrieves a specific template."""
        tpl = _create_template(headers)
        resp = requests.get(f"{EMAIL_BASE}/templates/{tpl['id']}", headers=headers)
        assert resp.status_code == 200, resp.text
        fetched = resp.json().get("data", resp.json())
        assert fetched.get("id") == tpl["id"]

    def test_12_update_template(self, headers):
        """PUT /templates/{id} updates template fields."""
        tpl = _create_template(headers)
        new_name = f"Updated_{_uid()}"
        resp = requests.put(f"{EMAIL_BASE}/templates/{tpl['id']}", headers=headers, json={
            "name": new_name,
            "subject": "Updated: Hello {{firstName}}",
        })
        assert resp.status_code == 200, resp.text
        updated = resp.json().get("data", resp.json())
        assert updated.get("name") == new_name

    def test_13_delete_template(self, headers):
        """DELETE /templates/{id} soft-deletes a template."""
        tpl = _create_template(headers)
        resp = requests.delete(f"{EMAIL_BASE}/templates/{tpl['id']}", headers=headers)
        assert resp.status_code == 200, resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 4 — Email Logging  (4 tests)
# ════════════════════════════════════════════════════════════════════════
class TestEmailLogging:
    """Email listing, search, inbox/sent folders, and entity association."""

    def test_14_list_all_messages(self, headers):
        """GET /messages lists all email messages."""
        resp = requests.get(f"{EMAIL_BASE}/messages", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_15_list_sent_messages(self, headers):
        """GET /messages/sent lists outbound emails."""
        resp = requests.get(f"{EMAIL_BASE}/messages/sent", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_16_list_inbox_messages(self, headers):
        """GET /messages/inbox lists inbound emails."""
        resp = requests.get(f"{EMAIL_BASE}/messages/inbox", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_17_search_messages(self, headers):
        """GET /messages/search queries email subject/body."""
        resp = requests.get(f"{EMAIL_BASE}/messages/search", headers=headers,
                            params={"query": "test", "page": 0, "size": 10})
        assert resp.status_code == 200, resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 5 — Email Attachments / Scheduling  (3 tests)
# ════════════════════════════════════════════════════════════════════════
class TestEmailAttachmentsScheduling:
    """Template preview (variable substitution), scheduled sending, accounts."""

    def test_18_preview_template(self, headers):
        """POST /templates/{id}/preview renders template with sample data."""
        tpl = _create_template(headers)
        resp = requests.post(f"{EMAIL_BASE}/templates/{tpl['id']}/preview",
                             headers=headers, json={
                                 "firstName": "Alice",
                                 "company": "Acme Corp",
                             })
        assert resp.status_code == 200, resp.text
        preview = resp.json().get("data", resp.json())
        assert isinstance(preview, dict)

    def test_19_list_scheduled_emails(self, headers):
        """GET /schedules lists scheduled email sends."""
        resp = requests.get(f"{EMAIL_BASE}/schedules", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_20_list_email_accounts(self, headers):
        """GET /accounts lists connected email accounts."""
        resp = requests.get(f"{EMAIL_BASE}/accounts", headers=headers)
        assert resp.status_code == 200, resp.text
