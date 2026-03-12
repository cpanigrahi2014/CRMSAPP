"""
Domain models for the AI Testing Agent.
All structured data that flows between subsystems is defined here.
"""
from __future__ import annotations

import uuid
from datetime import datetime
from enum import Enum
from typing import Any, Optional

from pydantic import BaseModel, Field


# ── Enumerations ─────────────────────────────────────────────────────────────

class TestType(str, Enum):
    API = "api"
    UI = "ui"
    E2E = "e2e"
    SMOKE = "smoke"
    REGRESSION = "regression"


class Severity(str, Enum):
    BLOCKER = "blocker"
    CRITICAL = "critical"
    MAJOR = "major"
    MINOR = "minor"
    TRIVIAL = "trivial"


class TestStatus(str, Enum):
    PENDING = "pending"
    RUNNING = "running"
    PASSED = "passed"
    FAILED = "failed"
    SKIPPED = "skipped"
    ERROR = "error"


class Priority(str, Enum):
    P0 = "P0"
    P1 = "P1"
    P2 = "P2"
    P3 = "P3"


# ── CRM module metadata ─────────────────────────────────────────────────────

class CRMField(BaseModel):
    name: str
    field_type: str  # text, email, phone, select, number, date, boolean
    required: bool = False
    options: list[str] = Field(default_factory=list)
    placeholder: str = ""


class APIEndpoint(BaseModel):
    method: str  # GET, POST, PUT, DELETE, PATCH
    path: str
    description: str = ""
    request_body_example: dict[str, Any] = Field(default_factory=dict)
    expected_status: int = 200


class UISelector(BaseModel):
    """Playwright selectors for a specific UI element."""
    element: str
    selector: str
    description: str = ""


class CRMModuleDefinition(BaseModel):
    """Complete definition of a CRM module — drives test generation."""
    name: str
    display_name: str
    description: str = ""
    base_api_path: str
    ui_route: str
    fields: list[CRMField] = Field(default_factory=list)
    api_endpoints: list[APIEndpoint] = Field(default_factory=list)
    ui_selectors: list[UISelector] = Field(default_factory=list)
    features: list[str] = Field(default_factory=list)


# ── Test case & result ───────────────────────────────────────────────────────

class TestStep(BaseModel):
    order: int
    action: str
    target: str = ""
    value: str = ""
    expected: str = ""


class TestCase(BaseModel):
    id: str = Field(default_factory=lambda: uuid.uuid4().hex[:12])
    module: str
    title: str
    description: str = ""
    test_type: TestType = TestType.API
    severity: Severity = Severity.MAJOR
    priority: Priority = Priority.P1
    preconditions: list[str] = Field(default_factory=list)
    steps: list[TestStep] = Field(default_factory=list)
    expected_result: str = ""
    tags: list[str] = Field(default_factory=list)
    generated_at: datetime = Field(default_factory=datetime.utcnow)

    # Optional: raw generated code (filled later by script gen)
    api_code: str = ""
    ui_code: str = ""


class Assertion(BaseModel):
    field: str
    expected: Any = None
    actual: Any = None
    passed: bool = False
    message: str = ""


class TestResult(BaseModel):
    test_case_id: str
    status: TestStatus = TestStatus.PENDING
    duration_ms: float = 0.0
    assertions: list[Assertion] = Field(default_factory=list)
    error_message: str = ""
    stack_trace: str = ""
    screenshot_path: str = ""
    video_path: str = ""
    logs: list[str] = Field(default_factory=list)
    started_at: Optional[datetime] = None
    finished_at: Optional[datetime] = None


# ── Test suite ───────────────────────────────────────────────────────────────

class TestSuite(BaseModel):
    id: str = Field(default_factory=lambda: uuid.uuid4().hex[:8])
    name: str
    module: str
    test_type: TestType = TestType.API
    test_cases: list[TestCase] = Field(default_factory=list)
    created_at: datetime = Field(default_factory=datetime.utcnow)


class TestRunSummary(BaseModel):
    suite_id: str
    total: int = 0
    passed: int = 0
    failed: int = 0
    skipped: int = 0
    errors: int = 0
    duration_ms: float = 0.0
    results: list[TestResult] = Field(default_factory=list)
    started_at: Optional[datetime] = None
    finished_at: Optional[datetime] = None


# ── Bug report ───────────────────────────────────────────────────────────────

class BugReport(BaseModel):
    id: str = Field(default_factory=lambda: uuid.uuid4().hex[:10])
    title: str
    module: str
    severity: Severity = Severity.MAJOR
    priority: Priority = Priority.P1
    description: str = ""
    steps_to_reproduce: list[str] = Field(default_factory=list)
    expected_behavior: str = ""
    actual_behavior: str = ""
    environment: str = ""
    screenshot_paths: list[str] = Field(default_factory=list)
    test_case_id: str = ""
    test_result: Optional[TestResult] = None
    jira_ticket_key: str = ""
    created_at: datetime = Field(default_factory=datetime.utcnow)


# ── Agent command / response ─────────────────────────────────────────────────

class AgentCommand(BaseModel):
    """A high-level instruction from the user / scheduler to the agent."""
    action: str  # generate, execute, report, full-run
    modules: list[str] = Field(default_factory=list)  # empty → all
    test_types: list[TestType] = Field(default_factory=list)
    tags: list[str] = Field(default_factory=list)
    options: dict[str, Any] = Field(default_factory=dict)


class AgentResponse(BaseModel):
    success: bool = True
    message: str = ""
    suites_generated: int = 0
    tests_executed: int = 0
    bugs_filed: int = 0
    report_path: str = ""
    run_summary: Optional[TestRunSummary] = None
