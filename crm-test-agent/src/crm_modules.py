"""
CRM module definitions — the single source of truth that drives test generation.

Each module describes:
  • Fields & validations
  • API endpoints (method + path + examples)
  • UI route & Playwright selectors
  • Feature list consumed by the AI generator
"""
from __future__ import annotations

from src.models import (
    APIEndpoint,
    CRMField,
    CRMModuleDefinition,
    UISelector,
)

# ── Lead ─────────────────────────────────────────────────────────────────────

LEAD_MODULE = CRMModuleDefinition(
    name="lead",
    display_name="Leads",
    description="Manage sales leads from capture to conversion",
    base_api_path="/api/v1/leads",
    ui_route="/leads",
    fields=[
        CRMField(name="firstName", field_type="text", required=True, placeholder="First Name"),
        CRMField(name="lastName", field_type="text", required=True, placeholder="Last Name"),
        CRMField(name="email", field_type="email", required=True, placeholder="Email"),
        CRMField(name="phone", field_type="phone", placeholder="Phone"),
        CRMField(name="company", field_type="text", placeholder="Company"),
        CRMField(name="title", field_type="text", placeholder="Job Title"),
        CRMField(
            name="status",
            field_type="select",
            required=True,
            options=["NEW", "CONTACTED", "QUALIFIED", "UNQUALIFIED", "CONVERTED"],
        ),
        CRMField(
            name="source",
            field_type="select",
            options=["WEB", "PHONE", "EMAIL", "REFERRAL", "SOCIAL", "OTHER"],
        ),
    ],
    api_endpoints=[
        APIEndpoint(method="GET", path="/api/v1/leads", description="List all leads"),
        APIEndpoint(
            method="POST",
            path="/api/v1/leads",
            description="Create a new lead",
            request_body_example={
                "firstName": "Jane",
                "lastName": "Doe",
                "email": "jane.doe@example.com",
                "phone": "+1234567890",
                "company": "Acme Inc",
                "status": "NEW",
                "source": "WEB",
            },
            expected_status=201,
        ),
        APIEndpoint(method="GET", path="/api/v1/leads/{id}", description="Get lead by ID"),
        APIEndpoint(method="PUT", path="/api/v1/leads/{id}", description="Update a lead"),
        APIEndpoint(method="DELETE", path="/api/v1/leads/{id}", description="Delete a lead", expected_status=204),
    ],
    ui_selectors=[
        UISelector(element="page_title", selector="h4:has-text('Leads')", description="Page heading"),
        UISelector(element="add_button", selector="button:has-text('Add Lead')", description="Create lead button"),
        UISelector(element="data_grid", selector="div.MuiDataGrid-root", description="Leads grid"),
        UISelector(element="search_input", selector="input[placeholder*='Search']", description="Search field"),
    ],
    features=[
        "Create lead with all required fields",
        "Edit existing lead",
        "Delete lead",
        "Search & filter leads",
        "Lead status transitions",
        "Import/Export leads",
        "Convert lead to opportunity",
        "Bulk actions on leads",
        "Lead scoring",
        "Duplicate detection",
    ],
)

# ── Account ──────────────────────────────────────────────────────────────────

ACCOUNT_MODULE = CRMModuleDefinition(
    name="account",
    display_name="Accounts",
    description="Manage company / organisation accounts",
    base_api_path="/api/v1/accounts",
    ui_route="/accounts",
    fields=[
        CRMField(name="name", field_type="text", required=True, placeholder="Account Name"),
        CRMField(name="industry", field_type="text", placeholder="Industry"),
        CRMField(name="website", field_type="text", placeholder="Website URL"),
        CRMField(name="phone", field_type="phone", placeholder="Phone"),
        CRMField(name="address", field_type="text", placeholder="Address"),
        CRMField(
            name="type",
            field_type="select",
            options=["PROSPECT", "CUSTOMER", "PARTNER", "VENDOR", "OTHER"],
        ),
        CRMField(name="annualRevenue", field_type="number", placeholder="Annual Revenue"),
        CRMField(name="employees", field_type="number", placeholder="Number of Employees"),
    ],
    api_endpoints=[
        APIEndpoint(method="GET", path="/api/v1/accounts", description="List all accounts"),
        APIEndpoint(
            method="POST",
            path="/api/v1/accounts",
            description="Create a new account",
            request_body_example={
                "name": "Acme Corporation",
                "industry": "Technology",
                "website": "https://acme.com",
                "phone": "+1234567890",
                "type": "CUSTOMER",
            },
            expected_status=201,
        ),
        APIEndpoint(method="GET", path="/api/v1/accounts/{id}", description="Get account by ID"),
        APIEndpoint(method="PUT", path="/api/v1/accounts/{id}", description="Update an account"),
        APIEndpoint(method="DELETE", path="/api/v1/accounts/{id}", description="Delete an account", expected_status=204),
    ],
    ui_selectors=[
        UISelector(element="page_title", selector="h4:has-text('Accounts')", description="Page heading"),
        UISelector(element="add_button", selector="button:has-text('Add Account')", description="Create button"),
        UISelector(element="data_grid", selector="div.MuiDataGrid-root", description="Accounts grid"),
    ],
    features=[
        "Create account with all fields",
        "Edit existing account",
        "Delete account",
        "Search & filter accounts",
        "Account hierarchy / parent-child",
        "Related contacts list",
        "Related opportunities list",
        "Activity timeline on account",
    ],
)

# ── Contact ──────────────────────────────────────────────────────────────────

CONTACT_MODULE = CRMModuleDefinition(
    name="contact",
    display_name="Contacts",
    description="Manage individual contacts linked to accounts",
    base_api_path="/api/v1/contacts",
    ui_route="/contacts",
    fields=[
        CRMField(name="firstName", field_type="text", required=True, placeholder="First Name"),
        CRMField(name="lastName", field_type="text", required=True, placeholder="Last Name"),
        CRMField(name="email", field_type="email", required=True, placeholder="Email"),
        CRMField(name="phone", field_type="phone", placeholder="Phone"),
        CRMField(name="title", field_type="text", placeholder="Job Title"),
        CRMField(name="department", field_type="text", placeholder="Department"),
        CRMField(name="accountId", field_type="text", placeholder="Associated Account ID"),
    ],
    api_endpoints=[
        APIEndpoint(method="GET", path="/api/v1/contacts", description="List all contacts"),
        APIEndpoint(
            method="POST",
            path="/api/v1/contacts",
            description="Create a contact",
            request_body_example={
                "firstName": "John",
                "lastName": "Smith",
                "email": "john.smith@acme.com",
                "phone": "+1987654321",
                "title": "CTO",
            },
            expected_status=201,
        ),
        APIEndpoint(method="GET", path="/api/v1/contacts/{id}", description="Get contact by ID"),
        APIEndpoint(method="PUT", path="/api/v1/contacts/{id}", description="Update a contact"),
        APIEndpoint(method="DELETE", path="/api/v1/contacts/{id}", description="Delete a contact", expected_status=204),
    ],
    ui_selectors=[
        UISelector(element="page_title", selector="h4:has-text('Contacts')", description="Page heading"),
        UISelector(element="add_button", selector="button:has-text('Add Contact')", description="Create button"),
        UISelector(element="data_grid", selector="div.MuiDataGrid-root", description="Contacts grid"),
    ],
    features=[
        "Create contact with all fields",
        "Edit existing contact",
        "Delete contact",
        "Link contact to account",
        "Search & filter contacts",
        "Activity history on contact",
        "Send email from contact page",
        "Merge duplicate contacts",
    ],
)

# ── Opportunity ──────────────────────────────────────────────────────────────

OPPORTUNITY_MODULE = CRMModuleDefinition(
    name="opportunity",
    display_name="Opportunities",
    description="Track deals through the sales pipeline",
    base_api_path="/api/v1/opportunities",
    ui_route="/opportunities",
    fields=[
        CRMField(name="name", field_type="text", required=True, placeholder="Opportunity Name"),
        CRMField(name="amount", field_type="number", required=True, placeholder="Deal Amount"),
        CRMField(
            name="stage",
            field_type="select",
            required=True,
            options=["PROSPECTING", "QUALIFICATION", "PROPOSAL", "NEGOTIATION", "CLOSED_WON", "CLOSED_LOST"],
        ),
        CRMField(name="closeDate", field_type="date", required=True, placeholder="Close Date"),
        CRMField(name="probability", field_type="number", placeholder="Probability %"),
        CRMField(name="accountId", field_type="text", placeholder="Associated Account ID"),
        CRMField(name="contactId", field_type="text", placeholder="Primary Contact ID"),
    ],
    api_endpoints=[
        APIEndpoint(method="GET", path="/api/v1/opportunities", description="List opportunities"),
        APIEndpoint(
            method="POST",
            path="/api/v1/opportunities",
            description="Create an opportunity",
            request_body_example={
                "name": "Enterprise Deal",
                "amount": 50000,
                "stage": "PROSPECTING",
                "closeDate": "2025-06-30",
                "probability": 25,
            },
            expected_status=201,
        ),
        APIEndpoint(method="GET", path="/api/v1/opportunities/{id}", description="Get opportunity"),
        APIEndpoint(method="PUT", path="/api/v1/opportunities/{id}", description="Update opportunity"),
        APIEndpoint(method="DELETE", path="/api/v1/opportunities/{id}", description="Delete opportunity", expected_status=204),
    ],
    ui_selectors=[
        UISelector(element="page_title", selector="h4:has-text('Opportunities')", description="Page heading"),
        UISelector(element="add_button", selector="button:has-text('Add Opportunity')", description="Create button"),
        UISelector(element="data_grid", selector="div.MuiDataGrid-root", description="Opportunities grid"),
        UISelector(element="pipeline_board", selector="[data-testid='pipeline-board']", description="Kanban board"),
    ],
    features=[
        "Create opportunity",
        "Edit opportunity",
        "Delete opportunity",
        "Pipeline / Kanban board view",
        "Stage progression drag-and-drop",
        "Win/Loss tracking",
        "Revenue forecasting",
        "Opportunity-to-account association",
    ],
)

# ── Activity ─────────────────────────────────────────────────────────────────

ACTIVITY_MODULE = CRMModuleDefinition(
    name="activity",
    display_name="Activities",
    description="Tasks, calls, meetings, and events",
    base_api_path="/api/v1/activities",
    ui_route="/activities",
    fields=[
        CRMField(name="subject", field_type="text", required=True, placeholder="Subject"),
        CRMField(
            name="type",
            field_type="select",
            required=True,
            options=["TASK", "CALL", "MEETING", "EMAIL", "NOTE"],
        ),
        CRMField(
            name="status",
            field_type="select",
            options=["OPEN", "IN_PROGRESS", "COMPLETED", "CANCELLED"],
        ),
        CRMField(name="dueDate", field_type="date", placeholder="Due Date"),
        CRMField(name="description", field_type="text", placeholder="Description"),
        CRMField(name="relatedTo", field_type="text", placeholder="Related Record ID"),
    ],
    api_endpoints=[
        APIEndpoint(method="GET", path="/api/v1/activities", description="List activities"),
        APIEndpoint(
            method="POST",
            path="/api/v1/activities",
            description="Create an activity",
            request_body_example={
                "subject": "Follow-up call",
                "type": "CALL",
                "status": "OPEN",
                "dueDate": "2025-03-15",
                "description": "Discuss pricing",
            },
            expected_status=201,
        ),
        APIEndpoint(method="GET", path="/api/v1/activities/{id}", description="Get activity"),
        APIEndpoint(method="PUT", path="/api/v1/activities/{id}", description="Update activity"),
        APIEndpoint(method="DELETE", path="/api/v1/activities/{id}", description="Delete activity", expected_status=204),
    ],
    ui_selectors=[
        UISelector(element="page_title", selector="h4:has-text('Activities')", description="Page heading"),
        UISelector(element="add_button", selector="button:has-text('Add Activity')", description="Create button"),
    ],
    features=[
        "Create task / call / meeting",
        "Mark activity complete",
        "Edit activity",
        "Delete activity",
        "Filter by type / status",
        "Calendar view",
        "Activity reminders",
    ],
)

# ── Pipeline ─────────────────────────────────────────────────────────────────

PIPELINE_MODULE = CRMModuleDefinition(
    name="pipeline",
    display_name="Pipeline",
    description="Visual pipeline management and stage configuration",
    base_api_path="/api/v1/opportunities",
    ui_route="/opportunities",
    fields=[],
    api_endpoints=[
        APIEndpoint(method="GET", path="/api/v1/opportunities", description="List pipeline deals"),
        APIEndpoint(method="PUT", path="/api/v1/opportunities/{id}", description="Move deal to next stage"),
    ],
    ui_selectors=[
        UISelector(element="pipeline_board", selector="[data-testid='pipeline-board']", description="Kanban board"),
        UISelector(element="stage_column", selector="[data-testid='stage-column']", description="Stage column"),
        UISelector(element="deal_card", selector="[data-testid='deal-card']", description="Deal card"),
    ],
    features=[
        "Drag-and-drop deal between stages",
        "Aggregate deal values per stage",
        "Pipeline metrics dashboard",
        "Stage conversion rates",
        "Weighted pipeline value",
    ],
)

# ── Reports ──────────────────────────────────────────────────────────────────

REPORTS_MODULE = CRMModuleDefinition(
    name="reports",
    display_name="Reports",
    description="Sales reports, charts, and analytics dashboards",
    base_api_path="/api/v1",
    ui_route="/reports",
    fields=[],
    api_endpoints=[
        APIEndpoint(method="GET", path="/api/v1/leads", description="Leads data for reports"),
        APIEndpoint(method="GET", path="/api/v1/opportunities", description="Opportunity data for reports"),
        APIEndpoint(method="GET", path="/api/v1/activities", description="Activity data for reports"),
    ],
    ui_selectors=[
        UISelector(element="page_title", selector="h4:has-text('Reports')", description="Page heading"),
        UISelector(element="chart_container", selector="div.recharts-wrapper", description="Report chart"),
    ],
    features=[
        "Sales summary dashboard",
        "Lead conversion report",
        "Pipeline forecast report",
        "Activity report",
        "Revenue by period chart",
        "Export report to CSV",
    ],
)

# ── User management ─────────────────────────────────────────────────────────

USER_MODULE = CRMModuleDefinition(
    name="user",
    display_name="User Management",
    description="User registration, login, roles, and settings",
    base_api_path="/api/v1/auth",
    ui_route="/settings",
    fields=[
        CRMField(name="email", field_type="email", required=True, placeholder="Email"),
        CRMField(name="password", field_type="text", required=True, placeholder="Password"),
        CRMField(name="firstName", field_type="text", required=True, placeholder="First Name"),
        CRMField(name="lastName", field_type="text", required=True, placeholder="Last Name"),
        CRMField(name="tenantId", field_type="text", required=True, placeholder="Tenant ID"),
    ],
    api_endpoints=[
        APIEndpoint(
            method="POST",
            path="/api/v1/auth/login",
            description="Authenticate user",
            request_body_example={
                "email": "admin@crm.test",
                "password": "Admin@12345",
                "tenantId": "default",
            },
            expected_status=200,
        ),
        APIEndpoint(
            method="POST",
            path="/api/v1/auth/register",
            description="Register new user",
            request_body_example={
                "email": "new.user@crm.test",
                "password": "NewUser@123",
                "firstName": "New",
                "lastName": "User",
                "tenantId": "default",
            },
            expected_status=201,
        ),
    ],
    ui_selectors=[
        UISelector(element="login_email", selector="input[name='email']", description="Email input"),
        UISelector(element="login_password", selector="input[name='password']", description="Password input"),
        UISelector(element="login_button", selector="button[type='submit']", description="Login button"),
        UISelector(element="settings_page", selector="h4:has-text('Settings')", description="Settings heading"),
    ],
    features=[
        "User login with valid credentials",
        "Login validation — wrong password",
        "Login validation — empty fields",
        "User registration",
        "Registration validation — duplicate email",
        "JWT token management",
        "User profile update",
        "Role-based access control",
    ],
)

# ── Email ────────────────────────────────────────────────────────────────────

EMAIL_MODULE = CRMModuleDefinition(
    name="email",
    display_name="Email",
    description="Email activities linked to contacts and leads",
    base_api_path="/api/v1/activities",
    ui_route="/activities",
    fields=[
        CRMField(name="subject", field_type="text", required=True, placeholder="Subject"),
        CRMField(name="type", field_type="select", required=True, options=["EMAIL"]),
        CRMField(name="description", field_type="text", placeholder="Body"),
        CRMField(name="relatedTo", field_type="text", placeholder="Related Record ID"),
    ],
    api_endpoints=[
        APIEndpoint(
            method="POST",
            path="/api/v1/activities",
            description="Log an email activity",
            request_body_example={
                "subject": "Proposal Follow-up",
                "type": "EMAIL",
                "status": "COMPLETED",
                "description": "Sent the proposal PDF.",
            },
            expected_status=201,
        ),
    ],
    ui_selectors=[],
    features=[
        "Log outgoing email",
        "Link email to contact / lead",
        "Email template rendering",
        "Email open tracking",
    ],
)

# ── Registry ─────────────────────────────────────────────────────────────────

ALL_MODULES: dict[str, CRMModuleDefinition] = {
    "lead": LEAD_MODULE,
    "account": ACCOUNT_MODULE,
    "contact": CONTACT_MODULE,
    "opportunity": OPPORTUNITY_MODULE,
    "activity": ACTIVITY_MODULE,
    "pipeline": PIPELINE_MODULE,
    "reports": REPORTS_MODULE,
    "user": USER_MODULE,
    "email": EMAIL_MODULE,
}


def get_module(name: str) -> CRMModuleDefinition:
    key = name.lower().rstrip("s")  # "leads" → "lead"
    if key not in ALL_MODULES:
        raise KeyError(f"Unknown CRM module: {name!r}  (available: {list(ALL_MODULES)})")
    return ALL_MODULES[key]


def get_all_modules() -> list[CRMModuleDefinition]:
    return list(ALL_MODULES.values())
