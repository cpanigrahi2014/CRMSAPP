# CRM Application — Manual Test Cases

> **Application URL:** http://34.193.72.13  
> **Test Credentials:** demo@crm.com / Demo@2026! (ADMIN role)  
> **Total Modules:** 18 | **Total Test Cases:** 320+  
> **Last Updated:** March 15, 2026

---

## Table of Contents

1. [TC-AUTH: Authentication & Authorization](#1-tc-auth-authentication--authorization)
2. [TC-DASH: Dashboard](#2-tc-dash-dashboard)
3. [TC-LEAD: Lead Management](#3-tc-lead-lead-management)
4. [TC-ACCT: Account Management](#4-tc-acct-account-management)
5. [TC-CONT: Contact Management](#5-tc-cont-contact-management)
6. [TC-OPP: Opportunity Management](#6-tc-opp-opportunity-management)
7. [TC-ACT: Activity Management](#7-tc-act-activity-management)
8. [TC-EMAIL: Email Module](#8-tc-email-email-module)
9. [TC-CASE: Case / Support Management](#9-tc-case-case--support-management)
10. [TC-CAMP: Campaign Management](#10-tc-camp-campaign-management)
11. [TC-WF: Workflow & Automation](#11-tc-wf-workflow--automation)
12. [TC-COLLAB: Collaboration](#12-tc-collab-collaboration)
13. [TC-COMM: Communications (SMS/WhatsApp/Calls)](#13-tc-comm-communications)
14. [TC-AI: AI Features & Insights](#14-tc-ai-ai-features--insights)
15. [TC-SEC: Security & Administration](#15-tc-sec-security--administration)
16. [TC-INTEG: Integrations](#16-tc-integ-integrations)
17. [TC-DEV: Developer Portal](#17-tc-dev-developer-portal)
18. [TC-REPORT: Reports & Analytics](#18-tc-report-reports--analytics)
19. [TC-MISC: Settings, Pricing & Zero-Config](#19-tc-misc-settings-pricing--zero-config)
20. [TC-CROSS: Cross-Module & E2E Scenarios](#20-tc-cross-cross-module--e2e-scenarios)

---

## 1. TC-AUTH: Authentication & Authorization

### TC-AUTH-001: User Registration
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Browser open, navigate to `/auth/register` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://<host>/auth/register` | Registration page loads with fields: First Name, Last Name, Email, Password, Tenant ID |
| 2 | Leave all fields blank and click **Register** | Validation errors shown for all required fields |
| 3 | Enter invalid email format (e.g., "abc") | Email validation error displayed |
| 4 | Enter password less than 8 characters | Password strength/length error displayed |
| 5 | Fill valid data: First=Test, Last=User, Email=test@crm.com, Password=Test@2026!, Tenant=default | No validation errors |
| 6 | Click **Register** | Success message, redirected to login or dashboard. User created with USER role |
| 7 | Try registering same email+tenant again | Error: "User already exists" or duplicate message |

---

### TC-AUTH-002: User Login
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | User demo@crm.com registered |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `http://<host>/auth/login` | Login page loads with Email, Password, Tenant ID fields |
| 2 | Enter wrong password | Error: "Invalid credentials" or 401 message |
| 3 | Enter wrong email | Error: "Invalid credentials" |
| 4 | Enter correct credentials: demo@crm.com / Demo@2026! / default | Success: redirected to `/dashboard` |
| 5 | Verify JWT token stored in browser (LocalStorage or cookie) | `accessToken` and `refreshToken` present |
| 6 | Refresh the page | User remains logged in (token persists) |

---

### TC-AUTH-003: Forgot / Reset Password
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | User registered |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/auth/forgot-password` | Forgot password form loads |
| 2 | Enter registered email and submit | Success message: "Reset link sent" (API returns 200) |
| 3 | Enter unregistered email | Appropriate error or generic "If account exists, link sent" message |
| 4 | Navigate to `/auth/reset-password` with valid token | Reset form loads with new password fields |
| 5 | Enter mismatched passwords | Validation error |
| 6 | Enter valid matching password and submit | Password updated successfully |
| 7 | Login with old password | Fails |
| 8 | Login with new password | Success |

---

### TC-AUTH-004: User Profile (/me)
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | User logged in |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | After login, check that top-right user menu/avatar loads | User name and email displayed |
| 2 | Click user profile/avatar | Profile dropdown or page shown |
| 3 | Verify displayed info matches: name, email, roles, tenant | All fields correct |

---

### TC-AUTH-005: Logout
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | User logged in |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Logout** button | Redirected to `/auth/login` |
| 2 | Try navigating to `/dashboard` directly | Redirected back to login (auth guard) |
| 3 | Check local storage | Tokens cleared |

---

### TC-AUTH-006: Role-Based Access Control
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Two users: one ADMIN, one USER-only |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login as USER-only (no ADMIN role) | Dashboard loads |
| 2 | Navigate to `/security` | Page loads but API calls return 403. Admin features hidden or error shown |
| 3 | Navigate to `/integrations` | 403 on integration management endpoints |
| 4 | Login as ADMIN | Dashboard loads |
| 5 | Navigate to `/security` | All security management features accessible (roles, permissions, audit logs) |
| 6 | Navigate to `/integrations` | Full integration management available |

---

## 2. TC-DASH: Dashboard

### TC-DASH-001: Dashboard Page Load
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | User logged in |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | After login, verify redirect to `/dashboard` | Dashboard page loads without errors |
| 2 | Check for pipeline summary widget | Shows opportunity stages and counts |
| 3 | Check for revenue/conversion analytics | Charts/graphs render (may show zeros if no data) |
| 4 | Check opportunity dashboard data | Pipeline value, win rate, deal counts displayed |
| 5 | Check sales quota widget | Active quotas shown (or empty state) |
| 6 | Verify no console errors (F12 → Console) | No 500 or uncaught errors |

---

### TC-DASH-002: Dashboard Navigation
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | User on dashboard |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click sidebar link **Leads** | Navigates to `/leads` |
| 2 | Click sidebar link **Accounts** | Navigates to `/accounts` |
| 3 | Click sidebar link **Contacts** | Navigates to `/contacts` |
| 4 | Click sidebar link **Opportunities** | Navigates to `/opportunities` |
| 5 | Click sidebar link **Activities** | Navigates to `/activities` |
| 6 | Click sidebar link **Email** | Navigates to `/email` |
| 7 | Click sidebar link **Cases** | Navigates to `/cases` |
| 8 | Click sidebar link **Campaigns** | Navigates to `/campaigns` |
| 9 | Click sidebar link **Reports** | Navigates to `/reports` |
| 10 | Click sidebar link **Workflows** | Navigates to `/workflows` |
| 11 | Verify all pages load without errors | Each page renders its list/empty state |

---

## 3. TC-LEAD: Lead Management

### TC-LEAD-001: Lead List Page
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | User logged in, navigate to `/leads` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/leads` | Leads list page loads |
| 2 | Verify table/list headers | Columns: Name, Email, Phone, Status, Source, Score, Owner, Created |
| 3 | Verify pagination controls | Page size selector, page numbers, next/prev buttons visible |
| 4 | Change page size to 25 | Table refreshes with up to 25 rows |
| 5 | Click column header (e.g., Created) | Rows sorted by that column |
| 6 | If no data, verify empty state message | "No leads found" or similar message |

---

### TC-LEAD-002: Create Lead
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | User on leads page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **+ New Lead** / **Create Lead** button | Lead creation form/modal opens |
| 2 | Leave required fields blank and submit | Validation errors for required fields |
| 3 | Fill in: First Name=John, Last Name=Doe, Email=john@example.com, Phone=+1234567890, Company=TestCorp, Status=NEW, Source=WEBSITE | All fields populated |
| 4 | Click **Save** / **Create** | Lead created successfully. Redirected to lead detail or back to list |
| 5 | Verify lead appears in the list | New lead visible with correct data |

---

### TC-LEAD-003: View Lead Detail
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | At least one lead exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click on a lead row in the list | Navigates to `/leads/{id}` |
| 2 | Verify lead details displayed | Name, email, phone, company, status, source, score, owner |
| 3 | Verify tabs/sections present | Notes, Activities, Tags, Attachments sections visible |
| 4 | Click browser back button | Returns to leads list |

---

### TC-LEAD-004: Edit Lead
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | On lead detail page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Edit** button on lead detail | Edit form opens with pre-filled data |
| 2 | Change Company to "UpdatedCorp" | Field updated |
| 3 | Change Status from NEW to CONTACTED | Status updated |
| 4 | Click **Save** | Success message, lead detail reflects changes |
| 5 | Refresh page | Changes persisted |

---

### TC-LEAD-005: Delete Lead
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | At least one lead exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On lead detail, click **Delete** | Confirmation dialog appears |
| 2 | Click **Cancel** | Dialog closes, lead intact |
| 3 | Click **Delete** again, then **Confirm** | Lead soft-deleted, redirected to leads list |
| 4 | Verify lead no longer appears in list | Lead removed from active list |

---

### TC-LEAD-006: Search Leads
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Multiple leads exist |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Type search term (e.g., "John") in search box | List filtered to matching leads |
| 2 | Clear search | Full list restored |
| 3 | Search by email domain (e.g., "@example.com") | Matching leads displayed |
| 4 | Search for non-existent term | Empty results with appropriate message |

---

### TC-LEAD-007: Lead Notes
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | On lead detail page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to Notes section/tab | Notes area visible |
| 2 | Click **Add Note** | Note input appears |
| 3 | Enter note text: "Initial call made, prospect interested" | Text written |
| 4 | Click **Save** | Note saved, appears in notes list with timestamp |
| 5 | Verify note displays author name and timestamp | Correct metadata shown |
| 6 | Click **Delete** on the note (if ADMIN/MANAGER) | Note removed |

---

### TC-LEAD-008: Lead Tags
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | On lead detail page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to Tags section | Tags area visible |
| 2 | Add a tag (e.g., "Hot Lead") | Tag appears on the lead |
| 3 | Add another tag (e.g., "Enterprise") | Second tag appears |
| 4 | Remove a tag | Tag removed from lead |

---

### TC-LEAD-009: Lead Assignment
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | ADMIN/MANAGER role, lead exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On lead detail, find **Assign** action | Assign option available |
| 2 | Select an assignee user | Lead assigned to selected user |
| 3 | Verify assignee displayed on lead detail | New owner/assignee shown |

---

### TC-LEAD-010: Convert Lead to Opportunity
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | ADMIN/MANAGER role, qualified lead exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On lead detail, click **Convert** button | Conversion form/dialog opens |
| 2 | Fill conversion details (account name, opportunity name, amount) | Fields populated |
| 3 | Click **Convert** | Lead status changes to CONVERTED. New opportunity created |
| 4 | Navigate to Opportunities | New opportunity visible in list |
| 5 | Verify the converted lead is no longer in active leads | Lead marked as converted |

---

### TC-LEAD-011: Lead Import/Export CSV
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | ADMIN/MANAGER role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Export** button on leads page | CSV file downloaded with current leads |
| 2 | Open CSV and verify data columns | Columns match lead fields |
| 3 | Prepare an import CSV with new leads | Valid CSV file ready |
| 4 | Click **Import** → select CSV file | Import dialog shows field mapping |
| 5 | Confirm import | Leads imported, visible in list |

---

### TC-LEAD-012: Lead Scoring & Analytics
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | Leads exist with scores |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View lead detail and check score field | Lead score displayed (0-100) |
| 2 | Navigate to lead analytics section | Analytics charts load: leads by status, source, conversion rates |
| 3 | Check SLA-breached leads view | List of leads past SLA response time |

---

### TC-LEAD-013: Duplicate Detection
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | Multiple leads with similar data |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create a lead with same email as existing lead | Duplicate warning shown |
| 2 | View duplicate detection results | Matching leads listed with similarity indicators |

---

### TC-LEAD-014: Lead Attachments
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | On lead detail page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to Attachments section | Attachment area visible |
| 2 | Click **Upload** and select a file (PDF/image) | File uploads with progress indicator |
| 3 | Verify attachment listed with name, size, date | Metadata correct |
| 4 | Click **Download** on the attachment | File downloads correctly |
| 5 | Click **Delete** (ADMIN/MANAGER) | Attachment removed |

---

### TC-LEAD-015: Web Form Lead Capture
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | ADMIN/MANAGER role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View web forms list | Existing web forms shown |
| 2 | Create a new web form with name and fields | Web form created |
| 3 | Submit the web form via public endpoint (no auth) | Lead created from form submission |
| 4 | Verify new lead in leads list with source=WEBFORM | Lead present with form data |

---

## 4. TC-ACCT: Account Management

### TC-ACCT-001: Account List Page
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | User logged in, navigate to `/accounts` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/accounts` | Accounts list page loads |
| 2 | Verify table headers | Columns: Name, Type, Industry, Phone, Website, Owner, Health Score |
| 3 | Verify pagination | Page controls visible and functional |
| 4 | Sort by name | Rows sorted alphabetically |

---

### TC-ACCT-002: Create Account
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | User on accounts page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **+ New Account** | Account creation form opens |
| 2 | Fill: Name=Acme Corp, Type=CUSTOMER, Industry=Technology, Phone=+1234567890, Website=acme.com | Fields populated |
| 3 | Set Territory, Segment, Lifecycle Stage if available | Fields set |
| 4 | Click **Save** | Account created, appears in list |
| 5 | Verify account detail page | All entered data displayed correctly |

---

### TC-ACCT-003: Edit Account
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Account exists, on account detail |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Edit** | Edit form with pre-filled data |
| 2 | Change Industry to "Healthcare" | Field updated |
| 3 | Update phone number | Field updated |
| 4 | Click **Save** | Changes saved and reflected |

---

### TC-ACCT-004: Delete Account
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | ADMIN/MANAGER, account exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Delete** on account detail | Confirmation dialog |
| 2 | Confirm deletion | Account removed from list |

---

### TC-ACCT-005: Account Search & Filters
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Multiple accounts exist |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Search by account name | Matching accounts shown |
| 2 | Filter by Type (CUSTOMER, PROSPECT, etc.) | Filtered results |
| 3 | Filter by Territory | Territory-specific accounts shown |
| 4 | Filter by Segment | Segment-specific accounts shown |
| 5 | Filter by Lifecycle stage | Stage-specific accounts shown |

---

### TC-ACCT-006: Account Notes & Attachments
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | On account detail page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Add a note to the account | Note saved with timestamp and author |
| 2 | View notes list | All notes displayed chronologically |
| 3 | Delete a note (ADMIN/MANAGER) | Note removed |
| 4 | Add an attachment | File uploaded successfully |
| 5 | Download an attachment | File downloads correctly |
| 6 | Delete attachment (ADMIN/MANAGER) | Attachment removed |

---

### TC-ACCT-007: Account Tags
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | On account detail page |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create a new tag (ADMIN/MANAGER) | Tag created with name and color |
| 2 | Add tag to account | Tag appears on account |
| 3 | View account tags | Tagged accounts shown |
| 4 | Remove tag from account | Tag removed |

---

### TC-ACCT-008: Account Health & Engagement Scores
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | ADMIN/MANAGER role, account exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View health score on account detail | Score displayed (0-100) |
| 2 | Update health score | New score saved and displayed |
| 3 | Update engagement score | New score saved and displayed |

---

### TC-ACCT-009: Account Hierarchy (Parent/Child)
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | Parent account exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create a child account with parentAccountId set | Child account created |
| 2 | View parent account → Children section | Child account listed |
| 3 | Click child account | Navigates to child detail |

---

### TC-ACCT-010: Account Merge & Duplicate Detection
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | ADMIN/MANAGER, duplicate accounts exist |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run duplicate detection | Potential duplicates identified |
| 2 | Select two accounts to merge | Merge dialog opens |
| 3 | Choose primary account and confirm | Accounts merged, secondary deleted |

---

### TC-ACCT-011: Account Import/Export
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | ADMIN/MANAGER role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Export** | CSV downloaded with account data |
| 2 | Prepare import CSV | Valid CSV ready |
| 3 | Click **Import** → select file | Accounts imported |

---

### TC-ACCT-012: Account Analytics
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | Accounts exist |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View account analytics | Charts: accounts by type, industry, territory, health distribution |

---

## 5. TC-CONT: Contact Management

### TC-CONT-001: Contact List Page
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Navigate to `/contacts` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Contact list displayed with columns: Name, Email, Phone, Account, Title, Segment |
| 2 | Verify pagination and sorting | Controls functional |
| 3 | If no data, empty state shown | "No contacts found" message |

---

### TC-CONT-002: Create Contact
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **+ New Contact** | Contact form opens |
| 2 | Fill: First=Jane, Last=Smith, Email=jane@acme.com, Phone=+9876543210, Account=Acme Corp | Fields populated |
| 3 | Set Title=CTO, Segment=ENTERPRISE, Lifecycle=CUSTOMER | Additional fields set |
| 4 | Click **Save** | Contact created successfully |

---

### TC-CONT-003: View & Edit Contact
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click contact row | Detail page at `/contacts/{id}` loads |
| 2 | Verify all fields displayed | Name, email, phone, account, title, segment, lifecycle |
| 3 | Click **Edit**, update phone number | Edit form pre-filled |
| 4 | Save changes | Updated data persisted |

---

### TC-CONT-004: Delete Contact
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Delete** on contact detail | Confirmation dialog |
| 2 | Confirm | Contact removed |

---

### TC-CONT-005: Contact Communication History
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Contact exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to Communication section on contact detail | Communication log visible |
| 2 | Log a new communication: Type=CALL, Subject="Follow-up", Notes="Discussed pricing" | Communication logged |
| 3 | View logged communications | Entry with type, subject, notes, timestamp |
| 4 | Delete communication (ADMIN/MANAGER) | Entry removed |

---

### TC-CONT-006: Contact Consent Management
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On contact detail, find consent/marketing preferences | Consent fields visible |
| 2 | Update marketing consent to opt-out | Consent updated |
| 3 | Verify contact marked as opted-out | Consent status reflected |

---

### TC-CONT-007: Contact Search & Filters
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Search by name | Matching contacts shown |
| 2 | Filter by segment | Segment-specific contacts |
| 3 | Filter by lifecycle stage | Stage-specific contacts |
| 4 | Filter by account | Account-specific contacts |

---

### TC-CONT-008: Contact Tags
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Add tag to contact | Tag appears |
| 2 | View tags | All tags listed |
| 3 | Remove tag (ADMIN/MANAGER) | Tag removed |

---

### TC-CONT-009: Contact Merge & Duplicates
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Run duplicate detection | Potential duplicates shown |
| 2 | Merge two contacts | Merged successfully |

---

### TC-CONT-010: Contact Import/Export & Analytics
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Export contacts as CSV | CSV downloaded |
| 2 | Import contacts from CSV | Contacts imported |
| 3 | View contact analytics | Charts: contacts by segment, lifecycle, engagement |

---

## 6. TC-OPP: Opportunity Management

### TC-OPP-001: Opportunity List Page
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Navigate to `/opportunities` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Opportunity list with columns: Name, Account, Stage, Amount, Close Date, Probability, Owner |
| 2 | Verify pagination and sorting | Functional |
| 3 | Verify pipeline view toggle (if available) | Kanban/board view of stages |

---

### TC-OPP-002: Create Opportunity
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **+ New Opportunity** | Opportunity form opens |
| 2 | Fill: Name=Acme Enterprise Deal, Account=Acme Corp, Stage=PROSPECTING, Amount=500000, Close Date=2026-06-30, Probability=30 | Fields populated |
| 3 | Click **Save** | Opportunity created, visible in list |

---

### TC-OPP-003: View Opportunity Detail
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click opportunity row | Detail page at `/opportunities/{id}` |
| 2 | Verify sections: Details, Products, Competitors, Notes, Activities, Collaborators, Reminders | All sections present |
| 3 | Verify stage pipeline visualization | Current stage highlighted in pipeline bar |

---

### TC-OPP-004: Edit Opportunity
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Edit** | Edit form with pre-filled data |
| 2 | Update Amount to 600000 | Field changed |
| 3 | Update Probability to 50 | Field changed |
| 4 | Save | Changes persisted |

---

### TC-OPP-005: Stage Progression
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On opportunity detail, click stage in pipeline (or use Stage dropdown) | Stage update dialog opens |
| 2 | Move from PROSPECTING → QUALIFICATION | Stage updated |
| 3 | Move from QUALIFICATION → PROPOSAL | Stage updated |
| 4 | Move to CLOSED_WON | Stage set to won, opportunity shows as won |
| 5 | Move a different opportunity to CLOSED_LOST, provide lost reason and competitor | Lost reason saved |

---

### TC-OPP-006: Opportunity Products
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | On opportunity detail |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to Products section | Products tab/section visible |
| 2 | Click **Add Product** | Product form opens |
| 3 | Fill: Name=Enterprise License, Quantity=100, Unit Price=500 | Fields populated |
| 4 | Save | Product added, total amount reflected |
| 5 | Delete product (ADMIN/MANAGER) | Product removed |

---

### TC-OPP-007: Opportunity Competitors
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to Competitors section | Competitors area visible |
| 2 | Add competitor: Name=Salesforce, Strengths="Market leader", Weaknesses="Expensive" | Competitor added |
| 3 | View competitors list | Entry displayed |
| 4 | Delete competitor | Removed |

---

### TC-OPP-008: Opportunity Notes
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Add note: "Meeting scheduled with CTO for next week" | Note saved |
| 2 | View notes list | Note displayed with author and timestamp |
| 3 | Delete note (ADMIN/MANAGER) | Note removed |

---

### TC-OPP-009: Opportunity Collaborators
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | ADMIN/MANAGER role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Add collaborator with role (e.g., userId, role=CONTRIBUTOR) | Collaborator added |
| 2 | View collaborators list | All collaborators shown with roles |
| 3 | Remove collaborator | Collaborator removed |

---

### TC-OPP-010: Opportunity Reminders
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Add reminder: "Send proposal by Friday" with due date | Reminder created |
| 2 | View reminders list | Reminder displayed |
| 3 | Mark reminder complete | Status changes to completed |
| 4 | View overdue reminders | Due reminders listed |
| 5 | Delete reminder (ADMIN/MANAGER) | Removed |

---

### TC-OPP-011: Pipeline Dashboard & Forecast
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View pipeline view | Kanban board showing opportunities by stage |
| 2 | View revenue forecast | Forecast data with weighted pipeline value |
| 3 | View win/loss analysis | Win rate, lost reasons breakdown |
| 4 | View conversion analytics | Stage-to-stage conversion rates |
| 5 | View performance/velocity metrics | Average days in stage, velocity trends |

---

### TC-OPP-012: Opportunity Search & Filters
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Search by opportunity name | Matching results |
| 2 | Filter by stage | Stage-specific opportunities |
| 3 | Filter by account | Account-specific opportunities |
| 4 | Filter by date range | Date-range filtered results |
| 5 | Filter by assignee | Assignee-specific opportunities |

---

### TC-OPP-013: Opportunity Import/Export
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Export opportunities as CSV | CSV downloaded |
| 2 | Import from CSV | Opportunities imported |

---

### TC-OPP-014: Opportunity Alerts
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View alerts section | Overdue, stale, and closing-soon alerts shown |
| 2 | Click on an alert | Navigates to the relevant opportunity |

---

## 7. TC-ACT: Activity Management

### TC-ACT-001: Activity List Page
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Navigate to `/activities` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Activity list with columns: Subject, Type, Status, Due Date, Related Entity, Assignee |
| 2 | Filter by type (CALL, MEETING, TASK, EMAIL) | Filtered activities |
| 3 | Filter by status (SCHEDULED, COMPLETED, OVERDUE) | Filtered activities |
| 4 | Verify pagination and sorting | Controls functional |

---

### TC-ACT-002: Create Activity
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **+ New Activity** | Activity form opens |
| 2 | Fill: Type=MEETING, Subject="Demo Call with Acme", Due Date=tomorrow, Related Entity=Lead/John Doe | Fields populated |
| 3 | Set assignee | Assignee selected |
| 4 | Click **Save** | Activity created |

---

### TC-ACT-003: Edit & Complete Activity
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click activity to view detail | Detail displayed |
| 2 | Click **Edit**, change subject | Saved successfully |
| 3 | Click **Mark Complete** | Status changes to COMPLETED |
| 4 | Verify completed activity in list | Shows completed status |

---

### TC-ACT-004: Delete Activity
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Delete** (ADMIN/MANAGER) | Confirmation dialog |
| 2 | Confirm | Activity removed |

---

### TC-ACT-005: Activity Timeline
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View full timeline | All activities chronologically |
| 2 | View entity-specific timeline (from lead/account detail) | Activities for that entity |
| 3 | View upcoming activities (next 7 days) | Upcoming list shown |
| 4 | View overdue activities | Overdue items highlighted |

---

### TC-ACT-006: Activity Stream (Real-time)
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View activity stream | Stream of recent events |
| 2 | Create an entity in another tab | Stream updates with new event |
| 3 | Record a stream event manually | Event appears in stream |

---

### TC-ACT-007: Activity Analytics
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View activity analytics | Charts: activities by type, completion rates, overdue trends |

---

## 8. TC-EMAIL: Email Module

### TC-EMAIL-001: Email Page Load
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Navigate to `/email` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Email interface with Inbox, Sent, Compose sections |
| 2 | If no email accounts configured | "Connect email account" prompt shown |

---

### TC-EMAIL-002: Email Account Setup (SMTP)
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Add Account** / **Connect** | Account setup form |
| 2 | Select SMTP and fill host, port, username, password | Fields populated |
| 3 | Save | Email account connected |
| 4 | Set as default account | Default indicator shown |

---

### TC-EMAIL-003: Compose & Send Email
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Email account configured |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Compose** | Email composer opens |
| 2 | Fill To, Subject, Body | Fields populated |
| 3 | Click **Send** | Email sent, appears in Sent folder |
| 4 | Check sent messages list | Email visible |

---

### TC-EMAIL-004: Email Inbox & Threading
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View inbox | Received emails listed |
| 2 | Click an email | Email content displayed |
| 3 | View email thread | Threaded conversation shown |
| 4 | View emails by entity (lead/account/contact) | Entity-linked emails shown |

---

### TC-EMAIL-005: Email Templates
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to email templates section | Template list shown |
| 2 | Create new template: Name="Welcome", Subject="Welcome {{name}}", Body with variables | Template created |
| 3 | Preview template with sample data | Rendered preview shown |
| 4 | Edit template | Changes saved |
| 5 | Delete template | Template removed |
| 6 | Search templates | Matching templates shown |

---

### TC-EMAIL-006: Email Scheduling
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View scheduled emails | List of scheduled sends |
| 2 | Cancel a scheduled email | Email cancelled |

---

### TC-EMAIL-007: Email Tracking & Analytics
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Send email with tracking enabled | Email sent with tracking pixel |
| 2 | View tracking events for a sent email | Opens, clicks listed |
| 3 | View email analytics dashboard | Metrics: sent, opened, clicked, open rate, click rate |

---

## 9. TC-CASE: Case / Support Management

### TC-CASE-001: Cases List Page
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Navigate to `/cases` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Cases list with columns: Case Number, Subject, Status, Priority, Account, Contact, Created |
| 2 | Filter by status (OPEN, IN_PROGRESS, RESOLVED, CLOSED) | Filtered cases |
| 3 | Filter by priority (LOW, MEDIUM, HIGH, URGENT) | Filtered cases |
| 4 | Sort by created date | Sorted results |

---

### TC-CASE-002: Create Case
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **+ New Case** | Case form opens |
| 2 | Fill: Subject="Login issue", Description="User cannot login", Priority=HIGH, Account=Acme Corp | Fields populated |
| 3 | Click **Save** | Case created with auto-generated case number |
| 4 | Verify in list | Case visible with case number |

---

### TC-CASE-003: View & Edit Case
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click case row | Case detail page |
| 2 | Verify: case number, subject, description, status, priority, assignee, SLA | All fields correct |
| 3 | Edit case description | Updated successfully |

---

### TC-CASE-004: Case Lifecycle (Resolve/Close/Escalate)
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Resolve** on open case | Resolution notes prompt |
| 2 | Enter resolution notes, confirm | Status changes to RESOLVED |
| 3 | Click **Close** on resolved case | Status changes to CLOSED |
| 4 | On a separate case, click **Escalate** (ADMIN/MANAGER) | Case escalated, priority may increase |

---

### TC-CASE-005: CSAT (Customer Satisfaction)
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On resolved/closed case, submit CSAT rating (1-5) | Rating saved |
| 2 | View CSAT score on case | Score displayed |

---

### TC-CASE-006: Case Analytics
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View case analytics | Metrics: open/resolved/closed counts, avg resolution time, SLA compliance |

---

### TC-CASE-007: Omni-Channel Routing — Queues
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | ADMIN/MANAGER role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create a routing queue: Name="Tier 1 Support" | Queue created |
| 2 | View queues list | Queue visible |
| 3 | Edit queue settings | Updated |
| 4 | Delete queue | Removed |

---

### TC-CASE-008: Omni-Channel Routing — Agent Presence & Skills
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Set agent presence (AVAILABLE, BUSY, OFFLINE) | Presence updated |
| 2 | View my presence | Current status shown |
| 3 | Add agent skill (ADMIN/MANAGER) | Skill added |
| 4 | View agent skills | Skills listed |

---

### TC-CASE-009: Omni-Channel Routing — Route & Work Items
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Route a case to an agent (ADMIN/MANAGER) | Work item created |
| 2 | Agent accepts work item | Status changes to IN_PROGRESS |
| 3 | Agent completes work item | Status changes to COMPLETED |
| 4 | Agent declines work item | Re-routed to another agent |

---

## 10. TC-CAMP: Campaign Management

### TC-CAMP-001: Campaign List Page
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Navigate to `/campaigns` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Campaign list with columns: Name, Type, Status, Start Date, End Date, Budget |
| 2 | Filter by status (DRAFT, ACTIVE, COMPLETED) | Filtered results |
| 3 | Filter by type | Type-specific campaigns |

---

### TC-CAMP-002: Create Campaign
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **+ New Campaign** | Campaign form opens |
| 2 | Fill: Name="Q2 Email Blast", Type=EMAIL, Status=DRAFT, Start/End dates, Budget=10000, Expected Revenue=50000 | Fields populated |
| 3 | Click **Save** | Campaign created |

---

### TC-CAMP-003: Edit & Delete Campaign
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click campaign to view detail | Detail page loads |
| 2 | Edit campaign name and budget | Changes saved |
| 3 | Delete campaign | Removed from list |

---

### TC-CAMP-004: Campaign Members
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | On campaign detail, add members (leads/contacts) | Members added |
| 2 | View members list | Members with status shown |
| 3 | Update member status (e.g., SENT → RESPONDED) | Status updated |

---

### TC-CAMP-005: Campaign ROI
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View campaign ROI | ROI calculation: (revenue - cost) / cost, conversion rates |

---

## 11. TC-WF: Workflow & Automation

### TC-WF-001: Workflow Rules List
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Navigate to `/workflows` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Workflow rules list with: Name, Entity Type, Trigger, Status (Enabled/Disabled) |

---

### TC-WF-002: Create Workflow Rule
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | ADMIN/MANAGER role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **+ New Workflow** | Workflow creation form |
| 2 | Fill: Name="Auto-Assign Hot Leads", Entity=LEAD, Trigger=ON_CREATE, Conditions, Actions | Rule configured |
| 3 | Click **Save** | Workflow rule created |

---

### TC-WF-003: Enable/Disable Workflow
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Enable** on a disabled rule | Rule activated |
| 2 | Click **Disable** on an enabled rule | Rule deactivated |
| 3 | Verify status indicator changes | Status toggle reflects state |

---

### TC-WF-004: Edit & Delete Workflow
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Edit workflow conditions/actions | Changes saved |
| 2 | Delete workflow | Removed from list |

---

### TC-WF-005: Workflow Execution Logs
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View execution logs | List of past workflow executions |
| 2 | View logs for a specific rule | Filtered executions for that rule |
| 3 | Verify execution details: trigger entity, result, timestamp | Details correct |

---

### TC-WF-006: Automation Templates
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | Navigate to `/automation` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View workflow templates | Pre-built templates listed |
| 2 | Filter by entity type | Entity-specific templates |

---

### TC-WF-007: AI Automation Suggestions
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View AI automation suggestions | Suggested workflows based on data patterns |
| 2 | View pending suggestions | Unapproved suggestions listed |
| 3 | Accept a suggestion (ADMIN/MANAGER) | Workflow created from suggestion |
| 4 | Dismiss a suggestion | Suggestion removed from pending |
| 5 | Generate new AI suggestions | New suggestions generated |

---

### TC-WF-008: Proposals
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Opportunity exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create proposal for an opportunity | Proposal created with DRAFT status |
| 2 | View proposals list | All proposals shown |
| 3 | View proposals by opportunity | Filtered to opportunity |
| 4 | Send proposal (ADMIN/MANAGER) | Status changes to SENT |
| 5 | Accept/reject proposal | Status updated |
| 6 | Delete proposal | Removed |

---

### TC-WF-009: Contracts
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create contract for an opportunity | Contract created as DRAFT |
| 2 | View contracts list | All contracts shown |
| 3 | Send contract (ADMIN/MANAGER) | Status = SENT |
| 4 | Sign contract | Status = SIGNED |
| 5 | Execute contract (ADMIN/MANAGER) | Status = EXECUTED |
| 6 | Cancel contract (ADMIN/MANAGER) | Status = CANCELLED |
| 7 | Delete contract (ADMIN/MANAGER) | Removed |

---

## 12. TC-COLLAB: Collaboration

### TC-COLLAB-001: Collaboration Page
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Navigate to `/collaboration` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Collaboration hub with Chat, Mentions, Approvals, Comments sections |

---

### TC-COLLAB-002: Deal Chat
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Opportunity exists |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Open deal chat for an opportunity | Chat messages loaded |
| 2 | Send a new message | Message appears in chat |
| 3 | Edit a message | Message updated, "edited" indicator shown |
| 4 | Delete a message | Message removed |
| 5 | Reply to a message (thread) | Reply attached to parent message |

---

### TC-COLLAB-003: Mentions
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View my mentions | List of mentions |
| 2 | View unread mention count | Badge/count shown |
| 3 | View unread mentions | Unread items listed |
| 4 | Mark mention as read | Unread count decreases |
| 5 | Mark all mentions as read | Count goes to zero |

---

### TC-COLLAB-004: Deal Approvals
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create a deal approval: Type=DISCOUNT, Approver, Title, Description | Approval created as PENDING |
| 2 | View pending approvals | Open approval requests shown |
| 3 | View approvals by deal | Deal-specific approvals |
| 4 | Approve a request with comment | Status = APPROVED |
| 5 | Reject a request with comment | Status = REJECTED |
| 6 | View my requests | Requests I created shown |
| 7 | View pending count | Badge/count accurate |

---

### TC-COLLAB-005: Record Comments
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Add comment to an opportunity or account | Comment saved |
| 2 | View comments by record type and ID | Comments listed |
| 3 | Edit a comment | Updated text shown |
| 4 | Pin/unpin a comment | Pin indicator toggled |
| 5 | Delete a comment | Comment removed |

---

## 13. TC-COMM: Communications

### TC-COMM-001: Communications Page
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Navigate to `/communications` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Tabs/sections for SMS, WhatsApp, Calls, Unified Inbox |

---

### TC-COMM-002: Send SMS
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Send SMS** | SMS compose form |
| 2 | Fill: To number, Message body | Fields populated |
| 3 | Send | SMS queued/sent, appears in SMS list |
| 4 | View SMS list | All sent SMS shown with status |
| 5 | View SMS by phone number | Conversation view |

---

### TC-COMM-003: Send WhatsApp
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Click **Send WhatsApp** | WhatsApp compose form |
| 2 | Fill: To number, Template, Message | Fields populated |
| 3 | Send | Message sent |
| 4 | View WhatsApp messages | Messages listed |
| 5 | Mark message as read | Read status updated |

---

### TC-COMM-004: Calls
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Initiate a call | Call initiated, record created |
| 2 | Update call notes | Notes saved |
| 3 | End call | Duration recorded |
| 4 | View calls list | All calls with status, duration |
| 5 | View calls by phone number | Number-specific call log |

---

### TC-COMM-005: Unified Inbox
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View unified inbox | All channels (SMS, WhatsApp, Calls, Email) in one view |
| 2 | Filter by channel | Channel-specific messages |
| 3 | Filter by entity (lead, contact, account) | Entity-specific communications |

---

## 14. TC-AI: AI Features & Insights

### TC-AI-001: AI Insights Page
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Navigate to `/ai-insights` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | AI insights dashboard with sections: Lead Scores, Win Probability, Forecasts, Churn |
| 2 | View predictive lead scores | Leads ranked by AI score |
| 3 | View win probability for opportunities | Probability percentages shown |
| 4 | View AI sales forecasts (ADMIN/MANAGER) | Revenue forecast data |
| 5 | View churn predictions (ADMIN/MANAGER) | At-risk accounts identified |

---

### TC-AI-002: AI Lead Scoring
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Request AI lead score for a lead | Score returned (0-100) with explanation |
| 2 | View scoring factors | AI explains why score is high/low |

---

### TC-AI-003: AI Email Drafting
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Request AI email draft: context, recipient, purpose | AI-generated email returned |
| 2 | Request AI email reply given original email | Contextual reply generated |
| 3 | Copy/use generated content | Content usable in email composer |

---

### TC-AI-004: AI Next Best Action
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Request next best action for a lead/opportunity | AI recommendations returned |
| 2 | View recommendation details | Actionable suggestion with explanation |

---

### TC-AI-005: AI Meeting Summary
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit meeting notes/transcript for summarization | AI summary returned |
| 2 | View key points and action items | Structured summary displayed |

---

### TC-AI-006: AI Auto-Lead Extraction
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit content (email/text) for auto-lead extraction | Lead data extracted by AI |
| 2 | View extracted lead with fields pre-filled | Name, email, company, etc. |
| 3 | Approve auto-lead (ADMIN/MANAGER) | Lead created in system |
| 4 | Reject auto-lead | Lead discarded |

---

### TC-AI-007: AI Sentiment Analysis & Transcription
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Submit text for sentiment analysis | Sentiment score: POSITIVE/NEUTRAL/NEGATIVE |
| 2 | Submit audio/text for transcription | Transcribed text returned |

---

### TC-AI-008: AI Onboarding
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View onboarding status | Checklist with completed/pending steps |
| 2 | Complete an onboarding step | Progress updated |
| 3 | Request AI guidance for a step | Contextual help provided |
| 4 | Reset onboarding (ADMIN) | All steps reset to pending |

---

### TC-AI-009: AI Configuration (Zero-Config)
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Navigate to `/ai-config` or `/zero-config` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | AI agent configuration interface |
| 2 | Send a natural language instruction (e.g., "Create a field called Source on leads") | AI parses instruction and proposes change |
| 3 | View AI audit logs | History of AI actions/proposals |
| 4 | Confirm a pending AI change | Change applied to CRM |
| 5 | Reject a pending AI change | Change discarded |
| 6 | View conversation sessions | Session history listed |

---

## 15. TC-SEC: Security & Administration

### TC-SEC-001: Security Page Load
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |
| **Preconditions** | Navigate to `/security`, ADMIN role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Tabs: Roles, Permissions, Field Security, SSO, MFA, Audit Logs, Users |

---

### TC-SEC-002: Role Management
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View roles list | ADMIN, MANAGER, USER roles shown |
| 2 | Create new role: Name="SALES_REP", Description="Sales representative" | Role created |
| 3 | Delete a custom role | Role removed |
| 4 | Assign role to user (from Auth API) | User's roles updated |

---

### TC-SEC-003: Permission Management
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View permissions list | All permissions shown |
| 2 | Create permission: Name="lead:export", Description="Export leads" | Permission created |
| 3 | Edit permission description | Updated |
| 4 | Delete permission | Removed |

---

### TC-SEC-004: Field Security
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View field security rules | Rules listed |
| 2 | Create rule: Entity=LEAD, Field=phone, Role=USER, Access=READ_ONLY | Rule created |
| 3 | Edit rule | Updated |
| 4 | Delete rule | Removed |

---

### TC-SEC-005: SSO Configuration
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View SSO providers | List (may be empty) |
| 2 | Create SSO provider: Name, Provider Type, Client ID, Secret | Provider created |
| 3 | Edit provider details | Updated |
| 4 | Delete provider | Removed |

---

### TC-SEC-006: MFA Configuration
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View MFA configurations | MFA settings listed |
| 2 | Setup MFA: Type=TOTP | MFA enabled |
| 3 | Remove MFA config | MFA disabled |

---

### TC-SEC-007: Audit Logs
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View audit logs | Paginated list of audit events |
| 2 | Verify events include: timestamp, user, action, entity, details | All metadata present |
| 3 | Filter by user | User-specific logs |
| 4 | Navigate pages | Pagination works |

---

### TC-SEC-008: User Management
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View users list | All tenant users shown |
| 2 | Verify user details: email, name, roles, enabled status | Data correct |
| 3 | Assign ADMIN role to a user | Role added |

---

### TC-SEC-009: Plan Management
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View current plan | Plan name and limits shown |
| 2 | Upgrade plan (ADMIN) | Plan upgraded, new limits applied |

---

## 16. TC-INTEG: Integrations

### TC-INTEG-001: Integrations Page
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Navigate to `/integrations`, ADMIN role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Sections: Integrations, Connectors, APIs, Webhooks, Data Syncs, Auth Configs |

---

### TC-INTEG-002: Integration Management
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View integrations list | Configured integrations shown |
| 2 | Create integration (ADMIN) | Integration configured |
| 3 | Edit integration | Updated |
| 4 | Delete integration | Removed |

---

### TC-INTEG-003: Connector Management
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View connectors | Available connectors listed |
| 2 | Create connector (ADMIN): Name, Type, Configuration | Connector created |
| 3 | Edit connector | Updated |
| 4 | Delete connector | Removed |

---

### TC-INTEG-004: API Endpoint Management
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View API endpoints | Endpoints listed |
| 2 | Create API endpoint (ADMIN) | Created |
| 3 | Edit endpoint | Updated |
| 4 | Delete endpoint | Removed |

---

### TC-INTEG-005: Webhook Management
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View webhooks | Configured webhooks listed |
| 2 | Create webhook (ADMIN): URL, Events, Secret | Webhook created |
| 3 | Edit webhook | Updated |
| 4 | Delete webhook | Removed |

---

### TC-INTEG-006: Data Sync
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View data syncs | Sync configurations shown |
| 2 | Create sync (ADMIN): Source, Target, Mapping | Sync created |
| 3 | Edit sync | Updated |
| 4 | Delete sync | Removed |

---

### TC-INTEG-007: Integration Health & Errors
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View integration health | Health status for all integrations |
| 2 | View error logs | Recent integration errors listed |
| 3 | Filter errors by severity level | Filtered results |

---

## 17. TC-DEV: Developer Portal

### TC-DEV-001: Developer Portal Page
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Navigate to `/developer`, ADMIN role |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Sections: API Keys, Webhooks, Marketplace, Widgets, Custom Apps |

---

### TC-DEV-002: API Key Management
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View API keys | Current keys listed |
| 2 | Generate new API key (ADMIN) | Key generated, displayed once |
| 3 | Revoke API key | Key deactivated |
| 4 | Delete API key | Key removed |

---

### TC-DEV-003: Webhook Deliveries
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View webhook delivery logs | Recent deliveries with status |
| 2 | View deliveries for specific webhook | Filtered logs |
| 3 | Test webhook (ADMIN) | Test payload sent, delivery logged |

---

### TC-DEV-004: Marketplace
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Browse marketplace | Available plugins listed |
| 2 | Filter by category | Category-specific plugins |
| 3 | Install a plugin (ADMIN) | Plugin installed |
| 4 | Uninstall a plugin | Plugin removed |
| 5 | View installed plugins | List of installed plugins |
| 6 | Create plugin (ADMIN) | Plugin created in marketplace |
| 7 | Publish plugin (ADMIN) | Plugin published |

---

### TC-DEV-005: Embeddable Widgets
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View widgets | Widgets listed |
| 2 | Create widget (ADMIN): Name, Type, Config | Widget created |
| 3 | Edit widget | Updated |
| 4 | Delete widget | Removed |

---

### TC-DEV-006: Custom Apps
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View custom apps | Apps listed |
| 2 | Create app (ADMIN): Name, Description, Config | App created |
| 3 | Edit app | Updated |
| 4 | Publish app (ADMIN) | App published |
| 5 | Delete app | Removed |

---

## 18. TC-REPORT: Reports & Analytics

### TC-REPORT-001: Reports Page
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Navigate to `/reports` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Reports dashboard with multiple report types |
| 2 | View lead analytics | Lead source, status, conversion charts |
| 3 | View account analytics | Account type, territory, health charts |
| 4 | View contact analytics | Contact segment, lifecycle charts |
| 5 | View opportunity analytics | Pipeline, revenue, win/loss charts |
| 6 | View activity analytics | Activity type, completion charts |
| 7 | View case analytics | Case status, priority, resolution time charts |
| 8 | View email analytics | Email sent, open, click rate charts |

---

### TC-REPORT-002: AI-Powered Reports
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | View AI report insights | AI-generated insights about trends and anomalies |
| 2 | Filter by insight type | Type-specific insights |
| 3 | View AI sales insights | Sales-specific AI insights |

---

## 19. TC-MISC: Settings, Pricing & Zero-Config

### TC-MISC-001: Settings Page
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | Navigate to `/settings` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Settings interface with configuration options |
| 2 | Modify available settings | Settings saved |

---

### TC-MISC-002: Pricing Page
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | Navigate to `/pricing` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Pricing tiers displayed (FREE, STARTER, PROFESSIONAL, ENTERPRISE) |
| 2 | View feature comparison | Features listed per plan |

---

### TC-MISC-003: Landing Page
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Navigate to `/landing` (when logged out) | Marketing landing page with product info |
| 2 | Click **Sign Up** / **Get Started** | Navigates to `/auth/register` |
| 3 | Click **Login** | Navigates to `/auth/login` |

---

### TC-MISC-004: Object Manager
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |
| **Preconditions** | Navigate to `/object-manager` |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Page loads | Custom object/field management interface |
| 2 | View existing CRM objects | Standard objects listed |
| 3 | Create custom field (if supported) | Field added |

---

## 20. TC-CROSS: Cross-Module & E2E Scenarios

### TC-CROSS-001: Lead-to-Opportunity-to-Close (Full Sales Cycle)
| Field | Value |
|-------|-------|
| **Priority** | P0 — Critical |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create Account: "E2E Corp" | Account created |
| 2 | Create Lead: "John E2E", Source=WEBSITE, Company=E2E Corp | Lead created |
| 3 | Add note to lead: "High interest in enterprise plan" | Note saved |
| 4 | Assign lead to self | Lead assigned |
| 5 | Convert lead to opportunity: "E2E Corp Deal", Amount=100000, Stage=QUALIFICATION | Opportunity created, lead converted |
| 6 | Add product to opportunity: "Enterprise License" x 10 @ $10000 | Product added |
| 7 | Add competitor: "Competitor X" | Competitor added |
| 8 | Progress stage: QUALIFICATION → PROPOSAL → NEGOTIATION | Stage updated at each step |
| 9 | Create activity: MEETING with "E2E Corp" | Activity linked to opportunity |
| 10 | Complete the activity | Marked as complete |
| 11 | Move opportunity to CLOSED_WON | Deal won |
| 12 | Verify dashboard reflects: pipeline change, revenue added, win | Dashboard updated |
| 13 | View activity timeline on opportunity | Full history shown |

---

### TC-CROSS-002: Support Case Lifecycle with Communication
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create Account: "Support Corp" | Account created |
| 2 | Create Contact: "Support User" for account | Contact created |
| 3 | Log communication on contact: CALL, "Reported billing issue" | Communication logged |
| 4 | Create Case: Subject="Billing discrepancy", Priority=HIGH, Account=Support Corp | Case created |
| 5 | View case detail | All fields correct |
| 6 | Escalate case | Escalated |
| 7 | Resolve case with notes: "Billing corrected, credit issued" | Resolved |
| 8 | Submit CSAT: Rating=5 | CSAT recorded |
| 9 | Close case | Closed |
| 10 | Verify case analytics updated | Resolution time, CSAT reflected |

---

### TC-CROSS-003: Campaign with Lead Tracking
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create Campaign: "Spring Webinar", Type=EVENT | Campaign created |
| 2 | Create Lead: "Campaign Lead", Source=WEBINAR | Lead created |
| 3 | Add lead as campaign member | Member added |
| 4 | Update member status to RESPONDED | Status updated |
| 5 | Convert lead to opportunity | Opportunity tracks campaign source |
| 6 | View campaign ROI | Conversion tracked |

---

### TC-CROSS-004: Collaboration on a Deal
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create opportunity: "Collab Deal" | Created |
| 2 | Add collaborator to deal | Collaborator added |
| 3 | Send chat message in deal chat | Message appears |
| 4 | Add comment with @mention | Comment saved, mention created |
| 5 | View unread mentions | Mentioned user sees notification |
| 6 | Create deal approval: DISCOUNT approval | Approval pending |
| 7 | Approve the discount request | Approved |
| 8 | Verify activity stream reflects all actions | Events logged |

---

### TC-CROSS-005: Workflow Automation E2E
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create workflow rule: When lead created with source=WEBFORM → auto-assign to user X | Rule created |
| 2 | Enable the rule | Rule active |
| 3 | Submit a lead via web form (public endpoint) | Lead created |
| 4 | Verify lead auto-assigned to user X | Assignment applied |
| 5 | View workflow execution log | Execution recorded |

---

### TC-CROSS-006: Email with Tracking E2E
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create email template with merge fields | Template created |
| 2 | Preview template with sample data | Rendered correctly |
| 3 | Compose email using template for a contact | Email populated |
| 4 | Send email with tracking | Email sent |
| 5 | Simulate open (tracking pixel hit) | Open event logged |
| 6 | View tracking events | Open tracked |
| 7 | View email analytics updated | Metrics reflect new email |

---

### TC-CROSS-007: AI Insights Validation
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create several leads with varying engagement | Leads created |
| 2 | Request AI lead scores | Scores returned, high-engagement leads score higher |
| 3 | Create opportunities at different stages | Opportunities created |
| 4 | View AI win probabilities | Probability correlates with stage progression |
| 5 | Request AI email draft for a lead | Contextual email generated |
| 6 | Request AI next best action | Relevant recommendation |

---

### TC-CROSS-008: Multi-Entity Activity Timeline
| Field | Value |
|-------|-------|
| **Priority** | P2 — Medium |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Create account, contact, lead, opportunity (all linked) | Entities created |
| 2 | Create activities linked to each entity | Activities created |
| 3 | View activity stream | All activities across entities shown |
| 4 | View entity-specific timeline on each detail page | Only relevant activities |
| 5 | View upcoming activities for next 7 days | Upcoming shown |
| 6 | View overdue activities | Past-due items shown |

---

### TC-CROSS-009: Bulk Operations
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |
| **Preconditions** | ADMIN/MANAGER role, multiple records exist |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Select multiple leads | Checkbox selection works |
| 2 | Bulk update status to CONTACTED | All selected leads updated |
| 3 | Select multiple accounts | Checkbox selection works |
| 4 | Bulk delete selected accounts | Confirmation, then deleted |
| 5 | Import 50+ leads from CSV | All imported successfully |
| 6 | Export all leads | CSV contains all leads |

---

### TC-CROSS-010: Session & Token Handling
| Field | Value |
|-------|-------|
| **Priority** | P1 — High |

| Step | Action | Expected Result |
|------|--------|-----------------|
| 1 | Login and note access token expiry | Token has expiration time |
| 2 | Use app normally for extended period | Refresh token auto-renews session |
| 3 | Navigate multiple pages rapidly | No auth errors (token attached to all requests) |
| 4 | Open app in new browser tab | Same session maintained |
| 5 | Clear cookies/storage and refresh | Redirected to login |

---

## Appendix A: Test Data Summary

| Entity | Test Record | Key Fields |
|--------|------------|------------|
| User (ADMIN) | demo@crm.com | Password: Demo@2026!, Tenant: default, Roles: ADMIN+MANAGER+USER |
| Account | Acme Corp | Type: CUSTOMER, Industry: Technology |
| Lead | John Doe | Email: john@example.com, Source: WEBSITE |
| Contact | Jane Smith | Email: jane@acme.com, Account: Acme Corp |
| Opportunity | Acme Enterprise Deal | Amount: 500000, Stage: PROSPECTING |
| Case | Login Issue | Priority: HIGH, Status: OPEN |
| Campaign | Q2 Email Blast | Type: EMAIL, Budget: 10000 |
| Activity | Demo Call with Acme | Type: MEETING |

---

## Appendix B: Priority Legend

| Priority | Description | Test Phase |
|----------|-------------|------------|
| P0 — Critical | Core functionality, must work for demo | Smoke Test |
| P1 — High | Important features, key user flows | Regression |
| P2 — Medium | Secondary features, edge cases | Full Test |

---

## Appendix C: Browser Compatibility Matrix

| Browser | Version | Status |
|---------|---------|--------|
| Chrome | 120+ | Primary |
| Edge | 120+ | Primary |
| Firefox | 120+ | Secondary |
| Safari | 17+ | Secondary |
| Mobile Chrome | Latest | Responsive check |
| Mobile Safari | Latest | Responsive check |
