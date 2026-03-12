# Healthcare Clinic CRM — Customer Demo Guide

> **Use Case**: Configure a complete CRM for a healthcare clinic using natural language AI — zero manual configuration required.
>
> **Demo Duration**: ~15 minutes
>
> **Audience**: Healthcare clinic administrators, IT decision-makers, product evaluators

---

## Pre-Demo Setup

1. **All services running**: `docker compose up -d`
2. **App URL**: [http://localhost:3000](http://localhost:3000) (or ngrok public URL)
3. **Login**: `sarah.chen@acmecorp.com` / `Demo@2026!` / Tenant: `default`
4. Ensure the **AI Config** page is accessible from the sidebar

---

## Demo Script

### Opening — The Problem Statement (1 min)

> *"Most CRM platforms require days or weeks of manual configuration — defining data models, building automations, creating dashboards. What if you could set up an entire healthcare clinic CRM in under 15 minutes, just by describing what you need in plain English?"*

---

### Part 1 — Create Custom Objects (5 min)

Navigate to **AI Config** page from the sidebar.

#### Step 1.1 — Patients Object

Type in the chat:

```
Create a custom object called Patients with fields:
full_name (text, required), date_of_birth (date), phone (phone),
email (email), insurance_provider (text),
blood_type (picklist with options A+, A-, B+, B-, AB+, AB-, O+, O-)
```

**What to highlight to customer:**
- AI understands natural language — no forms, no drag-and-drop builders
- Field types are auto-detected (phone, email, date, picklist)
- Picklist values are parsed from the sentence
- Required fields are marked correctly
- Review the confirmation card → Click **Confirm**

> *"In one sentence, we've created a Patients table with 6 typed fields, including a blood type dropdown. No schema designer needed."*

#### Step 1.2 — Doctors Object

```
Create a custom object called Doctors with fields:
doctor_name (text, required), specialization (text), phone (phone),
email (email), license_number (text, unique),
available_days (multi_picklist with options Monday, Tuesday, Wednesday, Thursday, Friday, Saturday)
```

**What to highlight:**
- Multi-picklist for multi-select values (available days)
- Unique constraint on `license_number`
- Click **Confirm**

#### Step 1.3 — Appointments Object

```
Create a custom object called Appointments with fields:
appointment_date (datetime, required), patient_name (text, required),
doctor_name (text, required), department (text),
status (picklist with options Scheduled, In Progress, Completed, Cancelled, No-Show),
notes (textarea)
```

**What to highlight:**
- Status picklist covers the full appointment lifecycle
- Datetime field for precise scheduling
- Click **Confirm**

#### Step 1.4 — Medical Records Object

```
Create a custom object called Medical Records with fields:
record_date (date, required), patient_name (text, required),
doctor_name (text, required), diagnosis (textarea),
prescription (textarea), follow_up_date (date),
record_type (picklist with options Consultation, Lab Test, Imaging, Procedure, Follow-up)
```

**What to highlight:**
- Textarea fields for long-form clinical notes
- Follow-up date for automatic scheduling potential
- Click **Confirm**

#### Step 1.5 — Billing Object

```
Create a custom object called Billing with fields:
patient_name (text, required), appointment_date (date, required),
service_description (text), amount (currency, required),
payment_status (picklist with options Pending, Paid, Partially Paid, Overdue, Waived),
insurance_claim_id (text), payment_method (picklist with options Cash, Card, Insurance, Online)
```

**What to highlight:**
- Currency field auto-formats money values
- Insurance claim tracking built in
- Click **Confirm**

> *"In under 3 minutes, we've created 5 custom objects with 30+ fields — a complete healthcare data model. Traditional CRM setup would take hours."*

---

### Part 2 — Create Automated Workflows (4 min)

#### Step 2.1 — SMS Reminder on Appointment Booking

```
Create a workflow: When a new appointment is created, send an SMS reminder
to the patient with the appointment date and doctor name
```

**What to highlight:**
- AI creates trigger (`on_create`), targets the Appointments object
- Generates SMS webhook action with dynamic fields
- Click **Confirm**

> *"No-shows cost clinics real money. This automation fires the moment a booking is made — zero manual effort."*

#### Step 2.2 — Auto-Generate Billing Record

```
Create a workflow: When an appointment status changes to Completed,
automatically create a billing record for the patient
```

**What to highlight:**
- Field change trigger on `status` field
- Cascading action — one event creates another record
- Click **Confirm**

> *"The billing department never has to manually enter completed appointments. The system does it."*

#### Step 2.3 — Auto-Assign Primary Doctor on Registration

```
Create a workflow: When a new patient is registered, create a task
to assign a primary care doctor within 24 hours
```

**What to highlight:**
- `on_create` trigger on Patients object
- Creates a follow-up task (not just a notification)
- Click **Confirm**

> *"Every new patient gets a primary doctor assignment task — nothing falls through the cracks."*

---

### Part 3 — Create Dashboards (3 min)

#### Step 3.1 — Clinic Operations Dashboard

```
Create a dashboard called Clinic Operations with widgets:
Today's Appointments (metric showing count of today's appointments),
Doctor Utilization (bar chart showing appointments per doctor),
Patient Visits per Month (line chart showing monthly patient visit trends),
Appointment Status Breakdown (pie chart showing scheduled vs completed vs cancelled)
```

**What to highlight:**
- 4 widgets created from one sentence
- Mix of metric, bar chart, line chart, and pie chart
- Auto-positioned in a grid layout
- Click **Confirm**

> *"One sentence, four widgets, a complete operational dashboard. The clinic manager can see everything at a glance."*

#### Step 3.2 — Billing Dashboard (Optional)

```
Create a dashboard called Billing Overview with widgets:
Monthly Revenue (metric showing total billing amount),
Payment Status Distribution (pie chart of pending vs paid vs overdue),
Revenue by Payment Method (bar chart by cash, card, insurance, online)
```

**What to highlight:**
- Financial visibility out of the box
- Insurance vs direct payment tracking
- Click **Confirm**

---

### Part 4 — Set Up Roles & Permissions (2 min)

#### Step 4.1 — Doctor Role

```
Create a role called Doctor with permissions:
can read and update Patients, can read and create Appointments,
can read and create Medical Records, cannot access Billing
```

**What to highlight:**
- Role-based access control from plain English
- Doctors can see patients and appointments but not billing data
- HIPAA-friendly permission model
- Click **Confirm**

#### Step 4.2 — Front Desk Role

```
Create a role called Front Desk with permissions:
can create and read Patients, can create read and update Appointments,
can read Billing, cannot access Medical Records
```

**What to highlight:**
- Front desk can manage scheduling but not clinical records
- Separation of concerns enforced by the system
- Click **Confirm**

> *"Role-based access in two sentences — no admin panel needed. The AI understands who should see what."*

---

### Closing — Summary & Value Proposition (1 min)

> *"In under 15 minutes, with zero technical knowledge, we've configured:"*

| Component | Count | Details |
|-----------|-------|---------|
| **Custom Objects** | 5 | Patients, Doctors, Appointments, Medical Records, Billing |
| **Custom Fields** | 30+ | Text, date, phone, email, currency, picklists, textareas |
| **Workflows** | 3 | SMS reminders, auto-billing, doctor assignment |
| **Dashboards** | 2 | Clinic operations (4 widgets) + Billing overview (3 widgets) |
| **Roles** | 2 | Doctor, Front Desk — with object-level permissions |

> *"This is the power of Zero-Config AI. No consultants, no implementation team, no weeks of setup. Just describe your business in English and the CRM configures itself."*

---

## Full Prompt Sequence (Copy-Paste Ready)

For a fast demo, paste these prompts one by one in the **AI Config** chat, confirming each:

```
1. Create a custom object called Patients with fields: full_name (text, required), date_of_birth (date), phone (phone), email (email), insurance_provider (text), blood_type (picklist with options A+, A-, B+, B-, AB+, AB-, O+, O-)

2. Create a custom object called Doctors with fields: doctor_name (text, required), specialization (text), phone (phone), email (email), license_number (text, unique), available_days (multi_picklist with options Monday, Tuesday, Wednesday, Thursday, Friday, Saturday)

3. Create a custom object called Appointments with fields: appointment_date (datetime, required), patient_name (text, required), doctor_name (text, required), department (text), status (picklist with options Scheduled, In Progress, Completed, Cancelled, No-Show), notes (textarea)

4. Create a custom object called Medical Records with fields: record_date (date, required), patient_name (text, required), doctor_name (text, required), diagnosis (textarea), prescription (textarea), follow_up_date (date), record_type (picklist with options Consultation, Lab Test, Imaging, Procedure, Follow-up)

5. Create a custom object called Billing with fields: patient_name (text, required), appointment_date (date, required), service_description (text), amount (currency, required), payment_status (picklist with options Pending, Paid, Partially Paid, Overdue, Waived), insurance_claim_id (text), payment_method (picklist with options Cash, Card, Insurance, Online)

6. Create a workflow: When a new appointment is created, send an SMS reminder to the patient with the appointment date and doctor name

7. Create a workflow: When an appointment status changes to Completed, automatically create a billing record for the patient

8. Create a workflow: When a new patient is registered, create a task to assign a primary care doctor within 24 hours

9. Create a dashboard called Clinic Operations with widgets: Today's Appointments (metric showing count), Doctor Utilization (bar chart showing appointments per doctor), Patient Visits per Month (line chart showing monthly trend), Appointment Status Breakdown (pie chart by status)

10. Create a dashboard called Billing Overview with widgets: Monthly Revenue (metric showing total), Payment Status Distribution (pie chart), Revenue by Payment Method (bar chart)

11. Create a role called Doctor with permissions: can read and update Patients, can read and create Appointments, can read and create Medical Records, cannot access Billing

12. Create a role called Front Desk with permissions: can create and read Patients, can create read and update Appointments, can read Billing, cannot access Medical Records
```

---

## Key Talking Points for Customer

### Why Zero-Config AI?

| Traditional CRM Setup | Zero-Config AI Setup |
|----------------------|---------------------|
| Hire implementation consultant | Type in plain English |
| 2-4 weeks configuration | 15 minutes |
| Requires admin training | Anyone can do it |
| Rigid pre-built templates | Custom to your clinic |
| Extra cost for customization | Included in platform |

### Healthcare-Specific Value

- **HIPAA-Ready Permissions**: Role-based access separates clinical data from billing
- **Appointment Lifecycle**: Full status tracking from scheduled → completed → billed
- **No-Show Prevention**: Automated SMS reminders on booking
- **Billing Automation**: Completed appointments auto-generate billing records
- **Clinical Records**: Structured medical record objects with diagnosis and prescription tracking
- **Multi-Specialty Support**: Doctor specialization and availability management
- **Insurance Integration**: Claim ID tracking and insurance payment method support

### Scalable to Other Healthcare Use Cases

The same AI can configure:
- **Hospital CRM** — Wards, Bed Management, Staff Scheduling
- **Dental Clinic** — Treatment Plans, Insurance Claims, X-Ray Records
- **Mental Health Practice** — Session Notes, Treatment Goals, Patient Assessments
- **Pharmacy** — Inventory, Prescriptions, Supplier Management
- **Veterinary Clinic** — Pets, Owners, Vaccinations, Visit History

> *"The CRM adapts to you — not the other way around."*

---

## Troubleshooting

| Issue | Solution |
|-------|---------|
| AI Config page shows error | Check AI agent is running: `docker compose logs ai-agent --tail 20` |
| "Invalid or expired token" | Re-login to refresh the session |
| Object creation fails | Check PostgreSQL is running: `docker compose ps postgres` |
| Workflow shows warning | Warnings are informational — the workflow still gets created |
| Dashboard widgets empty | Widgets are configured but data needs to be populated first |
