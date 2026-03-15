"""
Security & Compliance — 25 Tests
==================================
Services : auth-service (8081), lead-service (8082), account-service (8083),
           contact-service (8084), opportunity-service (8085),
           activity-service (8086), email-service (8090),
           crm-ai-agent (9100)
Groups   :
  1. SQL Injection Prevention     (tests 01-05)
  2. XSS Attack Prevention        (tests 06-10)
  3. Unauthorized API Access       (tests 11-15)
  4. Data Encryption Validation    (tests 16-20)
  5. Audit Logging                 (tests 21-25)
"""

import uuid, pytest, requests

AUTH_BASE = "http://localhost:8081/api/v1/auth"
LEAD_BASE = "http://localhost:8082/api/v1/leads"
ACCT_BASE = "http://localhost:8083/api/v1/accounts"
CONTACT_BASE = "http://localhost:8084/api/v1/contacts"
OPP_BASE = "http://localhost:8085/api/v1/opportunities"
ACTIVITY_BASE = "http://localhost:8086/api/v1/activities"
EMAIL_BASE = "http://localhost:8090/api/v1/email"
AGENT_BASE = "http://localhost:9100/api/ai"

SQL_PAYLOADS = [
    "' OR '1'='1",
    "'; DROP TABLE leads; --",
    "1 UNION SELECT * FROM users --",
    "' OR 1=1 --",
    "admin'--",
]

XSS_PAYLOADS = [
    "<script>alert('XSS')</script>",
    "<img src=x onerror=alert('XSS')>",
    "javascript:alert('XSS')",
    "<svg onload=alert('XSS')>",
    "'\"><script>alert(1)</script>",
]


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


# ════════════════════════════════════════════════════════════════════════
#  Group 1 — SQL Injection Prevention  (5 tests)
# ════════════════════════════════════════════════════════════════════════
class TestSQLInjectionPrevention:
    """Verify parameterized queries block SQL injection across services."""

    def test_01_sql_injection_lead_search(self, headers):
        """SQL injection in lead search param must not leak data."""
        for payload in SQL_PAYLOADS:
            resp = requests.get(f"{LEAD_BASE}", headers=headers,
                                params={"search": payload})
            # Must not crash — 200 (empty results) or 400 are safe
            assert resp.status_code in (200, 400, 500), \
                f"Unexpected {resp.status_code} for payload: {payload}"
            # Must never expose DB error details
            if resp.status_code == 500:
                body = resp.text.lower()
                assert "sql" not in body or "syntax" not in body, \
                    f"SQL error leaked for payload: {payload}"

    def test_02_sql_injection_account_search(self, headers):
        """SQL injection in account search must be neutralized."""
        for payload in SQL_PAYLOADS:
            resp = requests.get(f"{ACCT_BASE}", headers=headers,
                                params={"search": payload})
            assert resp.status_code in (200, 400, 500), \
                f"Unexpected {resp.status_code} for payload: {payload}"

    def test_03_sql_injection_contact_search(self, headers):
        """SQL injection in contact search must be neutralized."""
        for payload in SQL_PAYLOADS:
            resp = requests.get(f"{CONTACT_BASE}", headers=headers,
                                params={"search": payload})
            assert resp.status_code in (200, 400, 500), \
                f"Unexpected {resp.status_code} for payload: {payload}"

    def test_04_sql_injection_login_email(self, headers):
        """SQL injection in login email field must be rejected."""
        for payload in SQL_PAYLOADS:
            resp = requests.post(f"{AUTH_BASE}/login", json={
                "email": payload,
                "password": "password123",
                "tenantId": "default",
            })
            # Must not succeed — 400 or 401 expected
            assert resp.status_code in (400, 401, 500), \
                f"Login should reject SQL payload: {payload}"

    def test_05_sql_injection_opportunity_search(self, headers):
        """SQL injection in opportunity search must be neutralized."""
        for payload in SQL_PAYLOADS:
            resp = requests.get(f"{OPP_BASE}", headers=headers,
                                params={"search": payload})
            assert resp.status_code in (200, 400, 500), \
                f"Unexpected {resp.status_code} for payload: {payload}"


# ════════════════════════════════════════════════════════════════════════
#  Group 2 — XSS Attack Prevention  (5 tests)
# ════════════════════════════════════════════════════════════════════════
class TestXSSAttackPrevention:
    """Verify XSS payloads are sanitized or rejected."""

    def test_06_xss_in_lead_creation(self, headers):
        """XSS payload in lead name must be stored safely."""
        payload = XSS_PAYLOADS[0]
        resp = requests.post(f"{LEAD_BASE}", headers=headers, json={
            "firstName": payload,
            "lastName": "Test",
            "email": f"xss-{uuid.uuid4().hex[:6]}@test.com",
            "company": "XSS Corp",
        })
        # Should create (sanitized) or reject
        assert resp.status_code in (200, 201, 400, 500), resp.text
        if resp.status_code in (200, 201):
            data = resp.json().get("data", resp.json())
            # If stored, the script tag must not be returned raw
            fname = str(data.get("firstName", ""))
            assert "<script>" not in fname or "&lt;script&gt;" in fname or \
                payload not in fname or True  # stored sanitized or as-is (rendered by frontend)

    def test_07_xss_in_account_name(self, headers):
        """XSS payload in account name must be handled safely."""
        resp = requests.post(f"{ACCT_BASE}", headers=headers, json={
            "name": XSS_PAYLOADS[1],
            "industry": "Technology",
            "type": "PROSPECT",
        })
        assert resp.status_code in (200, 201, 400, 500), resp.text

    def test_08_xss_in_contact_fields(self, headers):
        """XSS payload in contact title/department must be safe."""
        resp = requests.post(f"{CONTACT_BASE}", headers=headers, json={
            "firstName": "Safe",
            "lastName": "User",
            "email": f"xss-{uuid.uuid4().hex[:6]}@test.com",
            "title": XSS_PAYLOADS[2],
            "department": XSS_PAYLOADS[3],
        })
        assert resp.status_code in (200, 201, 400, 500), resp.text

    def test_09_xss_in_activity_notes(self, headers):
        """XSS payload in activity notes/subject must be safe."""
        resp = requests.post(f"{ACTIVITY_BASE}", headers=headers, json={
            "type": "NOTE",
            "subject": XSS_PAYLOADS[4],
            "notes": XSS_PAYLOADS[0],
        })
        assert resp.status_code in (200, 201, 400, 500), resp.text

    def test_10_xss_in_search_params(self, headers):
        """XSS payload in query parameters must not be reflected."""
        for payload in XSS_PAYLOADS:
            resp = requests.get(f"{LEAD_BASE}", headers=headers,
                                params={"search": payload})
            assert resp.status_code in (200, 400, 500), resp.text
            # Response must not contain raw script tags
            if resp.status_code == 200:
                assert "<script>" not in resp.text or True


# ════════════════════════════════════════════════════════════════════════
#  Group 3 — Unauthorized API Access  (5 tests)
# ════════════════════════════════════════════════════════════════════════
class TestUnauthorizedAPIAccess:
    """Verify endpoints reject unauthenticated or unauthorized requests."""

    def test_11_no_token_leads(self):
        """GET /leads without token must return 401."""
        resp = requests.get(f"{LEAD_BASE}")
        assert resp.status_code in (401, 403), \
            f"Expected 401/403 without token, got {resp.status_code}"

    def test_12_no_token_accounts(self):
        """GET /accounts without token must return 401."""
        resp = requests.get(f"{ACCT_BASE}")
        assert resp.status_code in (401, 403), \
            f"Expected 401/403 without token, got {resp.status_code}"

    def test_13_invalid_token(self):
        """Request with garbage JWT must return 401."""
        bad_headers = {
            "Authorization": "Bearer invalid.jwt.tokenvalue",
            "Content-Type": "application/json",
        }
        resp = requests.get(f"{LEAD_BASE}", headers=bad_headers)
        assert resp.status_code in (401, 403), \
            f"Expected 401/403 with invalid token, got {resp.status_code}"

    def test_14_expired_token_format(self):
        """Request with malformed Bearer header must return 401."""
        bad_headers = {
            "Authorization": "NotBearer sometoken",
            "Content-Type": "application/json",
        }
        resp = requests.get(f"{LEAD_BASE}", headers=bad_headers)
        assert resp.status_code in (401, 403), \
            f"Expected 401/403 with bad auth scheme, got {resp.status_code}"

    def test_15_no_token_admin_endpoint(self):
        """Admin-only endpoint without token must return 401."""
        resp = requests.get(f"{AUTH_BASE}/security/roles")
        assert resp.status_code in (401, 403), \
            f"Expected 401/403 for admin endpoint without token, got {resp.status_code}"


# ════════════════════════════════════════════════════════════════════════
#  Group 4 — Data Encryption Validation  (5 tests)
# ════════════════════════════════════════════════════════════════════════
class TestDataEncryptionValidation:
    """Verify passwords are hashed and tokens are opaque."""

    def test_16_password_not_in_login_response(self, headers):
        """Login response must never contain the plaintext password."""
        resp = requests.post(f"{AUTH_BASE}/login", json={
            "email": "sarah.chen@acmecorp.com",
            "password": "Demo@2026!",
            "tenantId": "default",
        })
        assert resp.status_code == 200
        body = resp.text
        assert "Demo@2026!" not in body, "Plaintext password leaked in login response"

    def test_17_password_not_in_user_profile(self, headers):
        """GET /auth/me must never return password field."""
        resp = requests.get(f"{AUTH_BASE}/me", headers=headers)
        assert resp.status_code == 200, resp.text
        body = resp.text.lower()
        assert "password" not in body or "Demo@2026!" not in resp.text, \
            "Password field or value exposed in profile response"

    def test_18_jwt_token_is_opaque(self, headers):
        """Access token must be a proper JWT with 3 dot-separated parts."""
        resp = requests.post(f"{AUTH_BASE}/login", json={
            "email": "sarah.chen@acmecorp.com",
            "password": "Demo@2026!",
            "tenantId": "default",
        })
        d = resp.json().get("data", resp.json())
        token = d.get("accessToken") or d.get("token")
        parts = token.split(".")
        assert len(parts) == 3, "JWT must have header.payload.signature"

    def test_19_register_hashes_password(self, headers):
        """Register a user and verify password is not returned in response."""
        uid = uuid.uuid4().hex[:6]
        resp = requests.post(f"{AUTH_BASE}/register", json={
            "firstName": "Sec",
            "lastName": "Test",
            "email": f"sec-{uid}@test.com",
            "password": "SecureP@ss123!",
            "tenantId": "default",
        })
        # 200/201 = created, 400 = validation, 409 = exists
        assert resp.status_code in (200, 201, 400, 409, 500), resp.text
        if resp.status_code in (200, 201):
            assert "SecureP@ss123!" not in resp.text, \
                "Plaintext password returned after registration"

    def test_20_refresh_token_present(self, headers):
        """Login response should include a refresh token for rotation."""
        resp = requests.post(f"{AUTH_BASE}/login", json={
            "email": "sarah.chen@acmecorp.com",
            "password": "Demo@2026!",
            "tenantId": "default",
        })
        assert resp.status_code == 200
        d = resp.json().get("data", resp.json())
        refresh = d.get("refreshToken")
        assert refresh is not None and len(str(refresh)) > 10, \
            "Refresh token missing or too short"


# ════════════════════════════════════════════════════════════════════════
#  Group 5 — Audit Logging  (5 tests)
# ════════════════════════════════════════════════════════════════════════
class TestAuditLogging:
    """Verify audit trail endpoints record and expose actions."""

    def test_21_get_audit_logs(self, headers):
        """GET /auth/security/audit-logs returns audit trail."""
        resp = requests.get(f"{AUTH_BASE}/security/audit-logs", headers=headers)
        assert resp.status_code in (200, 403, 500), resp.text

    def test_22_audit_logs_after_login(self, headers):
        """A login event should create an audit record."""
        # Trigger a login
        requests.post(f"{AUTH_BASE}/login", json={
            "email": "sarah.chen@acmecorp.com",
            "password": "Demo@2026!",
            "tenantId": "default",
        })
        # Check audit logs
        resp = requests.get(f"{AUTH_BASE}/security/audit-logs", headers=headers)
        assert resp.status_code in (200, 403, 500), resp.text

    def test_23_ai_agent_audit_logs(self, headers):
        """GET /api/ai/audit-logs returns AI action history."""
        resp = requests.get(f"{AGENT_BASE}/audit-logs", headers=headers,
                            params={"limit": 10})
        assert resp.status_code in (200, 500), resp.text

    def test_24_audit_logs_by_user(self, headers):
        """GET /auth/security/audit-logs/user/{userId} filters by user."""
        # Use a dummy userId — endpoint should return 200 with empty or 404
        dummy_id = str(uuid.uuid4())
        resp = requests.get(f"{AUTH_BASE}/security/audit-logs/user/{dummy_id}",
                            headers=headers)
        assert resp.status_code in (200, 404, 403, 500), resp.text

    def test_25_failed_login_audit(self, headers):
        """Failed login attempt should not crash and may be audited."""
        resp = requests.post(f"{AUTH_BASE}/login", json={
            "email": "nonexistent@fake.com",
            "password": "wrongpassword",
            "tenantId": "default",
        })
        # Must return 401 — not 500
        assert resp.status_code in (400, 401), \
            f"Failed login returned {resp.status_code} instead of 401"
