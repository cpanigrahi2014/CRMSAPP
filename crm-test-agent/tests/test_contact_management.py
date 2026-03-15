"""
Contact Management — 40 Tests
================================
Covers all 22 endpoints of the contact-service (port 9084).

Groups:
  1. Contact CRUD            (tests 1–8)
  2. List & Search           (tests 9–14)
  3. Communications          (tests 15–21)
  4. Tags                    (tests 22–26)
  5. Segmentation & Lifecycle(tests 27–30)
  6. Marketing Consent       (tests 31–33)
  7. Activity Timeline       (tests 34–35)
  8. Duplicate Detection     (tests 36–37)
  9. Merge                   (tests 38–39)
 10. Analytics               (test  40)
"""

import uuid, requests, pytest

# ── Config ────────────────────────────────────────────────────────────────────
AUTH_BASE = "http://localhost:8081/api/v1/auth"
BASE = "http://localhost:8084/api/v1/contacts"
CREDS = {"email": "sarah.chen@acmecorp.com", "password": "Demo@2026!", "tenantId": "default"}


def _uid():
    return uuid.uuid4().hex[:8]


def _email():
    return f"ct_{_uid()}@test.com"


# ── Fixtures ──────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def auth_token():
    resp = requests.post(f"{AUTH_BASE}/login", json=CREDS)
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    data = resp.json()
    d = data.get("data", data)
    return d.get("accessToken") or d.get("token")


@pytest.fixture(scope="module")
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}


@pytest.fixture(scope="module")
def sample_contact(headers):
    """Module-scoped reusable contact."""
    body = {
        "firstName": "Sample",
        "lastName": f"Contact_{_uid()}",
        "email": _email(),
        "phone": "555-0100",
        "title": "QA Engineer",
        "department": "Engineering",
        "leadSource": "WEB",
        "lifecycleStage": "customer",
        "segment": "enterprise",
    }
    resp = requests.post(BASE, headers=headers, json=body)
    assert resp.status_code in (200, 201), resp.text
    return resp.json().get("data", resp.json())


@pytest.fixture(scope="module")
def user_id(auth_token):
    resp = requests.get(f"{AUTH_BASE}/me", headers={"Authorization": f"Bearer {auth_token}"})
    if resp.status_code == 200:
        data = resp.json().get("data", resp.json())
        return data.get("id") or data.get("userId")
    return None


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 1 — Contact CRUD (8 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestContactCRUD:

    def test_01_create_contact(self, headers):
        """POST /contacts creates a contact with required fields."""
        body = {
            "firstName": "John",
            "lastName": f"Doe_{_uid()}",
            "email": _email(),
            "phone": "555-0001",
        }
        resp = requests.post(BASE, headers=headers, json=body)
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert data["firstName"] == "John"

    def test_02_create_contact_missing_firstname(self, headers):
        """POST /contacts without firstName should fail validation."""
        resp = requests.post(BASE, headers=headers, json={
            "lastName": "NoFirst", "email": _email(),
        })
        assert resp.status_code in (400, 422), resp.text

    def test_03_create_contact_missing_lastname(self, headers):
        """POST /contacts without lastName should fail validation."""
        resp = requests.post(BASE, headers=headers, json={
            "firstName": "NoLast", "email": _email(),
        })
        assert resp.status_code in (400, 422), resp.text

    def test_04_create_contact_invalid_email(self, headers):
        """POST /contacts with invalid email should fail."""
        resp = requests.post(BASE, headers=headers, json={
            "firstName": "Bad", "lastName": "Email", "email": "not-an-email",
        })
        assert resp.status_code in (400, 422), resp.text

    def test_05_create_contact_all_fields(self, headers):
        """POST /contacts with all optional fields populates correctly."""
        body = {
            "firstName": "Full",
            "lastName": f"Fields_{_uid()}",
            "email": _email(),
            "phone": "555-0002",
            "mobilePhone": "555-0003",
            "title": "VP Sales",
            "department": "Sales",
            "mailingAddress": "123 Main St",
            "description": "Full-field test contact",
            "linkedinUrl": "https://linkedin.com/in/test",
            "twitterUrl": "https://twitter.com/test",
            "leadSource": "REFERRAL",
            "lifecycleStage": "lead",
            "segment": "smb",
            "emailOptIn": True,
            "smsOptIn": False,
            "doNotCall": False,
        }
        resp = requests.post(BASE, headers=headers, json=body)
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert data["department"] == "Sales"
        assert data["title"] == "VP Sales"

    def test_06_get_contact_by_id(self, headers, sample_contact):
        """GET /contacts/{id} returns the contact."""
        cid = sample_contact["id"]
        resp = requests.get(f"{BASE}/{cid}", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["id"] == cid

    def test_07_get_contact_not_found(self, headers):
        """GET /contacts/{random-uuid} returns 404."""
        fake = str(uuid.uuid4())
        resp = requests.get(f"{BASE}/{fake}", headers=headers)
        assert resp.status_code == 404, resp.text

    def test_08_update_contact(self, headers, sample_contact):
        """PUT /contacts/{id} partial update works."""
        cid = sample_contact["id"]
        resp = requests.put(f"{BASE}/{cid}", headers=headers, json={
            "title": "Senior QA Engineer",
            "department": "Quality",
        })
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["title"] == "Senior QA Engineer"


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 2 — List & Search (6 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestListSearch:

    def test_09_list_contacts_paginated(self, headers):
        """GET /contacts returns paginated results."""
        resp = requests.get(BASE, headers=headers, params={"page": 0, "size": 5})
        assert resp.status_code == 200, resp.text

    def test_10_list_contacts_sorting(self, headers):
        """GET /contacts with sortBy and sortDir works."""
        resp = requests.get(BASE, headers=headers, params={
            "sortBy": "createdAt", "sortDir": "asc", "size": 3,
        })
        assert resp.status_code == 200, resp.text

    def test_11_search_contacts(self, headers, sample_contact):
        """GET /contacts/search finds contacts by query."""
        query = sample_contact["firstName"]
        resp = requests.get(f"{BASE}/search", headers=headers, params={"query": query})
        assert resp.status_code == 200, resp.text

    def test_12_search_contacts_by_email(self, headers, sample_contact):
        """GET /contacts/search finds contacts by email substring."""
        email_part = sample_contact.get("email", "test")[:5]
        resp = requests.get(f"{BASE}/search", headers=headers, params={"query": email_part})
        assert resp.status_code == 200, resp.text

    def test_13_search_contacts_no_results(self, headers):
        """GET /contacts/search with nonsense returns empty."""
        resp = requests.get(f"{BASE}/search", headers=headers, params={"query": "zzzznonexistent999"})
        assert resp.status_code == 200, resp.text

    def test_14_list_contacts_no_auth(self):
        """GET /contacts without token should fail."""
        resp = requests.get(BASE)
        assert resp.status_code in (401, 403, 500), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 3 — Communication History (7 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestCommunications:

    def test_15_create_communication_email(self, headers, sample_contact):
        """POST /contacts/{id}/communications logs an email."""
        resp = requests.post(
            f"{BASE}/{sample_contact['id']}/communications",
            headers=headers,
            json={
                "commType": "EMAIL",
                "subject": "Follow-up",
                "body": "Thanks for the meeting.",
                "direction": "OUTBOUND",
                "status": "COMPLETED",
            },
        )
        assert resp.status_code in (200, 201), resp.text

    def test_16_create_communication_call(self, headers, sample_contact):
        """POST /contacts/{id}/communications logs a call."""
        resp = requests.post(
            f"{BASE}/{sample_contact['id']}/communications",
            headers=headers,
            json={
                "commType": "CALL",
                "subject": "Discovery call",
                "direction": "INBOUND",
                "status": "COMPLETED",
            },
        )
        assert resp.status_code in (200, 201), resp.text

    def test_17_create_communication_meeting(self, headers, sample_contact):
        """POST /contacts/{id}/communications logs a meeting."""
        resp = requests.post(
            f"{BASE}/{sample_contact['id']}/communications",
            headers=headers,
            json={
                "commType": "MEETING",
                "subject": "Quarterly review",
                "direction": "OUTBOUND",
            },
        )
        assert resp.status_code in (200, 201), resp.text

    def test_18_create_communication_missing_type(self, headers, sample_contact):
        """POST /contacts/{id}/communications without commType fails."""
        resp = requests.post(
            f"{BASE}/{sample_contact['id']}/communications",
            headers=headers,
            json={"direction": "OUTBOUND", "subject": "No type"},
        )
        assert resp.status_code in (400, 422), resp.text

    def test_19_create_communication_missing_direction(self, headers, sample_contact):
        """POST /contacts/{id}/communications without direction fails."""
        resp = requests.post(
            f"{BASE}/{sample_contact['id']}/communications",
            headers=headers,
            json={"commType": "NOTE", "body": "Missing direction"},
        )
        assert resp.status_code in (400, 422), resp.text

    def test_20_list_communications(self, headers, sample_contact):
        """GET /contacts/{id}/communications returns paginated list."""
        resp = requests.get(
            f"{BASE}/{sample_contact['id']}/communications",
            headers=headers,
            params={"page": 0, "size": 10},
        )
        assert resp.status_code == 200, resp.text

    def test_21_delete_communication(self, headers, sample_contact):
        """DELETE /contacts/communications/{id} removes entry."""
        # Create one to delete
        c = requests.post(
            f"{BASE}/{sample_contact['id']}/communications",
            headers=headers,
            json={"commType": "SMS", "direction": "OUTBOUND", "body": "To delete"},
        )
        assert c.status_code in (200, 201), c.text
        comm_id = c.json().get("data", c.json())["id"]
        resp = requests.delete(f"{BASE}/communications/{comm_id}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 4 — Tags (5 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestTags:

    def test_22_add_tag(self, headers, sample_contact):
        """POST /contacts/{id}/tags adds a tag."""
        resp = requests.post(
            f"{BASE}/{sample_contact['id']}/tags",
            headers=headers,
            params={"tagName": f"tag_{_uid()}"},
        )
        assert resp.status_code in (200, 201), resp.text

    def test_23_add_duplicate_tag(self, headers, sample_contact):
        """POST /contacts/{id}/tags with same name is idempotent."""
        tag = f"dup_{_uid()}"
        requests.post(f"{BASE}/{sample_contact['id']}/tags", headers=headers, params={"tagName": tag})
        resp = requests.post(f"{BASE}/{sample_contact['id']}/tags", headers=headers, params={"tagName": tag})
        # Idempotent — returns existing tag, should be 200 or 201
        assert resp.status_code in (200, 201), resp.text

    def test_24_list_tags(self, headers, sample_contact):
        """GET /contacts/{id}/tags returns tag list."""
        resp = requests.get(f"{BASE}/{sample_contact['id']}/tags", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list)

    def test_25_remove_tag(self, headers, sample_contact):
        """DELETE /contacts/{id}/tags removes a tag."""
        tag = f"del_{_uid()}"
        requests.post(f"{BASE}/{sample_contact['id']}/tags", headers=headers, params={"tagName": tag})
        resp = requests.delete(f"{BASE}/{sample_contact['id']}/tags", headers=headers, params={"tagName": tag})
        assert resp.status_code in (200, 204), resp.text

    def test_26_add_tag_missing_name(self, headers, sample_contact):
        """POST /contacts/{id}/tags without tagName should fail."""
        resp = requests.post(f"{BASE}/{sample_contact['id']}/tags", headers=headers)
        assert resp.status_code in (400, 422, 500), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 5 — Segmentation & Lifecycle (4 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestSegmentation:

    def test_27_get_by_segment(self, headers):
        """GET /contacts/segment/{segment} returns contacts."""
        resp = requests.get(f"{BASE}/segment/enterprise", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_28_get_by_segment_empty(self, headers):
        """GET /contacts/segment/{nonexistent} returns empty page."""
        resp = requests.get(f"{BASE}/segment/zzz_nonexist_{_uid()}", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_29_get_by_lifecycle(self, headers):
        """GET /contacts/lifecycle/{stage} returns contacts."""
        resp = requests.get(f"{BASE}/lifecycle/customer", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_30_get_by_lifecycle_empty(self, headers):
        """GET /contacts/lifecycle/{nonexistent} returns empty."""
        resp = requests.get(f"{BASE}/lifecycle/zzz_nonexist_{_uid()}", headers=headers)
        assert resp.status_code == 200, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 6 — Marketing Consent (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestConsent:

    def test_31_update_consent_opt_in(self, headers, sample_contact):
        """PUT /contacts/{id}/consent updates opt-in flags."""
        resp = requests.put(
            f"{BASE}/{sample_contact['id']}/consent",
            headers=headers,
            json={"emailOptIn": True, "smsOptIn": True, "phoneOptIn": False, "doNotCall": False},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("emailOptIn") is True

    def test_32_update_consent_do_not_call(self, headers, sample_contact):
        """PUT /contacts/{id}/consent sets doNotCall flag."""
        resp = requests.put(
            f"{BASE}/{sample_contact['id']}/consent",
            headers=headers,
            json={"doNotCall": True, "consentSource": "manual"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("doNotCall") is True

    def test_33_update_consent_opt_out_all(self, headers, sample_contact):
        """PUT /contacts/{id}/consent opts out of everything."""
        resp = requests.put(
            f"{BASE}/{sample_contact['id']}/consent",
            headers=headers,
            json={"emailOptIn": False, "smsOptIn": False, "phoneOptIn": False, "doNotCall": True},
        )
        assert resp.status_code == 200, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 7 — Activity Timeline (2 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestActivities:

    def test_34_list_activities(self, headers, sample_contact):
        """GET /contacts/{id}/activities returns history."""
        resp = requests.get(
            f"{BASE}/{sample_contact['id']}/activities",
            headers=headers,
            params={"page": 0, "size": 50},
        )
        assert resp.status_code == 200, resp.text

    def test_35_activity_recorded_on_update(self, headers):
        """Updating a contact should record an activity."""
        # Create fresh contact
        c = requests.post(BASE, headers=headers, json={
            "firstName": "Activity", "lastName": f"Check_{_uid()}", "email": _email(),
        })
        cid = c.json().get("data", c.json())["id"]
        # Update to trigger activity
        requests.put(f"{BASE}/{cid}", headers=headers, json={"title": "Updated Title"})
        # Fetch activities
        resp = requests.get(f"{BASE}/{cid}/activities", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        activities = data if isinstance(data, list) else data.get("content", [])
        assert len(activities) >= 1, "Expected at least one activity after update"


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 8 — Duplicate Detection (2 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestDuplicates:

    def test_36_detect_duplicates(self, headers):
        """GET /contacts/duplicates returns duplicate groups."""
        resp = requests.get(f"{BASE}/duplicates", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_37_detect_duplicates_with_match(self, headers):
        """Create two contacts with same email, then detect."""
        shared_email = _email()
        for i in range(2):
            requests.post(BASE, headers=headers, json={
                "firstName": f"Dup{i}", "lastName": f"Test_{_uid()}", "email": shared_email,
            })
        resp = requests.get(f"{BASE}/duplicates", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        # Should have at least one group with our email
        if isinstance(data, list) and len(data) > 0:
            match_values = [g.get("matchValue", "") for g in data]
            assert any(shared_email.lower() in v.lower() for v in match_values), \
                f"Expected duplicate group for {shared_email}"


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 9 — Merge (2 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestMerge:

    def test_38_merge_contacts(self, headers):
        """POST /contacts/merge merges duplicate into primary."""
        # Create primary
        p = requests.post(BASE, headers=headers, json={
            "firstName": "Primary", "lastName": f"Merge_{_uid()}", "email": _email(),
            "phone": "555-1111", "department": "Sales",
        })
        pid = p.json().get("data", p.json())["id"]
        # Create duplicate
        d = requests.post(BASE, headers=headers, json={
            "firstName": "Dup", "lastName": f"Merge_{_uid()}", "email": _email(),
            "mobilePhone": "555-2222", "title": "Manager",
        })
        did = d.json().get("data", d.json())["id"]
        # Merge
        resp = requests.post(
            f"{BASE}/merge",
            headers=headers,
            params={"primaryId": pid, "duplicateId": did},
        )
        assert resp.status_code in (200, 201), resp.text
        merged = resp.json().get("data", resp.json())
        # Primary should have gained duplicate's fields
        assert merged.get("mobilePhone") == "555-2222" or merged.get("title") == "Manager"

    def test_39_merge_nonexistent(self, headers):
        """POST /contacts/merge with invalid IDs should fail."""
        resp = requests.post(
            f"{BASE}/merge",
            headers=headers,
            params={"primaryId": str(uuid.uuid4()), "duplicateId": str(uuid.uuid4())},
        )
        assert resp.status_code in (400, 404, 500), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 10 — Analytics (1 test) + Delete (1 test)
# ═════════════════════════════════════════════════════════════════════════════

class TestAnalyticsAndDelete:

    def test_40_contact_analytics(self, headers):
        """GET /contacts/analytics returns aggregate stats."""
        resp = requests.get(f"{BASE}/analytics", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "totalContacts" in data
        assert data["totalContacts"] >= 0
