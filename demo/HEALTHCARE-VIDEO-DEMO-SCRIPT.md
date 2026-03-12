# Healthcare Clinic CRM — Video Demo Script

> **Total Duration**: ~12–15 minutes
> **Presenter**: Narrator with screen recording
> **App URL**: http://localhost:3000
> **Resolution**: 1920 × 1080 (Full HD)
> **Use Case**: Configure a complete Healthcare Clinic CRM using AI — zero manual setup

---

## Pre-Recording Checklist

- [ ] All services running: `docker compose up -d`
- [ ] AI Agent running: `docker compose logs ai-agent --tail 5` (confirm healthy)
- [ ] Browser: Chrome (clean profile, no extensions bar)
- [ ] Screen recording tool ready (OBS / Camtasia / ScreenPal)
- [ ] Microphone tested
- [ ] Browser zoom: 100%
- [ ] Close all notifications / popups
- [ ] Clear any previous AI config history (optional — fresh demo looks cleaner)

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
> "Today, we're going to do something remarkable — configure an entire Healthcare Clinic CRM in under 15 minutes, using nothing but plain English.
>
> No consultants. No schema designers. No weeks of implementation. Just natural language AI that understands your business and sets up everything automatically.
>
> Let's log in and get started."

---

## SCENE 2 — Navigate to AI Config (0:45 – 1:15)

### Screen Actions
1. Briefly show the Dashboard — point to the sidebar navigation
2. Click **AI Config** in the sidebar
3. The AI chat interface loads — show the clean, empty chat area
4. Pause on the screen for 2 seconds

### Narration
> "This is the AI Configuration page — it's a simple chat interface. You type what you need in plain English, the AI parses your intent, generates a structured configuration, and asks for your confirmation before executing.
>
> Let's build a healthcare clinic from scratch. We'll start with the data model."

---

## SCENE 3 — Create Patients Object (1:15 – 2:30)

### Screen Actions
1. Click the chat input field
2. Type slowly (or paste):
   ```
   Create a custom object called Patients with fields: full_name (text, required), date_of_birth (date), phone (phone), email (email), insurance_provider (text), blood_type (picklist with options A+, A-, B+, B-, AB+, AB-, O+, O-)
   ```
3. Press Enter
4. Wait for AI response — show the confirmation card with all fields listed
5. **Pause** — hover over each field to show the AI understood types correctly
6. Click **Confirm**
7. Show the success message

### Narration
> "Our first object is Patients. Watch what happens — I'm typing a single sentence describing the fields I need: full name, date of birth, phone, email, insurance provider, and blood type as a dropdown.
>
> The AI parses everything. It detects field types — phone fields, email fields, dates, and even creates a picklist with all eight blood type options. The full name field is correctly marked as required.
>
> Let me confirm. And just like that — the Patients object is created with six fields. In a traditional CRM, this would have been five minutes of clicking through a schema builder."

### On-Screen Callouts (Post-Production)
- Arrow pointing to field types: "Auto-detected: phone, email, date, picklist"
- Highlight on `required: true`: "Required flag parsed from natural language"
- Highlight on picklist options: "8 blood type options created automatically"

---

## SCENE 4 — Create Doctors Object (2:30 – 3:30)

### Screen Actions
1. Type in the chat:
   ```
   Create a custom object called Doctors with fields: doctor_name (text, required), specialization (text), phone (phone), email (email), license_number (text, unique), available_days (multi_picklist with options Monday, Tuesday, Wednesday, Thursday, Friday, Saturday)
   ```
2. Press Enter
3. Show the confirmation card — highlight the multi_picklist and unique constraint
4. Click **Confirm**
5. Show success

### Narration
> "Next, the Doctors object. This one has a few advanced features — a unique constraint on the license number so no duplicates are allowed, and a multi-select picklist for available days, so each doctor can select multiple days they're in the clinic.
>
> The AI handles both of these from a single sentence. Confirmed."

### On-Screen Callouts
- Arrow on `unique: true`: "Unique constraint — prevents duplicate license numbers"
- Arrow on `multi_picklist`: "Multi-select — doctors pick multiple available days"

---

## SCENE 5 — Create Appointments Object (3:30 – 4:30)

### Screen Actions
1. Type in the chat:
   ```
   Create a custom object called Appointments with fields: appointment_date (datetime, required), patient_name (text, required), doctor_name (text, required), department (text), status (picklist with options Scheduled, In Progress, Completed, Cancelled, No-Show), notes (textarea)
   ```
2. Press Enter
3. Show confirmation with the 5 status values
4. Click **Confirm**

### Narration
> "Appointments are the core of any clinic. We need a datetime for scheduling, patient and doctor names, a department field, and a status dropdown that covers the complete appointment lifecycle — from Scheduled all the way through to No-Show.
>
> The notes field is a textarea for longer clinical observations. One sentence, six fields, confirmed."

### On-Screen Callouts
- Highlight status picklist: "Full lifecycle: Scheduled → In Progress → Completed / Cancelled / No-Show"
- Arrow on `datetime`: "Datetime — captures both date and time for precise scheduling"

---

## SCENE 6 — Create Medical Records Object (4:30 – 5:30)

### Screen Actions
1. Type in the chat:
   ```
   Create a custom object called Medical Records with fields: record_date (date, required), patient_name (text, required), doctor_name (text, required), diagnosis (textarea), prescription (textarea), follow_up_date (date), record_type (picklist with options Consultation, Lab Test, Imaging, Procedure, Follow-up)
   ```
2. Press Enter
3. Show confirmation — highlight the textarea fields for clinical data
4. Click **Confirm**

### Narration
> "Medical Records need space for detailed clinical information. The diagnosis and prescription fields are textareas — long-form text fields that can hold detailed clinical notes.
>
> We also have a follow-up date for scheduling return visits, and a record type picklist to categorize each record — whether it's a consultation, lab test, imaging, procedure, or follow-up.
>
> Confirmed. That's four objects done."

---

## SCENE 7 — Create Billing Object (5:30 – 6:30)

### Screen Actions
1. Type in the chat:
   ```
   Create a custom object called Billing with fields: patient_name (text, required), appointment_date (date, required), service_description (text), amount (currency, required), payment_status (picklist with options Pending, Paid, Partially Paid, Overdue, Waived), insurance_claim_id (text), payment_method (picklist with options Cash, Card, Insurance, Online)
   ```
2. Press Enter
3. Show confirmation — highlight the currency field and dual picklists
4. Click **Confirm**

### Narration
> "The last object is Billing. This one has a currency field that automatically formats monetary values. Two picklists track payment status and payment method — including Insurance as a payment type with a claim ID field for insurance tracking.
>
> And with that — five custom objects, over 30 fields, our complete healthcare data model is done. All from five sentences."

### On-Screen Callouts
- Counter overlay: "✅ 5 Objects Created | 30+ Fields | ~5 minutes"

---

## SCENE 8 — Transition to Workflows (6:30 – 6:45)

### Screen Actions
1. Pause on the chat showing the 5 successful creations
2. Slow scroll to show the conversation history

### Narration
> "Now that our data model is in place, let's add intelligence — automated workflows that eliminate manual tasks for the clinic staff."

---

## SCENE 9 — Workflow: SMS Reminder (6:45 – 7:45)

### Screen Actions
1. Type in the chat:
   ```
   Create a workflow: When a new appointment is created, send an SMS reminder to the patient with the appointment date and doctor name
   ```
2. Press Enter
3. Show confirmation — highlight the trigger type (`on_create`) and SMS webhook action
4. Click **Confirm**

### Narration
> "First automation — appointment reminders. No-shows are one of the biggest revenue losses for clinics. This workflow fires the moment an appointment is booked.
>
> The AI creates an `on_create` trigger on the Appointments object and configures a webhook action that sends an SMS with the appointment date and doctor name. All automatic.
>
> Confirmed. Every new booking now triggers a patient reminder — zero manual effort."

### On-Screen Callouts
- Arrow on trigger: "Trigger: on_create — fires immediately on booking"
- Arrow on action: "Action: SMS webhook — sends patient reminder"

---

## SCENE 10 — Workflow: Auto-Billing (7:45 – 8:45)

### Screen Actions
1. Type in the chat:
   ```
   Create a workflow: When an appointment status changes to Completed, automatically create a billing record for the patient
   ```
2. Press Enter
3. Show confirmation — highlight the field change trigger and create action
4. Click **Confirm**

### Narration
> "Second automation — auto-billing. When a doctor marks an appointment as Completed, the system automatically generates a billing record. No more manual data entry for the billing department.
>
> The AI detects this as a field-change trigger on the status field, with a cascading action that creates a record in the Billing object. One event creates another.
>
> Confirmed. The clinic just saved hours of daily admin work."

### On-Screen Callouts
- Flow diagram overlay: "Appointment Completed → Billing Record Created → Payment Tracking Begins"

---

## SCENE 11 — Workflow: Doctor Assignment (8:45 – 9:30)

### Screen Actions
1. Type in the chat:
   ```
   Create a workflow: When a new patient is registered, create a task to assign a primary care doctor within 24 hours
   ```
2. Press Enter
3. Show confirmation — highlight the task creation action
4. Click **Confirm**

### Narration
> "Third automation — when a new patient registers, the system creates a task for clinic staff to assign a primary care doctor within 24 hours. Nothing falls through the cracks.
>
> Confirmed. Three workflows, three critical clinic operations automated."

### On-Screen Callouts
- Counter overlay: "✅ 3 Workflows Active | SMS + Billing + Assignment"

---

## SCENE 12 — Transition to Dashboards (9:30 – 9:45)

### Screen Actions
1. Brief pause on the chat

### Narration
> "Data and automation are in place. Now let's give the clinic manager real-time visibility with AI-generated dashboards."

---

## SCENE 13 — Dashboard: Clinic Operations (9:45 – 11:00)

### Screen Actions
1. Type in the chat:
   ```
   Create a dashboard called Clinic Operations with widgets: Today's Appointments (metric showing count of today's appointments), Doctor Utilization (bar chart showing appointments per doctor), Patient Visits per Month (line chart showing monthly patient visit trends), Appointment Status Breakdown (pie chart showing scheduled vs completed vs cancelled)
   ```
2. Press Enter
3. Show confirmation — highlight the 4 widgets with their types and grid positions
4. Slowly scroll through the confirmation to show each widget's configuration
5. Click **Confirm**

### Narration
> "Watch this — from one sentence, the AI creates a complete dashboard with four widgets. A metric card for today's appointment count, a bar chart for doctor utilization, a line chart for monthly patient trends, and a pie chart for appointment status breakdown.
>
> Each widget is automatically positioned in a grid layout. The AI even selects the right chart type for each metric — metrics for counts, bar charts for comparisons, line charts for trends, and pie charts for distributions.
>
> Confirmed. The clinic manager now has a command center."

### On-Screen Callouts
- Labels on each widget card: "Metric | Bar Chart | Line Chart | Pie Chart"
- Arrow on position values: "Auto-positioned in responsive grid"

---

## SCENE 14 — Dashboard: Billing Overview (11:00 – 11:45)

### Screen Actions
1. Type in the chat:
   ```
   Create a dashboard called Billing Overview with widgets: Monthly Revenue (metric showing total billing amount), Payment Status Distribution (pie chart of pending vs paid vs overdue), Revenue by Payment Method (bar chart by cash, card, insurance, online)
   ```
2. Press Enter
3. Show confirmation with 3 widgets
4. Click **Confirm**

### Narration
> "A second dashboard for financial visibility. Monthly revenue at a glance, payment status distribution to spot overdue accounts, and revenue breakdown by payment method — so the clinic knows how much comes from insurance versus direct payments.
>
> Confirmed. Two dashboards, seven widgets — built from two sentences."

---

## SCENE 15 — Roles & Permissions (11:45 – 13:00)

### Screen Actions
1. Type in the chat:
   ```
   Create a role called Doctor with permissions: can read and update Patients, can read and create Appointments, can read and create Medical Records, cannot access Billing
   ```
2. Press Enter
3. Show confirmation — highlight the per-object permission matrix
4. Click **Confirm**
5. Then type:
   ```
   Create a role called Front Desk with permissions: can create and read Patients, can create read and update Appointments, can read Billing, cannot access Medical Records
   ```
6. Press Enter
7. Show confirmation
8. Click **Confirm**

### Narration
> "Security and compliance are critical in healthcare. Let's set up role-based access control — again, using plain English.
>
> The Doctor role can view and update patient records, manage appointments and medical records, but cannot access billing data. This enforces a clean separation of clinical and financial information.
>
> The Front Desk role is the opposite — they manage scheduling and patient intake, can view billing, but have no access to medical records. Patient clinical data stays private.
>
> This is HIPAA-friendly access control, configured in two sentences."

### On-Screen Callouts
- Permission matrix overlay:
  ```
  | Object          | Doctor      | Front Desk  |
  |-----------------|-------------|-------------|
  | Patients        | Read/Update | Create/Read |
  | Appointments    | Read/Create | Full Access |
  | Medical Records | Read/Create | No Access   |
  | Billing         | No Access   | Read Only   |
  ```

---

## SCENE 16 — Recap & Summary (13:00 – 14:00)

### Screen Actions
1. Slowly scroll up through the entire chat conversation to show all 12 commands
2. Pause at the top

### Narration
> "Let's recap what we just built. In under 15 minutes, using only natural language:
>
> **Five custom objects** — Patients, Doctors, Appointments, Medical Records, and Billing — with over 30 typed fields including picklists, multi-selects, currency, and unique constraints.
>
> **Three automated workflows** — SMS appointment reminders, auto-generated billing records, and new-patient doctor assignment tasks.
>
> **Two dashboards** with seven widgets — real-time clinic operations and financial overview.
>
> **Two roles** with object-level permissions — HIPAA-friendly access control for Doctors and Front Desk staff."

### On-Screen Callouts
- Summary overlay:
  ```
  ✅ 5 Custom Objects    | 30+ Fields
  ✅ 3 Workflows         | SMS + Billing + Assignment
  ✅ 2 Dashboards        | 7 Widgets
  ✅ 2 Roles             | Object-Level Permissions
  ⏱️ Total Time          | < 15 Minutes
  ```

---

## SCENE 17 — Closing & Call to Action (14:00 – 14:45)

### Screen Actions
1. Return to the Dashboard page
2. Zoom out to show the full application
3. Fade to closing slide

### Narration
> "This is the power of Zero-Config AI. Whether you're a healthcare clinic, a law firm, a real estate agency, or any specialized business — you describe what you need, and the CRM configures itself.
>
> No implementation consultants. No weeks of setup. No technical expertise required.
>
> Traditional CRM setup — 2 to 4 weeks and thousands of dollars. Zero-Config AI — 15 minutes, included in the platform.
>
> Ready to see how it works for your business? Let's talk."

### Closing Slide (Post-Production)
```
┌─────────────────────────────────────┐
│                                     │
│    Zero-Config AI CRM Platform      │
│                                     │
│    "Describe your business.         │
│     We'll build the CRM."          │
│                                     │
│    🌐 www.yourcrm.com              │
│    📧 sales@yourcrm.com            │
│                                     │
└─────────────────────────────────────┘
```

---

## Post-Production Notes

### Text Overlays to Add

| Timestamp | Overlay |
|-----------|---------|
| 0:00 | Title card: "Healthcare Clinic CRM — Zero-Config AI Demo" |
| 1:15 | Section title: "Part 1 — Building the Data Model" |
| 2:30 | Object counter: "Object 2 of 5: Doctors" |
| 3:30 | Object counter: "Object 3 of 5: Appointments" |
| 4:30 | Object counter: "Object 4 of 5: Medical Records" |
| 5:30 | Object counter: "Object 5 of 5: Billing" |
| 6:30 | Milestone: "✅ Data Model Complete — 5 Objects, 30+ Fields" |
| 6:45 | Section title: "Part 2 — Automating Clinic Operations" |
| 9:30 | Milestone: "✅ 3 Workflows Active" |
| 9:45 | Section title: "Part 3 — Real-Time Dashboards" |
| 11:45 | Section title: "Part 4 — Security & Compliance" |
| 13:00 | Section title: "Summary" |
| 14:00 | Closing card |

### Background Music
- Upbeat, corporate/tech feel
- Low volume during narration, normal during transitions
- Suggested: Artlist or Epidemic Sound — search "tech product demo"

### Transitions
- Simple fade cuts between scenes (0.5s)
- No flashy animations — keep it professional
- Use subtle zoom on key UI elements to draw attention

---

## Recording Tips

| Tip | Detail |
|-----|--------|
| **Typing speed** | Type at a natural, readable pace — not too fast |
| **Mouse movement** | Move slowly and deliberately; hover on key items |
| **Pauses** | 2–3 seconds after each AI response before clicking Confirm |
| **Wait for response** | AI may take 2–5 seconds to respond — pause narration during wait |
| **Scrolling** | Slow, smooth scrolling through confirmation cards |
| **Click timing** | Wait for animations to complete before proceeding |
| **Audio** | Record narration separately with Audacity — sync in post |
| **B-roll** | Record extra footage of each step for editing flexibility |
| **Retakes** | Better to re-record a scene than fix in post |

---

## Recommended Tools

| Tool | Purpose | Cost |
|------|---------|------|
| **OBS Studio** | Screen recording | Free |
| **Camtasia** | Recording + editing | Paid |
| **ScreenPal** | Quick recordings | Freemium |
| **DaVinci Resolve** | Professional editing | Free |
| **Audacity** | Audio recording/editing | Free |
| **Canva** | Intro/outro slides, overlays | Freemium |

---

## Quick Reference — All 12 Prompts

Copy-paste these in order during recording:

```
1. Create a custom object called Patients with fields: full_name (text, required), date_of_birth (date), phone (phone), email (email), insurance_provider (text), blood_type (picklist with options A+, A-, B+, B-, AB+, AB-, O+, O-)

2. Create a custom object called Doctors with fields: doctor_name (text, required), specialization (text), phone (phone), email (email), license_number (text, unique), available_days (multi_picklist with options Monday, Tuesday, Wednesday, Thursday, Friday, Saturday)

3. Create a custom object called Appointments with fields: appointment_date (datetime, required), patient_name (text, required), doctor_name (text, required), department (text), status (picklist with options Scheduled, In Progress, Completed, Cancelled, No-Show), notes (textarea)

4. Create a custom object called Medical Records with fields: record_date (date, required), patient_name (text, required), doctor_name (text, required), diagnosis (textarea), prescription (textarea), follow_up_date (date), record_type (picklist with options Consultation, Lab Test, Imaging, Procedure, Follow-up)

5. Create a custom object called Billing with fields: patient_name (text, required), appointment_date (date, required), service_description (text), amount (currency, required), payment_status (picklist with options Pending, Paid, Partially Paid, Overdue, Waived), insurance_claim_id (text), payment_method (picklist with options Cash, Card, Insurance, Online)

6. Create a workflow: When a new appointment is created, send an SMS reminder to the patient with the appointment date and doctor name

7. Create a workflow: When an appointment status changes to Completed, automatically create a billing record for the patient

8. Create a workflow: When a new patient is registered, create a task to assign a primary care doctor within 24 hours

9. Create a dashboard called Clinic Operations with widgets: Today's Appointments (metric showing count of today's appointments), Doctor Utilization (bar chart showing appointments per doctor), Patient Visits per Month (line chart showing monthly patient visit trends), Appointment Status Breakdown (pie chart showing scheduled vs completed vs cancelled)

10. Create a dashboard called Billing Overview with widgets: Monthly Revenue (metric showing total billing amount), Payment Status Distribution (pie chart of pending vs paid vs overdue), Revenue by Payment Method (bar chart by cash, card, insurance, online)

11. Create a role called Doctor with permissions: can read and update Patients, can read and create Appointments, can read and create Medical Records, cannot access Billing

12. Create a role called Front Desk with permissions: can create and read Patients, can create read and update Appointments, can read Billing, cannot access Medical Records
```
