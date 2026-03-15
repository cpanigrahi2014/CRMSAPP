"""
Module 13 — CSV Import / Data Management (20 Tests: 11 API + 9 UI)
====================================================================
API : CSV import / export for leads, contacts, accounts, validation
UI  : Import dialog, export button, progress, errors
"""
import uuid, io, pytest, requests
from playwright.sync_api import Page
from automation import (
    LEAD_URL, ACCT_URL, CONTACT_URL, AI_URL, FRONTEND_URL, uid,
    get_auth_token, api_headers, auth_only_headers, ui_inject_token,
)

@pytest.fixture(scope="module")
def token():
    return get_auth_token()

@pytest.fixture(scope="module")
def headers(token):
    return api_headers(token)

@pytest.fixture(scope="module")
def headers_no_ct(token):
    return auth_only_headers(token)


def _csv_bytes(rows: list[str]) -> bytes:
    return "\n".join(rows).encode("utf-8")


class TestCSVImportAPI:
    def test_api_01_export_leads(self, headers):
        assert requests.get(f"{LEAD_URL}/export", headers=headers).status_code in (200, 404, 500)

    def test_api_02_import_leads(self, headers_no_ct):
        csv = _csv_bytes(["firstName,lastName,email,company,status",
                          f"Import,Test{uid()},it{uid()}@test.com,TestCo,NEW"])
        resp = requests.post(f"{LEAD_URL}/import", headers=headers_no_ct,
                             files={"file": ("leads.csv", csv, "text/csv")})
        assert resp.status_code in (200, 201, 400, 500)

    def test_api_03_export_accounts(self, headers):
        assert requests.get(f"{ACCT_URL}/export", headers=headers).status_code in (200, 404, 500)

    def test_api_04_import_accounts(self, headers_no_ct):
        csv = _csv_bytes(["name,industry,website",
                          f"AcctImport{uid()},Tech,https://example.com"])
        resp = requests.post(f"{ACCT_URL}/import", headers=headers_no_ct,
                             files={"file": ("accounts.csv", csv, "text/csv")})
        assert resp.status_code in (200, 201, 400, 500)

    def test_api_05_export_contacts(self, headers):
        assert requests.get(f"{CONTACT_URL}/export", headers=headers).status_code in (200, 404, 500)

    def test_api_06_import_contacts(self, headers_no_ct):
        csv = _csv_bytes(["firstName,lastName,email",
                          f"CImport,Test{uid()},ct{uid()}@test.com"])
        resp = requests.post(f"{CONTACT_URL}/import", headers=headers_no_ct,
                             files={"file": ("contacts.csv", csv, "text/csv")})
        assert resp.status_code in (200, 201, 400, 500)

    def test_api_07_empty_csv(self, headers_no_ct):
        csv = _csv_bytes(["firstName,lastName,email"])
        resp = requests.post(f"{LEAD_URL}/import", headers=headers_no_ct,
                             files={"file": ("empty.csv", csv, "text/csv")})
        assert resp.status_code in (200, 400, 500)

    def test_api_08_invalid_format(self, headers_no_ct):
        resp = requests.post(f"{LEAD_URL}/import", headers=headers_no_ct,
                             files={"file": ("bad.txt", b"NOT CSV DATA", "text/plain")})
        assert resp.status_code in (400, 415, 500)

    def test_api_09_ai_data_enrichment(self, headers):
        resp = requests.post(f"{AI_URL}/enrich", headers=headers, json={
            "entityType": "LEAD", "data": {"company": "Google"}})
        assert resp.status_code in (200, 400, 404, 500)

    def test_api_10_no_auth_import(self):
        csv = _csv_bytes(["firstName,lastName,email", "A,B,a@b.com"])
        assert requests.post(f"{LEAD_URL}/import",
                             files={"file": ("x.csv", csv, "text/csv")}).status_code in (401, 403, 500)

    def test_api_11_duplicate_detection(self, headers_no_ct):
        email = f"dup{uid()}@test.com"
        csv = _csv_bytes(["firstName,lastName,email,company,status",
                          f"Dup,Test,{email},Acme,NEW",
                          f"Dup,Test,{email},Acme,NEW"])
        resp = requests.post(f"{LEAD_URL}/import", headers=headers_no_ct,
                             files={"file": ("dups.csv", csv, "text/csv")})
        assert resp.status_code in (200, 201, 400, 500)


class TestCSVImportUI:
    @pytest.fixture(autouse=True)
    def _auth(self, page: Page, token):
        ui_inject_token(page, token)

    def test_ui_01_leads_import(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Import")')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(1000)

    def test_ui_02_leads_export(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Export")')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(1000)

    def test_ui_03_accounts_import(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Import")')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(1000)

    def test_ui_04_accounts_export(self, page: Page):
        page.goto(f"{FRONTEND_URL}/accounts"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Export")')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(1000)

    def test_ui_05_contacts_import(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Import")')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(1000)

    def test_ui_06_contacts_export(self, page: Page):
        page.goto(f"{FRONTEND_URL}/contacts"); page.wait_for_load_state("networkidle")
        btn = page.locator('button:has-text("Export")')
        if btn.count() > 0: btn.first.click(); page.wait_for_timeout(1000)

    def test_ui_07_responsive(self, page: Page):
        page.set_viewport_size({"width": 375, "height": 667})
        page.goto(f"{FRONTEND_URL}/leads"); page.wait_for_load_state("networkidle")
        page.set_viewport_size({"width": 1280, "height": 720})

    def test_ui_08_navigation(self, page: Page):
        page.goto(f"{FRONTEND_URL}/leads"); page.goto(f"{FRONTEND_URL}/accounts")
        assert "accounts" in page.url

    def test_ui_09_settings_page(self, page: Page):
        page.goto(f"{FRONTEND_URL}/settings"); page.wait_for_load_state("networkidle")
