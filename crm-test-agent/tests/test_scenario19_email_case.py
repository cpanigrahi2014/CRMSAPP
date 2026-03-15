"""
Scenario 19: Email → Case
 
End-to-end integration test that verifies the complete inbound email support flow:
1. Customer sends email to support@
2. Case auto-created with priority
3. Email attached as Activity on the case
"""
from __future__ import annotations

import time
import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth/login"
INTEGRATION_URL = "http://localhost:8091/api/v1/integrations/channels/email-support"
CASE_URL = "http://localhost:8092/api/v1/cases"
ACTIVITY_URL = "http://localhost:8086/api/v1/activities"

CREDENTIALS = {
    "email": "sarah.chen@acmecorp.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}


# ── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def auth_token():
    """Obtain a JWT token for API calls."""
    resp = requests.post(AUTH_URL, json=CREDENTIALS, timeout=15)
    assert resp.status_code == 200, f"Auth failed: {resp.text}"
    data = resp.json()
    token = data.get("data", {}).get("accessToken") or data.get("token", "")
    assert token, "No token returned from auth"
    return token


@pytest.fixture(scope="module")
def api_headers(auth_token):
    """Standard headers for API calls."""
    return {
        "Authorization": f"Bearer {auth_token}",
        "Content-Type": "application/json",
        "X-Tenant-ID": "default",
    }


@pytest.fixture
def unique_email():
    """Generate a unique sender email per test."""
    ts = int(time.time() * 1000)
    return f"customer-{ts}@testmail.com"


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 1: Email Webhook – Case Auto-Creation
# ══════════════════════════════════════════════════════════════════════════════

class TestEmailCaseCreation:
    """Tests for Email → Case auto-creation via webhook."""

    def test_email_creates_case(self, api_headers, unique_email):
        """S19-01: Inbound support email auto-creates a case."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "Cannot login to my account",
            "bodyText": "Hi, I have been locked out of my account since yesterday. Please help.",
            "contactName": "Alice TestCustomer",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201, f"Expected 201, got {resp.status_code}: {resp.text}"
        body = resp.json()
        assert body["success"] is True
        data = body["data"]
        assert data["caseId"] is not None, "caseId should be present"
        assert data["caseNumber"] is not None, "caseNumber should be present"
        assert data["caseStatus"] == "OPEN", f"Expected OPEN, got {data['caseStatus']}"

    def test_email_case_has_priority(self, api_headers, unique_email):
        """S19-02: Auto-created case gets a priority assigned."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "Urgent billing issue",
            "bodyText": "I was charged twice on my last invoice. Need immediate resolution.",
            "contactName": "Bob Billing",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["casePriority"] is not None, "Case should have a priority"
        assert data["casePriority"] in ("LOW", "MEDIUM", "HIGH", "CRITICAL"), \
            f"Unexpected priority: {data['casePriority']}"

    def test_email_activity_attached(self, api_headers, unique_email):
        """S19-03: Email is attached as an Activity on the case."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "Feature request - bulk export",
            "bodyText": "Please add a bulk CSV export feature to the reports module.",
            "contactName": "Charlie Feature",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["emailActivityId"] is not None, "Email activity should be created"

    def test_email_case_number_format(self, api_headers, unique_email):
        """S19-04: Case number follows expected pattern (e.g. CS-XXXXXX)."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "General inquiry",
            "bodyText": "How do I reset my password?",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        case_number = resp.json()["data"]["caseNumber"]
        assert case_number.startswith("CS-"), f"Case number should start with CS-, got {case_number}"

    def test_email_requires_from_address(self, api_headers):
        """S19-05: Webhook rejects request without fromAddress."""
        payload = {
            "toAddress": "support@acmecorp.com",
            "subject": "No sender",
            "bodyText": "This should fail.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code in (400, 422), f"Expected 400/422, got {resp.status_code}"

    def test_email_requires_to_address(self, api_headers):
        """S19-06: Webhook rejects request without toAddress."""
        payload = {
            "fromAddress": "someone@test.com",
            "subject": "No recipient",
            "bodyText": "This should fail too.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code in (400, 422), f"Expected 400/422, got {resp.status_code}"


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 2: Case Verification
# ══════════════════════════════════════════════════════════════════════════════

class TestEmailCaseVerification:
    """Tests verifying the case created from email is valid and fetchable."""

    def test_case_is_fetchable(self, api_headers, unique_email):
        """S19-07: Created case can be fetched from case-service."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "API access issue",
            "bodyText": "I cannot access the REST API. Getting 403 errors.",
            "contactName": "Dave Developer",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        case_id = resp.json()["data"]["caseId"]

        case_resp = requests.get(f"{CASE_URL}/{case_id}", headers=api_headers, timeout=15)
        assert case_resp.status_code == 200, f"Could not fetch case: {case_resp.text}"
        case_data = case_resp.json().get("data", case_resp.json())
        assert case_data.get("subject") or case_data.get("title"), "Case should have a subject"

    def test_case_has_email_origin(self, api_headers, unique_email):
        """S19-08: Case origin should be EMAIL."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "Integration query",
            "bodyText": "Does your CRM integrate with Salesforce?",
            "contactName": "Eve Integrator",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        case_id = resp.json()["data"]["caseId"]

        case_resp = requests.get(f"{CASE_URL}/{case_id}", headers=api_headers, timeout=15)
        if case_resp.status_code == 200:
            case_data = case_resp.json().get("data", case_resp.json())
            origin = case_data.get("origin", "")
            assert origin == "EMAIL", f"Expected origin EMAIL, got {origin}"

    def test_case_fetchable_by_number(self, api_headers, unique_email):
        """S19-09: Case is also fetchable by its case number."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "Fetch by number test",
            "bodyText": "Verifying case number lookup works.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        case_number = resp.json()["data"]["caseNumber"]

        num_resp = requests.get(
            f"{CASE_URL}/number/{case_number}", headers=api_headers, timeout=15
        )
        assert num_resp.status_code == 200, f"Could not fetch by number: {num_resp.text}"

    def test_email_with_html_body(self, api_headers, unique_email):
        """S19-10: Email with HTML body is processed correctly."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "HTML email test",
            "bodyText": "Plain text fallback.",
            "bodyHtml": "<h1>Help!</h1><p>My dashboard is <b>not loading</b>.</p>",
            "contactName": "Frank HtmlUser",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["caseId"] is not None
        assert data["emailActivityId"] is not None

    def test_email_with_cc(self, api_headers, unique_email):
        """S19-11: Email with CC addresses is processed correctly."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "CC recipients test",
            "bodyText": "Copying my manager on this issue.",
            "ccAddresses": "manager@testcorp.com,cto@testcorp.com",
            "contactName": "Grace CcUser",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["caseId"] is not None


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 3: End-to-End Flow
# ══════════════════════════════════════════════════════════════════════════════

class TestEmailCaseE2E:
    """Full end-to-end scenario: Email → Case with all verifications."""

    def test_complete_scenario_19_flow(self, api_headers, unique_email):
        """S19-12: Complete Scenario 19 — Email → Case → Activity (all steps)."""
        payload = {
            "fromAddress": unique_email,
            "toAddress": "support@acmecorp.com",
            "subject": "Account suspended - URGENT",
            "bodyText": (
                "Dear Support,\n\n"
                "My company account has been suspended without notice. "
                "We have 50 active users who cannot access the system. "
                "This is affecting our daily operations. Please resolve ASAP.\n\n"
                "Regards,\nHenry Urgent"
            ),
            "contactName": "Henry Urgent",
        }

        # Step 1: Send email webhook
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201, f"Email webhook failed: {resp.text}"
        result = resp.json()
        assert result["success"] is True
        data = result["data"]

        case_id = data["caseId"]
        case_number = data["caseNumber"]
        activity_id = data["emailActivityId"]

        # Step 2: Verify case was created
        assert case_id is not None, "Case should be created"
        assert case_number is not None, "Case number should be assigned"
        assert data["caseStatus"] == "OPEN", "Case should be OPEN"
        assert data["casePriority"] is not None, "Case should have priority"

        # Step 3: Fetch case from case-service
        case_resp = requests.get(f"{CASE_URL}/{case_id}", headers=api_headers, timeout=15)
        assert case_resp.status_code == 200, f"Case fetch failed: {case_resp.text}"

        # Step 4: Verify email activity was attached
        assert activity_id is not None, "Email activity should be created"

        # Step 5: All entities linked
        assert case_id and case_number and activity_id, \
            "All entities (case, case number, activity) must be present"

    def test_multiple_emails_create_separate_cases(self, api_headers):
        """S19-13: Multiple emails from different senders create separate cases."""
        case_ids = []
        for i in range(3):
            ts = int(time.time() * 1000) + i
            payload = {
                "fromAddress": f"sender-{ts}@testcorp.com",
                "toAddress": "support@acmecorp.com",
                "subject": f"Support request #{i}",
                "bodyText": f"Issue number {i}.",
            }
            resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
            assert resp.status_code == 201, f"Email {i} failed: {resp.text}"
            case_ids.append(resp.json()["data"]["caseId"])

        assert len(set(case_ids)) == 3, "Each email should create a separate case"

    def test_email_no_auth_rejected(self):
        """S19-14: Email webhook without auth token is rejected."""
        payload = {
            "fromAddress": "noauth@test.com",
            "toAddress": "support@acmecorp.com",
            "subject": "Should be rejected",
            "bodyText": "No auth token provided.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, timeout=15,
                             headers={"Content-Type": "application/json"})
        assert resp.status_code in (401, 403), \
            f"Expected 401/403 without auth, got {resp.status_code}"
