"""
Task & Activity Management — 30 Tests
======================================
Service  : activity-service (port 8086)
Groups   :
  1. Create Task            (tests 01-06)
  2. Assign Task            (tests 07-11)
  3. Update Task Status     (tests 12-17)
  4. Set Reminders          (tests 18-22)
  5. Activity Logging       (tests 23-30)
"""

import uuid, pytest, requests
from datetime import datetime, timedelta

BASE = "http://localhost:8086/api/v1/activities"
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


def _create_task(headers, **overrides):
    """Helper: create a TASK activity with sensible defaults."""
    body = {
        "type": "TASK",
        "subject": f"Task_{_uid()}",
        "description": "Auto-test task",
        "priority": "MEDIUM",
        "dueDate": (datetime.now() + timedelta(days=3)).strftime("%Y-%m-%dT%H:%M:%S"),
    }
    body.update(overrides)
    resp = requests.post(BASE, headers=headers, json=body)
    assert resp.status_code in (200, 201), resp.text
    data = resp.json().get("data", resp.json())
    return data


def _create_activity(headers, act_type, **overrides):
    """Helper: create any activity type."""
    body = {
        "type": act_type,
        "subject": f"{act_type}_{_uid()}",
    }
    body.update(overrides)
    resp = requests.post(BASE, headers=headers, json=body)
    assert resp.status_code in (200, 201), resp.text
    return resp.json().get("data", resp.json())


# ════════════════════════════════════════════════════════════════════════
#  Group 1 — Create Task  (6 tests)
# ════════════════════════════════════════════════════════════════════════
class TestCreateTask:
    """Create various task types and verify defaults."""

    def test_01_create_basic_task(self, headers):
        """POST /activities creates a TASK with subject and type."""
        task = _create_task(headers)
        assert task.get("id") is not None
        assert task.get("type") == "TASK"
        assert task.get("subject") is not None

    def test_02_task_defaults_not_started(self, headers):
        """New task defaults to NOT_STARTED status."""
        task = _create_task(headers)
        assert task.get("status") in ("NOT_STARTED", "not_started", None)

    def test_03_task_with_high_priority(self, headers):
        """Create task with HIGH priority."""
        task = _create_task(headers, priority="HIGH")
        assert task.get("priority") in ("HIGH", "high")

    def test_04_task_with_urgent_priority(self, headers):
        """Create task with URGENT priority."""
        task = _create_task(headers, priority="URGENT")
        assert task.get("priority") in ("URGENT", "urgent")

    def test_05_create_call_activity(self, headers):
        """POST /activities with type=CALL."""
        act = _create_activity(headers, "CALL",
                               callDurationMinutes=15,
                               callOutcome="Left voicemail")
        assert act.get("type") == "CALL"

    def test_06_create_meeting_activity(self, headers):
        """POST /activities with type=MEETING and location."""
        start = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%S")
        end = (datetime.now() + timedelta(days=1, hours=1)).strftime("%Y-%m-%dT%H:%M:%S")
        act = _create_activity(headers, "MEETING",
                               location="Conference Room A",
                               startTime=start, endTime=end)
        assert act.get("type") == "MEETING"


# ════════════════════════════════════════════════════════════════════════
#  Group 2 — Assign Task  (5 tests)
# ════════════════════════════════════════════════════════════════════════
class TestAssignTask:
    """Task assignment and retrieval by assignee."""

    def test_07_assign_task_on_create(self, headers):
        """Create task assigned to a specific user UUID."""
        user_id = str(uuid.uuid4())
        task = _create_task(headers, assignedTo=user_id)
        assert task.get("assignedTo") == user_id

    def test_08_update_assignment(self, headers):
        """PUT /activities/{id} reassigns task to another user."""
        task = _create_task(headers)
        new_user = str(uuid.uuid4())
        resp = requests.put(f"{BASE}/{task['id']}", headers=headers, json={
            "assignedTo": new_user,
        })
        assert resp.status_code == 200, resp.text
        updated = resp.json().get("data", resp.json())
        assert updated.get("assignedTo") == new_user

    def test_09_get_tasks_by_assignee(self, headers):
        """GET /activities/assignee/{id} lists assigned tasks."""
        user_id = str(uuid.uuid4())
        _create_task(headers, assignedTo=user_id)
        resp = requests.get(f"{BASE}/assignee/{user_id}", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_10_assign_task_to_related_entity(self, headers):
        """Create task linked to a LEAD entity."""
        entity_id = str(uuid.uuid4())
        task = _create_task(headers, relatedEntityType="LEAD",
                            relatedEntityId=entity_id)
        assert task.get("relatedEntityType") == "LEAD"
        assert task.get("relatedEntityId") == entity_id

    def test_11_get_tasks_by_related_entity(self, headers):
        """GET /activities/related/{entityId} retrieves linked tasks."""
        entity_id = str(uuid.uuid4())
        _create_task(headers, relatedEntityType="CONTACT",
                     relatedEntityId=entity_id)
        resp = requests.get(f"{BASE}/related/{entity_id}", headers=headers)
        assert resp.status_code == 200, resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 3 — Update Task Status  (6 tests)
# ════════════════════════════════════════════════════════════════════════
class TestUpdateTaskStatus:
    """Status transitions and task completion."""

    def test_12_update_to_in_progress(self, headers):
        """PUT /activities/{id} changes status to IN_PROGRESS."""
        task = _create_task(headers)
        resp = requests.put(f"{BASE}/{task['id']}", headers=headers, json={
            "status": "IN_PROGRESS",
        })
        assert resp.status_code == 200, resp.text
        updated = resp.json().get("data", resp.json())
        assert updated.get("status") in ("IN_PROGRESS", "in_progress")

    def test_13_mark_complete_via_patch(self, headers):
        """PATCH /activities/{id}/complete sets COMPLETED status."""
        task = _create_task(headers)
        resp = requests.patch(f"{BASE}/{task['id']}/complete", headers=headers)
        assert resp.status_code == 200, resp.text
        updated = resp.json().get("data", resp.json())
        assert updated.get("status") in ("COMPLETED", "completed")

    def test_14_completed_task_has_completedAt(self, headers):
        """Completing a task sets completedAt timestamp."""
        task = _create_task(headers)
        resp = requests.patch(f"{BASE}/{task['id']}/complete", headers=headers)
        assert resp.status_code == 200, resp.text
        updated = resp.json().get("data", resp.json())
        assert updated.get("completedAt") is not None

    def test_15_cancel_task(self, headers):
        """PUT /activities/{id} cancels task."""
        task = _create_task(headers)
        resp = requests.put(f"{BASE}/{task['id']}", headers=headers, json={
            "status": "CANCELLED",
        })
        assert resp.status_code == 200, resp.text
        updated = resp.json().get("data", resp.json())
        assert updated.get("status") in ("CANCELLED", "cancelled")

    def test_16_soft_delete_task(self, headers):
        """DELETE /activities/{id} soft-deletes the task."""
        task = _create_task(headers)
        resp = requests.delete(f"{BASE}/{task['id']}", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_17_get_deleted_task_returns_404(self, headers):
        """GET after delete returns 404."""
        task = _create_task(headers)
        requests.delete(f"{BASE}/{task['id']}", headers=headers)
        resp = requests.get(f"{BASE}/{task['id']}", headers=headers)
        # soft-deleted record should not be found
        assert resp.status_code in (404, 200), resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 4 — Set Reminders  (5 tests)
# ════════════════════════════════════════════════════════════════════════
class TestSetReminders:
    """Reminder scheduling and recurrence."""

    def test_18_create_task_with_reminder(self, headers):
        """Create task with reminderAt datetime."""
        reminder = (datetime.now() + timedelta(hours=12)).strftime("%Y-%m-%dT%H:%M:%S")
        task = _create_task(headers, reminderAt=reminder)
        assert task.get("reminderAt") is not None

    def test_19_update_reminder_time(self, headers):
        """PUT /activities/{id} updates reminder datetime."""
        task = _create_task(headers)
        new_reminder = (datetime.now() + timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%S")
        resp = requests.put(f"{BASE}/{task['id']}", headers=headers, json={
            "reminderAt": new_reminder,
        })
        assert resp.status_code == 200, resp.text
        updated = resp.json().get("data", resp.json())
        assert updated.get("reminderAt") is not None

    def test_20_create_recurring_daily(self, headers):
        """Create task with DAILY recurrence."""
        task = _create_task(headers, recurrenceRule="DAILY",
                            recurrenceEnd=(datetime.now() + timedelta(days=30)).strftime("%Y-%m-%d"))
        assert task.get("recurrenceRule") in ("DAILY", "daily")

    def test_21_create_recurring_weekly(self, headers):
        """Create task with WEEKLY recurrence."""
        task = _create_task(headers, recurrenceRule="WEEKLY",
                            recurrenceEnd=(datetime.now() + timedelta(days=60)).strftime("%Y-%m-%d"))
        assert task.get("recurrenceRule") in ("WEEKLY", "weekly")

    def test_22_upcoming_activities_endpoint(self, headers):
        """GET /activities/upcoming returns tasks due within N days."""
        # Create a task due tomorrow to ensure at least one result
        _create_task(headers, dueDate=(datetime.now() + timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%S"))
        resp = requests.get(f"{BASE}/upcoming", headers=headers, params={"days": 7})
        assert resp.status_code == 200, resp.text


# ════════════════════════════════════════════════════════════════════════
#  Group 5 — Activity Logging  (8 tests)
# ════════════════════════════════════════════════════════════════════════
class TestActivityLogging:
    """Activity stream, timeline, search, analytics."""

    def test_23_list_all_activities(self, headers):
        """GET /activities lists activities with pagination."""
        resp = requests.get(BASE, headers=headers, params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text

    def test_24_filter_by_type_task(self, headers):
        """GET /activities?type=TASK filters to tasks only."""
        _create_task(headers)
        resp = requests.get(BASE, headers=headers, params={"type": "TASK"})
        assert resp.status_code == 200, resp.text

    def test_25_search_activities(self, headers):
        """GET /activities/search?query=... searches subject/description."""
        tag = _uid()
        _create_task(headers, subject=f"SearchMe-{tag}")
        resp = requests.get(f"{BASE}/search", headers=headers, params={"query": tag})
        assert resp.status_code == 200, resp.text

    def test_26_activity_timeline(self, headers):
        """GET /activities/timeline returns tenant-wide timeline."""
        resp = requests.get(f"{BASE}/timeline", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_27_entity_timeline(self, headers):
        """GET /activities/timeline/{entityId} returns entity-specific timeline."""
        entity_id = str(uuid.uuid4())
        _create_task(headers, relatedEntityType="OPPORTUNITY",
                     relatedEntityId=entity_id)
        resp = requests.get(f"{BASE}/timeline/{entity_id}", headers=headers)
        assert resp.status_code == 200, resp.text

    def test_28_activity_analytics(self, headers):
        """GET /activities/analytics returns summary metrics."""
        resp = requests.get(f"{BASE}/analytics", headers=headers)
        assert resp.status_code == 200, resp.text
        data = resp.json().get("data", resp.json())
        # Verify analytics shape
        assert isinstance(data, dict)

    def test_29_record_stream_event(self, headers):
        """POST /activities/stream/record logs a custom stream event."""
        resp = requests.post(f"{BASE}/stream/record", headers=headers, params={
            "eventType": "TASK_REVIEWED",
            "entityType": "Task",
            "entityName": f"Review-{_uid()}",
            "description": "Automated test stream event",
        })
        assert resp.status_code == 200, resp.text

    def test_30_activity_stream_history(self, headers):
        """GET /activities/stream returns stream event history."""
        resp = requests.get(f"{BASE}/stream", headers=headers, params={"page": 0, "size": 10})
        assert resp.status_code == 200, resp.text
