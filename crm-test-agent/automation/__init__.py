"""
CRM Automation Agent — UI + API Validation Framework
=====================================================
Master configuration and shared utilities for the combined
UI (Playwright) + API (requests) automation agent that covers
all 15 modules and 515 test cases.

Usage:
    cd crm-test-agent
    python -m pytest automation/ -v --tb=short
    python -m pytest automation/ -v -k "api"      # API only
    python -m pytest automation/ -v -k "ui"        # UI only
    python -m pytest automation/test_mod01_auth.py  # Single module
"""
from __future__ import annotations

import uuid, time, requests
from dataclasses import dataclass, field

# ── Service URLs (Docker ports) ──────────────────────────────────────────────
AUTH_URL    = "http://localhost:8081/api/v1/auth"
LEAD_URL    = "http://localhost:8082/api/v1/leads"
ACCT_URL    = "http://localhost:8083/api/v1/accounts"
CONTACT_URL = "http://localhost:8084/api/v1/contacts"
OPP_URL     = "http://localhost:8085/api/v1/opportunities"
ACTIVITY_URL= "http://localhost:8086/api/v1/activities"
NOTIF_URL   = "http://localhost:8087/api/v1/notifications"
WORKFLOW_URL= "http://localhost:8088/api/v1/workflows"
AI_URL      = "http://localhost:8089/api/v1/ai"
EMAIL_URL   = "http://localhost:8090/api/v1/email"
INTEG_URL   = "http://localhost:8091/api/v1/developer"
AGENT_URL   = "http://localhost:9100/api/ai"
QUOTA_URL   = "http://localhost:8085/api/v1/quotas"

FRONTEND_URL = "http://localhost:3000"

# ── Credentials ──────────────────────────────────────────────────────────────
CREDS = {
    "email": "sarah.chen@acmecorp.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}

# ── Helpers ──────────────────────────────────────────────────────────────────
def uid() -> str:
    return uuid.uuid4().hex[:8]

def get_auth_token() -> str:
    """Obtain a JWT access token from auth-service."""
    resp = requests.post(f"{AUTH_URL}/login", json=CREDS)
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    d = resp.json().get("data", resp.json())
    return d.get("accessToken") or d.get("token")

def api_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

def auth_only_headers(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


@dataclass
class TestResult:
    """Track individual test result for summary report."""
    module: str
    test_id: str
    test_type: str  # "api" or "ui"
    name: str
    status: str = "pending"  # pending, passed, failed, skipped
    error: str = ""
    duration_ms: float = 0


# ── UI Helpers ───────────────────────────────────────────────────────────────
def ui_login(page, email=None, password=None, tenant_id=None):
    """Perform login on the frontend."""
    page.goto(f"{FRONTEND_URL}/auth/login")
    page.wait_for_load_state("networkidle")
    page.fill('input[type="email"]', email or CREDS["email"])
    page.fill('input[type="password"]', password or CREDS["password"])
    # Fill tenant ID
    tenant_inputs = page.locator('input').all()
    for inp in tenant_inputs:
        placeholder = inp.get_attribute("placeholder") or ""
        label_text = inp.evaluate("el => el.closest('.MuiFormControl-root')?.querySelector('label')?.textContent || ''")
        if "tenant" in placeholder.lower() or "tenant" in label_text.lower():
            inp.fill(tenant_id or CREDS["tenantId"])
            break
    page.click('button:has-text("Sign In")')
    page.wait_for_load_state("networkidle")


def ui_navigate(page, path: str):
    """Navigate to a path, waiting for load."""
    page.goto(f"{FRONTEND_URL}{path}")
    page.wait_for_load_state("networkidle")


def ui_inject_token(page, token: str):
    """Inject JWT into localStorage and reload for authenticated access."""
    page.goto(FRONTEND_URL)
    page.evaluate(f"""() => {{
        localStorage.setItem('token', '{token}');
        localStorage.setItem('accessToken', '{token}');
    }}""")
    page.reload()
    page.wait_for_load_state("networkidle")
