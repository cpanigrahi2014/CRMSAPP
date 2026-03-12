# CRMS Platform — Product Demo Guide

## Quick Start

1. **Start all services**: `docker compose up -d`
2. **Seed demo data**: `.\demo\seed-demo.ps1`
3. **Open the app**: [http://localhost:3000](http://localhost:3000)

---

## Demo Login Credentials

| Role    | Email                          | Name             |
|---------|--------------------------------|------------------|
| Admin   | sarah.chen@acmecorp.com        | Sarah Chen       |
| Manager | james.wilson@acmecorp.com      | James Wilson     |
| Rep     | emily.rodriguez@acmecorp.com   | Emily Rodriguez  |
| Rep     | michael.park@acmecorp.com      | Michael Park     |
| Rep     | lisa.thompson@acmecorp.com     | Lisa Thompson    |

> **Password**: `Demo@2026!` &nbsp;|&nbsp; **Tenant ID**: `default`

---

## Demo Walkthrough

### 1. Dashboard & Overview
- Log in as **Sarah Chen** (Admin) to see the full dashboard
- View pipeline metrics, revenue forecasts, and activity feed
- Highlight the real-time data across all modules

### 2. Accounts (10 companies)
Navigate to **Accounts** to showcase:
- **Enterprise clients**: TechVista Solutions ($45M), GlobalRetail Inc ($120M), FinEdge Capital ($75M), GreenEnergy Dynamics ($200M)
- **Mid-market prospects**: MedHealth Partners, EduPath Learning, CloudNine SaaS, Atlas Logistics
- **SMB**: StartupHub Ventures
- Account notes, territory assignment, health scores, engagement metrics
- Hierarchical account views and industry segmentation

### 3. Contacts (20 contacts)
Navigate to **Contacts** to showcase:
- Contacts linked to accounts with titles, departments, LinkedIn links
- Decision makers, influencers, champions across organizations
- Lifecycle stages (Customer, Lead) and segmentation
- Email/SMS/phone opt-in consent tracking

### 4. Leads (15 leads)
Navigate to **Leads** to showcase:
- Leads from diverse sources: Web, Trade Show, Referral, Phone, Email, Social Media
- Companies across industries: tech, retail, healthcare, legal, manufacturing, agriculture, automotive
- Lead qualification and scoring
- Lead-to-opportunity conversion workflow

### 5. Opportunities ($4.6M Pipeline)
Navigate to **Opportunities** to showcase:

| Deal | Amount | Stage |
|------|--------|-------|
| GreenEnergy Fleet Management | $1,200,000 | Negotiation |
| GlobalRetail Omnichannel Suite | $850,000 | Proposal |
| FinEdge Risk Analytics Module | $520,000 | Qualification |
| TechVista Platform License | $450,000 | Negotiation |
| Precision Mfg ERP Connect | $380,000 | Proposal |
| Atlas Logistics Route Optimization | $290,000 | **Closed Won** |
| MedHealth HIPAA Compliance Package | $280,000 | Needs Analysis |
| EduPath Learning Management CRM | $180,000 | Prospecting |
| CloudNine Partnership Integration | $150,000 | **Closed Won** |
| TechVista AI Add-on | $120,000 | Needs Analysis |
| StartupHub Portfolio CRM | $95,000 | Qualification |
| GlobalRetail Data Migration | $75,000 | **Closed Lost** |

Key features to demo:
- **Pipeline board**: drag-and-drop stage management
- **Products**: line items with pricing, discounts, totals
- **Competitors**: competitive intelligence per deal (Salesforce, HubSpot, Microsoft Dynamics, etc.)
- **Notes**: collaboration notes on each deal
- **Forecasting**: commit, best case, pipeline categories
- **Win/loss analytics**: revenue reports, win rate analysis

### 6. Activities (25 items)
Navigate to **Activities** to showcase:
- **Meetings**: Discovery calls, demos, contract negotiations, pipeline reviews
- **Tasks**: Proposals, competitor analysis, ROI calculators, executive briefings
- **Calls**: Follow-ups, reference calls, pricing discussions
- **Emails**: Welcome emails, newsletters, follow-ups
- Priority levels (Urgent, High, Medium, Low)
- Activity timeline linked to accounts, opportunities, leads, contacts

### 7. Workflow Automation (5 rules)
Navigate to **Workflows** to showcase:
- Auto-assign new web leads
- Large deal notifications ($500K+)
- Stale opportunity follow-ups
- Welcome emails for new contacts
- Overdue activity escalation

### 8. Email Templates (5 templates)
Navigate to **Email** to showcase:
- Welcome New Client (onboarding)
- Meeting Follow-Up (sales)
- Proposal Follow-Up (sales)
- Quarterly Business Review Invite (customer success)
- Deal Won celebration (internal)

### 9. AI Features
Navigate to **AI** section to showcase:
- Lead scoring predictions
- Next-best-action recommendations
- Email draft generation
- Sentiment analysis
- Win probability forecasting
- Churn prediction

### 10. Reports & Analytics
Navigate to **Reports** to showcase:
- Revenue analytics across pipeline
- Win/loss analysis
- Sales forecasting
- Activity metrics and team performance

---

## Architecture Highlights

- **14 Microservices**: Auth, Lead, Account, Contact, Opportunity, Activity, Notification, Workflow, AI, Email, Integration + infrastructure
- **Tech Stack**: Spring Boot 3.2.3 / Java 21, React 18 + TypeScript, PostgreSQL 16, Redis 7, Apache Kafka
- **Multi-tenant**: Isolated data per tenant via JWT-based tenantId
- **Real-time**: SSE activity streams, Kafka event bus
- **Security**: JWT (HMAC-SHA512), RBAC (Admin/Manager/User), CORS, rate limiting

---

## Video Demo Assets

| File | Description |
|------|-------------|
| `demo/VIDEO-DEMO-SCRIPT.md` | Scene-by-scene narration script with timings (~10 min) |
| `demo/demo-presentation.html` | Self-running HTML slide deck (open in browser, press P to autoplay) |

### Quick Recording Workflow
1. Open `demo/demo-presentation.html` in Chrome
2. Start OBS Studio or your screen recorder
3. Press **P** to autoplay or use arrow keys to advance manually
4. Record narration using the script in `VIDEO-DEMO-SCRIPT.md`

---

## Reseed / Reset

To re-populate demo data, simply run the seed script again:
```powershell
.\demo\seed-demo.ps1
```
Existing users will be logged in (not duplicated). Other entities will be created fresh.
