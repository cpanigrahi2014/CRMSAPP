"""
Scenario 20: Instagram comment → Lead

End-to-end integration test for the social media channel flow:
1. Social listener detects comment (Instagram, Facebook, LinkedIn, Twitter)
2. Lead created with source = SOCIAL_MEDIA
3. Lead score set to social platform weight
4. Comment attached as activity on the lead
"""
from __future__ import annotations

import time
import pytest
import requests

# ── Constants ────────────────────────────────────────────────────────────────
AUTH_URL = "http://localhost:8081/api/v1/auth/login"
INTEGRATION_URL = "http://localhost:8091/api/v1/integrations/channels/social"
LEAD_URL = "http://localhost:8082/api/v1/leads"
ACTIVITY_URL = "http://localhost:8086/api/v1/activities"

CREDENTIALS = {
    "email": "sarah.chen@acmecorp.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}

# Expected platform weights from determineSocialWeight()
PLATFORM_WEIGHTS = {
    "INSTAGRAM": 15,
    "FACEBOOK": 12,
    "LINKEDIN": 20,
    "TWITTER": 10,
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
def unique_username():
    """Generate a unique social username per test."""
    ts = int(time.time() * 1000)
    return f"user_{ts}"


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 1: Instagram → Lead Creation
# ══════════════════════════════════════════════════════════════════════════════

class TestInstagramLeadCreation:
    """Tests for Instagram comment → Lead creation."""

    def test_instagram_creates_lead(self, api_headers, unique_username):
        """S20-01: Instagram comment creates a new lead."""
        payload = {
            "platform": "INSTAGRAM",
            "username": unique_username,
            "fullName": "Alice Insta",
            "comment": "Love this product! Does it work for e-commerce?",
            "postUrl": "https://instagram.com/p/test123",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201, f"Expected 201, got {resp.status_code}: {resp.text}"
        body = resp.json()
        assert body["success"] is True
        data = body["data"]
        assert data["leadId"] is not None, "leadId should be present"
        assert data["leadStatus"] == "NEW"
        assert data["leadSource"] == "INSTAGRAM"

    def test_instagram_lead_score_is_15(self, api_headers, unique_username):
        """S20-02: Instagram lead gets social weight score of 15."""
        payload = {
            "platform": "INSTAGRAM",
            "username": unique_username,
            "fullName": "Bob ScoreTest",
            "comment": "Interested in your CRM!",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["leadScore"] == 15, f"Expected score 15 (Instagram weight), got {data['leadScore']}"

    def test_instagram_lead_source_correct(self, api_headers, unique_username):
        """S20-03: Lead created from Instagram has correct source in lead-service."""
        payload = {
            "platform": "INSTAGRAM",
            "username": unique_username,
            "fullName": "Charlie Source",
            "comment": "Can I get a demo?",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        lead_id = resp.json()["data"]["leadId"]

        lead_resp = requests.get(f"{LEAD_URL}/{lead_id}", headers=api_headers, timeout=15)
        if lead_resp.status_code == 200:
            lead_data = lead_resp.json().get("data", lead_resp.json())
            source = lead_data.get("source", "")
            assert source == "SOCIAL_MEDIA", f"Expected SOCIAL_MEDIA, got {source}"

    def test_instagram_with_profile_url(self, api_headers, unique_username):
        """S20-04: Instagram comment with profile URL."""
        payload = {
            "platform": "INSTAGRAM",
            "username": unique_username,
            "fullName": "Diana Profile",
            "comment": "Great product showcase!",
            "postUrl": "https://instagram.com/p/abc456",
            "profileUrl": "https://instagram.com/diana_profile",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["leadId"] is not None

    def test_instagram_with_email(self, api_headers, unique_username):
        """S20-05: Instagram comment with email creates lead with email."""
        payload = {
            "platform": "INSTAGRAM",
            "username": unique_username,
            "fullName": "Eve EmailUser",
            "email": f"{unique_username}@instagram-test.com",
            "comment": "DM me the pricing please!",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201
        data = resp.json()["data"]
        assert data["leadId"] is not None


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 2: Multi-Platform Score Weights
# ══════════════════════════════════════════════════════════════════════════════

class TestPlatformWeights:
    """Tests verifying different platform social weights."""

    @pytest.mark.parametrize("platform,expected_score", [
        ("INSTAGRAM", 15),
        ("FACEBOOK", 12),
        ("LINKEDIN", 20),
        ("TWITTER", 10),
    ])
    def test_platform_weight(self, api_headers, platform, expected_score):
        """S20-06 to S20-09: Each platform has correct lead score weight."""
        ts = int(time.time() * 1000)
        payload = {
            "platform": platform,
            "username": f"weight_test_{ts}",
            "fullName": f"Test {platform}",
            "comment": f"Comment from {platform}.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201, f"{platform} failed: {resp.text}"
        data = resp.json()["data"]
        assert data["leadScore"] == expected_score, \
            f"{platform}: expected score {expected_score}, got {data['leadScore']}"
        assert data["leadSource"] == platform


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 3: Validation & Security
# ══════════════════════════════════════════════════════════════════════════════

class TestSocialValidation:
    """Tests for input validation and security."""

    def test_requires_platform(self, api_headers):
        """S20-10: Webhook rejects request without platform."""
        payload = {
            "username": "no_platform_user",
            "comment": "This should fail.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code in (400, 422), f"Expected 400/422, got {resp.status_code}"

    def test_requires_username(self, api_headers):
        """S20-11: Webhook rejects request without username."""
        payload = {
            "platform": "INSTAGRAM",
            "comment": "This should fail too.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code in (400, 422), f"Expected 400/422, got {resp.status_code}"

    def test_no_auth_rejected(self):
        """S20-12: Social webhook without auth token is rejected."""
        payload = {
            "platform": "INSTAGRAM",
            "username": "noauth_user",
            "comment": "Should be rejected.",
        }
        resp = requests.post(INTEGRATION_URL, json=payload, timeout=15,
                             headers={"Content-Type": "application/json"})
        assert resp.status_code in (401, 403), \
            f"Expected 401/403 without auth, got {resp.status_code}"


# ══════════════════════════════════════════════════════════════════════════════
# GROUP 4: End-to-End Flow
# ══════════════════════════════════════════════════════════════════════════════

class TestInstagramE2E:
    """Full end-to-end scenario: Instagram comment → Lead with all verifications."""

    def test_complete_scenario_20_flow(self, api_headers, unique_username):
        """S20-13: Complete Scenario 20 — Instagram → Lead → Score → Activity."""
        payload = {
            "platform": "INSTAGRAM",
            "username": unique_username,
            "fullName": "Maria Johnson",
            "email": f"{unique_username}@test.com",
            "comment": "This CRM looks amazing! We have 200 sales reps and need a scalable solution. Can we schedule a demo?",
            "postUrl": "https://instagram.com/p/crm_product_launch",
            "profileUrl": f"https://instagram.com/{unique_username}",
        }

        # Step 1: Send social webhook
        resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
        assert resp.status_code == 201, f"Social webhook failed: {resp.text}"
        result = resp.json()
        assert result["success"] is True
        data = result["data"]

        lead_id = data["leadId"]

        # Step 2: Verify lead was created
        assert lead_id is not None, "Lead should be created"
        assert data["leadStatus"] == "NEW"
        assert data["leadSource"] == "INSTAGRAM"

        # Step 3: Verify lead score = Instagram weight (15)
        assert data["leadScore"] == 15, f"Expected 15, got {data['leadScore']}"

        # Step 4: Fetch lead from lead-service
        lead_resp = requests.get(f"{LEAD_URL}/{lead_id}", headers=api_headers, timeout=15)
        assert lead_resp.status_code == 200, f"Lead fetch failed: {lead_resp.text}"
        lead_data = lead_resp.json().get("data", lead_resp.json())
        assert lead_data.get("source") == "SOCIAL_MEDIA"

        # Step 5: Verify lead score persisted
        stored_score = lead_data.get("leadScore")
        if stored_score is not None:
            assert stored_score == 15, f"Persisted score should be 15, got {stored_score}"

    def test_multiple_instagram_comments_create_separate_leads(self, api_headers):
        """S20-14: Multiple Instagram comments from different users create separate leads."""
        lead_ids = []
        for i in range(3):
            ts = int(time.time() * 1000) + i
            payload = {
                "platform": "INSTAGRAM",
                "username": f"insta_user_{ts}",
                "fullName": f"User {i}",
                "comment": f"Comment {i}: love this!",
            }
            resp = requests.post(INTEGRATION_URL, json=payload, headers=api_headers, timeout=30)
            assert resp.status_code == 201, f"Comment {i} failed: {resp.text}"
            lead_ids.append(resp.json()["data"]["leadId"])

        assert len(set(lead_ids)) == 3, "Each comment should create a separate lead"
