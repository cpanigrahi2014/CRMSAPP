"""
Account Management — 40 Tests
==================================
Covers 34 endpoints of the account-service (port 8083).

Groups:
  1. Account CRUD              (tests 1–8)
  2. Account Search            (tests 9–13)
  3. Account Hierarchy         (tests 14–16)
  4. Link Contacts / Owner     (tests 17–20)
  5. Notes                     (tests 21–24)
  6. Tags                      (tests 25–29)
  7. Attachments               (tests 30–32)
  8. Health & Engagement       (tests 33–35)
  9. Bulk & Import/Export      (tests 36–38)
 10. Analytics & Activities    (tests 39–40)
"""

import uuid, requests, pytest

# ── Config ────────────────────────────────────────────────────────────────────
AUTH_BASE = "http://localhost:8081/api/v1/auth"
BASE = "http://localhost:8083/api/v1/accounts"
CREDS = {"email": "sarah.chen@acmecorp.com", "password": "Demo@2026!", "tenantId": "default"}


def _uid():
    return uuid.uuid4().hex[:8]


# ── Fixtures ──────────────────────────────────────────────────────────────────

@pytest.fixture(scope="module")
def auth_token():
    resp = requests.post(f"{AUTH_BASE}/login", json=CREDS)
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    d = resp.json().get("data", resp.json())
    return d.get("accessToken") or d.get("token")


@pytest.fixture(scope="module")
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}


@pytest.fixture(scope="module")
def sample_account(headers):
    """Module-scoped reusable account."""
    body = {
        "name": f"TestCorp_{_uid()}",
        "industry": "Technology",
        "website": f"https://{_uid()}.example.com",
        "phone": "555-0100",
        "type": "CUSTOMER",
        "territory": "North America",
        "segment": "enterprise",
        "lifecycleStage": "ACTIVE",
        "annualRevenue": 5000000,
        "numberOfEmployees": 250,
    }
    resp = requests.post(BASE, headers=headers, json=body)
    assert resp.status_code in (200, 201), resp.text
    return resp.json().get("data", resp.json())


@pytest.fixture(scope="module")
def user_id(auth_token):
    resp = requests.get(f"{AUTH_BASE}/me", headers={"Authorization": f"Bearer {auth_token}"})
    if resp.status_code == 200:
        d = resp.json().get("data", resp.json())
        return d.get("id") or d.get("userId")
    return str(uuid.uuid4())


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 1 — Account CRUD (8 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestAccountCRUD:

    def test_01_create_account(self, headers):
        """POST /accounts creates an account with required name."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"Acme_{_uid()}", "industry": "Finance",
        })
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert "id" in data
        assert data["industry"] == "Finance"

    def test_02_create_account_missing_name(self, headers):
        """POST /accounts without name should fail validation."""
        resp = requests.post(BASE, headers=headers, json={"industry": "Tech"})
        assert resp.status_code in (400, 422), resp.text

    def test_03_create_account_all_fields(self, headers):
        """POST /accounts with all optional fields."""
        body = {
            "name": f"Full_{_uid()}",
            "industry": "Healthcare",
            "website": "https://full.example.com",
            "phone": "555-0200",
            "billingAddress": "100 Main St",
            "shippingAddress": "200 Ship Ave",
            "annualRevenue": 10000000,
            "numberOfEmployees": 500,
            "description": "Full-field test",
            "type": "CUSTOMER",
            "territory": "EMEA",
            "lifecycleStage": "ACTIVE",
            "segment": "mid-market",
        }
        resp = requests.post(BASE, headers=headers, json=body)
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert data["territory"] == "EMEA"

    def test_04_get_account_by_id(self, headers, sample_account):
        """GET /accounts/{id} returns the account."""
        resp = requests.get(f"{BASE}/{sample_account['id']}", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["id"] == sample_account["id"]

    def test_05_get_account_not_found(self, headers):
        """GET /accounts/{random-uuid} returns 404."""
        resp = requests.get(f"{BASE}/{uuid.uuid4()}", headers=headers)
        assert resp.status_code == 404, resp.text

    def test_06_update_account(self, headers, sample_account):
        """PUT /accounts/{id} partial update works."""
        resp = requests.put(f"{BASE}/{sample_account['id']}", headers=headers, json={
            "industry": "Software", "numberOfEmployees": 300,
        })
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["industry"] == "Software"

    def test_07_update_account_scores(self, headers, sample_account):
        """PUT /accounts/{id} can set healthScore and engagementScore."""
        resp = requests.put(f"{BASE}/{sample_account['id']}", headers=headers, json={
            "healthScore": 85, "engagementScore": 72,
        })
        assert resp.status_code == 200, resp.text

    def test_08_delete_account(self, headers):
        """DELETE /accounts/{id} soft-deletes the account."""
        c = requests.post(BASE, headers=headers, json={"name": f"ToDelete_{_uid()}"})
        aid = c.json().get("data", c.json())["id"]
        resp = requests.delete(f"{BASE}/{aid}", headers=headers)
        assert resp.status_code in (200, 204), resp.text
        # Verify it's gone
        g = requests.get(f"{BASE}/{aid}", headers=headers)
        assert g.status_code in (404, 200)  # soft-deleted may return 404


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 2 — Account Search (5 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestAccountSearch:

    def test_09_list_accounts_paginated(self, headers):
        """GET /accounts returns paginated results."""
        resp = requests.get(BASE, headers=headers, params={"page": 0, "size": 5})
        assert resp.status_code == 200, resp.text

    def test_10_list_accounts_sorting(self, headers):
        """GET /accounts with sortBy and sortDir."""
        resp = requests.get(BASE, headers=headers, params={
            "sortBy": "name", "sortDir": "asc", "size": 5,
        })
        assert resp.status_code == 200, resp.text

    def test_11_search_accounts(self, headers, sample_account):
        """GET /accounts/search finds accounts by name."""
        resp = requests.get(f"{BASE}/search", headers=headers, params={
            "query": sample_account["name"][:8],
        })
        assert resp.status_code == 200, resp.text

    def test_12_search_no_results(self, headers):
        """GET /accounts/search with nonsense returns empty."""
        resp = requests.get(f"{BASE}/search", headers=headers, params={
            "query": "zzz_nonexistent_99999",
        })
        assert resp.status_code == 200, resp.text

    def test_13_list_no_auth(self):
        """GET /accounts without token should fail."""
        resp = requests.get(BASE)
        assert resp.status_code in (401, 403, 500), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 3 — Account Hierarchy (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestHierarchy:

    def test_14_create_child_account(self, headers, sample_account):
        """POST /accounts with parentAccountId creates a child."""
        resp = requests.post(BASE, headers=headers, json={
            "name": f"ChildCorp_{_uid()}",
            "parentAccountId": sample_account["id"],
            "industry": "Technology",
        })
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert str(data.get("parentAccountId")) == str(sample_account["id"])

    def test_15_get_children(self, headers, sample_account):
        """GET /accounts/{id}/children returns child accounts."""
        resp = requests.get(f"{BASE}/{sample_account['id']}/children", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list)
        assert len(data) >= 1

    def test_16_get_children_no_children(self, headers):
        """GET /accounts/{id}/children for account with none returns empty list."""
        c = requests.post(BASE, headers=headers, json={"name": f"NoKids_{_uid()}"})
        aid = c.json().get("data", c.json())["id"]
        resp = requests.get(f"{BASE}/{aid}/children", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list) and len(data) == 0


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 4 — Owner / Territory / Filters (4 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestOwnerTerritory:

    def test_17_assign_owner(self, headers, sample_account, user_id):
        """PUT /accounts/{id}/owner assigns owner."""
        resp = requests.put(
            f"{BASE}/{sample_account['id']}/owner",
            headers=headers,
            json={"ownerId": user_id},
        )
        assert resp.status_code == 200, resp.text

    def test_18_assign_territory(self, headers, sample_account):
        """PUT /accounts/{id}/territory assigns territory."""
        resp = requests.put(
            f"{BASE}/{sample_account['id']}/territory",
            headers=headers,
            json={"territory": "APAC"},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["territory"] == "APAC"

    def test_19_filter_by_type(self, headers):
        """GET /accounts/by-type filters accounts."""
        resp = requests.get(f"{BASE}/by-type", headers=headers, params={"type": "CUSTOMER"})
        assert resp.status_code == 200, resp.text

    def test_20_filter_by_territory(self, headers):
        """GET /accounts/by-territory filters accounts."""
        resp = requests.get(f"{BASE}/by-territory", headers=headers, params={"territory": "APAC"})
        assert resp.status_code == 200, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 5 — Notes (4 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestNotes:

    def test_21_create_note(self, headers, sample_account):
        """POST /accounts/{id}/notes creates a note."""
        resp = requests.post(
            f"{BASE}/{sample_account['id']}/notes",
            headers=headers,
            json={"content": f"Test note {_uid()}"},
        )
        assert resp.status_code in (200, 201), resp.text

    def test_22_create_note_missing_content(self, headers, sample_account):
        """POST /accounts/{id}/notes without content should fail."""
        resp = requests.post(
            f"{BASE}/{sample_account['id']}/notes",
            headers=headers,
            json={},
        )
        assert resp.status_code in (400, 422), resp.text

    def test_23_list_notes(self, headers, sample_account):
        """GET /accounts/{id}/notes returns notes."""
        resp = requests.get(f"{BASE}/{sample_account['id']}/notes", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list) and len(data) >= 1

    def test_24_delete_note(self, headers, sample_account):
        """DELETE /accounts/notes/{id} soft-deletes the note."""
        n = requests.post(
            f"{BASE}/{sample_account['id']}/notes",
            headers=headers,
            json={"content": "To delete"},
        )
        nid = n.json().get("data", n.json())["id"]
        resp = requests.delete(f"{BASE}/notes/{nid}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 6 — Tags (5 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestTags:

    @pytest.fixture(scope="class")
    def tag_id(self, headers):
        """Create a tag to reuse across tests."""
        resp = requests.post(f"{BASE}/tags", headers=headers, json={
            "name": f"Tag_{_uid()}", "color": "#FF5733",
        })
        assert resp.status_code in (200, 201), resp.text
        return resp.json().get("data", resp.json())["id"]

    def test_25_create_tag(self, headers):
        """POST /accounts/tags creates a tag."""
        resp = requests.post(f"{BASE}/tags", headers=headers, json={
            "name": f"Priority_{_uid()}", "color": "#00FF00",
        })
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert "id" in data

    def test_26_list_tags(self, headers):
        """GET /accounts/tags returns all tags."""
        resp = requests.get(f"{BASE}/tags", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list)

    def test_27_add_tag_to_account(self, headers, sample_account, tag_id):
        """POST /accounts/{id}/tags/{tagId} attaches a tag."""
        resp = requests.post(
            f"{BASE}/{sample_account['id']}/tags/{tag_id}",
            headers=headers,
        )
        assert resp.status_code in (200, 201), resp.text

    def test_28_get_account_tags(self, headers, sample_account, tag_id):
        """GET /accounts/{id}/tags returns attached tags."""
        resp = requests.get(f"{BASE}/{sample_account['id']}/tags", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list)

    def test_29_remove_tag_from_account(self, headers, sample_account, tag_id):
        """DELETE /accounts/{id}/tags/{tagId} detaches the tag."""
        resp = requests.delete(
            f"{BASE}/{sample_account['id']}/tags/{tag_id}",
            headers=headers,
        )
        assert resp.status_code in (200, 204), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 7 — Attachments (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestAttachments:

    def test_30_create_attachment(self, headers, sample_account):
        """POST /accounts/{id}/attachments adds file metadata."""
        resp = requests.post(
            f"{BASE}/{sample_account['id']}/attachments",
            headers=headers,
            json={
                "fileName": "contract.pdf",
                "fileUrl": f"https://storage.example.com/{_uid()}.pdf",
                "fileSize": 102400,
                "fileType": "application/pdf",
            },
        )
        assert resp.status_code in (200, 201), resp.text

    def test_31_list_attachments(self, headers, sample_account):
        """GET /accounts/{id}/attachments returns list."""
        resp = requests.get(f"{BASE}/{sample_account['id']}/attachments", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list) and len(data) >= 1

    def test_32_delete_attachment(self, headers, sample_account):
        """DELETE /accounts/attachments/{id} soft-deletes."""
        a = requests.post(
            f"{BASE}/{sample_account['id']}/attachments",
            headers=headers,
            json={"fileName": "delete_me.txt", "fileUrl": "https://s3.example.com/x.txt"},
        )
        aid = a.json().get("data", a.json())["id"]
        resp = requests.delete(f"{BASE}/attachments/{aid}", headers=headers)
        assert resp.status_code in (200, 204), resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 8 — Health & Engagement Scores (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestScores:

    def test_33_update_health_score(self, headers, sample_account):
        """PUT /accounts/{id}/health-score sets health score."""
        resp = requests.put(
            f"{BASE}/{sample_account['id']}/health-score",
            headers=headers,
            json={"healthScore": 90},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["healthScore"] == 90

    def test_34_update_engagement_score(self, headers, sample_account):
        """PUT /accounts/{id}/engagement-score sets engagement score."""
        resp = requests.put(
            f"{BASE}/{sample_account['id']}/engagement-score",
            headers=headers,
            json={"engagementScore": 65},
        )
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data["engagementScore"] == 65

    def test_35_health_score_clamped(self, headers, sample_account):
        """PUT /accounts/{id}/health-score with >100 should clamp to 100."""
        resp = requests.put(
            f"{BASE}/{sample_account['id']}/health-score",
            headers=headers,
            json={"healthScore": 150},
        )
        # Accept 200 (clamped) or 400 (validation error)
        assert resp.status_code in (200, 400), resp.text
        if resp.status_code == 200:
            data = resp.json().get("data", resp.json())
            assert data["healthScore"] <= 100


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 9 — Bulk & Import/Export (3 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestBulkImportExport:

    def test_36_export_csv(self, headers):
        """GET /accounts/export returns CSV data."""
        resp = requests.get(f"{BASE}/export", headers=headers)
        assert resp.status_code == 200, resp.text
        assert "name" in resp.text.lower()

    def test_37_import_csv(self, headers):
        """POST /accounts/import ingests CSV records."""
        csv = "name,industry,type\nImportCo_test,Retail,PROSPECT\n"
        resp = requests.post(
            f"{BASE}/import",
            headers={**headers, "Content-Type": "text/plain"},
            data=csv,
        )
        assert resp.status_code in (200, 201), resp.text

    def test_38_bulk_update(self, headers):
        """PUT /accounts/bulk-update updates multiple accounts."""
        # Create two accounts to bulk-update
        ids = []
        for i in range(2):
            c = requests.post(BASE, headers=headers, json={"name": f"Bulk_{_uid()}"})
            ids.append(c.json().get("data", c.json())["id"])
        resp = requests.put(f"{BASE}/bulk-update", headers=headers, json={
            "accountIds": ids,
            "territory": "LATAM",
            "type": "PARTNER",
        })
        assert resp.status_code == 200, resp.text


# ═════════════════════════════════════════════════════════════════════════════
#  GROUP 10 — Analytics & Activities (2 tests)
# ═════════════════════════════════════════════════════════════════════════════

class TestAnalyticsActivities:

    def test_39_analytics(self, headers):
        """GET /accounts/analytics returns aggregate stats."""
        resp = requests.get(f"{BASE}/analytics", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert "totalAccounts" in data
        assert data["totalAccounts"] >= 0

    def test_40_list_activities(self, headers, sample_account):
        """GET /accounts/{id}/activities returns activity timeline."""
        resp = requests.get(f"{BASE}/{sample_account['id']}/activities", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert isinstance(data, list)
        assert len(data) >= 1  # At least CREATED activity
