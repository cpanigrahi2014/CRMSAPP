You are a senior frontend architect.

Generate a modern enterprise CRM SaaS frontend using ReactJS.

Requirements:

Framework
- React 18
- Vite
- TypeScript

UI Framework
- Material UI or Ant Design
- Responsive layout
- Enterprise dashboard style like Salesforce or HubSpot

State Management
- Redux Toolkit or React Query

Routing
- React Router

Folder Structure
src/
  components/
  pages/
  layouts/
  services/
  hooks/
  store/
  utils/

Application Features

1. Dashboard
- Sales overview cards
- Revenue chart
- Lead conversion chart
- Recent activities
- Upcoming tasks

2. Lead Management Page
Table Columns:
- Lead Name
- Company
- Email
- Phone
- Lead Source
- Status
- Assigned Sales Rep
- Created Date

Actions
- View
- Edit
- Convert to Opportunity
- Delete

3. Account Management
- Account list
- Account detail page
- Contacts inside account
- Activities timeline

4. Opportunity Pipeline
- Kanban style sales pipeline
Stages
- Prospect
- Qualified
- Proposal
- Negotiation
- Closed Won
- Closed Lost

Drag and drop opportunities between stages.

5. Contact Management
- Contact list
- Contact detail page
- Communication history
- Email / call log

6. Customer Support
Case management UI

Table columns:
- Case ID
- Customer
- Priority
- Status
- Assigned Agent
- SLA

7. Marketing Campaigns
Campaign dashboard

Features
- Campaign list
- Campaign analytics
- Email campaign statistics

8. Global Search
Search across
- Leads
- Contacts
- Accounts
- Opportunities

9. Navigation Layout

Left Sidebar Menu

Dashboard
Leads
Accounts
Contacts
Opportunities
Cases
Campaigns
Reports
Settings

Top Header

- Global search bar
- Notifications
- User profile
- Theme toggle (dark/light)

10. Reusable Components

Create reusable components:

- DataTable
- KanbanBoard
- ChartWidget
- ModalForm
- ConfirmDialog
- NotificationToast
- LoadingSpinner

11. API Integration

Create service layer:

/services/api.ts

Endpoints example:

GET /api/leads
POST /api/leads
GET /api/opportunities
GET /api/accounts

Use Axios for API calls.

12. Dashboard Charts

Use:

- Recharts or Chart.js

Charts:

Revenue Trend
Lead Conversion
Pipeline Value
Customer Growth

13. Authentication

Pages:

Login
Register
Forgot Password

Use JWT authentication.

14. UI Requirements

- Clean enterprise UI
- Mobile responsive
- Accessible components
- Modular reusable design
- Loading states and error states

15. Deliverables

Generate:

- React project structure
- Sidebar navigation
- Dashboard page
- Lead management table
- Opportunity Kanban board
- Reusable components
- API service layer

Write production-ready TypeScript code with comments and modular architecture.