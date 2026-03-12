# Zero-Config AI CRM Platform — User Manual

### For Real Estate Professionals

> **Version**: 1.0  
> **Last Updated**: March 2026  
> **App URL**: https://your-crm-domain.com (or http://localhost:3000 for demo)

---

## Table of Contents

1. [Getting Started](#1-getting-started)
2. [Dashboard — Your Command Center](#2-dashboard--your-command-center)
3. [Managing Leads](#3-managing-leads)
4. [Managing Accounts (Clients & Agencies)](#4-managing-accounts)
5. [Managing Contacts](#5-managing-contacts)
6. [Managing Opportunities (Deals)](#6-managing-opportunities)
7. [Activities — Tasks, Calls & Meetings](#7-activities--tasks-calls--meetings)
8. [Email — Send, Track & Automate](#8-email--send-track--automate)
9. [Cases — Client Support Tickets](#9-cases--client-support-tickets)
10. [Campaigns — Marketing & Advertising](#10-campaigns--marketing--advertising)
11. [Reports & Analytics](#11-reports--analytics)
12. [Workflows — Automate Your Business](#12-workflows--automate-your-business)
13. [AI Configuration — Build Your CRM With Words](#13-ai-configuration--build-your-crm-with-words)
14. [AI Insights — Predictive Intelligence](#14-ai-insights--predictive-intelligence)
15. [Zero-Config — Instant Setup](#15-zero-config--instant-setup)
16. [Communications Hub](#16-communications-hub)
17. [Collaboration & Team Features](#17-collaboration--team-features)
18. [Smart Automation](#18-smart-automation)
19. [Object Manager — Customize Everything](#19-object-manager--customize-everything)
20. [Security & Access Control](#20-security--access-control)
21. [Integrations — Connect Your Tools](#21-integrations--connect-your-tools)
22. [Developer Portal](#22-developer-portal)
23. [Settings & Preferences](#23-settings--preferences)
24. [Real Estate Quick-Start Guide](#24-real-estate-quick-start-guide)
25. [FAQ & Troubleshooting](#25-faq--troubleshooting)

---

## 1. Getting Started

### 1.1 Creating Your Account

1. Open your browser and navigate to the CRM URL
2. Click **"Register"** on the login page
3. Fill in your details:
   - **First Name** and **Last Name**
   - **Email Address** (this will be your login)
   - **Password** (must be strong — use letters, numbers, and symbols)
   - **Confirm Password**
   - **Tenant ID** — your company workspace (provided by your admin, or use `default`)
4. Click **Register**
5. You will be redirected to the dashboard

### 1.2 Logging In

1. Navigate to the CRM URL
2. Enter your **Email** and **Password**
3. Enter your **Tenant ID**
4. Click **Sign In**
5. You will land on the **Dashboard**

### 1.3 Navigating the App

The left sidebar is your main navigation. It contains:

| Menu Item | What It Does |
|-----------|-------------|
| **Dashboard** | Overview of your sales pipeline, revenue, and KPIs |
| **Leads** | Manage incoming buyer/seller/renter inquiries |
| **Accounts** | Manage client companies, agencies, and developers |
| **Contacts** | Manage individual people — buyers, sellers, agents |
| **Opportunities** | Track deals — property sales, listings, closings |
| **Activities** | Tasks, calls, meetings, and follow-ups |
| **Email** | Send, receive, and track emails |
| **Cases** | Client support issues and complaints |
| **Campaigns** | Marketing campaigns — ads, open houses, mailers |
| **Reports** | Analytics, charts, and business intelligence |
| **Workflows** | Automation rules that save you time |
| **Security** | User roles, permissions, and audit logs |
| **Integrations** | Connect to other tools (Zapier, Slack, etc.) |
| **AI Config** | Configure your CRM using plain English |
| **AI Insights** | Predictive lead scoring, forecasting, and recommendations |
| **Zero Config** | Auto-setup wizards and data import tools |
| **Communications** | SMS, WhatsApp, and phone call hub |
| **Collaboration** | Team chat, mentions, and deal approvals |
| **Automation** | Smart workflow templates and proposals |
| **Developer** | API keys, webhooks, and custom apps |
| **Object Manager** | Browse and manage all custom objects, fields, and pipelines |
| **Settings** | Your profile, preferences, and notification settings |

> **Tip**: On mobile devices, tap the hamburger menu (☰) at the top left to open the sidebar.

---

## 2. Dashboard — Your Command Center

The Dashboard gives you a real-time overview of your business at a glance.

### What You'll See

| Widget | Description |
|--------|-------------|
| **Weighted Pipeline** | Total value of all active deals, weighted by probability |
| **Average Deal Size** | Average value across your opportunities |
| **Won Deals** | Number of deals successfully closed |
| **Lost Deals** | Number of deals lost |
| **Total Revenue** | Cumulative revenue from closed deals |
| **Pipeline by Stage** | Bar chart showing deal value at each stage |
| **Revenue by Lead Source** | Donut chart — where your best deals come from |
| **Forecast Categories** | Pipeline vs. Best Case vs. Commit vs. Closed revenue |
| **Stage Conversion Rates** | How efficiently deals move from one stage to the next |

### Dashboard Actions

- **Auto-Refresh**: Toggle the refresh button to keep data updated automatically
- **Drag & Rearrange**: Click and drag widget headers to rearrange your dashboard layout
- **Deal Alerts**: The system highlights overdue deals, deals closing soon, and stale deals

> **Real Estate Example**: If you see 5 deals in "Negotiation" worth $2.5M and your average conversion from Negotiation → Closed Won is 60%, your expected revenue is $1.5M.

---

## 3. Managing Leads

Leads are your incoming inquiries — potential buyers, sellers, renters, or investors who have expressed interest.

### 3.1 Viewing Leads

1. Click **Leads** in the sidebar
2. You'll see a table with all your leads
3. Use the **search bar** at the top to find leads by name, email, or company
4. Each row shows: Name, Email, Phone, Company, Status, Source, Lead Score

### 3.2 Creating a New Lead

1. Click the **"+ New Lead"** button (top right)
2. Fill in the lead form:
   - **First Name** & **Last Name** (required)
   - **Email** — buyer's email address
   - **Phone** — contact number
   - **Company** — their company (if applicable)
   - **Title** — e.g., "Property Buyer", "Investor"
   - **Source** — how they found you:
     - `WEB` — website inquiry
     - `PHONE` — phone call
     - `EMAIL` — email inquiry
     - `REFERRAL` — referred by someone
     - `SOCIAL_MEDIA` — Facebook, Instagram, etc.
     - `TRADE_SHOW` — property expo or open house
     - `OTHER`
   - **Description** — notes about what they're looking for
3. Click **Save**

### 3.3 Lead Statuses

| Status | Meaning |
|--------|---------|
| **NEW** | Just came in — hasn't been contacted yet |
| **CONTACTED** | You've reached out to them |
| **QUALIFIED** | Confirmed as a genuine buyer/seller with budget |
| **UNQUALIFIED** | Not a fit — wrong budget, timeline, or area |
| **CONVERTED** | Turned into an opportunity (active deal) |
| **LOST** | Lead went cold or chose a competitor |

### 3.4 Converting a Lead to a Deal

When a lead is ready to become an active deal:

1. Open the lead's detail page (click on their name)
2. Click **"Convert to Opportunity"**
3. Fill in the conversion form:
   - **Opportunity Name** — e.g., "Smith Family — 123 Oak St Purchase"
   - **Amount** — expected deal value
   - **Stage** — usually starts at `PROSPECTING`
   - **Close Date** — expected closing date
   - Optionally create a linked **Account** and **Contact**
4. Click **Convert**

The lead status changes to `CONVERTED` and a new Opportunity is created.

### 3.5 Lead Scoring

Every lead gets an AI-generated score from 0 to 100. Higher scores mean higher likelihood of conversion. The score is based on:
- Engagement history (emails opened, calls answered)
- Demographics and budget fit
- Behavioral patterns compared to past successful deals

> **Real Estate Tip**: Focus your energy on leads with scores above 70 — they're most likely to close.

---

## 4. Managing Accounts

Accounts represent organizations — real estate agencies, property developers, investment firms, or corporate clients.

### 4.1 Creating an Account

1. Click **Accounts** in the sidebar
2. Click **"+ New Account"**
3. Fill in:
   - **Name** (required) — e.g., "Sunrise Properties LLC"
   - **Industry** — e.g., "Real Estate", "Construction", "Finance"
   - **Website** — company website
   - **Phone** — main office number
   - **Type**: `PROSPECT`, `CUSTOMER`, `PARTNER`, or `VENDOR`
   - **Annual Revenue** — estimated company revenue
   - **Number of Employees**
   - **Billing Address** and **Shipping Address**
   - **Description** — notes about the company
4. Click **Save**

### 4.2 Account Types for Real Estate

| Type | Use It For |
|------|-----------|
| **PROSPECT** | A developer or agency you're pitching to |
| **CUSTOMER** | An active client — you're managing their properties |
| **PARTNER** | A co-listing agent, mortgage broker, or title company |
| **VENDOR** | A contractor, inspector, or staging company |

### 4.3 Account Detail Page

Click on any account to see:
- **Associated Contacts** — all people linked to this company
- **Associated Opportunities** — all deals with this company
- **Activity Timeline** — history of calls, meetings, and emails
- **Notes** — team notes about the relationship

---

## 5. Managing Contacts

Contacts are individual people — buyers, sellers, agents, mortgage brokers, title officers, inspectors, etc.

### 5.1 Creating a Contact

1. Click **Contacts** in the sidebar
2. Click **"+ New Contact"**
3. Fill in:
   - **First Name** & **Last Name** (required)
   - **Email** and **Phone**
   - **Mobile Phone**
   - **Title** — e.g., "Buyer", "Seller", "Mortgage Officer"
   - **Department** — e.g., "Lending", "Title", "Sales"
   - **Account** — link to their company
   - **Mailing Address**
   - **Lifecycle Stage**: `SUBSCRIBER` → `LEAD` → `MQL` → `SQL` → `OPPORTUNITY` → `CUSTOMER` → `EVANGELIST`
   - **Segment**: `Enterprise`, `Mid-Market`, `SMB`, `Startup`, `Individual`
   - **Lead Source** — how they found you
4. Click **Save**

### 5.2 Contact Filters

Use the filter bar above the table to narrow down contacts:
- **By Lifecycle Stage** — see only customers, or only leads
- **By Segment** — filter by client size
- **By Lead Source** — see contacts from a specific channel

### 5.3 Contact Detail Page

Click on a contact to see:
- **Communication History** — all emails, calls, and meetings
- **Linked Account** — the company they belong to
- **Activity Timeline** — chronological history of all interactions
- **Tags** — custom labels for organization (e.g., "VIP", "First-Time Buyer", "Investor")

---

## 6. Managing Opportunities

Opportunities are your active deals — property sales, purchases, lease agreements, and listings.

### 6.1 Two Views

**Kanban Board** (default) — visual pipeline with drag-and-drop cards:
- Each column represents a deal stage
- Drag cards between columns to update the stage
- Cards show: Deal Name, Amount, Close Date, Owner

**List View** — traditional table with sorting and filtering:
- Toggle using the view switch button at the top right

### 6.2 Deal Stages

| Stage | What It Means | Probability |
|-------|--------------|-------------|
| **PROSPECTING** | Initial interest — exploring options | 10% |
| **QUALIFICATION** | Confirmed budget, timeline, and needs | 20% |
| **NEEDS_ANALYSIS** | Property search, showings, market analysis | 40% |
| **PROPOSAL** | Offer submitted on a property | 60% |
| **NEGOTIATION** | Back and forth on price, terms, inspections | 75% |
| **CLOSED_WON** | Deal closed — property sold/purchased! | 100% |
| **CLOSED_LOST** | Deal fell through — lost to competitor or withdrawn | 0% |

### 6.3 Creating an Opportunity

1. Click **Opportunities** in the sidebar
2. Click **"+ New Opportunity"**
3. Fill in:
   - **Name** (required) — e.g., "Johnson Family — 456 Elm St Purchase"
   - **Account** — the client's company (if applicable)
   - **Amount** — deal value (e.g., `450000` for a $450K sale)
   - **Stage** — where the deal currently stands
   - **Close Date** — expected closing date
   - **Probability** — likelihood of closing (auto-set based on stage)
   - **Lead Source** — how this deal originated
   - **Next Step** — e.g., "Schedule showing on Tuesday"
   - **Description** — property details, client notes, etc.
4. Click **Save**

### 6.4 Opportunity Detail Page

Click any deal to access:
- **Stage History** — see when the deal moved through each stage
- **Related Products** — add line items (e.g., listing fee, commission, closing costs)
- **Competitors** — track competing agents or offers
- **Collaborators** — add team members working on this deal
- **Activity Timeline** — every call, email, and meeting related to this deal
- **Notes & Comments** — team discussion about the deal
- **Win/Loss Reason** — record why deals close or fall through

### 6.5 Pipeline Management Tips for Real Estate

- **Move deals promptly** — when you submit an offer, drag the card from "Needs Analysis" to "Proposal"
- **Update amounts** — property prices change during negotiation; keep the amount current
- **Set realistic close dates** — property transactions typically take 30–60 days from offer
- **Track competitors** — note if there are multiple offers on a property

---

## 7. Activities — Tasks, Calls & Meetings

Activities keep you organized and ensure nothing falls through the cracks.

### 7.1 Activity Types

| Type | Use It For |
|------|-----------|
| **TASK** | Follow-up reminders, document deadlines, inspections |
| **CALL** | Phone calls to clients, agents, lenders |
| **MEETING** | Property showings, listing appointments, closings |
| **EMAIL** | Email follow-ups (logged automatically) |

### 7.2 Creating an Activity

1. Click **Activities** in the sidebar
2. Click **"+ New Activity"**
3. Select the **Type** (Task, Call, Meeting, or Email)
4. Fill in:
   - **Subject** — e.g., "Follow up with buyer about offer"
   - **Due Date** — when it needs to happen
   - **Priority**: `LOW`, `NORMAL`, `HIGH`, `URGENT`
   - **Status**: `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`
   - **Assigned To** — team member responsible
   - **Related To** — link to a lead, contact, or opportunity
   - **Description** — additional details
5. Click **Save**

### 7.3 Activity Status

| Status | Meaning |
|--------|---------|
| **NOT_STARTED** | Scheduled but not yet begun |
| **IN_PROGRESS** | Currently working on it |
| **COMPLETED** | Done — task finished |
| **CANCELLED** | No longer needed |

### 7.4 Call Logging

When you make or receive a call:
1. Create a new **Call** activity
2. After the call, update with:
   - **Duration** — how long the call lasted
   - **Outcome** — what happened (interested, no answer, voicemail, etc.)
   - **Notes** — key points discussed

### 7.5 Recurring Activities

For tasks that repeat (e.g., weekly market update calls):
- Set a **recurring schedule**: daily, weekly, monthly
- The system auto-creates the next activity when you complete the current one

---

## 8. Email — Send, Track & Automate

### 8.1 Connecting Your Email Account

1. Click **Email** in the sidebar
2. Go to the **Accounts** tab
3. Click **"Add Email Account"**
4. Choose your provider:
   - **Gmail** — connects via Google OAuth (secure)
   - **Outlook** — connects via Microsoft OAuth
   - **SMTP** — any email provider (enter host, port, username, password)
5. Follow the authentication prompts
6. Your account appears in the connected accounts list

### 8.2 Sending an Email

1. Go to the **Compose** tab
2. Fill in:
   - **To** — recipient email (search by contact name)
   - **Cc / Bcc** — optional
   - **Subject** — your email subject
   - **Body** — write your message
   - Check **"Track Opens"** to know when they read it
   - Check **"Track Clicks"** to know when they click links
3. Click **Send** (or **Schedule** to send later)

### 8.3 Email Templates

Save time with reusable templates:

1. Go to the **Templates** tab
2. Click **"Create Template"**
3. Enter:
   - **Name** — e.g., "New Listing Alert"
   - **Subject** — e.g., "New Property Just Listed — {{property_address}}"
   - **Body** — your template with placeholders
4. Click **Save**

Use templates when composing emails by clicking **"Use Template"** in the compose view.

**Real Estate Template Ideas**:
- New listing notification
- Open house invitation
- Offer follow-up
- Closing congratulations
- Market update newsletter
- Price reduction alert

### 8.4 Email Tracking & Analytics

Go to the **Analytics** tab to see:
- **Open Rate** — % of emails that were opened
- **Click Rate** — % of links clicked
- **Delivery Rate** — % successfully delivered
- **Bounce Rate** — % that failed to deliver

### 8.5 Scheduled Emails

Schedule emails to send at optimal times:
1. When composing, click **Schedule** instead of Send
2. Pick a date and time
3. The email sends automatically

---

## 9. Cases — Client Support Tickets

Track and resolve client issues, complaints, and service requests.

### 9.1 Creating a Case

1. Click **Cases** in the sidebar
2. Click **"+ New Case"**
3. Fill in:
   - **Subject** — e.g., "Leaky faucet in Unit 3B"
   - **Priority**: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`
   - **Contact** — the client reporting the issue
   - **Account** — the associated property management company
   - **Description** — detailed description of the issue
   - **Due Date** — when it should be resolved
4. Click **Save**

### 9.2 Case Statuses

| Status | Meaning |
|--------|---------|
| **OPEN** | New case — not yet being worked on |
| **IN_PROGRESS** | Actively being resolved |
| **ESCALATED** | Elevated to a senior team member |
| **RESOLVED** | Fix applied — pending client confirmation |
| **CLOSED** | Confirmed resolved — case complete |

### 9.3 Real Estate Case Examples

- Maintenance requests from tenants
- Inspection issue follow-ups
- Contract discrepancy disputes
- Commission payment inquiries
- Closing document corrections

---

## 10. Campaigns — Marketing & Advertising

Plan, execute, and measure your marketing campaigns.

### 10.1 Creating a Campaign

1. Click **Campaigns** in the sidebar
2. Click **"+ New Campaign"**
3. Fill in:
   - **Name** — e.g., "Spring Open House Series 2026"
   - **Type**: `EMAIL`, `SOCIAL`, `WEBINAR`, `EVENT`, `PAID_ADS`, `CONTENT`
   - **Status**: `PLANNED`, `ACTIVE`, `COMPLETED`, `ABORTED`
   - **Start Date** and **End Date**
   - **Budget** — planned spend
   - **Expected Revenue** — what you hope to generate
   - **Description** — campaign details
4. Click **Save**

### 10.2 Campaign Dashboard

The campaigns page shows:
- **Total Budget** across all campaigns
- **Leads Generated** from campaigns
- **Conversions** — leads that became deals
- **Active Campaigns** count
- **Budget vs. Actual** spending chart
- **Lead Generation by Type** — which campaign types bring the most leads

### 10.3 Real Estate Campaign Ideas

| Campaign Type | Example |
|--------------|---------|
| **EMAIL** | Monthly market report to buyers |
| **SOCIAL** | Instagram listing showcase series |
| **EVENT** | Weekend open house with refreshments |
| **PAID_ADS** | Google Ads for "homes for sale in {city}" |
| **CONTENT** | Blog: "10 Tips for First-Time Home Buyers" |
| **WEBINAR** | "Real Estate Investment 101" virtual seminar |

---

## 11. Reports & Analytics

Data-driven insights to grow your business.

### 11.1 Available Report Types

**Win/Loss Analysis Tab**:
- How many deals you won vs. lost
- Your win rate percentage
- Average deal size comparison
- Reasons for lost deals

**Revenue Analytics Tab**:
- Total revenue by property type or service
- Revenue by lead source (Referrals vs. Web vs. Ads)
- Month-over-month revenue trends
- Segment-level revenue breakdown

**Stage Conversion Analytics Tab**:
- How efficiently deals move through your pipeline
- Conversion rate between each stage
- Average days spent in each stage
- Identify bottleneck stages where deals stall

**Pipeline Performance Tab**:
- Overall pipeline health
- Performance by agent/team member
- Segment-based pipeline analysis
- Forecast accuracy

### 11.2 Exporting Reports

- Click the **Export** button on any report
- Choose format: **CSV**, **Excel**, or **PDF**
- Reports can also be scheduled for automatic email delivery

### 11.3 Key Real Estate Metrics to Track

| Metric | Why It Matters |
|--------|---------------|
| **Average Days to Close** | Measures your transaction speed |
| **Lead-to-Close Rate** | How many inquiries become closed deals |
| **Revenue by Source** | Shows which marketing channels are most profitable |
| **Pipeline Value** | Total potential revenue in your active deals |
| **Win Rate** | Your batting average — deals won / total deals |

---

## 12. Workflows — Automate Your Business

Workflows eliminate repetitive tasks by triggering automatic actions when things happen in your CRM.

### 12.1 How Workflows Work

A workflow has three parts:
1. **Trigger** — what event starts the workflow
2. **Conditions** — optional filters (only run if criteria are met)
3. **Actions** — what happens automatically

### 12.2 Creating a Workflow

1. Click **Workflows** in the sidebar
2. Click **"+ New Workflow"**
3. Configure:
   - **Name** — e.g., "Auto-assign new leads"
   - **Object** — what it applies to (Lead, Opportunity, Contact, etc.)
   - **Trigger Event**:
     - `CREATED` — when a new record is created
     - `UPDATED` — when a record is modified
     - `STATUS_CHANGED` — when status changes
     - `STAGE_CHANGED` — when deal stage moves
     - `ASSIGNED` — when record is assigned to someone
   - **Conditions** — e.g., "Source equals WEB" and "Amount > 500000"
   - **Actions**:
     - `Send Email` — automatically send an email
     - `Create Task` — auto-create a follow-up task
     - `Update Field` — change a field value automatically
     - `Send Notification` — send an in-app alert
     - `Assign To` — auto-assign to a team member
4. Click **Save** and toggle the workflow **ON**

### 12.3 Real Estate Workflow Examples

| Workflow | Trigger | Action |
|----------|---------|--------|
| **New Lead Alert** | Lead created | Send notification to agent on duty |
| **Auto Follow-Up** | Lead status = CONTACTED | Create task: "Follow up in 3 days" |
| **Offer Submitted** | Stage changes to PROPOSAL | Send email: "Offer confirmation" to client |
| **Deal Won Celebration** | Stage changes to CLOSED_WON | Send email: "Congratulations!" + Create task: "Process commission" |
| **Stale Lead Reminder** | Lead updated > 7 days ago | Create task: "Re-engage cold lead" |
| **Escalate High-Value** | Amount > $1,000,000 | Assign to senior agent + Send notification to manager |

### 12.4 Workflow Monitoring

- Go to the **Execution Logs** tab to see every workflow execution
- View: which rule fired, what action ran, when it happened, and if it succeeded
- Debug failed executions with detailed error messages

---

## 13. AI Configuration — Build Your CRM With Words

This is the most powerful feature — configure your entire CRM using plain English.

### 13.1 Accessing AI Config

1. Click **AI Config** in the sidebar
2. You'll see a chat interface — type what you need

### 13.2 What You Can Create

| You Say... | The AI Creates... |
|-----------|-------------------|
| "Create a custom object called Properties with fields: address (text, required), price (currency), bedrooms (number), type (picklist with options House, Condo, Townhouse, Land)" | A new Properties object with 4 typed fields |
| "Create a workflow: When a new lead is created from WEB source, send email welcome to the lead" | A workflow with on_create trigger and email action |
| "Create a dashboard called Agent Performance with widgets: Monthly Closings (metric), Revenue by Agent (bar chart), Listings by Status (pie chart)" | A dashboard with 3 widgets |
| "Create a role called Agent with permissions: can create and read Leads, can read Accounts, cannot delete Opportunities" | A role with per-object CRUD permissions |
| "Create a pipeline called Listing Pipeline with stages: Pre-Listing (10%), Active (30%), Under Contract (60%), Pending (80%), Closed (100%)" | A pipeline with 5 stages and probabilities |

### 13.3 How It Works

1. **Type your instruction** in plain English
2. The AI **parses your intent** and generates a structured configuration
3. You see a **confirmation card** with all the details
4. Review the details carefully
5. Click **Confirm** to apply — or **Cancel** to discard
6. The configuration is applied and you see a success message

### 13.4 Tips for Best Results

- **Be specific** — include field types, required flags, and picklist options
- **Use parentheses** for field specs — e.g., `price (currency, required)`
- **List options explicitly** — e.g., `(picklist with options House, Condo, Townhouse)`
- **One command at a time** — create one object, one workflow, or one dashboard per message
- **Review before confirming** — check that all fields, types, and options are correct

### 13.5 Real Estate AI Config Examples (Copy & Paste)

**Custom Objects:**
```
Create a custom object called Properties with fields: address (text, required), city (text, required), state (text), zip_code (text), price (currency, required), bedrooms (number), bathrooms (number), square_feet (number), lot_size (number), year_built (number), property_type (picklist with options Single Family, Condo, Townhouse, Multi-Family, Land, Commercial), listing_status (picklist with options Active, Pending, Sold, Withdrawn, Expired), mls_number (text, unique), description (textarea)
```

```
Create a custom object called Showings with fields: showing_date (datetime, required), property_address (text, required), agent_name (text, required), buyer_name (text, required), buyer_phone (phone), feedback (textarea), showing_status (picklist with options Scheduled, Completed, Cancelled, No-Show), interest_level (picklist with options Very Interested, Interested, Neutral, Not Interested)
```

```
Create a custom object called Offers with fields: offer_date (date, required), property_address (text, required), buyer_name (text, required), offer_amount (currency, required), asking_price (currency), earnest_money (currency), financing_type (picklist with options Cash, Conventional, FHA, VA, USDA, Other), contingencies (textarea), expiration_date (date), offer_status (picklist with options Submitted, Countered, Accepted, Rejected, Withdrawn, Expired)
```

```
Create a custom object called Commissions with fields: agent_name (text, required), property_address (text, required), sale_price (currency, required), commission_rate (percent), commission_amount (currency, required), closing_date (date, required), payment_status (picklist with options Pending, Processing, Paid), split_type (picklist with options Full, Co-Listing, Buyer-Agent), notes (textarea)
```

```
Create a custom object called Open Houses with fields: event_date (datetime, required), property_address (text, required), hosting_agent (text, required), duration_hours (number), visitor_count (number), leads_generated (number), marketing_method (picklist with options MLS, Social Media, Signage, Email Blast, Flyers), notes (textarea), status (picklist with options Scheduled, In Progress, Completed, Cancelled)
```

**Workflows:**
```
Create a workflow: When a new lead is created, send an email welcome to the lead with property recommendations based on their preferences
```

```
Create a workflow: When a showing is completed, create a task for the agent to follow up with the buyer within 24 hours
```

```
Create a workflow: When an offer status changes to Accepted, create a task to order home inspection and send congratulations email to buyer
```

**Dashboards:**
```
Create a dashboard called Agent Dashboard with widgets: Active Listings (metric showing count of active properties), Monthly Closings (metric showing deals closed this month), Revenue by Agent (bar chart showing commission per agent), Property Types (pie chart showing distribution of property types), Pipeline Value (metric showing total pipeline amount)
```

```
Create a dashboard called Listing Performance with widgets: Average Days on Market (metric), Listings by Status (pie chart of active vs pending vs sold), Price vs Sold Price (bar chart comparing asking vs sold), Showings per Listing (bar chart)
```

**Roles:**
```
Create a role called Listing Agent with permissions: can create and read Properties, can create and read Showings, can create and read Open Houses, can read and update Opportunities, cannot access Commissions
```

```
Create a role called Office Admin with permissions: can create read update and delete all objects, can manage Commissions, can read Reports
```

---

## 14. AI Insights — Predictive Intelligence

AI-powered analytics that help you sell smarter.

### 14.1 Available Insights

| Insight | What It Does |
|---------|-------------|
| **Predictive Lead Scoring** | Scores every lead 0–100 based on likelihood to convert |
| **Win Probability** | Predicts the % chance each deal will close |
| **Sales Forecasting** | AI-generated revenue predictions (best case, conservative, commit) |
| **Churn Prediction** | Identifies clients at risk of leaving |
| **Next Best Action** | Recommends what to do next on each deal (call, email, meeting, proposal) |
| **AI Reporting** | Detects anomalies and trends in your data automatically |
| **Email Reply Suggestions** | AI-generated reply drafts for incoming emails |
| **Meeting Summaries** | Auto-transcribe and summarize meeting notes |

### 14.2 How to Use AI Insights

1. Click **AI Insights** in the sidebar
2. Browse the insight categories
3. Click on any insight for detail
4. Use the recommendations to prioritize your work

> **Real Estate Example**: "Lead John Smith scored 89/100 — he's a serious buyer. Next best action: Schedule a property showing this week."

---

## 15. Zero-Config — Instant Setup

Shortcuts to get your CRM configured faster.

### 15.1 Features

| Feature | What It Does |
|---------|-------------|
| **Natural Language Setup** | Describe your business and the AI configures your CRM |
| **Auto Pipeline** | Create industry-specific pipeline stages automatically |
| **CSV Import** | Upload a spreadsheet and auto-detect columns and field types |
| **Workflow Suggestions** | AI suggests common automation rules for your business |
| **Deduplication** | Detect and merge duplicate leads, contacts, and accounts |
| **Data Enrichment** | Auto-fill missing contact data (phone, title, company info) |
| **Dashboard Generation** | AI creates relevant dashboards based on your business type |
| **Onboarding Wizard** | Step-by-step guided setup for new users |

### 15.2 Quick Start with CSV Import

If you have an existing client list in Excel or CSV:
1. Click **Zero Config** in the sidebar
2. Go to the **CSV Import** section
3. Upload your file or paste data
4. The AI detects columns and suggests field mappings
5. Review and confirm
6. Records are imported automatically

---

## 16. Communications Hub

Manage all client communications in one place.

### 16.1 Channels

| Channel | What You Can Do |
|---------|----------------|
| **SMS** | Send/receive text messages to clients |
| **WhatsApp** | Send messages with images, documents, property photos |
| **Phone Calls** | Log call records, track duration and outcomes |
| **Email** | Integrated with the Email module |

### 16.2 Unified Inbox

1. Click **Communications** in the sidebar
2. See all messages across all channels in one view
3. Filter by channel (SMS, WhatsApp, Phone, Email)
4. Click on any conversation to view the full thread
5. Reply directly from the inbox

### 16.3 AI Features

- **Transcription** — call recordings are auto-transcribed to text
- **Sentiment Analysis** — AI detects if a client is happy, frustrated, or indifferent

> **Real Estate Tip**: Send a quick SMS after every showing: "Thanks for visiting 123 Oak St today! Let me know if you have questions." The system tracks delivery and responses.

---

## 17. Collaboration & Team Features

Work together on deals and stay aligned.

### 17.1 Features

| Feature | Description |
|---------|-------------|
| **Deal Chat** | Chat threads on each opportunity — discuss strategy with teammates |
| **@Mentions** | Tag team members with @name to get their attention |
| **Shared Notes** | Collaborative notes on deals, contacts, and accounts |
| **Activity Stream** | Real-time feed of all changes across the CRM |
| **Deal Approvals** | Request manager approval for discounts, special terms, or large deals |
| **Comments** | Add threaded comments on any record |

### 17.2 Real Estate Collaboration Examples

- **Co-listing management** — two agents discuss listing strategy in deal chat
- **Manager approval** — senior agent must approve offers above $2M
- **Team notes** — document inspection findings for the whole team
- **@Mention** — "@Sarah, can you schedule the appraisal for Thursday?"

---

## 18. Smart Automation

Pre-built templates and intelligent suggestions.

### 18.1 Features

| Feature | Description |
|---------|-------------|
| **Workflow Templates** | Pre-built automation for common scenarios — activate with one click |
| **AI Suggestions** | AI analyzes your data and recommends automation opportunities |
| **Intelligent Routing** | Auto-assign leads to agents based on territory, skills, or workload |
| **Reminder Automation** | Auto-create reminders based on deal stages and dates |
| **Proposal Generation** | Generate proposals from templates with auto-filled deal data |
| **Contract Management** | Create, track, and e-sign contracts |

### 18.2 Contract Lifecycle

| Status | Meaning |
|--------|---------|
| **DRAFT** | Contract being prepared |
| **SENT** | Sent to client for review |
| **VIEWED** | Client opened the document |
| **SIGNED** | Client signed the contract |
| **EXECUTED** | Both parties signed — contract is active |

---

## 19. Object Manager — Customize Everything

Browse and manage all the objects, fields, pipelines, and configurations in your CRM.

### 19.1 Accessing Object Manager

1. Click **Object Manager** in the sidebar
2. You'll see a dashboard with summary cards and tabs

### 19.2 Tabs

**Custom Objects Tab**:
- View all custom objects you've created (Properties, Showings, Offers, etc.)
- Expand any object to see its fields
- Each field shows: Name, Type, Required/Unique flags, Picklist Options

**System Objects Tab**:
- View built-in objects (Lead, Account, Contact, Opportunity, Activity, etc.)
- These are the core CRM objects — always available

**Pipelines Tab**:
- View all pipelines with their stages
- See probability, sort order, and stage colors

**Workflows Tab**:
- View all workflow automations
- See trigger type, object, and active/inactive status

**Dashboards Tab**:
- View all custom dashboards and their widgets
- See widget types and configurations

**Roles Tab**:
- View all roles and their permission matrix
- See CRUD permissions per object for each role

---

## 20. Security & Access Control

Protect your data and control who can see what.

### 20.1 Features Overview

| Feature | Description |
|---------|-------------|
| **Role Hierarchy** | Admin → Manager → Agent → Viewer — each level sees appropriate data |
| **User Management** | Add, edit, and deactivate team members |
| **Permission Sets** | Granular control over what each role can create, read, update, or delete |
| **Field-Level Security** | Hide sensitive fields from certain roles (e.g., commission amounts) |
| **Record-Level Access** | Agents see their own records; Managers see their team's; Admins see all |
| **SSO / MFA** | Single Sign-On and Multi-Factor Authentication for enterprise security |
| **Audit Logs** | Complete record of every action — who did what, when, and what changed |

### 20.2 Recommended Roles for Real Estate

| Role | Permissions |
|------|------------|
| **Admin** | Full access to everything — manage users, settings, billing |
| **Broker** | View all deals, all agents' pipelines, approve large transactions |
| **Agent** | Create/manage own leads, contacts, opportunities; view assigned properties |
| **Transaction Coordinator** | View deals, manage documents and tasks, no lead access |
| **Office Assistant** | Create contacts and leads, manage activities, no deal amounts |

### 20.3 Audit Logs

Every action is logged:
- Who made the change
- What was changed (before and after values)
- When it happened (timestamp)
- IP address of the user

Access audit logs via **Security → Audit Logs**.

---

## 21. Integrations — Connect Your Tools

Connect your CRM with the tools you already use.

### 21.1 Available Integrations

| Integration | Description |
|-------------|-------------|
| **Zapier** | Connect to 5,000+ apps via Zapier (Zillow, Realtor.com, Facebook Leads, etc.) |
| **Slack** | Get deal notifications and alerts in your Slack channels |
| **Salesforce** | Bidirectional sync with Salesforce (for enterprise teams) |
| **HubSpot** | Import/export leads to/from HubSpot |
| **Custom REST API** | Connect any system using our REST API |
| **Webhooks** | Send real-time event notifications to any URL |

### 21.2 Setting Up a Webhook

1. Click **Integrations** in the sidebar
2. Go to the **Webhooks** tab
3. Click **"+ New Webhook"**
4. Enter:
   - **URL** — the endpoint to receive events
   - **Events** — which events to send (e.g., `LEAD_CREATED`, `OPPORTUNITY_STAGE_CHANGED`)
5. Click **Save**

### 21.3 Real Estate Integration Ideas

- **Zillow → CRM**: Auto-create leads from Zillow inquiries (via Zapier)
- **Realtor.com → CRM**: Sync leads from Realtor.com
- **Facebook Ads → CRM**: Auto-import leads from Facebook Lead Ads
- **CRM → Slack**: Notify the team when a deal moves to "Under Contract"
- **CRM → Google Calendar**: Auto-create calendar events for showings and closings
- **DocuSign → CRM**: Update deal status when contracts are signed

---

## 22. Developer Portal

For tech-savvy team members or your IT department.

### 22.1 Features

| Feature | Description |
|---------|-------------|
| **API Keys** | Generate API keys for external system access |
| **Webhooks** | Configure event-driven notifications |
| **API Documentation** | Interactive API docs for all endpoints |
| **Custom Apps** | Build custom integrations using the API |

### 22.2 Accessing

1. Click **Developer** in the sidebar
2. Generate API keys for your integrations
3. Configure webhooks for event notifications
4. Test API endpoints directly from the portal

---

## 23. Settings & Preferences

Customize the app to suit your workflow.

### 23.1 Profile Settings

1. Click **Settings** in the sidebar
2. Update:
   - **Name** and **Phone**
   - **Avatar** — upload a profile photo
   - **Bio** — brief description

### 23.2 Preferences

| Preference | Options |
|-----------|---------|
| **Timezone** | US Eastern, Pacific, Central, UTC, and more |
| **Date Format** | MM/DD/YYYY, DD/MM/YYYY, YYYY-MM-DD |
| **Time Format** | 12-hour (AM/PM) or 24-hour |
| **Language** | English (more coming soon) |

### 23.3 Notification Settings

Toggle notifications on/off:
- Email alerts for new leads
- Push notifications for deal updates
- Weekly digest emails (summary of activity)
- Task reminder notifications
- Deal update alerts

### 23.4 Display Settings

- **Theme**: Light mode or Dark mode
- **Items Per Page**: How many records to show per page (10, 25, 50)
- **Default View**: List view or Kanban view for opportunities

---

## 24. Real Estate Quick-Start Guide

Follow these steps to set up your Real Estate CRM in under 20 minutes.

### Step 1 — Log In (1 min)
Log in with your credentials.

### Step 2 — Create Custom Objects via AI Config (10 min)

Go to **AI Config** and paste these commands one at a time, confirming each:

**1. Properties**
```
Create a custom object called Properties with fields: address (text, required), city (text, required), state (text), zip_code (text), price (currency, required), bedrooms (number), bathrooms (number), square_feet (number), property_type (picklist with options Single Family, Condo, Townhouse, Multi-Family, Land, Commercial), listing_status (picklist with options Active, Pending, Sold, Withdrawn, Expired), mls_number (text, unique), description (textarea), listing_date (date), listing_agent (text)
```

**2. Showings**
```
Create a custom object called Showings with fields: showing_date (datetime, required), property_address (text, required), agent_name (text, required), buyer_name (text, required), buyer_phone (phone), feedback (textarea), showing_status (picklist with options Scheduled, Completed, Cancelled, No-Show), interest_level (picklist with options Very Interested, Interested, Neutral, Not Interested)
```

**3. Offers**
```
Create a custom object called Offers with fields: offer_date (date, required), property_address (text, required), buyer_name (text, required), offer_amount (currency, required), asking_price (currency), earnest_money (currency), financing_type (picklist with options Cash, Conventional, FHA, VA, USDA, Other), contingencies (textarea), expiration_date (date), offer_status (picklist with options Submitted, Countered, Accepted, Rejected, Withdrawn, Expired)
```

**4. Commissions**
```
Create a custom object called Commissions with fields: agent_name (text, required), property_address (text, required), sale_price (currency, required), commission_rate (percent), commission_amount (currency, required), closing_date (date, required), payment_status (picklist with options Pending, Processing, Paid), split_type (picklist with options Full, Co-Listing, Buyer-Agent), notes (textarea)
```

**5. Open Houses**
```
Create a custom object called Open Houses with fields: event_date (datetime, required), property_address (text, required), hosting_agent (text, required), duration_hours (number), visitor_count (number), leads_generated (number), marketing_method (picklist with options MLS, Social Media, Signage, Email Blast, Flyers), notes (textarea), status (picklist with options Scheduled, In Progress, Completed, Cancelled)
```

### Step 3 — Create Workflows (3 min)

```
Create a workflow: When a new lead is created, send an email welcome to the lead and create a task to call the lead within 4 hours
```

```
Create a workflow: When a showing is completed, create a task for the agent to follow up with the buyer within 24 hours
```

```
Create a workflow: When an offer status changes to Accepted, send a congratulations email and create a task to order home inspection
```

### Step 4 — Create Dashboards (3 min)

```
Create a dashboard called Real Estate Command Center with widgets: Active Listings (metric showing count of active properties), Monthly Revenue (metric showing total commission amount this month), Properties by Type (pie chart of property types), Deals by Stage (bar chart showing pipeline stages), Agent Performance (bar chart showing closings per agent)
```

```
Create a dashboard called Lead Analytics with widgets: New Leads This Month (metric), Leads by Source (pie chart by web, referral, social, event), Lead Conversion Rate (metric showing percentage), Lead Score Distribution (bar chart)
```

### Step 5 — Set Up Roles (2 min)

```
Create a role called Agent with permissions: can create and read Leads, can create and read Contacts, can create and read Properties, can create and read Showings, can read and update Opportunities, can create Open Houses, cannot access Commissions
```

```
Create a role called Broker with permissions: can create read update and delete all objects, can manage Commissions, can manage Settings
```

### Step 6 — Import Your Data (2 min)

1. Go to **Zero Config**
2. Upload your existing client CSV file
3. Let the AI map your columns
4. Confirm and import

### Step 7 — Invite Your Team

1. Go to **Security**
2. Add new users with appropriate roles
3. They'll receive login credentials via email

### You're Done!

Your Real Estate CRM is now configured with:
- 5 custom objects (Properties, Showings, Offers, Commissions, Open Houses)
- 3 automated workflows
- 2 dashboards with 10+ widgets
- 2 roles with proper permissions
- All your imported data

---

## 25. FAQ & Troubleshooting

### General

**Q: Can I undo an action in AI Config?**  
A: You can cancel before confirming. After confirmation, the object/workflow/dashboard is created. You can deactivate objects, workflows, and dashboards from the Object Manager.

**Q: Can I edit a field after creating it?**  
A: Yes — go to Object Manager, find the object, and modify its fields.

**Q: How many custom objects can I create?**  
A: There is no hard limit. Create as many objects as your business needs.

**Q: Can I create multiple objects in one AI Config message?**  
A: Create one object per message for best results. The AI handles one command at a time.

### Real Estate Specific

**Q: Can I track both buyers and sellers?**  
A: Yes — use the **Contacts** module with tags or custom fields to differentiate. Or create separate custom objects for Buyers and Sellers using AI Config.

**Q: How do I track a dual-agency deal?**  
A: Create the Opportunity with both buyer and seller contacts linked. Use the Collaborators feature to add both agents.

**Q: Can I integrate with my MLS?**  
A: Use the Integration module to connect via REST API or Zapier. Many MLS systems offer data feeds that can push to a webhook.

**Q: How do I track property showings?**  
A: Use the Showings custom object (created in Step 2 above). Each showing links to a property, buyer, and agent with feedback tracking.

**Q: Can I generate commission reports?**  
A: Use the Commissions custom object plus the Reports module. Filter by agent, date range, or property type to see commission summaries.

### Troubleshooting

**Q: I'm getting a "Session expired" message**  
A: Your login token has expired. Log out and log back in.

**Q: My workflow isn't firing**  
A: Check that the workflow is **enabled** (toggle ON). Verify the trigger event matches what's happening in your data.

**Q: AI Config says "Invalid configuration"**  
A: Try rewording your instruction. Be explicit about field types and ensure object names are single words or quoted.

**Q: I can't see some data that I know exists**  
A: Check your role permissions in Security. You may not have read access to certain objects.

**Q: The dashboard shows no data**  
A: Dashboards display data from existing records. Make sure you have actual records (leads, opportunities, etc.) in the system.

---

## Support

For additional help:
- Email: support@yourcrm.com
- Phone: 1-800-XXX-XXXX
- Knowledge Base: https://docs.yourcrm.com
- Live Chat: Available in-app (bottom right corner)

---

*© 2026 Zero-Config AI CRM Platform. All rights reserved.*
