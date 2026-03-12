# CRMS Platform — Zero-Configuration AI Features: Testing & Demo Guide

> Covers: Natural Language CRM Config, Auto-Create Pipelines, CSV Field Detection, AI Workflows, Data Deduplication, Contact Enrichment, Smart Dashboards, AI Onboarding Assistant

---

## Prerequisites

1. **All services running**: `docker compose up -d`
2. **Demo data seeded**: `.\demo\seed-demo.ps1`
3. **App URL**: [http://localhost:3000](http://localhost:3000) (or via ngrok)
4. **Login**: `sarah.chen@acmecorp.com` / `Demo@2026!` / Tenant: `default`

### Service Ports Reference

| Service           | Port | Base URL                                  |
|-------------------|------|-------------------------------------------|
| Auth Service      | 8081 | `http://localhost:8081/api/v1/auth`       |
| AI Service        | 9089 | `http://localhost:9089/api/v1/ai`         |
| AI Agent (Node)   | 9100 | `http://localhost:9100/api/ai`            |
| Contact Service   | 9084 | `http://localhost:9084/api/v1/contacts`   |
| Lead Service      | 9082 | `http://localhost:9082/api/v1/leads`      |
| Account Service   | 9083 | `http://localhost:9083/api/v1/accounts`   |
| Opportunity Svc   | 9085 | `http://localhost:9085/api/v1/opportunities` |
| Workflow Service   | 9088 | `http://localhost:9088/api/v1/workflows`  |

---

## Step 0 — Get Auth Token

All API calls require a Bearer token. Get one first:

```bash
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sarah.chen@acmecorp.com","password":"Demo@2026!","tenantId":"default"}'
```

Save the `accessToken` from response as `$TOKEN`. All subsequent commands use:
```
-H "Authorization: Bearer $TOKEN"
```

---

## Feature 1: Natural Language CRM Configuration

**What it does**: Type plain English instructions → AI parses them into structured CRM commands and executes them.

**Service**: crm-ai-agent (Port 9100)  
**Frontend**: Sidebar → **AI Config** → Chat interface

### API Testing

#### Step 1 — Send a Natural Language Instruction
```bash
curl -X POST http://localhost:9100/api/ai/configure \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "Create a custom object called Project with fields: project name (text), budget (currency), start date (date), status (dropdown: Planning, Active, On Hold, Completed), and priority (dropdown: Low, Medium, High, Critical)"
  }'
```

**Expected Response**:
```json
{
  "status": "pending_confirmation",
  "commands": [
    {
      "action": "create_object",
      "name": "Project",
      "fields": [
        { "name": "project_name", "type": "text" },
        { "name": "budget", "type": "currency" },
        { "name": "start_date", "type": "date" },
        { "name": "status", "type": "dropdown", "options": ["Planning", "Active", "On Hold", "Completed"] },
        { "name": "priority", "type": "dropdown", "options": ["Low", "Medium", "High", "Critical"] }
      ]
    }
  ],
  "sessionId": "uuid",
  "auditLogId": "uuid"
}
```

#### Step 2 — Confirm the Command
```bash
curl -X POST http://localhost:9100/api/ai/confirm \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "auditLogId": "<auditLogId-from-step1>",
    "sessionId": "<sessionId-from-step1>"
  }'
```

#### Step 3 — Try Another Instruction (Create a Relationship)
```bash
curl -X POST http://localhost:9100/api/ai/configure \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "Add a relationship between Account and Project so each account can have multiple projects"
  }'
```

#### Step 4 — Reject a Command
```bash
curl -X POST http://localhost:9100/api/ai/reject \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "auditLogId": "<auditLogId>"
  }'
```

### UI Demo Steps
1. Navigate to **AI Config** in the sidebar
2. Type in the chat box: *"Create a custom object called Project with fields for name, budget, start date, status, and priority"*
3. View the AI's response showing the parsed command with colored action chips
4. Click **Confirm** to execute, or **Reject** to cancel
5. Type: *"Add a field called 'assigned_to' to the Project object"*
6. Show the conversation history maintaining context
7. Type: *"Create a role called Project Manager with access to Projects and Accounts"*

### What to Verify
- [ ] AI correctly parses natural language into structured JSON commands
- [ ] Commands include correct action types (create_object, create_field, etc.)
- [ ] Confirm executes the command and returns success
- [ ] Reject cancels the pending command
- [ ] Conversation history persists across the session
- [ ] Complex multi-field instructions are parsed correctly

---

## Feature 2: Auto-Create Pipelines from Description

**What it does**: Describe your sales process in words → AI creates a full pipeline with stages, probabilities, and settings.

**Service**: crm-ai-agent (Port 9100)  
**Frontend**: Sidebar → **AI Config** → Chat

### API Testing

#### Step 1 — Create Pipeline via Natural Language
```bash
curl -X POST http://localhost:9100/api/ai/configure \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "Create a sales pipeline called Enterprise Sales with these stages: Lead Qualification (10% probability), Discovery Call (20%), Technical Demo (40%), Proposal Sent (60%), Negotiation (75%), Contract Review (90%), Closed Won (100%), and Closed Lost (0%)"
  }'
```

**Expected Response**:
```json
{
  "commands": [{
    "action": "create_pipeline",
    "name": "Enterprise Sales",
    "object": "Opportunity",
    "stages": [
      { "name": "Lead Qualification", "probability": 10, "sortOrder": 1 },
      { "name": "Discovery Call", "probability": 20, "sortOrder": 2 },
      { "name": "Technical Demo", "probability": 40, "sortOrder": 3 },
      { "name": "Proposal Sent", "probability": 60, "sortOrder": 4 },
      { "name": "Negotiation", "probability": 75, "sortOrder": 5 },
      { "name": "Contract Review", "probability": 90, "sortOrder": 6 },
      { "name": "Closed Won", "probability": 100, "sortOrder": 7, "is_won": true, "is_closed": true },
      { "name": "Closed Lost", "probability": 0, "sortOrder": 8, "is_closed": true }
    ]
  }]
}
```

#### Step 2 — Confirm and Create
```bash
curl -X POST http://localhost:9100/api/ai/confirm \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "auditLogId": "<auditLogId>", "sessionId": "<sessionId>" }'
```

#### Step 3 — Add a Stage to Existing Pipeline
```bash
curl -X POST http://localhost:9100/api/ai/configure \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "Add a new stage called Proof of Concept at 50% probability between Technical Demo and Proposal Sent in the Enterprise Sales pipeline"
  }'
```

### UI Demo Steps
1. Navigate to **AI Config**
2. Type: *"Create a sales pipeline called Enterprise Sales with stages: Lead Qualification (10%), Discovery Call (20%), Technical Demo (40%), Proposal (60%), Negotiation (75%), Contract Review (90%), Closed Won (100%), Closed Lost (0%)"*
3. View the parsed pipeline with all stages and probabilities
4. Click **Confirm** to create the pipeline
5. Type: *"Add a POC stage at 50% between Demo and Proposal"*
6. Navigate to **Opportunities** to see the new pipeline in action

### What to Verify
- [ ] AI correctly extracts stage names and probabilities from description
- [ ] Stages are ordered correctly (sortOrder)
- [ ] Closed Won has `is_won: true` and `is_closed: true`
- [ ] Closed Lost has `is_closed: true` and 0% probability
- [ ] Additional stages can be inserted at specific positions
- [ ] Pipeline appears in the Opportunities module after creation

---

## Feature 3: Auto-Detect Data Fields from CSV Imports

**What it does**: Paste CSV data → AI intelligently maps column headers to CRM fields using fuzzy matching.

**Service**: ai-service (Port 9089)  
**Frontend**: Sidebar → **Zero Config** → **CSV Detection** tab

### API Testing

#### Step 1 — Detect Fields for Account CSV
```bash
curl -X POST http://localhost:9089/api/v1/ai/csv-detect-fields \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "csvContent": "Company Name,Industry,Website URL,Phone Number,Annual Rev,# Employees,Custom Field 1,Internal Code\nAcme Corp,Technology,https://acme.com,+1-555-0123,$5000000,250,custom1,IC001\nBeta Inc,Healthcare,https://beta.io,+1-555-0456,$12000000,800,custom2,IC002",
    "entityType": "account"
  }'
```

**Expected Response**:
```json
{
  "entityType": "account",
  "fieldMappings": [
    { "csvHeader": "Company Name", "crmField": "name", "dataType": "string", "confidence": 0.95, "sampleValue": "Acme Corp" },
    { "csvHeader": "Industry", "crmField": "industry", "dataType": "string", "confidence": 0.98, "sampleValue": "Technology" },
    { "csvHeader": "Website URL", "crmField": "website", "dataType": "url", "confidence": 0.92, "sampleValue": "https://acme.com" },
    { "csvHeader": "Phone Number", "crmField": "phone", "dataType": "phone", "confidence": 0.94, "sampleValue": "+1-555-0123" },
    { "csvHeader": "Annual Rev", "crmField": "annual_revenue", "dataType": "currency", "confidence": 0.88, "sampleValue": "$5,000,000" },
    { "csvHeader": "# Employees", "crmField": "number_of_employees", "dataType": "number", "confidence": 0.91, "sampleValue": "250" }
  ],
  "unmappedColumns": ["Custom Field 1", "Internal Code"],
  "totalColumns": 8,
  "mappedColumns": 6
}
```

#### Step 2 — Detect Fields for Contact CSV
```bash
curl -X POST http://localhost:9089/api/v1/ai/csv-detect-fields \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "csvContent": "First,Last,Email Address,Mobile,Job Title,Dept,LinkedIn Profile\nJohn,Smith,john@acme.com,+1-555-1234,VP Engineering,Engineering,https://linkedin.com/in/johnsmith\nJane,Doe,jane@beta.io,+1-555-5678,CFO,Finance,https://linkedin.com/in/janedoe",
    "entityType": "contact"
  }'
```

#### Step 3 — Detect Fields for Lead CSV
```bash
curl -X POST http://localhost:9089/api/v1/ai/csv-detect-fields \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "csvContent": "Lead Name,Company,Source,Est. Deal Size,Status,Notes\nAlice Brown,TechStartup,Web Form,$50000,New,Interested in enterprise\nBob Wilson,BigRetail,Trade Show,$200000,Contacted,Met at CES 2026",
    "entityType": "lead"
  }'
```

### UI Demo Steps
1. Navigate to **Zero Config** in the sidebar
2. Click the **CSV Detection** tab
3. Click **Load Sample** to auto-fill sample CSV data, OR paste your own
4. Select **Entity Type**: Account / Contact / Lead
5. Click **Detect Fields**
6. View the results table:
   - **CSV Header** → **CRM Field** mapping
   - **Data Type** detected (string, email, phone, url, currency, number)
   - **Confidence** score with color coding (green ≥85%, yellow 60-84%, red <60%)
   - **Sample Value** from the CSV
7. Note the **Unmapped Columns** alert at the bottom for fields that don't match CRM schema
8. Discuss how unmapped columns can become custom fields

### What to Verify
- [ ] All standard CRM headers are mapped (name, email, phone, etc.)
- [ ] Data types detected correctly (url, currency, phone vs. generic string)
- [ ] Confidence scores are reasonable (standard fields ≥0.85)
- [ ] Unmapped columns are listed separately
- [ ] Abbreviated headers like "# Employees" or "Annual Rev" are still matched
- [ ] Different entity types (account/contact/lead) produce different mappings
- [ ] Works with both clean and messy CSV headers

---

## Feature 4: Auto-Create Workflows using AI

**What it does**: Describe an automation in plain English → AI creates a workflow with triggers, conditions, and actions.

**Service**: crm-ai-agent (Port 9100) + workflow-service (Port 9088)  
**Frontend**: Sidebar → **AI Config** → Chat

### API Testing

#### Step 1 — Create Workflow via Natural Language
```bash
curl -X POST http://localhost:9100/api/ai/configure \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "Create a workflow that automatically sends a welcome email to the contact when a new lead is created with status New and source is Web"
  }'
```

**Expected Response**:
```json
{
  "commands": [{
    "action": "create_workflow",
    "name": "Welcome Email for Web Leads",
    "object": "Lead",
    "trigger_type": "record_created",
    "trigger_config": { "object": "Lead" },
    "conditions": [
      { "field": "status", "operator": "equals", "value": "New" },
      { "field": "source", "operator": "equals", "value": "Web" }
    ],
    "actions": [
      { "type": "send_email", "config": { "template": "welcome_email", "to": "{{contact_email}}" } }
    ]
  }]
}
```

#### Step 2 — Create Complex Workflow
```bash
curl -X POST http://localhost:9100/api/ai/configure \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "When an opportunity amount exceeds $500,000 and stage changes to Negotiation, notify the sales manager by email and create a high-priority task for contract review"
  }'
```

#### Step 3 — Create Escalation Workflow  
```bash
curl -X POST http://localhost:9100/api/ai/configure \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "instruction": "If a lead has not been contacted within 48 hours of creation, escalate to the team manager and change priority to urgent"
  }'
```

#### Step 4 — Verify via Workflow Service
```bash
curl -X GET "http://localhost:9088/api/v1/workflows?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

### UI Demo Steps
1. Navigate to **AI Config**
2. Type: *"Create a workflow that sends a welcome email when a new lead is created from the web"*
3. View the parsed workflow structure: trigger → conditions → actions
4. Click **Confirm** to create
5. Type: *"Create a workflow: when opportunity exceeds $500K and moves to Negotiation, notify the manager"*
6. Confirm and navigate to **Workflows** to see both rules created
7. Show that each workflow has the correct trigger type, conditions, and actions

### What to Verify
- [ ] AI extracts trigger type correctly (record_created, field_change, etc.)
- [ ] Conditions are parsed with correct operators (equals, greater_than, etc.)
- [ ] Actions are appropriate (send_email, create_task, update_field, notify)
- [ ] Complex multi-condition workflows are parsed correctly
- [ ] Workflow appears in the Workflows module after confirmation
- [ ] Trigger, conditions, and actions are structured correctly

---

## Feature 5: Automatic Data Deduplication

**What it does**: Automatically detects duplicate records by email, phone, and name — then supports one-click merge.

**Services**: contact-service (9084), lead-service (9082), account-service (9083)  
**Frontend**: Sidebar → **Contacts** / **Leads** / **Accounts**

### API Testing

#### Step 1 — Create Duplicate Contacts (Test Setup)
```bash
# Create first contact
curl -X POST http://localhost:9084/api/v1/contacts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "email": "john.smith@testcorp.com",
    "phone": "+1-555-999-0001",
    "title": "VP Engineering",
    "department": "Engineering"
  }'

# Create duplicate (same email)
curl -X POST http://localhost:9084/api/v1/contacts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "email": "john.smith@testcorp.com",
    "phone": "+1-555-999-0002",
    "title": "VP of Engineering",
    "department": "Tech"
  }'

# Create another duplicate (same name, different email)
curl -X POST http://localhost:9084/api/v1/contacts \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "email": "jsmith@testcorp.com",
    "phone": "+1-555-999-0001",
    "title": "Vice President Engineering"
  }'
```

#### Step 2 — Detect Duplicates (Contacts)
```bash
curl -X GET "http://localhost:9084/api/v1/contacts/duplicates" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response**:
```json
[
  {
    "matchType": "email",
    "matchValue": "john.smith@testcorp.com",
    "contacts": [
      { "id": "uuid-1", "firstName": "John", "lastName": "Smith", "email": "john.smith@testcorp.com", "title": "VP Engineering" },
      { "id": "uuid-2", "firstName": "John", "lastName": "Smith", "email": "john.smith@testcorp.com", "title": "VP of Engineering" }
    ]
  },
  {
    "matchType": "phone",
    "matchValue": "+1-555-999-0001",
    "contacts": [ ... ]
  }
]
```

#### Step 3 — Merge Duplicate Contacts
```bash
curl -X POST "http://localhost:9084/api/v1/contacts/merge?primaryId=<uuid-1>&duplicateId=<uuid-2>" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 4 — Detect Lead Duplicates
```bash
curl -X GET "http://localhost:9082/api/v1/leads/duplicates?email=john@test.com&phone=+1-555-0001" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 5 — Detect Account Duplicates
```bash
curl -X GET "http://localhost:9083/api/v1/accounts/duplicates?name=Acme&phone=+1-555-0123&website=acme.com" \
  -H "Authorization: Bearer $TOKEN"
```

### UI Demo Steps
1. Navigate to **Contacts**
2. Create two contacts with the same email (e.g., `john.smith@testcorp.com`)
3. Look for a **Duplicates** indicator or navigate to the deduplication view
4. View the duplicate groups:
   - Matched by **Email** (case-insensitive)
   - Matched by **Phone** (normalized)
   - Matched by **Name** (case-insensitive)
5. Click **Merge** → select the primary record
6. Verify the duplicate is merged and activities are transferred
7. Repeat for **Leads** and **Accounts** to show cross-entity dedup

### What to Verify
- [ ] Duplicates detected by email (case-insensitive matching)
- [ ] Duplicates detected by phone (normalized format)
- [ ] Duplicates detected by full name
- [ ] Merge transfers activities and relationships to primary record
- [ ] Duplicate record is removed after merge
- [ ] Works across contacts, leads, and accounts
- [ ] No false positives for different people with common names

---

## Feature 6: Automatic Contact Enrichment

**What it does**: Enter basic contact info → AI infers industry, seniority, department, timezone, LinkedIn, and more.

**Service**: ai-service (Port 9089)  
**Frontend**: Sidebar → **Zero Config** → **Enrichment** tab

### API Testing

#### Step 1 — Enrich a Contact (Complete Info)
```bash
curl -X POST http://localhost:9089/api/v1/ai/enrich-contact \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "contactId": "test-enrichment-1",
    "name": "David Kim",
    "email": "david.kim@techvista.io",
    "company": "TechVista Solutions",
    "title": "Chief Technology Officer",
    "phone": "+1-415-555-1101"
  }'
```

**Expected Response**:
```json
{
  "contactId": "test-enrichment-1",
  "enrichedFields": [
    { "field": "industry", "currentValue": "", "suggestedValue": "Software & Technology", "confidence": 0.92, "source": "Inferred from company name and domain" },
    { "field": "company_size", "currentValue": "", "suggestedValue": "Mid-Market (100-500)", "confidence": 0.78, "source": "TechVista Solutions company analysis" },
    { "field": "department", "currentValue": "", "suggestedValue": "Technology / Engineering", "confidence": 0.95, "source": "Extracted from title: CTO" },
    { "field": "seniority_level", "currentValue": "", "suggestedValue": "C-Level", "confidence": 0.98, "source": "Title: Chief Technology Officer" },
    { "field": "timezone", "currentValue": "", "suggestedValue": "America/Los_Angeles (PST)", "confidence": 0.85, "source": "Inferred from 415 area code (San Francisco)" },
    { "field": "professional_summary", "currentValue": "", "suggestedValue": "C-level technology executive leading engineering at a mid-market technology company.", "confidence": 0.72, "source": "Synthesized from title + company" }
  ],
  "overallConfidence": 0.87,
  "enrichmentSource": "AI-powered enrichment"
}
```

#### Step 2 — Enrich with Minimal Info
```bash
curl -X POST http://localhost:9089/api/v1/ai/enrich-contact \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "contactId": "test-enrichment-2",
    "name": "Amanda Foster",
    "email": "amanda.foster@globalretail.com"
  }'
```

#### Step 3 — Enrich with Just Email
```bash
curl -X POST http://localhost:9089/api/v1/ai/enrich-contact \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "contactId": "test-enrichment-3",
    "email": "priya.sharma@medhealth.org"
  }'
```

### UI Demo Steps
1. Navigate to **Zero Config** → Click **Enrichment** tab
2. Enter contact details:
   - **Name**: `David Kim`
   - **Email**: `david.kim@techvista.io`
   - **Company**: `TechVista Solutions`
   - **Title**: `CTO`
3. Click the **AI** button (🤖) to enrich
4. View the enrichment results table:
   - **Field** → **Current** vs. **Suggested Value**
   - **Confidence** score with color coding:
     - 🟢 Green: ≥85% confidence
     - 🟡 Yellow: 60-84%
     - 🔴 Red: below 60%
   - **Source** explaining how the value was inferred
5. Show **Overall Confidence** chip at the top
6. Try with minimal input (just an email) to show degraded but still useful enrichment

### What to Verify
- [ ] Industry correctly inferred from company name/domain
- [ ] Seniority level extracted from title (C-Level, Senior, Mid, Entry)
- [ ] Department inferred from role title
- [ ] Timezone inferred from phone area code or domain TLD
- [ ] Professional summary synthesized from available data
- [ ] Confidence scores decrease with less input data
- [ ] Overall confidence reflects accuracy of all enriched fields
- [ ] Color coding correctly reflects confidence levels

---

## Feature 7: Smart Default Dashboards

**What it does**: Pre-configured dashboards with pipeline metrics, revenue charts, forecasts, and conversion rates — ready from day one.

**Services**: opportunity-service (9085), account-service (9083)  
**Frontend**: Sidebar → **Dashboard**

### API Testing

#### Step 1 — Get Pipeline Analytics
```bash
curl -X GET "http://localhost:9085/api/v1/opportunities/pipeline" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response** (with demo data seeded):
```json
{
  "stageBreakdown": [
    { "stage": "PROSPECTING", "count": 1, "revenue": 180000, "percentage": 3.9 },
    { "stage": "QUALIFICATION", "count": 2, "revenue": 615000, "percentage": 13.3 },
    { "stage": "NEEDS_ANALYSIS", "count": 2, "revenue": 400000, "percentage": 8.6 },
    { "stage": "PROPOSAL", "count": 2, "revenue": 1230000, "percentage": 26.6 },
    { "stage": "NEGOTIATION", "count": 2, "revenue": 1650000, "percentage": 35.6 },
    { "stage": "CLOSED_WON", "count": 2, "revenue": 440000, "percentage": 9.5 },
    { "stage": "CLOSED_LOST", "count": 1, "revenue": 75000, "percentage": 1.6 }
  ],
  "totalPipeline": 4590000,
  "totalDeals": 12,
  "avgDealSize": 382500,
  "winRate": 16.66
}
```

#### Step 2 — Get Account Analytics
```bash
curl -X GET "http://localhost:9083/api/v1/accounts/analytics" \
  -H "Authorization: Bearer $TOKEN"
```

#### Step 3 — Get Opportunities by Stage
```bash
curl -X GET "http://localhost:9085/api/v1/opportunities?page=0&size=50" \
  -H "Authorization: Bearer $TOKEN"
```

### UI Demo Steps
1. Navigate to **Dashboard** in the sidebar
2. View the pre-configured widgets:
   - **Key Metrics Strip**: Total pipeline ($4.6M), deals (12), leads (15), contacts (20)
   - **Pipeline by Stage**: Bar chart showing revenue per stage
   - **Revenue Forecast**: Projected vs. actual revenue
   - **Conversion Rates**: Stage-to-stage conversion analysis
   - **Activity Feed**: Real-time team activity stream
3. Hover over the pipeline chart to see tooltips with deal count and revenue
4. Highlight that this dashboard is **automatically generated** — no manual widget configuration needed
5. Show how metrics update in real-time as deals move through stages

### What to Verify
- [ ] Dashboard loads automatically with no manual configuration
- [ ] Pipeline chart shows correct stage breakdown
- [ ] Total pipeline revenue matches sum of all opportunity amounts
- [ ] Win rate calculation is accurate
- [ ] Activity feed shows recent team activities
- [ ] Numbers update when opportunities are created/modified
- [ ] All charts render correctly with seeded data

---

## Feature 8: AI Onboarding Assistant

**What it does**: 10-step guided onboarding checklist with AI-generated hints, progress tracking, and contextual guidance.

**Service**: ai-service (Port 9089)  
**Frontend**: Sidebar → **Zero Config** → **Onboarding** tab

### API Testing

#### Step 1 — Get Onboarding Status
```bash
curl -X GET "http://localhost:9089/api/v1/ai/onboarding/status" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response**:
```json
{
  "completedSteps": 0,
  "totalSteps": 10,
  "progressPercent": 0,
  "steps": [
    { "id": "create_api_key", "title": "Create your API key", "description": "...", "category": "Setup", "completed": false, "actionUrl": "/settings", "aiHint": "..." },
    { "id": "connect_email", "title": "Connect your email provider", "description": "...", "category": "Setup", "completed": false, "actionUrl": "/integrations", "aiHint": "..." },
    { "id": "create_pipeline", "title": "Create your first sales pipeline", "description": "...", "category": "Setup", "completed": false, "actionUrl": "/ai-config", "aiHint": "..." },
    { "id": "import_data", "title": "Import your initial data", "description": "...", "category": "Data", "completed": false, "actionUrl": "/zero-config", "aiHint": "..." },
    { "id": "explore_dashboard", "title": "Explore your smart default dashboard", "description": "...", "category": "Insights", "completed": false, "actionUrl": "/dashboard", "aiHint": "..." },
    { "id": "add_users", "title": "Add team members", "description": "...", "category": "Team", "completed": false, "actionUrl": "/settings", "aiHint": "..." },
    { "id": "create_workflow", "title": "Create your first workflow", "description": "...", "category": "Automation", "completed": false, "actionUrl": "/ai-config", "aiHint": "..." },
    { "id": "configure_integrations", "title": "Connect external integrations", "description": "...", "category": "Integrations", "completed": false, "actionUrl": "/integrations", "aiHint": "..." },
    { "id": "invite_team", "title": "Invite your team", "description": "...", "category": "Team", "completed": false, "actionUrl": "/settings", "aiHint": "..." },
    { "id": "customize_fields", "title": "Customize fields", "description": "...", "category": "Setup", "completed": false, "actionUrl": "/ai-config", "aiHint": "..." }
  ],
  "nextRecommendation": "Start by creating your API key to enable integrations"
}
```

#### Step 2 — Complete a Step
```bash
curl -X POST "http://localhost:9089/api/v1/ai/onboarding/complete-step" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "stepId": "create_pipeline" }'
```

#### Step 3 — Get AI Guidance for a Step
```bash
curl -X GET "http://localhost:9089/api/v1/ai/onboarding/guidance/import_data" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected Response** (AI-generated):
```json
{
  "data": "To import your initial data, go to Zero Config > CSV Detection tab. Prepare your data as a CSV file with headers matching CRM fields like 'Company Name', 'Industry', 'Email', etc. Paste the CSV content and click 'Detect Fields' — the AI will automatically map your columns. For best results, start with your Accounts CSV first, then Contacts."
}
```

#### Step 4 — Complete Multiple Steps
```bash
# Mark several steps done
for stepId in "create_pipeline" "explore_dashboard" "import_data"; do
  curl -X POST "http://localhost:9089/api/v1/ai/onboarding/complete-step" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"stepId\": \"$stepId\"}"
done
```

#### Step 5 — Reset Onboarding (Admin/Manager only)
```bash
curl -X POST "http://localhost:9089/api/v1/ai/onboarding/reset" \
  -H "Authorization: Bearer $TOKEN"
```

### UI Demo Steps
1. Navigate to **Zero Config** → **Onboarding** tab (default tab)
2. View the **progress banner** at the top: "0/10 steps — 0%"
3. Walk through the **step-by-step checklist** (Stepper component):
   - Each step shows: title, description, category chip
   - **AI Hint** box with contextual guidance
   - **Go to [destination]** button → navigates to the relevant page
   - **Mark Complete** button → updates progress
   - **AI Guide** button → gets live AI-generated detailed guidance
4. Click **Mark Complete** on "Create Your Sales Pipeline"
5. Watch progress update to "1/10 — 10%"
6. Click **AI Guide** on "Import your initial data" → show AI-generated step-by-step guide
7. Note the **Next Recommendation** alert updates as steps are completed
8. Click **Reset** button to restart onboarding (shows all steps unchecked)

### What to Verify
- [ ] All 10 onboarding steps display with correct titles and descriptions
- [ ] Progress bar updates correctly as steps are completed
- [ ] Category chips are correctly assigned (Setup, Data, Sales, Automation, etc.)
- [ ] "Go to" buttons navigate to the correct pages
- [ ] "Mark Complete" updates step status and progress percentage
- [ ] "AI Guide" generates contextual, helpful guidance
- [ ] Next Recommendation changes based on completed steps
- [ ] Reset returns all steps to uncompleted state
- [ ] Progress persists across page refreshes (cached in Redis)

---

## Full Demo Script (12-Minute Walkthrough)

### Scene 1 — AI Onboarding Assistant (0:00 – 1:30)
**Narration**: *"The Zero-Configuration CRM eliminates setup complexity. Let's start with the AI Onboarding Assistant."*

1. Open **Zero Config** → show the 10-step onboarding checklist
2. Show progress at 0%
3. Click **AI Guide** on "Create Pipeline" → show AI-generated instructions
4. Click **Mark Complete** → progress jumps to 10%
5. Show the **Next Recommendation** updating

**Key Talking Points**:
- 10-step guided setup — no manuals needed
- AI hints provide contextual guidance for every step
- Progress tracking keeps the team on track
- Works offline with graceful fallbacks

---

### Scene 2 — Natural Language Configuration (1:30 – 3:30)
**Narration**: *"Instead of navigating admin panels, just tell the AI what you need in plain English."*

1. Navigate to **AI Config**
2. Type: *"Create a custom object called Project with fields for name, budget, start date, and status"*
3. Show the parsed command with action type and field details
4. Click **Confirm** → show success
5. Type: *"Add a high-priority dropdown field to Project"*
6. Show conversation history maintaining context

**Key Talking Points**:
- 11 supported action types: create objects, fields, relationships, workflows, pipelines, dashboards, roles, permissions
- Confirmation flow prevents accidental changes
- Conversation history for multi-step configuration
- Supports complex multi-field instructions

---

### Scene 3 — Auto-Create Pipelines (3:30 – 5:00)
**Narration**: *"Creating a sales pipeline used to require manual stage configuration. Now, just describe it."*

1. Type: *"Create a sales pipeline called Enterprise with stages: Qualification 10%, Demo 30%, Proposal 50%, Negotiation 70%, Contract 90%, Won 100%, Lost 0%"*
2. Show the parsed pipeline with all stages and probabilities
3. **Confirm** → pipeline created
4. Navigate to **Opportunities** → show the new pipeline in the stage selector

**Key Talking Points**:
- AI extracts stage names, probabilities, and closed/won flags
- Correct ordering with sort positions
- Add stages to existing pipelines via follow-up instructions

---

### Scene 4 — CSV Field Auto-Detection (5:00 – 6:30)
**Narration**: *"Importing data? No more manual column mapping."*

1. Navigate to **Zero Config** → **CSV Detection**
2. Click **Load Sample** → sample account CSV appears
3. Click **Detect Fields**
4. Walk through the results:
   - "Company Name" → `name` (95% confidence)
   - "Annual Rev" → `annual_revenue` (88% confidence)
   - "# Employees" → `number_of_employees` (91%)
5. Point out unmapped columns: "Custom Field 1", "Internal Code"
6. Change entity type to **Contact** and show different mappings

**Key Talking Points**:
- Fuzzy matching handles abbreviations ("Annual Rev", "# Employees")
- Detects data types: string, email, phone, url, currency, number
- Confidence scoring helps users verify accuracy
- Unmapped columns flagged for custom field creation

---

### Scene 5 — AI Workflow Automation (6:30 – 8:00)
**Narration**: *"Automations should be described, not coded."*

1. Navigate back to **AI Config**
2. Type: *"Create a workflow that sends a welcome email when a new lead is created from the web"*
3. Show the parsed workflow: trigger (record_created), conditions (source=Web), action (send_email)
4. **Confirm** → workflow created
5. Type: *"When an opportunity exceeds $500K and moves to Negotiation, notify the sales manager"*
6. Navigate to **Workflows** → show both new rules listed

**Key Talking Points**:
- Natural language → trigger + condition + action mapping
- Multi-condition support
- Multiple action types: email, task creation, field update, notifications
- Workflows run in real-time via Kafka events

---

### Scene 6 — Data Deduplication (8:00 – 9:30)
**Narration**: *"Duplicate data is the #1 CRM data quality problem. Our AI solves it automatically."*

1. Navigate to **Contacts**
2. Create two contacts with the same email
3. Go to **Duplicates** view
4. Show duplicate groups matched by email, phone, and name
5. Click **Merge** → select primary record
6. Verify the duplicate is removed and data is consolidated

**Key Talking Points**:
- Three matching strategies: email, phone (normalized), full name
- One-click merge preserves activities and relationships
- Works across contacts, leads, and accounts
- Prevents data entry of known duplicates

---

### Scene 7 — Contact Enrichment (9:30 – 10:30)
**Narration**: *"Enter a name and email — AI fills in the rest."*

1. Navigate to **Zero Config** → **Enrichment** tab
2. Enter: Name=`David Kim`, Email=`david.kim@techvista.io`, Company=`TechVista Solutions`, Title=`CTO`
3. Click **Enrich**
4. Walk through enriched fields:
   - **Industry**: Software & Technology (92%)
   - **Seniority**: C-Level (98%)
   - **Department**: Technology (95%)
   - **Timezone**: PST (85%)
   - **Summary**: "C-level technology executive..." (72%)
5. Point out confidence color coding: green/yellow/red

**Key Talking Points**:
- AI infers 6+ fields from basic input
- Confidence scoring so users know what to trust
- Works with minimal input (even just an email)
- Sources explained for transparency

---

### Scene 8 — Smart Dashboards (10:30 – 11:15)
**Narration**: *"Dashboards that work from day one — no widget configuration needed."*

1. Navigate to **Dashboard**
2. Show pre-configured widgets:
   - Pipeline breakdown: $4.6M across 7 stages
   - Win rate: 16.6%
   - Average deal size: $382K
3. Hover over charts for detailed tooltips
4. Show the real-time activity feed

**Key Talking Points**:
- Pipeline, revenue, and conversion analytics pre-built
- Real-time updates via SSE
- No manual dashboard creation required
- Automatically adapts as data grows

---

### Scene 9 — Feature Overview (11:15 – 12:00)
**Narration**: *"That's 8 features working together to make CRM setup effortless."*

1. Navigate to **Zero Config** → **All Features** tab
2. Show the 8-feature card grid — all marked "Active"
3. Recap: *"From onboarding to configuration, importing to enrichment — every setup task is AI-assisted."*

---

## Quick Smoke Test Script (PowerShell)

Run to validate all 8 features:

```powershell
$BASE = "http://localhost"
$token = "<your-token>"
$h = @{ "Authorization" = "Bearer $token"; "Content-Type" = "application/json" }

Write-Host "`n=== Testing Zero-Config AI Features ===" -ForegroundColor Cyan

# 1. NL Configuration
Write-Host "`n[1] NL Config - Send instruction" -ForegroundColor Yellow
$nlBody = '{"instruction":"Create a field called test_field on Lead with type text"}'
Invoke-RestMethod -Method POST -Uri "$BASE`:9100/api/ai/configure" -Headers $h -Body $nlBody | ConvertTo-Json -Depth 3

# 2. CSV Field Detection
Write-Host "`n[2] CSV Detection - Account" -ForegroundColor Yellow
$csvBody = '{"csvContent":"Company,Industry,Phone\nAcme,Tech,555-0001","entityType":"account"}'
Invoke-RestMethod -Method POST -Uri "$BASE`:9089/api/v1/ai/csv-detect-fields" -Headers $h -Body $csvBody | ConvertTo-Json -Depth 3

# 3. Contact Enrichment
Write-Host "`n[3] Contact Enrichment" -ForegroundColor Yellow
$enrBody = '{"contactId":"test-1","name":"David Kim","email":"david.kim@techvista.io","company":"TechVista","title":"CTO"}'
Invoke-RestMethod -Method POST -Uri "$BASE`:9089/api/v1/ai/enrich-contact" -Headers $h -Body $enrBody | ConvertTo-Json -Depth 3

# 4. Onboarding Status
Write-Host "`n[4] Onboarding - Get Status" -ForegroundColor Yellow
Invoke-RestMethod -Uri "$BASE`:9089/api/v1/ai/onboarding/status" -Headers $h | ConvertTo-Json -Depth 3

# 5. Onboarding - Complete Step
Write-Host "`n[5] Onboarding - Complete Step" -ForegroundColor Yellow
$stepBody = '{"stepId":"explore_dashboard"}'
Invoke-RestMethod -Method POST -Uri "$BASE`:9089/api/v1/ai/onboarding/complete-step" -Headers $h -Body $stepBody | ConvertTo-Json -Depth 3

# 6. Onboarding - AI Guidance
Write-Host "`n[6] Onboarding - AI Guidance" -ForegroundColor Yellow
Invoke-RestMethod -Uri "$BASE`:9089/api/v1/ai/onboarding/guidance/import_data" -Headers $h | ConvertTo-Json -Depth 3

# 7. Contact Duplicates
Write-Host "`n[7] Contact Deduplication" -ForegroundColor Yellow
Invoke-RestMethod -Uri "$BASE`:9084/api/v1/contacts/duplicates" -Headers $h | ConvertTo-Json -Depth 3

# 8. Pipeline Analytics (Smart Dashboard)
Write-Host "`n[8] Smart Dashboard - Pipeline" -ForegroundColor Yellow
Invoke-RestMethod -Uri "$BASE`:9085/api/v1/opportunities/pipeline" -Headers $h | ConvertTo-Json -Depth 3

Write-Host "`n=== All Zero-Config Tests Complete ===" -ForegroundColor Green
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Re-authenticate: `POST /api/v1/auth/login` |
| AI Config chat not responding | Check crm-ai-agent: `docker logs crm-ai-agent` (Port 9100) |
| CSV detection returns generic mapping | Check ai-service has OpenAI API key configured |
| Enrichment confidence all low | Provide more input fields (name + email + company + title) |
| Onboarding status not persisting | Check Redis is running: `docker ps \| grep redis` |
| Workflows not appearing after confirm | Check workflow-service: `docker logs crm-workflow-service` |
| Duplicates endpoint empty | Create duplicate records first, then call detect |
| Dashboard shows no data | Run `.\demo\seed-demo.ps1` to populate demo data |
