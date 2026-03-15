# 🏠 Real Estate CRM — End-to-End Business Demo Guide

> **Duration:** 30–45 minutes  
> **Audience:** Real Estate Brokers, Property Managers, Real Estate Tech Teams  
> **Industry:** Residential & Commercial Real Estate  
> **Login:** `rachel.morgan@premierrealty.com` / `Demo@2026!`  
> **URL:** [http://localhost:3000](http://localhost:3000)

---

## Table of Contents

1. [Pre-Demo Setup](#1-pre-demo-setup)
2. [Demo Scenario Overview](#2-demo-scenario-overview)
3. [Part 1: Lead Capture from Property Portals & Social Media](#part-1-lead-capture-from-property-portals--social-media)
4. [Part 2: Pipeline Management — Inquiry to Closed](#part-2-pipeline-management--inquiry-to-closed)
5. [Part 3: Contact Management with Property Preferences](#part-3-contact-management-with-property-preferences)
6. [Part 4: Automated Follow-Up Workflows After Viewings](#part-4-automated-follow-up-workflows-after-viewings)
7. [Part 5: Marketing Campaigns for Real Estate](#part-5-marketing-campaigns-for-real-estate)
8. [Part 6: Service Cases — Transaction Issues](#part-6-service-cases--transaction-issues)
9. [Part 7: Reports & Analytics](#part-7-reports--analytics)
10. [Part 8: AI-Powered Insights](#part-8-ai-powered-insights)
11. [Competitive Differentiators](#competitive-differentiators)
12. [ROI & Business Impact](#roi--business-impact)

---

## 1. Pre-Demo Setup

### Quick Start (One Command)

```powershell
# From the project root:
.\demo\seed-real-estate.ps1
```

This creates the full real estate demo dataset:

| Data Type    | Count | Description                                       |
|-------------|-------|---------------------------------------------------|
| Agents/Users | 5     | Broker, manager, buyer's agent, listing agent, leasing agent |
| Accounts     | 8     | Brokerages, developers, property managers, mortgage partners |
| Contacts     | 12    | Buyers, sellers, investors — each with property preferences |
| Leads        | 15    | From Zillow, Realtor.com, Facebook, Instagram, referrals |
| Deals        | 15    | Across all pipeline stages: Inquiry → Viewing → Offer → Negotiation → Closed |
| Properties   | 9     | Property details attached as line items to deals |
| Activities   | 15    | Viewings, follow-up calls, open houses, tasks |
| Workflows    | 6     | Post-viewing follow-up, portal lead routing, offer alerts |
| Cases        | 6     | Inspection disputes, title issues, appraisal gaps |
| Campaigns    | 8     | Open houses, Zillow ads, Instagram luxury showcase, webinars |
| Templates    | 5     | Post-viewing thank you, new listing alert, offer confirmation |
| Notes        | 10    | Deal and account context notes |
| Competitors  | 6     | Per-deal competitive intel (Compass, Douglas Elliman, etc.) |

### Team Credentials

| Name             | Email                                  | Role           |
|------------------|----------------------------------------|----------------|
| Rachel Morgan    | `rachel.morgan@premierrealty.com`      | Broker / Admin |
| David Kim        | `david.kim@premierrealty.com`          | Sales Manager  |
| Sofia Martinez   | `sofia.martinez@premierrealty.com`     | Buyer's Agent  |
| Jason Carter     | `jason.carter@premierrealty.com`       | Listing Agent  |
| Priya Patel      | `priya.patel@premierrealty.com`        | Leasing Agent  |

All passwords: `Demo@2026!`

---

## 2. Demo Scenario Overview

**Company:** Premier Realty Group — a full-service brokerage with 45 agents covering Los Angeles, Miami, San Diego, Austin, New York, and Phoenix.

**Business Challenge:** Premier Realty receives leads from 6+ sources (Zillow, Realtor.com, Facebook, Instagram, referrals, open houses) but has no unified system. Leads fall through cracks, follow-ups are inconsistent, and the broker has no visibility into the pipeline.

**Solution:** The CRM platform provides:

```
┌─────────────────────────────────────────────────────────────────┐
│                    REAL ESTATE CRM WORKFLOW                      │
│                                                                  │
│  LEAD SOURCES           PIPELINE STAGES        OUTCOMES          │
│  ────────────           ──────────────          ────────          │
│  🌐 Zillow         →   📋 Inquiry         →   🏠 Closed Won     │
│  🌐 Realtor.com    →   👁️ Viewing          →   📋 Referral      │
│  📱 Facebook       →   📝 Offer           →   🔄 Re-engage     │
│  📸 Instagram      →   🤝 Negotiation     →   ❌ Lost           │
│  🤝 Referrals      →   ✅ Closed                                │
│  📞 Open Houses                                                  │
│                                                                  │
│  AUTOMATION: Follow-up (24hr) → Lead Routing → Offer Alerts     │
└─────────────────────────────────────────────────────────────────┘
```

**Pipeline Stage Mapping:**

| Real Estate Stage | CRM Stage        | What Happens                              |
|-------------------|------------------|-------------------------------------------|
| **Inquiry**       | PROSPECTING      | Lead captured, initial contact made        |
| **Viewing**       | QUALIFICATION    | Property showings, buyer feedback collected |
| **Offer**         | PROPOSAL         | Offer submitted, awaiting seller response  |
| **Negotiation**   | NEGOTIATION      | Counter-offers, contingency discussions    |
| **Closed**        | CLOSED_WON       | Deal closed, keys handed over              |
| **Lost**          | CLOSED_LOST      | Deal fell through                          |

---

## Part 1: Lead Capture from Property Portals & Social Media

### 🎯 What to Show

> "Premier Realty gets leads from Zillow, Realtor.com, Facebook, Instagram, referrals, and open houses. All channels feed into one unified pipeline."

### Demo Steps

1. **Navigate to Leads** in the sidebar
2. **Point out the lead sources** — filter by source to show:
   - **WEB (8 leads):** Zillow inquiries (Tyler Brooks searching Santa Monica), Realtor.com leads (Hannah Lee looking at Miami multi-family), Redfin relocators
   - **SOCIAL_MEDIA (4 leads):** Facebook ad responses (Aisha Mohammed — Cactus Ridge), Instagram DMs (Megan Whitfield — Manhattan lofts), TikTok engagement (Samantha Reed — Miami Beach condos)
   - **REFERRAL (3 leads):** Client-to-client referrals (Derek Sullivan, Kevin Tran, Chris Taylor)
   - **PHONE/EMAIL:** Open house sign-ups, cold call conversions
3. **Click into Tyler Brooks** (Zillow lead) — show:
   - Source: WEB (Zillow)
   - Description: "Clicked on 12 listings in past week" — behavioral data
   - Assigned to Sofia Martinez (buyer's agent)
4. **Click into Aisha Mohammed** (Facebook lead) — show:
   - Source: SOCIAL_MEDIA (Facebook ad)
   - Description: First-time buyer, interested in new construction
   - Automatic assignment via workflow

### 💡 What to Highlight

> "Speed-to-lead is critical in real estate. Our automation ensures every portal lead gets a response within 5 minutes and is instantly assigned to the on-duty agent. No more leads sitting in Zillow unanswered."

---

## Part 2: Pipeline Management — Inquiry to Closed

### 🎯 What to Show

> "Every property deal flows through a clear pipeline: Inquiry → Viewing → Offer → Negotiation → Closed. Rachel can see her entire brokerage's pipeline at a glance."

### Demo Steps

1. **Navigate to Opportunities** — show the full pipeline:

   | Stage (Real Estate) | CRM Stage     | # Deals | Total Value     |
   |---------------------|---------------|---------|-----------------|
   | Inquiry             | PROSPECTING   | 4       | ~$8.4M          |
   | Viewing             | QUALIFICATION | 4       | ~$19.8M         |
   | Offer               | PROPOSAL      | 3       | ~$6.1M          |
   | Negotiation         | NEGOTIATION   | 3       | ~$13.9M         |
   | Closed              | CLOSED_WON    | 1       | $650K           |

2. **Click into "Zhang – Beverly Hills 4BR SFR"** (Negotiation stage):
   - Deal value: $2.85M
   - Probability: 75%
   - Product tab: Property details (456 Palm Dr, 4BR/3.5BA, 3200 sqft)
   - Notes: Buyer loved the pool, concerned about street noise
   - Competitors: Compass Beverly Hills, Coldwell Banker
   - Close date: April 15

3. **Click into "Rivera – OC Fix & Flip"** (Closed Won):
   - $650K purchase, closed March 1
   - Rehab budget $120K, ARV $920K
   - Show the full deal lifecycle — from inquiry to close

4. **Click into "Thompson – Bel Air Estate"** ($12.5M listing):
   - Show NDA requirement in notes
   - Private showings only — high-value deal management
   - Competitor: The Agency Bel Air

5. **Go to Dashboard** — show:
   - Total pipeline value (~$47M across 15 deals)
   - Win rate
   - Deal distribution by stage
   - Revenue forecasting

### 💡 What to Highlight

> "Rachel, as broker, sees the entire brokerage's pipeline. She can spot deals that need attention — like Amanda Collins' 1031 exchange with a tight deadline — and proactively assign resources. Every deal has full context: property details, buyer preferences, competitive intel, and agent notes."

---

## Part 3: Contact Management with Property Preferences

### 🎯 What to Show

> "Every contact record captures what matters in real estate: budget range, property type, preferred areas, bedroom/bath requirements, financing status, and timeline."

### Demo Steps

1. **Navigate to Contacts**
2. **Click into Michael Zhang** (Home Buyer):
   - Description: "Looking for 4BR single-family in Beverly Hills. Budget: $2.5M–$3.5M. Must have pool and home office. Pre-approved with Pacific Coast Mortgage."
   - Linked to Premier Realty Group account
   - Lifecycle: OPPORTUNITY
   - Segment: DECISION_MAKER
3. **Click into Robert Henderson** (Investor):
   - Description: "Portfolio investor — 12 rental units in Austin. Looking to acquire 5-10 more SFR. Budget $300K-$500K per unit. Cash buyer."
   - Shows investor-specific criteria and portfolio context
4. **Click into Emily Thompson** (Luxury Seller):
   - Description: "Listing 6BR estate in Bel Air. Asking $12.5M. Requires NDA and private showings."
   - Shows seller-side contact management
5. **Click into Sarah Nakamura** (First-Time Buyer):
   - Description: FHA financing, budget constraints, style preferences
   - Lifecycle: LEAD — still early stage

### 💡 What to Highlight

> "Each contact is a complete buyer/seller profile. Agents don't need to remember preferences — it's all in the CRM. When a new listing comes in, we can instantly match it to buyers whose criteria fit. And when contacts are linked to accounts and deals, nothing falls through the cracks."

---

## Part 4: Automated Follow-Up Workflows After Viewings

### 🎯 What to Show

> "The #1 complaint in real estate: 'My agent never followed up after the showing.' Our automated workflows eliminate that problem entirely."

### Demo Steps

1. **Navigate to Workflows** — show 6 automated workflows:

   | Workflow | Trigger | What It Does |
   |----------|---------|--------------|
   | **Post-Viewing Follow-Up (24hr)** | Viewing marked complete | Sends thank-you email, creates follow-up call task, notifies agent |
   | **New Portal Lead → Auto-Assign** | New lead from WEB/SOCIAL | Assigns to on-duty agent, sends 5-min response alert, emails buyer |
   | **Offer Submitted → Manager Alert** | Deal moves to Offer stage | Notifies manager, creates review task |
   | **Stale Lead Re-Engagement** | Lead stays NEW for 7 days | Sends new listings email, updates status |
   | **Closed Deal → Celebration** | Deal marked Closed Won | Team notification, creates referral request task |
   | **Negotiation → Urgency Tasks** | Deal enters Negotiation | Creates inspection + appraisal tasks |

2. **Click into "Post-Viewing Follow-Up"** — explain the flow:
   - **Trigger:** Activity type MEETING is marked COMPLETED
   - **Action 1:** Send email — "Thank you for touring the property!"
   - **Action 2:** Create task — "Follow-up call: Get feedback on property viewing"
   - **Action 3:** Send notification — "Viewing follow-up needed"

3. **Show Activities** — point out the scheduled viewings:
   - "Property Viewing: 456 Palm Dr, Beverly Hills" — 2 days out
   - "Open House: 1840 Ocean Blvd, La Jolla" — 3 days out
   - "Saturday Open House Tour: 3 Santa Monica properties" — upcoming

4. **Explain the automation chain:**
   > "When Sofia completes the Beverly Hills viewing with Michael Zhang, the system automatically sends Michael a thank-you email with property details, creates a follow-up call for Sofia the next morning, and pings David Kim (manager) that a high-value showing was completed. Zero manual effort."

### 💡 What to Highlight

> "National Association of Realtors data shows that 80% of buyers who don't hear back within 24 hours move on to another agent. Our workflows guarantee 100% follow-up rate — automatically. That's the difference between a $2.85M commission and a lost client."

---

## Part 5: Marketing Campaigns for Real Estate

### 🎯 What to Show

> "Premier Realty runs multi-channel campaigns: open house events, Zillow ads, Instagram luxury showcases, and market update emails — all tracked in one place."

### Demo Steps

1. **Navigate to Campaigns** — show 8 active/planned campaigns:

   | Campaign | Type | Status | Budget | Expected Revenue |
   |----------|------|--------|--------|------------------|
   | Spring Open House Weekend | EVENT | ACTIVE | $5K | $500K |
   | Zillow Premier Agent Boost | PAID_ADS | ACTIVE | $15K | $2M |
   | Instagram Luxury Showcase | SOCIAL | ACTIVE | $3K | $1.5M |
   | First-Time Buyer Webinar | WEBINAR | PLANNED | $2K | $800K |
   | Seller's Market Report Drip | EMAIL | ACTIVE | $1.2K | $3M |
   | Cactus Ridge Grand Opening | EVENT | PLANNED | $8K | $5M |
   | Investor Networking Mixer | EVENT | PLANNED | $4K | $10M |
   | Facebook/Google Retargeting | PAID_ADS | ACTIVE | $6K | $1.2M |

2. **Click into "Zillow Premier Agent Boost"** — show:
   - $15K quarterly budget across 5 markets
   - 200+ new leads/month target
   - ROI: $2M expected revenue = 133x return

3. **Click into "Instagram Luxury Showcase"** — show:
   - Weekly Reels with professional video tours
   - Target: $2M+ listings
   - 50K impressions/week goal

4. **Click into "Investor Networking Mixer"** — show:
   - Exclusive event format, 50 attendees max
   - $10M expected deal pipeline from one evening

### 💡 What to Highlight

> "Every marketing dollar is tracked from campaign spend to closed deal. When Tyler Brooks converts from a Zillow lead to a $1.35M purchase, Rachel can see the exact ROI of her Zillow Premier Agent investment."

---

## Part 6: Service Cases — Transaction Issues

### 🎯 What to Show

> "Real estate transactions are complex. Inspection disputes, title issues, appraisal gaps — all tracked as cases with full context and priority."

### Demo Steps

1. **Navigate to Cases** — show 6 real estate-specific cases:

   | Case | Priority | Status |
   |------|----------|--------|
   | Home inspection contingency dispute | HIGH | OPEN |
   | HOA document review delay | MEDIUM | OPEN |
   | Title search issue – Austin portfolio | HIGH | OPEN |
   | Listing photo complaint | LOW | OPEN |
   | Appraisal came in low – Sunrise Meadows | CRITICAL | OPEN |
   | Lead routing failure – Zillow leads | HIGH | OPEN |

2. **Click into "Appraisal came in low"** (CRITICAL):
   - $45K gap between appraisal ($680K) and offer ($725K)
   - Linked to contact Marcus Williams and Sunrise Meadows account
   - Show the resolution options: renegotiate, seller credit, or buyer cash

3. **Click into "Home inspection contingency dispute"**:
   - Foundation micro-cracks found at Beverly Hills property
   - Buyer and seller disagree on severity
   - Contingency deadline tracked

### 💡 What to Highlight

> "Every transaction issue is logged with full context — which deal, which contact, what's the deadline. No more email threads buried in inboxes. The broker sees all open issues across the brokerage and can intervene before deadlines pass."

---

## Part 7: Reports & Analytics

### 🎯 What to Show

1. **Navigate to Reports**
2. Show the key real estate metrics:
   - **Pipeline by stage:** How much value at each step
   - **Agent performance:** Deals per agent, conversion rates
   - **Lead source ROI:** Which portals generate the most closings
   - **Days in stage:** How long deals sit at each pipeline step
   - **Forecast:** Expected closings for next 30/60/90 days

3. **Navigate to Dashboard** for executive-level view:
   - Total pipeline: ~$47M
   - Deals in negotiation: 3 worth $13.9M
   - Win rate trending
   - This month's closings

### 💡 What to Highlight

> "Rachel can see that Zillow generates the most leads, but referrals have the highest close rate. That data drives her marketing budget decisions for next quarter."

---

## Part 8: AI-Powered Insights

### 🎯 What to Show

1. **Navigate to AI Insights**
2. Show how AI can:
   - **Predict deal probability** — "Zhang Beverly Hills has 75% close probability based on engagement patterns"
   - **Recommend next actions** — "Call Jennifer about Skyline Tower concerns before competitor One Park Tower schedules another showing"
   - **Summarize deal notes** — Instant summary of all interactions with a buyer
   - **Score leads** — Which portal leads are most likely to convert

3. **Navigate to AI Config** and show the prompt customization:
   > Example: "You are a real estate sales assistant for Premier Realty Group. Help agents prioritize leads by budget, timeline, and engagement level. Suggest properties from our listings that match buyer preferences."

### 💡 What to Highlight

> "AI turns raw CRM data into actionable coaching. Instead of Rachel reviewing 15 deals manually, AI highlights the 3 that need immediate attention — like Amanda's 1031 exchange deadline."

---

## Competitive Differentiators

### vs. Zillow Premier Agent CRM

| Feature | Zillow CRM | Our Platform |
|---------|-----------|--------------|
| Lead sources | Zillow only | All portals + social + referrals |
| Pipeline stages | Basic | Full: Inquiry → Viewing → Offer → Negotiation → Closed |
| Workflow automation | None | 6+ automated workflows |
| AI insights | None | Predictive analytics + recommendations |
| Team management | Single agent | Full brokerage (multi-agent) |
| Service cases | None | Full transaction issue tracking |

### vs. Follow Up Boss

| Feature | Follow Up Boss | Our Platform |
|---------|---------------|--------------|
| Contact property prefs | Tags only | Rich descriptions with budget, style, timeline |
| Marketing campaigns | Basic drip | Multi-channel (events, ads, social, email, webinars) |
| Commercial real estate | No | Full support |
| Competitor tracking | No | Per-deal competitive intel |
| Reports | Basic | Full analytics + forecasting |

### vs. kvCORE / BoomTown

| Feature | kvCORE/BoomTown | Our Platform |
|---------|----------------|--------------|
| Pricing | $500-$1500/mo | Flat platform fee |
| Customization | Limited | Fully configurable workflows |
| AI capabilities | Chatbot only | Full AI insights + predictions |
| API access | Limited | Full REST API |
| Multi-market | Difficult | Built for multi-market brokerages |

---

## ROI & Business Impact

### Speed-to-Lead Improvement

| Metric | Before CRM | After CRM |
|--------|-----------|-----------|
| Avg lead response time | 4.2 hours | < 5 minutes |
| Lead follow-up rate | 62% | 100% |
| Portal lead conversion | 2.1% | 5.8% |
| Revenue per lead | $1,200 | $3,400 |

### Agent Productivity

| Metric | Before | After |
|--------|--------|-------|
| Deals managed per agent | 8-10 | 15-20 |
| Admin time per deal | 6 hrs/week | 2 hrs/week |
| Time spent on data entry | 35% | 10% |
| Client satisfaction (NPS) | 42 | 78 |

### Brokerage Performance

| Metric | Impact |
|--------|--------|
| Pipeline visibility | 100% (vs. 40% before) |
| Revenue forecast accuracy | ±8% (vs. ±25% before) |
| Agent retention | +30% (better tools = happier agents) |
| Commission per agent | +22% (more deals closed faster) |

---

## Demo Talk Track — 30-Minute Version

| Time | Section | Key Points |
|------|---------|------------|
| 0:00-2:00 | **Opening** | "Premier Realty gets 200+ leads/month from 6 sources. 38% of leads go unanswered. They need a unified system." |
| 2:00-7:00 | **Lead Capture** | Show leads from Zillow, Facebook, referrals. Filter by source. Show behavioral data. |
| 7:00-14:00 | **Pipeline** | Walk through deals at each stage. Show property details, notes, competitors on Beverly Hills deal. Show the closed Rivera flip. |
| 14:00-18:00 | **Contacts** | Show buyer profiles with preferences. Show investor vs. first-timer vs. seller variations. |
| 18:00-23:00 | **Automation** | Show post-viewing workflow. Explain the 24hr follow-up guarantee. Show lead auto-assign. |
| 23:00-26:00 | **Campaigns** | Show Zillow ROI, Instagram strategy, upcoming events. |
| 26:00-28:00 | **Cases & Reports** | Show appraisal gap case. Show pipeline dashboard. |
| 28:00-30:00 | **Close** | ROI numbers. Competitive comparison. "With this platform, no lead goes unanswered, no viewing goes unfollowed, and no deal falls through the cracks." |

---

## Quick Reference — Key Data Points for Demo

| Metric | Value |
|--------|-------|
| Total Pipeline | ~$47.1M |
| Deals in Negotiation | 3 ($13.9M) |
| Largest Deal | $12.5M (Bel Air Estate) |
| Closed This Month | 1 ($650K flip) |
| Active Leads | 15 from 5 sources |
| Contacts with Preferences | 12 buyers/sellers/investors |
| Automated Workflows | 6 |
| Marketing Budget | $44.2K across 8 campaigns |
| Expected Campaign Revenue | $24M |
| Markets Covered | LA, Miami, San Diego, Austin, NYC, Phoenix |

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Seed script fails to auth | Ensure auth-service is running: `docker compose up -d auth-service` |
| No data appears after seeding | Login as `rachel.morgan@premierrealty.com` (not the default user) |
| Activities not showing | Check activity-service: `docker compose logs activity-service --tail=20` |
| Workflows not triggering | Workflows fire on future events — create a test lead to trigger "New Portal Lead" workflow |

---

*Document generated for Premier Realty Group CRM demo. All data is fictional.*
