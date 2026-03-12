# CRMS Platform — Video Demo Script

> **Total Duration**: ~8–10 minutes  
> **Presenter**: Narrator with screen recording  
> **App URL**: http://localhost:3000  
> **Resolution**: 1920 × 1080 (Full HD)

---

## Pre-Recording Checklist

- [ ] All services running: `docker compose up -d`
- [ ] Demo data seeded: `.\demo\seed-demo.ps1`
- [ ] Browser: Chrome (clean profile, no extensions bar)
- [ ] Screen recording tool ready (OBS / Camtasia / ScreenPal)
- [ ] Microphone tested
- [ ] Browser zoom: 100%
- [ ] Close all notifications / popups

---

## SCENE 1 — Opening & Login (0:00 – 0:45)

### Screen Actions
1. Open browser to `http://localhost:3000` → Login page is displayed
2. Type **Email**: `sarah.chen@acmecorp.com`
3. Type **Password**: `Demo@2026!`
4. Type **Tenant ID**: `default`
5. Click **Sign In**
6. Dashboard loads

### Narration
> "Welcome to the CRMS Platform — a production-grade, multi-tenant CRM built with a modern microservices architecture.
>
> Today I'll walk you through the key features: pipeline management, contact intelligence, AI-powered insights, workflow automation, and more.
>
> Let's start by logging in as Sarah Chen, our admin user. The platform supports role-based access — Admins, Managers, and Sales Reps each see a tailored experience."

---

## SCENE 2 — Dashboard Overview (0:45 – 1:45)

### Screen Actions
1. Pause on Dashboard — slowly scroll to reveal all widgets
2. Hover over pipeline chart to show tooltips
3. Point out key metrics: total revenue, open deals, active leads
4. Highlight the activity feed on the right side

### Narration
> "The Dashboard gives leadership a real-time view of the entire sales operation.
>
> At the top, we see key metrics: a **$4.6 million pipeline** across 12 active deals, **15 qualified leads**, and **20 contacts** across 10 accounts.
>
> The pipeline chart breaks down revenue by stage — from Prospecting through Closed Won. Below that, the activity feed shows recent team activity in real-time, powered by Server-Sent Events and Kafka.
>
> Every data point here is live — no batch refreshes, no stale numbers."

---

## SCENE 3 — Accounts (1:45 – 2:45)

### Screen Actions
1. Click **Accounts** in the sidebar
2. Show the accounts list — 10 companies with revenue, industry, status
3. Click on **TechVista Solutions** to open the account detail page
4. Scroll through account details: notes, contacts, opportunities linked
5. Go back to accounts list

### Narration
> "Moving to Accounts — we're managing 10 companies ranging from SMBs like StartupHub Ventures to enterprise clients like GreenEnergy Dynamics at $200 million in annual revenue.
>
> Let me open TechVista Solutions. Here you can see the full 360-degree account view: company details, associated contacts, open opportunities, account health score, and engagement history.
>
> Accounts support hierarchical relationships — parent-child structures — and territory assignment for sales teams."

---

## SCENE 4 — Contacts (2:45 – 3:30)

### Screen Actions
1. Click **Contacts** in the sidebar
2. Show the contacts grid — 20 contacts with titles, companies, lifecycle stage
3. Click on a contact to show detail view
4. Highlight the communication preferences / consent tracking section
5. Go back to contacts list

### Narration
> "In Contacts, we have 20 contacts linked across our accounts — decision makers, influencers, and champions.
>
> Each contact record includes their title, department, LinkedIn profile, lifecycle stage, and — importantly — communication consent tracking for email, SMS, and phone. This keeps us compliant with privacy regulations.
>
> Contacts are automatically linked to their accounts, so the relationship graph is always up-to-date."

---

## SCENE 5 — Leads (3:30 – 4:30)

### Screen Actions
1. Click **Leads** in the sidebar
2. Show the leads list — 15 leads with source, status, score
3. Point out the diverse lead sources: Web, Trade Show, Referral, Social Media
4. Click on a high-scoring lead to view details
5. Show the **Convert to Opportunity** button/workflow
6. Go back to leads list

### Narration
> "The Leads module manages 15 inbound leads from diverse sources — web forms, trade shows, referrals, social media, and more. Each lead is automatically scored based on engagement signals.
>
> Let me open this lead — you can see the full qualification details, company info, industry, and estimated deal size.
>
> When a lead is ready, a single click converts it into an Opportunity — carrying over all the context so the sales rep doesn't lose any information. This is the core lead-to-revenue workflow."

---

## SCENE 6 — Opportunities & Pipeline (4:30 – 6:00)

### Screen Actions
1. Click **Opportunities** in the sidebar
2. Show the pipeline board view (Kanban-style if available) or list view
3. Highlight key deals:
   - GreenEnergy Fleet Management — $1.2M (Negotiation)
   - GlobalRetail Omnichannel Suite — $850K (Proposal)
   - Atlas Logistics Route Optimization — $290K (Closed Won ✓)
4. Click on **GreenEnergy Fleet Management** to open deal detail
5. Show deal sections: products/line items, competitors, notes, stage history
6. Show the forecasting fields: commit/best case/pipeline categories
7. Go back to opportunities list

### Narration
> "Here's the heart of the CRM — the Opportunity Pipeline. We're tracking 12 deals totaling $4.6 million.
>
> The pipeline spans every stage from Prospecting to Closed Won. Two deals are already closed — Atlas Logistics at $290K and CloudNine at $150K.
>
> Let me open our largest deal — GreenEnergy Fleet Management at $1.2 million in Negotiation. Inside, we have full deal intelligence: product line items with pricing and discounts, competitive landscape tracking Salesforce, HubSpot, and Dynamics as competitors, collaboration notes, and stage history.
>
> For forecasting, each deal is categorized as Commit, Best Case, or Pipeline — giving leadership accurate revenue projections."

---

## SCENE 7 — Activities (6:00 – 6:45)

### Screen Actions
1. Click **Activities** in the sidebar
2. Show the activities list — 25 items across types: Meetings, Tasks, Calls, Emails
3. Filter by type (if filter exists) to show meetings
4. Point out priority levels: Urgent, High, Medium, Low
5. Show an activity linked to an opportunity

### Narration
> "Activities track every customer interaction — 25 items across meetings, tasks, calls, and emails.
>
> Each activity is linked to accounts, opportunities, leads, or contacts — so you always have the full context. Priority levels help reps focus on what matters most, from urgent contract reviews to routine follow-ups.
>
> The activity timeline becomes a complete audit trail of every customer touchpoint."

---

## SCENE 8 — Workflows & Automation (6:45 – 7:30)

### Screen Actions
1. Click **Workflows** in the sidebar
2. Show the 5 workflow rules
3. Expand one rule to show trigger → condition → action structure
4. Highlight specific rules:
   - Auto-assign new web leads
   - Large deal notification ($500K+)
   - Stale opportunity follow-up

### Narration
> "The Workflow engine automates repetitive tasks with a trigger-condition-action model.
>
> We have 5 rules configured: new web leads are automatically assigned to reps, any deal over $500K triggers a manager notification, stale opportunities get flagged for follow-up, new contacts receive a welcome email, and overdue activities escalate to managers.
>
> These rules run in real-time via Kafka events — no batch jobs, no delays."

---

## SCENE 9 — Email Templates (7:30 – 8:00)

### Screen Actions
1. Click **Email** in the sidebar
2. Show the 5 email templates
3. Click on one template to preview: Welcome New Client or Proposal Follow-Up
4. Show the template variables / merge fields

### Narration
> "The Email module includes 5 professional templates: client onboarding, meeting follow-ups, proposal follow-ups, quarterly business reviews, and deal-won celebrations.
>
> Each template supports merge fields for personalization — contact names, company details, deal values — so outreach is always relevant and professional."

---

## SCENE 10 — AI Insights (8:00 – 9:00)

### Screen Actions
1. Click **AI Insights** in the sidebar
2. Show AI features: lead scoring predictions, next-best-action recommendations
3. Show email draft generation (if interactive)
4. Highlight win probability forecasting on deals
5. Show sentiment analysis or churn prediction indicators

### Narration
> "This is where it gets exciting. The AI module, powered by LLM integration, adds intelligence across the entire platform.
>
> Lead scoring uses engagement signals and firmographic data to prioritize the best opportunities. Next-best-action recommendations tell reps exactly what to do next: send a proposal, schedule a demo, or follow up on a stale deal.
>
> The AI can also draft personalized emails, analyze customer sentiment, predict win probability on deals, and flag accounts at risk of churn. All of this is powered by a resilient integration layer with Resilience4j circuit breakers."

---

## SCENE 11 — Architecture Highlight (9:00 – 9:30)

### Screen Actions
1. (Optional) Switch to a slide or diagram showing the architecture
2. Or navigate to **Settings** / **Developer Portal** to show system info

### Narration
> "Under the hood, this platform runs on 14 microservices built with Java 21 and Spring Boot 3. The frontend is React 18 with TypeScript.
>
> Data flows through Apache Kafka for real-time event streaming, PostgreSQL 16 for persistence, and Redis 7 for caching. Security is handled with JWT tokens using HMAC-SHA512, with full RBAC — Admin, Manager, and Rep roles.
>
> Every service is containerized with Docker and orchestrated via Docker Compose. The multi-tenant architecture isolates data per tenant using a shared-database strategy with automatic Hibernate filters."

---

## SCENE 12 — Closing (9:30 – 10:00)

### Screen Actions
1. Return to the Dashboard
2. Zoom out or pause on the full dashboard view

### Narration
> "That's the CRMS Platform — a complete, production-ready CRM covering the full sales lifecycle: from lead capture through pipeline management to deal closure, with AI-powered intelligence and real-time automation at every step.
>
> Thank you for watching."

---

## Recording Tips

| Tip | Detail |
|-----|--------|
| **Mouse movement** | Move slowly and deliberately; avoid rapid scrolling |
| **Pauses** | Pause 2–3 seconds on key metrics before narrating |
| **Click timing** | Wait for pages to fully load before clicking next |
| **Zoom** | Use browser zoom (Ctrl +) to enlarge key areas if needed |
| **B-roll** | Record extra footage of each section for editing flexibility |
| **Audio** | Record narration separately for cleaner audio (sync in post) |
| **Transitions** | Use simple fades between scenes; avoid flashy transitions |

---

## Recommended Tools

| Tool | Purpose | Cost |
|------|---------|------|
| **OBS Studio** | Screen recording | Free |
| **Camtasia** | Recording + editing | Paid |
| **ScreenPal** | Quick recordings | Freemium |
| **DaVinci Resolve** | Professional editing | Free |
| **Audacity** | Audio recording/editing | Free |
| **Canva** | Intro/outro slides | Freemium |
