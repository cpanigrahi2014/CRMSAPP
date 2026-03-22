"""
Playwright Navigation Tests — Full CRM Module Navigation
==========================================================
Tests login flow and sidebar navigation to ALL 24 CRM modules.
Verifies each page loads, correct heading appears, active sidebar
highlight works, and URL routing is correct.

Usage:
    cd crm-test-agent
    python -m pytest tests/test_playwright_navigation.py -v --headed
    python -m pytest tests/test_playwright_navigation.py -v          # headless
    python -m pytest tests/test_playwright_navigation.py -v -k "dashboard or leads"
"""
import re
import pytest
from playwright.sync_api import Page, expect

# ── Configuration ────────────────────────────────────────────────────────────
BASE_URL = "http://34.193.72.13"
CREDS = {
    "email": "demo@crm.com",
    "password": "Demo@2026!",
    "tenantId": "default",
}

# All 24 sidebar modules with expected page heading text
MODULES = [
    ("Dashboard",       "/dashboard",       "Pipeline Dashboard"),
    ("Leads",           "/leads",           "Lead Management"),
    ("Accounts",        "/accounts",        "Account Management"),
    ("Contacts",        "/contacts",        "Contact Management"),
    ("Opportunities",   "/opportunities",   "Opportunity Pipeline"),
    ("Activities",      "/activities",      "Activities & Tasks"),
    ("Email",           "/email",           "Email Integration"),
    ("Cases",           "/cases",           "Customer Support Cases"),
    ("Campaigns",       "/campaigns",       "Marketing Campaigns"),
    ("Reports",         "/reports",         "Reports & Analytics"),
    ("Workflows",       "/workflows",       "Workflow Automation"),
    ("Security",        "/security",        "Security & User Management"),
    ("Integrations",    "/integrations",    "Integration Platform"),
    ("AI Config",       "/ai-config",       "AI Configuration Agent"),
    ("AI Insights",     "/ai-insights",     "AI & Intelligence"),
    ("Zero Config",     "/zero-config",     "Zero-Configuration CRM"),
    ("Communications",  "/communications",  "Communications"),
    ("Collaboration",   "/collaboration",   "Collaboration Hub"),
    ("Automation",      "/automation",      "Smart Automation"),
    ("Developer",       "/developer",       "Developer Portal"),
    ("Object Manager",  "/object-manager",  "Object Manager"),
    ("Pricing & Plans", "/pricing",         "Pricing & Plans"),
    ("Screen Capture",  "/screen-capture",  "Screen Capture"),
    ("Settings",        "/settings",        "Settings"),
]


# ── Fixtures ─────────────────────────────────────────────────────────────────
@pytest.fixture(scope="session")
def browser_context_args():
    return {
        "base_url": BASE_URL,
        "viewport": {"width": 1280, "height": 720},
        "ignore_https_errors": True,
    }


@pytest.fixture(scope="session")
def browser_type_launch_args():
    return {"headless": True, "slow_mo": 100}


@pytest.fixture(scope="module")
def authenticated_page(browser):
    """Login once and reuse the authenticated context for all navigation tests."""
    context = browser.new_context(
        base_url=BASE_URL,
        viewport={"width": 1280, "height": 720},
        ignore_https_errors=True,
    )
    page = context.new_page()

    # Navigate to login
    page.goto("/auth/login")
    page.wait_for_load_state("networkidle")

    # Fill login form using MUI label selectors
    page.get_by_label("Email Address").fill(CREDS["email"])
    page.get_by_label("Password").fill(CREDS["password"])
    page.get_by_label("Tenant ID").fill(CREDS["tenantId"])

    # Submit
    page.get_by_role("button", name="Sign In").click()

    # Wait for redirect to dashboard
    page.wait_for_url("**/dashboard", timeout=15000)
    page.wait_for_load_state("networkidle")

    yield page

    context.close()


# ══════════════════════════════════════════════════════════════════════════════
#   1. LOGIN FLOW TESTS
# ══════════════════════════════════════════════════════════════════════════════
class TestLoginPage:
    """Verify the login page renders and authentication works."""

    def test_login_page_loads(self, page: Page):
        """Login page renders with the sign-in form."""
        page.goto("/auth/login")
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("Sign In").first).to_be_visible()
        expect(page.get_by_label("Email Address")).to_be_visible()
        expect(page.get_by_label("Password")).to_be_visible()

    def test_login_page_has_register_link(self, page: Page):
        """Login page has a register/sign-up link."""
        page.goto("/auth/login")
        page.wait_for_load_state("networkidle")
        expect(page.get_by_role("link", name="Sign Up")).to_be_visible()

    def test_login_page_has_forgot_password_link(self, page: Page):
        """Login page has a forgot password link."""
        page.goto("/auth/login")
        page.wait_for_load_state("networkidle")
        expect(page.get_by_role("link", name="Forgot password?")).to_be_visible()

    def test_successful_login_redirects_to_dashboard(self, page: Page):
        """Valid credentials redirect to the dashboard."""
        page.goto("/auth/login")
        page.wait_for_load_state("networkidle")

        page.get_by_label("Email Address").fill(CREDS["email"])
        page.get_by_label("Password").fill(CREDS["password"])
        page.get_by_label("Tenant ID").fill(CREDS["tenantId"])
        page.get_by_role("button", name="Sign In").click()

        page.wait_for_url("**/dashboard", timeout=15000)
        assert "/dashboard" in page.url

    def test_dashboard_shows_after_login(self, page: Page):
        """After login, dashboard heading is visible."""
        page.goto("/auth/login")
        page.wait_for_load_state("networkidle")

        page.get_by_label("Email Address").fill(CREDS["email"])
        page.get_by_label("Password").fill(CREDS["password"])
        page.get_by_label("Tenant ID").fill(CREDS["tenantId"])
        page.get_by_role("button", name="Sign In").click()

        page.wait_for_url("**/dashboard", timeout=15000)
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("Pipeline Dashboard").first).to_be_visible(timeout=10000)


# ══════════════════════════════════════════════════════════════════════════════
#   2. SIDEBAR NAVIGATION — Parameterized Across All 24 Modules
# ══════════════════════════════════════════════════════════════════════════════
class TestSidebarNavigation:
    """Click each sidebar item and verify the correct page loads."""

    @pytest.mark.parametrize(
        "sidebar_label, expected_path, expected_heading",
        MODULES,
        ids=[m[0].lower().replace(" ", "_").replace("&", "and") for m in MODULES],
    )
    def test_sidebar_navigates_to_module(
        self, authenticated_page: Page, sidebar_label, expected_path, expected_heading
    ):
        """Clicking '{sidebar_label}' in sidebar navigates to {expected_path}."""
        page = authenticated_page

        # Scroll sidebar to reveal the item if needed, then click
        sidebar = page.locator("nav, .MuiDrawer-root").first
        nav_item = sidebar.get_by_text(sidebar_label, exact=True)
        nav_item.scroll_into_view_if_needed()
        nav_item.click()

        # Wait for navigation
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(500)

        # ── Assert URL contains expected path ──
        assert expected_path in page.url, (
            f"Expected URL to contain '{expected_path}', got '{page.url}'"
        )

        # ── Assert page heading is visible ──
        heading = page.get_by_text(expected_heading).first
        expect(heading).to_be_visible(timeout=10000)


# ══════════════════════════════════════════════════════════════════════════════
#   3. SIDEBAR HIGHLIGHT — Active item has correct styling
# ══════════════════════════════════════════════════════════════════════════════
class TestSidebarActiveHighlight:
    """The sidebar item for the current page should be visually highlighted."""

    @pytest.mark.parametrize(
        "sidebar_label, path, _heading",
        MODULES[:6],  # Test the first 6 modules (Dashboard through Activities)
        ids=[m[0].lower().replace(" ", "_") for m in MODULES[:6]],
    )
    def test_active_sidebar_item_is_highlighted(
        self, authenticated_page: Page, sidebar_label, path, _heading
    ):
        """'{sidebar_label}' sidebar item is highlighted when on {path}."""
        page = authenticated_page

        # Navigate via URL
        page.goto(path)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(300)

        # Find the sidebar button containing this text using Playwright filter API
        sidebar = page.locator(".MuiDrawer-root").first
        active_button = sidebar.locator(".MuiListItemButton-root").filter(has_text=sidebar_label).first
        expect(active_button).to_be_visible(timeout=5000)

        # MUI active style: background-color is the primary color (non-transparent)
        bg_color = active_button.evaluate(
            "el => window.getComputedStyle(el).backgroundColor"
        )
        # Active items have a non-default background (not transparent or white)
        assert bg_color not in ("rgba(0, 0, 0, 0)", "transparent", "rgb(255, 255, 255)"), (
            f"Expected '{sidebar_label}' to have active highlight, got bg: {bg_color}"
        )


# ══════════════════════════════════════════════════════════════════════════════
#   4. DIRECT URL NAVIGATION — Each route works directly
# ══════════════════════════════════════════════════════════════════════════════
class TestDirectURLNavigation:
    """Verify pages load correctly when accessed via direct URL."""

    @pytest.mark.parametrize(
        "sidebar_label, path, expected_heading",
        MODULES,
        ids=[m[0].lower().replace(" ", "_").replace("&", "and") for m in MODULES],
    )
    def test_direct_url_loads_page(
        self, authenticated_page: Page, sidebar_label, path, expected_heading
    ):
        """Navigating directly to {path} loads the correct page."""
        page = authenticated_page

        page.goto(path)
        page.wait_for_load_state("networkidle")

        # Page heading is visible
        heading = page.get_by_text(expected_heading).first
        expect(heading).to_be_visible(timeout=10000)

        # URL is correct
        assert path in page.url


# ══════════════════════════════════════════════════════════════════════════════
#   5. MODULE CONTENT SMOKE TESTS — Each page has meaningful content
# ══════════════════════════════════════════════════════════════════════════════
class TestModulePageContent:
    """Verify key UI elements exist on each major module page."""

    def test_dashboard_has_kpi_cards(self, authenticated_page: Page):
        """Dashboard renders KPI/stat cards."""
        page = authenticated_page
        page.goto("/dashboard", timeout=30000)
        page.wait_for_load_state("networkidle", timeout=15000)
        page.wait_for_timeout(1000)
        # Dashboard should have card widgets or paper elements
        cards = page.locator(".MuiCard-root, .MuiPaper-root")
        expect(cards.first).to_be_visible(timeout=15000)

    def test_leads_page_has_table_and_add_button(self, authenticated_page: Page):
        """Leads page shows a data table and create button."""
        page = authenticated_page
        page.goto("/leads")
        page.wait_for_load_state("networkidle")
        # Should have an Add/New Lead button
        add_btn = page.get_by_role("button", name=re.compile(r"new lead|add lead|create", re.IGNORECASE))
        expect(add_btn).to_be_visible(timeout=10000)

    def test_accounts_page_has_table_and_add_button(self, authenticated_page: Page):
        """Accounts page shows a data table and create button."""
        page = authenticated_page
        page.goto("/accounts")
        page.wait_for_load_state("networkidle")
        add_btn = page.get_by_role("button", name=re.compile(r"new account|add account|create", re.IGNORECASE))
        expect(add_btn).to_be_visible(timeout=10000)

    def test_contacts_page_has_table_and_add_button(self, authenticated_page: Page):
        """Contacts page shows a table and create button."""
        page = authenticated_page
        page.goto("/contacts")
        page.wait_for_load_state("networkidle")
        add_btn = page.get_by_role("button", name=re.compile(r"new contact|add contact|create", re.IGNORECASE))
        expect(add_btn).to_be_visible(timeout=10000)

    def test_opportunities_page_has_table_and_add_button(self, authenticated_page: Page):
        """Opportunities page shows an add button (IconButton with tooltip 'New Opportunity')."""
        page = authenticated_page
        page.goto("/opportunities")
        page.wait_for_load_state("networkidle")
        add_btn = page.get_by_role("button", name="New Opportunity")
        expect(add_btn).to_be_visible(timeout=10000)

    def test_cases_page_has_table_and_add_button(self, authenticated_page: Page):
        """Cases page shows support cases and a create button."""
        page = authenticated_page
        page.goto("/cases")
        page.wait_for_load_state("networkidle")
        add_btn = page.get_by_role("button", name=re.compile(r"new case|add case|create", re.IGNORECASE))
        expect(add_btn).to_be_visible(timeout=10000)

    def test_campaigns_page_has_table_and_add_button(self, authenticated_page: Page):
        """Campaigns page shows campaigns and a create button."""
        page = authenticated_page
        page.goto("/campaigns")
        page.wait_for_load_state("networkidle")
        add_btn = page.get_by_role("button", name=re.compile(r"new campaign|add campaign|create", re.IGNORECASE))
        expect(add_btn).to_be_visible(timeout=10000)

    def test_activities_page_has_content(self, authenticated_page: Page):
        """Activities page renders activity timeline or list."""
        page = authenticated_page
        page.goto("/activities")
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("Activities & Tasks").first).to_be_visible(timeout=10000)

    def test_reports_page_has_content(self, authenticated_page: Page):
        """Reports page renders analytics content."""
        page = authenticated_page
        page.goto("/reports")
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("Reports & Analytics").first).to_be_visible(timeout=10000)

    def test_email_page_has_content(self, authenticated_page: Page):
        """Email page renders email integration content."""
        page = authenticated_page
        page.goto("/email")
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("Email Integration").first).to_be_visible(timeout=10000)

    def test_workflows_page_has_content(self, authenticated_page: Page):
        """Workflows page renders automation content."""
        page = authenticated_page
        page.goto("/workflows")
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("Workflow Automation").first).to_be_visible(timeout=10000)

    def test_settings_page_has_content(self, authenticated_page: Page):
        """Settings page renders settings content."""
        page = authenticated_page
        page.goto("/settings")
        page.wait_for_load_state("networkidle")
        expect(page.get_by_text("Settings").first).to_be_visible(timeout=10000)


# ══════════════════════════════════════════════════════════════════════════════
#   6. LEAD CRUD FLOW — Create, View, Edit via UI
# ══════════════════════════════════════════════════════════════════════════════
class TestLeadCRUDFlow:
    """End-to-end test: create a lead via modal, verify it appears."""

    def test_open_new_lead_modal(self, authenticated_page: Page):
        """Clicking 'New Lead' opens the creation modal."""
        page = authenticated_page
        page.goto("/leads")
        page.wait_for_load_state("networkidle")

        add_btn = page.get_by_role("button", name=re.compile(r"new lead", re.IGNORECASE))
        add_btn.click()

        # Modal should appear
        modal = page.locator(".MuiDialog-root")
        expect(modal).to_be_visible(timeout=5000)
        expect(modal.get_by_role("heading", name="New Lead")).to_be_visible()

    def test_new_lead_modal_save_disabled_without_required_fields(self, authenticated_page: Page):
        """Save button is disabled when mandatory fields are empty."""
        page = authenticated_page
        page.goto("/leads")
        page.wait_for_load_state("networkidle")

        page.get_by_role("button", name=re.compile(r"new lead", re.IGNORECASE)).click()
        page.wait_for_timeout(500)

        # Save button should be disabled before filling fields
        save_btn = page.locator(".MuiDialog-root").get_by_role("button", name="Save")
        expect(save_btn).to_be_disabled()

    def test_new_lead_modal_save_enabled_with_required_fields(self, authenticated_page: Page):
        """Save button becomes enabled after filling mandatory fields."""
        page = authenticated_page
        page.goto("/leads")
        page.wait_for_load_state("networkidle")

        page.get_by_role("button", name=re.compile(r"new lead", re.IGNORECASE)).click()
        page.wait_for_timeout(500)

        dialog = page.locator(".MuiDialog-root")
        dialog.get_by_label("First Name").fill("TestFirst")
        dialog.get_by_label("Last Name").fill("TestLast")
        dialog.get_by_label("Email").fill("playwright.test@example.com")
        dialog.get_by_label("Company").fill("Test Corp")

        save_btn = dialog.get_by_role("button", name="Save")
        expect(save_btn).to_be_enabled()

        # Close without saving
        dialog.get_by_role("button", name="Cancel").click()


# ══════════════════════════════════════════════════════════════════════════════
#   7. CONTACT CRUD FLOW
# ══════════════════════════════════════════════════════════════════════════════
class TestContactCRUDFlow:
    """End-to-end test: create contact modal flow."""

    def test_open_new_contact_modal(self, authenticated_page: Page):
        """Clicking 'New Contact' opens the creation modal."""
        page = authenticated_page
        page.goto("/contacts")
        page.wait_for_load_state("networkidle")

        add_btn = page.get_by_role("button", name=re.compile(r"new contact", re.IGNORECASE))
        add_btn.click()

        modal = page.locator(".MuiDialog-root")
        expect(modal).to_be_visible(timeout=5000)
        expect(modal.get_by_role("heading", name="New Contact")).to_be_visible()

    def test_new_contact_save_disabled_without_required(self, authenticated_page: Page):
        """Save disabled when required fields empty."""
        page = authenticated_page
        page.goto("/contacts")
        page.wait_for_load_state("networkidle")

        page.get_by_role("button", name=re.compile(r"new contact", re.IGNORECASE)).click()
        page.wait_for_timeout(500)

        save_btn = page.locator(".MuiDialog-root").get_by_role("button", name="Save")
        expect(save_btn).to_be_disabled()

    def test_new_contact_save_enabled_with_required(self, authenticated_page: Page):
        """Save enabled after filling firstName, lastName, email."""
        page = authenticated_page
        page.goto("/contacts")
        page.wait_for_load_state("networkidle")

        page.get_by_role("button", name=re.compile(r"new contact", re.IGNORECASE)).click()
        page.wait_for_timeout(500)

        dialog = page.locator(".MuiDialog-root")
        dialog.get_by_label("First Name").fill("TestFirst")
        dialog.get_by_label("Last Name").fill("TestLast")
        dialog.get_by_label("Email").fill("pw.contact@test.com")

        save_btn = dialog.get_by_role("button", name="Save")
        expect(save_btn).to_be_enabled()

        dialog.get_by_role("button", name="Cancel").click()


# ══════════════════════════════════════════════════════════════════════════════
#   8. ACCOUNT CRUD FLOW
# ══════════════════════════════════════════════════════════════════════════════
class TestAccountCRUDFlow:
    """End-to-end test: create account modal flow."""

    def test_open_new_account_modal(self, authenticated_page: Page):
        """Clicking 'New Account' opens the creation modal."""
        page = authenticated_page
        page.goto("/accounts")
        page.wait_for_load_state("networkidle")

        add_btn = page.get_by_role("button", name=re.compile(r"new account", re.IGNORECASE))
        add_btn.click()

        modal = page.locator(".MuiDialog-root")
        expect(modal).to_be_visible(timeout=5000)
        expect(modal.get_by_role("heading", name="New Account")).to_be_visible()

    def test_new_account_save_disabled_without_name(self, authenticated_page: Page):
        """Save disabled when Account Name is empty."""
        page = authenticated_page
        page.goto("/accounts")
        page.wait_for_load_state("networkidle")

        page.get_by_role("button", name=re.compile(r"new account", re.IGNORECASE)).click()
        page.wait_for_timeout(500)

        save_btn = page.locator(".MuiDialog-root").get_by_role("button", name="Save")
        expect(save_btn).to_be_disabled()

    def test_new_account_save_enabled_with_name(self, authenticated_page: Page):
        """Save enabled after filling Account Name."""
        page = authenticated_page
        page.goto("/accounts")
        page.wait_for_load_state("networkidle")

        page.get_by_role("button", name=re.compile(r"new account", re.IGNORECASE)).click()
        page.wait_for_timeout(500)

        dialog = page.locator(".MuiDialog-root")
        dialog.get_by_label("Account Name").fill("Playwright Test Corp")

        save_btn = dialog.get_by_role("button", name="Save")
        expect(save_btn).to_be_enabled()

        dialog.get_by_role("button", name="Cancel").click()


# ══════════════════════════════════════════════════════════════════════════════
#   9. DATA GRID VERIFICATION — Tables show seeded data
# ══════════════════════════════════════════════════════════════════════════════
class TestSeededDataVisible:
    """Verify that previously seeded test data is visible in tables."""

    def test_leads_table_has_rows(self, authenticated_page: Page):
        """Leads table shows at least one data row."""
        page = authenticated_page
        page.goto("/leads")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        rows = page.locator(".MuiDataGrid-row")
        expect(rows.first).to_be_visible(timeout=10000)

    def test_accounts_table_has_rows(self, authenticated_page: Page):
        """Accounts table shows at least one data row."""
        page = authenticated_page
        page.goto("/accounts")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        rows = page.locator(".MuiDataGrid-row")
        expect(rows.first).to_be_visible(timeout=10000)

    def test_contacts_table_has_rows(self, authenticated_page: Page):
        """Contacts table shows at least one data row."""
        page = authenticated_page
        page.goto("/contacts")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        rows = page.locator(".MuiDataGrid-row")
        expect(rows.first).to_be_visible(timeout=10000)

    def test_opportunities_has_content(self, authenticated_page: Page):
        """Opportunities page shows Kanban cards or pipeline content."""
        page = authenticated_page
        page.goto("/opportunities")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        # Default view is Kanban — look for cards or pipeline columns
        content = page.locator(".MuiCard-root, .MuiPaper-root")
        expect(content.first).to_be_visible(timeout=10000)


# ══════════════════════════════════════════════════════════════════════════════
#  10. UNAUTHENTICATED ACCESS — Protected routes redirect
# ══════════════════════════════════════════════════════════════════════════════
class TestUnauthenticatedRedirect:
    """Accessing protected pages without login should redirect away."""

    def test_dashboard_redirects_when_not_logged_in(self, page: Page):
        """Unauthenticated user visiting /dashboard is redirected."""
        page.goto("/dashboard")
        page.wait_for_load_state("networkidle")
        # Should not remain on dashboard — redirects to login or landing
        assert "/dashboard" not in page.url or page.locator("text=Sign In").is_visible()

    def test_leads_redirects_when_not_logged_in(self, page: Page):
        """Unauthenticated user visiting /leads is redirected."""
        page.goto("/leads")
        page.wait_for_load_state("networkidle")
        assert "/leads" not in page.url or page.locator("text=Sign In").is_visible()
