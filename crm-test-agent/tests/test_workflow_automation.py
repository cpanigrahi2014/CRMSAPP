"""
Workflow Automation — 35 Tests
===============================
Service  : workflow-service (port 8088)
Groups   :
  1. Auto Assign Leads        (tests 01-07)
  2. Trigger Email Workflow    (tests 08-14)
  3. Conditional Automation    (tests 15-21)
  4. Escalation Rules          (tests 22-28)
  5. Scheduled Workflows       (tests 29-35)
"""

import uuid, pytest, requests
from datetime import datetime, timedelta

WF_BASE = "http://localhost:8088/api/v1/workflows"
AUTO_BASE = "http://localhost:8088/api/v1/automation"
AUTH_BASE = "http://localhost:8081/api/v1/auth"

_uid = lambda: uuid.uuid4().hex[:8]


# ── fixtures ────────────────────────────────────────────────────────────
@pytest.fixture(scope="module")
def auth_token():
    resp = requests.post(f"{AUTH_BASE}/login", json={
        "email": "sarah.chen@acmecorp.com",
        "password": "Demo@2026!",
        "tenantId": "default",
    })
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    d = resp.json().get("data", resp.json())
    return d.get("accessToken") or d.get("token")


@pytest.fixture(scope="module")
def headers(auth_token):
    return {"Authorization": f"Bearer {auth_token}", "Content-Type": "application/json"}


def _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                 conditions=None, actions=None, **overrides):
    """Helper: create a workflow rule."""
    body = {
        "name": f"WF_{_uid()}",
        "description": "Automated test workflow",
        "entityType": entity_type,
        "triggerEvent": trigger,
        "conditions": conditions or [],
        "actions": actions or [{
            "actionType": "SEND_NOTIFICATION",
            "targetField": "admin",
            "targetValue": "New entity created",
            "actionOrder": 0,
        }],
    }
    body.update(overrides)
    resp = requests.post(WF_BASE, headers=headers, json=body)
    assert resp.status_code in (200, 201), resp.text
    return resp.json().get("data", resp.json())


# ════════════════════════════════════════════════════════════════════════
#  Group 1 — Auto Assign Leads  (7 tests)
# ════════════════════════════════════════════════════════════════════════
class TestAutoAssignLeads:
    """Workflow rules that auto-assign leads using ASSIGN_TO action."""

    def test_01_create_lead_assign_rule(self, headers):
        """Create workflow: when LEAD CREATED → ASSIGN_TO rep."""
        rep_id = str(uuid.uuid4())
        rule = _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                            actions=[{
                                "actionType": "ASSIGN_TO",
                                "targetValue": rep_id,
                                "actionOrder": 0,
                            }])
        assert rule.get("id") is not None
        assert rule.get("entityType") == "LEAD"
        assert rule.get("triggerEvent") == "CREATED"

    def test_02_assign_rule_has_action(self, headers):
        """Verify ASSIGN_TO action is stored on the rule."""
        rep_id = str(uuid.uuid4())
        rule = _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                            actions=[{
                                "actionType": "ASSIGN_TO",
                                "targetValue": rep_id,
                                "actionOrder": 0,
                            }])
        actions = rule.get("actions", [])
        assert len(actions) >= 1
        assert any(a.get("actionType") == "ASSIGN_TO" for a in actions)

    def test_03_round_robin_assign(self, headers):
        """Create round-robin lead assignment rule."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                            actions=[{
                                "actionType": "ASSIGN_TO",
                                "targetValue": "round-robin",
                                "actionOrder": 0,
                            }])
        actions = rule.get("actions", [])
        assert any(a.get("targetValue") == "round-robin" for a in actions)

    def test_04_get_rule_by_id(self, headers):
        """GET /workflows/{id} retrieves a specific rule."""
        rule = _create_rule(headers)
        resp = requests.get(f"{WF_BASE}/{rule['id']}", headers=headers)
        assert resp.status_code == 200, resp.text
        fetched = resp.json().get("data", resp.json())
        assert fetched.get("id") == rule["id"]

    def test_05_list_lead_rules(self, headers):
        """GET /workflows/entity/LEAD lists lead-specific rules."""
        _create_rule(headers, entity_type="LEAD")
        resp = requests.get(f"{WF_BASE}/entity/LEAD", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_06_enable_rule(self, headers):
        """PATCH /workflows/{id}/enable activates rule."""
        rule = _create_rule(headers)
        resp = requests.patch(f"{WF_BASE}/{rule['id']}/enable", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("active") is True

    def test_07_disable_rule(self, headers):
        """PATCH /workflows/{id}/disable deactivates rule."""
        rule = _create_rule(headers)
        resp = requests.patch(f"{WF_BASE}/{rule['id']}/disable", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("active") is False


# ════════════════════════════════════════════════════════════════════════
#  Group 2 — Trigger Email Workflow  (7 tests)
# ════════════════════════════════════════════════════════════════════════
class TestTriggerEmailWorkflow:
    """Workflow rules with SEND_EMAIL actions on various entity events."""

    def test_08_email_on_lead_created(self, headers):
        """Trigger welcome email when a new lead is created."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                            actions=[{
                                "actionType": "SEND_EMAIL",
                                "targetField": "email",
                                "targetValue": "Welcome to our CRM! We will be in touch.",
                                "actionOrder": 0,
                            }])
        actions = rule.get("actions", [])
        assert any(a.get("actionType") == "SEND_EMAIL" for a in actions)

    def test_09_email_on_opportunity_stage_change(self, headers):
        """Email notification when opportunity stage changes."""
        rule = _create_rule(headers, entity_type="OPPORTUNITY",
                            trigger="STAGE_CHANGED",
                            actions=[{
                                "actionType": "SEND_EMAIL",
                                "targetField": "owner.email",
                                "targetValue": "Deal stage updated",
                                "actionOrder": 0,
                            }])
        assert rule.get("triggerEvent") == "STAGE_CHANGED"

    def test_10_multi_action_workflow(self, headers):
        """Workflow with both SEND_EMAIL and CREATE_TASK actions."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                            actions=[
                                {"actionType": "SEND_EMAIL", "targetField": "email",
                                 "targetValue": "Welcome!", "actionOrder": 0},
                                {"actionType": "CREATE_TASK", "targetField": "Follow up",
                                 "targetValue": "Schedule intro call", "actionOrder": 1},
                            ])
        actions = rule.get("actions", [])
        assert len(actions) >= 2
        types = [a.get("actionType") for a in actions]
        assert "SEND_EMAIL" in types
        assert "CREATE_TASK" in types

    def test_11_create_task_on_contact_created(self, headers):
        """Auto-create task when new contact is added."""
        rule = _create_rule(headers, entity_type="CONTACT", trigger="CREATED",
                            actions=[{
                                "actionType": "CREATE_TASK",
                                "targetField": "Onboard contact",
                                "targetValue": "Send onboarding materials",
                                "actionOrder": 0,
                            }])
        assert rule.get("entityType") == "CONTACT"

    def test_12_update_field_action(self, headers):
        """UPDATE_FIELD action modifies entity data."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                            actions=[{
                                "actionType": "UPDATE_FIELD",
                                "targetField": "status",
                                "targetValue": "QUALIFIED",
                                "actionOrder": 0,
                            }])
        actions = rule.get("actions", [])
        assert any(a.get("actionType") == "UPDATE_FIELD" for a in actions)

    def test_13_update_rule_actions(self, headers):
        """PUT /workflows/{id} updates rule actions."""
        rule = _create_rule(headers)
        resp = requests.put(f"{WF_BASE}/{rule['id']}", headers=headers, json={
            "actions": [{
                "actionType": "SEND_EMAIL",
                "targetField": "manager@acme.com",
                "targetValue": "Rule updated notification",
                "actionOrder": 0,
            }],
        })
        assert resp.status_code == 200, resp.text

    def test_14_delete_workflow_rule(self, headers):
        """DELETE /workflows/{id} soft-deletes a rule."""
        rule = _create_rule(headers)
        resp = requests.delete(f"{WF_BASE}/{rule['id']}", headers=headers)
        assert resp.status_code == 200, resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 3 — Conditional Automation  (7 tests)
# ════════════════════════════════════════════════════════════════════════
class TestConditionalAutomation:
    """Workflow rules with conditions using various operators."""

    def test_15_equals_condition(self, headers):
        """EQUALS operator: trigger only when field matches value."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="UPDATED",
                            conditions=[{
                                "fieldName": "status",
                                "operator": "EQUALS",
                                "value": "QUALIFIED",
                                "logicalOperator": "AND",
                            }],
                            actions=[{
                                "actionType": "SEND_NOTIFICATION",
                                "targetValue": "Lead qualified!",
                                "actionOrder": 0,
                            }])
        conds = rule.get("conditions", [])
        assert len(conds) >= 1
        assert conds[0].get("operator") == "EQUALS"

    def test_16_greater_than_condition(self, headers):
        """GREATER_THAN operator: trigger on high-value deals."""
        rule = _create_rule(headers, entity_type="OPPORTUNITY", trigger="CREATED",
                            conditions=[{
                                "fieldName": "amount",
                                "operator": "GREATER_THAN",
                                "value": "100000",
                                "logicalOperator": "AND",
                            }],
                            actions=[{
                                "actionType": "SEND_NOTIFICATION",
                                "targetValue": "High-value deal created!",
                                "actionOrder": 0,
                            }])
        conds = rule.get("conditions", [])
        assert any(c.get("operator") == "GREATER_THAN" for c in conds)

    def test_17_contains_condition(self, headers):
        """CONTAINS operator: match partial field values."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                            conditions=[{
                                "fieldName": "company",
                                "operator": "CONTAINS",
                                "value": "Enterprise",
                                "logicalOperator": "AND",
                            }],
                            actions=[{
                                "actionType": "ASSIGN_TO",
                                "targetValue": str(uuid.uuid4()),
                                "actionOrder": 0,
                            }])
        conds = rule.get("conditions", [])
        assert any(c.get("operator") == "CONTAINS" for c in conds)

    def test_18_is_not_null_condition(self, headers):
        """IS_NOT_NULL: trigger when field has a value."""
        rule = _create_rule(headers, entity_type="CONTACT", trigger="UPDATED",
                            conditions=[{
                                "fieldName": "email",
                                "operator": "IS_NOT_NULL",
                                "logicalOperator": "AND",
                            }],
                            actions=[{
                                "actionType": "SEND_EMAIL",
                                "targetField": "email",
                                "targetValue": "Thanks for updating your email",
                                "actionOrder": 0,
                            }])
        conds = rule.get("conditions", [])
        assert any(c.get("operator") == "IS_NOT_NULL" for c in conds)

    def test_19_multiple_and_conditions(self, headers):
        """Multiple AND conditions: all must match."""
        rule = _create_rule(headers, entity_type="OPPORTUNITY", trigger="UPDATED",
                            conditions=[
                                {"fieldName": "stage", "operator": "EQUALS",
                                 "value": "NEGOTIATION", "logicalOperator": "AND"},
                                {"fieldName": "amount", "operator": "GREATER_THAN",
                                 "value": "50000", "logicalOperator": "AND"},
                            ],
                            actions=[{
                                "actionType": "SEND_NOTIFICATION",
                                "targetValue": "Big deal in negotiation!",
                                "actionOrder": 0,
                            }])
        conds = rule.get("conditions", [])
        assert len(conds) >= 2

    def test_20_or_condition(self, headers):
        """OR logical operator between conditions."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="UPDATED",
                            conditions=[
                                {"fieldName": "source", "operator": "EQUALS",
                                 "value": "WEBSITE", "logicalOperator": "AND"},
                                {"fieldName": "source", "operator": "EQUALS",
                                 "value": "REFERRAL", "logicalOperator": "OR"},
                            ],
                            actions=[{
                                "actionType": "ASSIGN_TO",
                                "targetValue": "round-robin",
                                "actionOrder": 0,
                            }])
        conds = rule.get("conditions", [])
        assert any(c.get("logicalOperator") == "OR" for c in conds)

    def test_21_in_operator_condition(self, headers):
        """IN operator: match against a set of values."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="CREATED",
                            conditions=[{
                                "fieldName": "industry",
                                "operator": "IN",
                                "value": "Technology,Healthcare,Finance",
                                "logicalOperator": "AND",
                            }],
                            actions=[{
                                "actionType": "SEND_NOTIFICATION",
                                "targetValue": "High-priority industry lead",
                                "actionOrder": 0,
                            }])
        conds = rule.get("conditions", [])
        assert any(c.get("operator") == "IN" for c in conds)


# ════════════════════════════════════════════════════════════════════════
#  Group 4 — Escalation Rules  (7 tests)
# ════════════════════════════════════════════════════════════════════════
class TestEscalationRules:
    """Escalation via notifications, execution logs, and proposals."""

    def test_22_escalation_on_status_change(self, headers):
        """Notify manager when lead status changes (escalation)."""
        rule = _create_rule(headers, entity_type="LEAD", trigger="STATUS_CHANGED",
                            conditions=[{
                                "fieldName": "status",
                                "operator": "EQUALS",
                                "value": "AT_RISK",
                                "logicalOperator": "AND",
                            }],
                            actions=[{
                                "actionType": "SEND_NOTIFICATION",
                                "targetField": "manager",
                                "targetValue": "ESCALATION: Lead is at risk!",
                                "actionOrder": 0,
                            }])
        assert rule.get("triggerEvent") == "STATUS_CHANGED"

    def test_23_execution_logs_list(self, headers):
        """GET /workflows/executions returns execution history."""
        resp = requests.get(f"{WF_BASE}/executions", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_24_execution_logs_by_rule(self, headers):
        """GET /workflows/{ruleId}/executions for a specific rule."""
        rule = _create_rule(headers)
        resp = requests.get(f"{WF_BASE}/{rule['id']}/executions", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_25_create_proposal_for_escalation(self, headers):
        """POST /automation/proposals creates a deal proposal."""
        opp_id = str(uuid.uuid4())
        resp = requests.post(f"{AUTO_BASE}/proposals", headers=headers, json={
            "opportunityId": opp_id,
            "title": f"Proposal_{_uid()}",
            "amount": 50000,
            "recipientName": "John Doe",
            "recipientEmail": "john@example.com",
            "lineItems": [{
                "productName": "CRM License",
                "description": "Annual subscription",
                "quantity": 10,
                "unitPrice": 5000,
                "discount": 0,
            }],
        })
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("id") is not None

    def test_26_list_proposals(self, headers):
        """GET /automation/proposals lists all proposals."""
        resp = requests.get(f"{AUTO_BASE}/proposals", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_27_send_proposal(self, headers):
        """POST /automation/proposals/{id}/send marks proposal as SENT."""
        opp_id = str(uuid.uuid4())
        resp = requests.post(f"{AUTO_BASE}/proposals", headers=headers, json={
            "opportunityId": opp_id,
            "title": f"SendProp_{_uid()}",
            "amount": 25000,
            "lineItems": [],
        })
        assert resp.status_code in (200, 201), resp.text
        prop = resp.json().get("data", resp.json())
        send_resp = requests.post(f"{AUTO_BASE}/proposals/{prop['id']}/send",
                                  headers=headers)
        assert send_resp.status_code == 200, send_resp.text

    def test_28_accept_proposal(self, headers):
        """POST /automation/proposals/{id}/accept marks as ACCEPTED."""
        opp_id = str(uuid.uuid4())
        resp = requests.post(f"{AUTO_BASE}/proposals", headers=headers, json={
            "opportunityId": opp_id,
            "title": f"AcceptProp_{_uid()}",
            "amount": 30000,
            "lineItems": [],
        })
        assert resp.status_code in (200, 201), resp.text
        prop = resp.json().get("data", resp.json())
        # Send first, then accept
        requests.post(f"{AUTO_BASE}/proposals/{prop['id']}/send", headers=headers)
        acc_resp = requests.post(f"{AUTO_BASE}/proposals/{prop['id']}/accept",
                                 headers=headers)
        assert acc_resp.status_code == 200, acc_resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 5 — Scheduled Workflows  (7 tests)
# ════════════════════════════════════════════════════════════════════════
class TestScheduledWorkflows:
    """Templates, AI suggestions, contracts, and scheduled automation."""

    def test_29_list_workflow_templates(self, headers):
        """GET /automation/templates lists available templates."""
        resp = requests.get(f"{AUTO_BASE}/templates", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_30_templates_by_entity(self, headers):
        """GET /automation/templates/entity/LEAD filters templates."""
        resp = requests.get(f"{AUTO_BASE}/templates/entity/LEAD", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_31_generate_suggestions(self, headers):
        """POST /automation/suggestions/generate creates best-practice suggestions."""
        resp = requests.post(f"{AUTO_BASE}/suggestions/generate", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_32_list_suggestions(self, headers):
        """GET /automation/suggestions lists all AI suggestions."""
        resp = requests.get(f"{AUTO_BASE}/suggestions", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_33_pending_suggestion_count(self, headers):
        """GET /automation/suggestions/pending-count returns count."""
        resp = requests.get(f"{AUTO_BASE}/suggestions/pending-count", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_34_create_contract(self, headers):
        """POST /automation/contracts creates a contract."""
        opp_id = str(uuid.uuid4())
        resp = requests.post(f"{AUTO_BASE}/contracts", headers=headers, json={
            "opportunityId": opp_id,
            "title": f"Contract_{_uid()}",
            "amount": 100000,
            "startDate": datetime.now().strftime("%Y-%m-%d"),
            "endDate": (datetime.now() + timedelta(days=365)).strftime("%Y-%m-%d"),
            "signerName": "Jane Smith",
            "signerEmail": "jane@example.com",
        })
        assert resp.status_code in (200, 201), resp.text
        data = resp.json().get("data", resp.json())
        assert data.get("id") is not None

    def test_35_list_contracts(self, headers):
        """GET /automation/contracts lists all contracts."""
        resp = requests.get(f"{AUTO_BASE}/contracts", headers=headers,
                            params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text
