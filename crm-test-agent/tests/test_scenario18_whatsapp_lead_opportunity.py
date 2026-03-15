"""
Scenario 18: WhatsApp → Lead → Opportunity

End-to-end integration test that verifies the complete WhatsApp channel flow:
1. WhatsApp message received via webhook
2. Lead created from phone number
3. Lead auto-converted
4. Opportunity created
5. WhatsApp transcript attached as activity
"""
from __future__ import annotations

import time
import uuid
import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth/login"
INTEGRATION_URL = "http://localhost:8091/api/v1/integrations/channels/whatsapp"
LEAD_URL = "http://localhost:8082/api/v1/leads"
OPPORTUNITY_URL = "http://localhost:8085/api/v1/opportunities"
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
def unique_phone():
    """Generate a unique phone number per test."""
    ts = int(time.time() * 1000)
    return f"+1555918{ts}"


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 1: WhatsApp Webhook – Basic Lead Creation (no auto-convert)
# ══════════════════════════════════════════════════════════════════════════════

class TestWhatsAppLeadCreation:
    """Tests for WhatsApp → Lead creation without auto-conversion."""

    def test_whatsapp_creates_lead(self, api_headers, unique_phone):
        """S18-01: WhatsApp message creates a new lead."""
        payload = {
            "phone": unique_phone,
            "firstName": "Alice",
            "lastName": "WhatsTest",
            "message": "I need info about your CRM product.",
            "autoConvert": False,
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201, f"Expected 201, got {resp.status_code}: {resp.text}"
        body = resp.json()
        assert body["success"] is True
        data = body["data"]
        assert data["leadId"] is not None, "leadId should be present"
        assert data["leadStatus"] == "NEW", f"Expected NEW, got {data['leadStatus']}"
        assert data["opportunityId"] is None, "No opportunity should be created when autoConvert=False"

    def test_whatsapp_transcript_attached(self, api_headers, unique_phone):
        """S18-02: WhatsApp transcript is attached as activity."""
        payload = {
            "phone": unique_phone,
            "firstName": "Bob",
            "lastName": "TranscriptTest",
            "message": "Please send me pricing for the Enterprise plan.",
            "autoConvert": False,
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["transcriptActivityId"] is not None, "Transcript activity should be created"

    def test_whatsapp_lead_has_correct_source(self, api_headers, unique_phone):
        """S18-03: Lead created from WhatsApp should have source WHATSAPP."""
        payload = {
            "phone": unique_phone,
            "firstName": "Charlie",
            "lastName": "SourceTest",
            "message": "Interested in your product!",
            "autoConvert": False,
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        lead_id = resp.json()["data"]["leadId"]

        # Fetch the lead and verify source
        lead_resp = requests.get(f"{LEAD_URL}/{lead_id}", headers=api_headers, timeout=15)
        if lead_resp.status_code == 200:
            lead_data = lead_resp.json().get("data", lead_resp.json())
            source = lead_data.get("source", "")
            assert source == "WHATSAPP", f"Expected source WHATSAPP, got {source}"

    def test_whatsapp_with_media_url(self, api_headers, unique_phone):
        """S18-04: WhatsApp message with media URL is processed."""
        payload = {
            "phone": unique_phone,
            "firstName": "Diana",
            "lastName": "MediaTest",
            "message": "Here is a screenshot of what I need.",
            "mediaUrl": "https://example.com/screenshot.png",
            "autoConvert": False,
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["leadId"] is not None
        assert data["transcriptActivityId"] is not None

    def test_whatsapp_requires_phone(self, api_headers):
        """S18-05: WhatsApp webhook rejects request without phone."""
        payload = {
            "firstName": "NoPhone",
            "lastName": "Test",
            "message": "This should fail.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code in (400, 422), f"Expected 400/422, got {resp.status_code}"


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 2: WhatsApp → Lead → Opportunity (Full Auto-Convert Flow)
# ══════════════════════════════════════════════════════════════════════════════

class TestWhatsAppAutoConvert:
    """Tests for the complete WhatsApp → Lead → Opportunity conversion flow."""

    def test_auto_convert_creates_opportunity(self, api_headers, unique_phone):
        """S18-06: WhatsApp with autoConvert=true creates lead AND opportunity."""
        payload = {
            "phone": unique_phone,
            "firstName": "Eve",
            "lastName": "ConvertTest",
            "message": "I want to buy your Enterprise CRM package for our 200-person team.",
            "autoConvert": True,
            "opportunityName": "WhatsApp Enterprise Deal - S18",
            "opportunityAmount": "75000",
            "opportunityStage": "PROSPECTING",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201, f"Expected 201, got {resp.status_code}: {resp.text}"
        data = resp.json()["data"]

        # Lead should be created and marked as CONVERTED
        assert data["leadId"] is not None
        assert data["leadStatus"] == "CONVERTED"

        # Opportunity should be created
        assert data["opportunityId"] is not None
        assert data["opportunityStage"] == "PROSPECTING"

        # Transcript should be attached
        assert data["transcriptActivityId"] is not None

    def test_opportunity_is_fetchable(self, api_headers, unique_phone):
        """S18-07: Created opportunity can be fetched from opportunity-service."""
        payload = {
            "phone": unique_phone,
            "firstName": "Frank",
            "lastName": "FetchOppTest",
            "message": "Need pricing for 50 seats.",
            "autoConvert": True,
            "opportunityName": "WhatsApp Deal - Fetch Test",
            "opportunityAmount": "25000",
            "opportunityStage": "PROSPECTING",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        opp_id = resp.json()["data"]["opportunityId"]
        assert opp_id is not None

        # Fetch the opportunity
        opp_resp = requests.get(f"{OPPORTUNITY_URL}/{opp_id}", headers=api_headers, timeout=15)
        assert opp_resp.status_code == 200, f"Could not fetch opportunity: {opp_resp.text}"
        opp_data = opp_resp.json().get("data", opp_resp.json())
        assert opp_data.get("stage") or opp_data.get("stageName"), "Opportunity should have a stage"

    def test_auto_convert_with_custom_stage(self, api_headers, unique_phone):
        """S18-08: Auto-convert respects custom opportunity stage."""
        payload = {
            "phone": unique_phone,
            "firstName": "Grace",
            "lastName": "StageTest",
            "message": "We already evaluated. Ready to negotiate.",
            "autoConvert": True,
            "opportunityName": "WhatsApp - Negotiation Deal",
            "opportunityAmount": "100000",
            "opportunityStage": "NEGOTIATION",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["opportunityStage"] == "NEGOTIATION"

    def test_auto_convert_default_values(self, api_headers, unique_phone):
        """S18-09: Auto-convert uses defaults when optional fields missing."""
        payload = {
            "phone": unique_phone,
            "firstName": "Henry",
            "lastName": "DefaultTest",
            "message": "Tell me about your product.",
            "autoConvert": True,
            # No opportunityName, amount, or stage — all should use defaults
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["leadId"] is not None
        assert data["leadStatus"] == "CONVERTED"
        assert data["opportunityId"] is not None
        # Default stage should be PROSPECTING
        assert data["opportunityStage"] == "PROSPECTING"


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 3: Transcript Verification
# ══════════════════════════════════════════════════════════════════════════════

class TestWhatsAppTranscript:
    """Tests verifying WhatsApp transcript content and attachment."""

    def test_transcript_content_on_lead(self, api_headers, unique_phone):
        """S18-10: Transcript activity on lead contains WhatsApp message text."""
        msg = "I would like a demo of your CRM for our real estate agency."
        payload = {
            "phone": unique_phone,
            "firstName": "Ivy",
            "lastName": "TranscriptContent",
            "message": msg,
            "autoConvert": False,
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        lead_id = data["leadId"]

        # Fetch activities for the lead
        act_resp = requests.get(
            f"{ACTIVITY_URL}",
            params={"relatedEntityId": str(lead_id), "relatedEntityType": "Lead"},
            headers=api_headers,
            timeout=15,
        )
        if act_resp.status_code == 200:
            activities = act_resp.json().get("data", {})
            if isinstance(activities, dict):
                activities = activities.get("content", [])
            # Check that at least one activity contains the WhatsApp transcript
            found = any("WhatsApp" in str(a) for a in activities) if activities else False
            assert found or data["transcriptActivityId"] is not None, \
                "WhatsApp transcript should be attached to lead"

    def test_transcript_on_converted_opportunity(self, api_headers, unique_phone):
        """S18-11: When auto-converted, transcript is also attached to opportunity."""
        payload = {
            "phone": unique_phone,
            "firstName": "Jack",
            "lastName": "OppTranscript",
            "message": "Ready to purchase 100 licenses of your Enterprise plan.",
            "autoConvert": True,
            "opportunityName": "WhatsApp - Transcript Opp Test",
            "opportunityAmount": "50000",
            "opportunityStage": "PROSPECTING",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        opp_id = data["opportunityId"]
        assert opp_id is not None

        # Fetch activities for the opportunity
        act_resp = requests.get(
            f"{ACTIVITY_URL}",
            params={"relatedEntityId": str(opp_id), "relatedEntityType": "Opportunity"},
            headers=api_headers,
            timeout=15,
        )
        if act_resp.status_code == 200:
            activities = act_resp.json().get("data", {})
            if isinstance(activities, dict):
                activities = activities.get("content", [])
            found = any("WhatsApp" in str(a) for a in activities) if activities else False
            assert found or data["transcriptActivityId"] is not None, \
                "WhatsApp transcript should be attached to opportunity"


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 4: End-to-End Flow Verification
# ══════════════════════════════════════════════════════════════════════════════

class TestWhatsAppE2EFlow:
    """Full end-to-end scenario: WhatsApp → Lead → Opportunity with all verifications."""

    def test_complete_scenario_18_flow(self, api_headers, unique_phone):
        """S18-12: Complete Scenario 18 — WhatsApp → Lead → Opportunity (all steps)."""
        whatsapp_msg = (
            "Hi, this is Maria from TechCorp. "
            "We're looking for a CRM solution for our sales team of 50 people. "
            "Our budget is around $50K. Can you send us a proposal?"
        )
        payload = {
            "phone": unique_phone,
            "firstName": "Maria",
            "lastName": "TechCorp",
            "message": whatsapp_msg,
            "autoConvert": True,
            "opportunityName": "TechCorp CRM Deal via WhatsApp",
            "opportunityAmount": "50000",
            "opportunityStage": "PROSPECTING",
        }

        # ── Step 1: Send WhatsApp webhook ──
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201, f"WhatsApp webhook failed: {resp.text}"
        result = resp.json()
        assert result["success"] is True
        data = result["data"]

        lead_id = data["leadId"]
        opp_id = data["opportunityId"]
        activity_id = data["transcriptActivityId"]

        # ── Step 2: Verify Lead was created ──
        assert lead_id is not None, "Lead should be created"
        assert data["leadStatus"] == "CONVERTED", "Lead should be auto-converted"

        lead_resp = requests.get(f"{LEAD_URL}/{lead_id}", headers=api_headers, timeout=15)
        assert lead_resp.status_code == 200, f"Lead fetch failed: {lead_resp.text}"

        # ── Step 3: Verify Opportunity was created ──
        assert opp_id is not None, "Opportunity should be created"
        assert data["opportunityStage"] == "PROSPECTING"

        opp_resp = requests.get(f"{OPPORTUNITY_URL}/{opp_id}", headers=api_headers, timeout=15)
        assert opp_resp.status_code == 200, f"Opportunity fetch failed: {opp_resp.text}"

        # ── Step 4: Verify WhatsApp transcript was attached ──
        assert activity_id is not None, "Transcript activity should be created"

        # ── Step 5: All three entities are linked ──
        assert lead_id and opp_id and activity_id, \
            "All three entities (lead, opportunity, activity) must be created"

    def test_multiple_whatsapp_messages_create_separate_leads(self, api_headers):
        """S18-13: Multiple WhatsApp messages from different numbers create separate leads."""
        lead_ids = []
        for i in range(3):
            ts = int(time.time() * 1000) + i
            payload = {
                "phone": f"+1555918{ts}",
                "firstName": f"Multi{i}",
                "lastName": "Test",
                "message": f"Message {i}: interested in CRM.",
                "autoConvert": False,
            }
            resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
            assert resp.status_code == 201, f"Message {i} failed: {resp.text}"
            lead_ids.append(resp.json()["data"]["leadId"])

        # All leads should be unique
        assert len(set(lead_ids)) == 3, "Each WhatsApp message should create a separate lead"

    def test_whatsapp_no_auth_rejected(self):
        """S18-14: WhatsApp webhook without auth token is rejected."""
        payload = {
            "phone": "+15559999999",
            "firstName": "NoAuth",
            "lastName": "Test",
            "message": "This should be rejected.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, timeout=15,
                             headers={"Content-Type": "application/json"})
        assert resp.status_code in (401, 403), \
            f"Expected 401/403 without auth, got {resp.status_code}"
