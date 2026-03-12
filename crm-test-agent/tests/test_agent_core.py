"""
Unit tests for the AI Test Agent core components.
These run WITHOUT OpenAI calls by mocking the AI layer.
"""
from __future__ import annotations

import json
from pathlib import Path
from unittest.mock import MagicMock, patch

import pytest

from src.models import (
    AgentCommand,
    BugReport,
    CRMModuleDefinition,
    Priority,
    Severity,
    TestCase,
    TestResult,
    TestRunSummary,
    TestStatus,
    TestStep,
    TestSuite,
    TestType,
)
from src.crm_modules import ALL_MODULES, get_module, get_all_modules
from src.ai_test_generator import AITestGenerator
from src.playwright_generator import PlaywrightScriptGenerator
from src.api_test_engine import APITestEngine
from src.bug_reporter import BugReporter


# ── CRM Modules ──────────────────────────────────────────────────────────────

class TestCRMModules:
    def test_all_modules_loaded(self):
        assert len(ALL_MODULES) == 9

    def test_get_module_by_name(self):
        mod = get_module("lead")
        assert mod.name == "lead"
        assert mod.display_name == "Leads"

    def test_get_module_strips_plural(self):
        mod = get_module("leads")
        assert mod.name == "lead"

    def test_get_module_unknown_raises(self):
        with pytest.raises(KeyError):
            get_module("nonexistent")

    def test_all_modules_have_features(self):
        for mod in get_all_modules():
            assert len(mod.features) > 0, f"Module {mod.name} has no features"

    @pytest.mark.parametrize("name", ["lead", "account", "contact", "opportunity", "activity"])
    def test_crud_modules_have_api_endpoints(self, name):
        mod = get_module(name)
        assert len(mod.api_endpoints) >= 2


# ── Models ───────────────────────────────────────────────────────────────────

class TestModels:
    def test_test_case_default_id(self):
        tc = TestCase(module="lead", title="Test lead creation")
        assert len(tc.id) == 12

    def test_test_suite_contains_cases(self):
        tc1 = TestCase(module="lead", title="Create lead")
        tc2 = TestCase(module="lead", title="Delete lead")
        suite = TestSuite(name="Lead suite", module="lead", test_cases=[tc1, tc2])
        assert len(suite.test_cases) == 2

    def test_test_result_defaults(self):
        result = TestResult(test_case_id="abc123")
        assert result.status == TestStatus.PENDING
        assert result.duration_ms == 0.0

    def test_bug_report_creation(self):
        bug = BugReport(
            title="Login fails",
            module="user",
            severity=Severity.CRITICAL,
            priority=Priority.P0,
            description="Cannot login",
        )
        assert bug.severity == Severity.CRITICAL
        assert len(bug.id) == 10


# ── AI Test Generator (mocked) ──────────────────────────────────────────────

MOCK_AI_RESPONSE = json.dumps({
    "test_cases": [
        {
            "title": "Create lead with valid data",
            "description": "Test lead creation API with all required fields",
            "test_type": "api",
            "severity": "major",
            "priority": "P1",
            "preconditions": ["User is authenticated"],
            "steps": [
                {"order": 1, "action": "POST /api/v1/leads", "target": "/api/v1/leads", "value": "{}", "expected": "201"},
            ],
            "expected_result": "Lead is created successfully",
            "tags": ["smoke", "crud"],
        },
        {
            "title": "Verify lead list page loads",
            "description": "Navigate to leads page and verify grid is visible",
            "test_type": "ui",
            "severity": "critical",
            "priority": "P0",
            "preconditions": ["User is logged in"],
            "steps": [
                {"order": 1, "action": "Navigate to /leads", "target": "/leads", "value": "", "expected": "Page loads"},
                {"order": 2, "action": "Assert grid visible", "target": "div.MuiDataGrid-root", "value": "", "expected": "Grid is displayed"},
            ],
            "expected_result": "Leads page renders with data grid",
            "tags": ["smoke", "ui"],
        },
    ]
})


class TestAITestGenerator:
    @patch("src.ai_test_generator.OpenAI")
    def test_generate_suite(self, mock_openai_cls):
        # Setup mock
        mock_client = MagicMock()
        mock_openai_cls.return_value = mock_client
        mock_response = MagicMock()
        mock_response.choices = [MagicMock(message=MagicMock(content=MOCK_AI_RESPONSE))]
        mock_client.chat.completions.create.return_value = mock_response

        gen = AITestGenerator(api_key="test-key")
        mod = get_module("lead")
        suite = gen.generate_test_suite(mod)

        assert suite.module == "lead"
        assert len(suite.test_cases) == 2
        assert suite.test_cases[0].title == "Create lead with valid data"
        assert suite.test_cases[1].test_type == TestType.UI

    @patch("src.ai_test_generator.OpenAI")
    def test_handles_malformed_json(self, mock_openai_cls):
        mock_client = MagicMock()
        mock_openai_cls.return_value = mock_client
        mock_response = MagicMock()
        mock_response.choices = [MagicMock(message=MagicMock(content="not valid json"))]
        mock_client.chat.completions.create.return_value = mock_response

        gen = AITestGenerator(api_key="test-key")
        mod = get_module("lead")
        suite = gen.generate_test_suite(mod)

        assert len(suite.test_cases) == 0  # graceful degradation


# ── Playwright Script Generator ──────────────────────────────────────────────

class TestPlaywrightGenerator:
    def test_generates_ui_and_api_files(self, tmp_path):
        tc_ui = TestCase(
            module="lead", title="UI test", test_type=TestType.UI,
            steps=[TestStep(order=1, action="click", target="button", value="", expected="")],
        )
        tc_api = TestCase(
            module="lead", title="API test", test_type=TestType.API,
            steps=[TestStep(order=1, action="GET /api/v1/leads", target="/api/v1/leads", value="", expected="200")],
        )
        suite = TestSuite(name="Lead Tests", module="lead", test_cases=[tc_ui, tc_api])

        gen = PlaywrightScriptGenerator()
        files = gen.generate_scripts(suite, output_dir=tmp_path)

        assert len(files) == 2
        assert any("ui" in f.name for f in files)
        assert any("api" in f.name for f in files)

        # Verify files are valid Python (syntax check)
        for f in files:
            content = f.read_text(encoding="utf-8")
            compile(content, str(f), "exec")  # raises SyntaxError if broken


# ── Bug Reporter ─────────────────────────────────────────────────────────────

class TestBugReporter:
    def test_creates_reports_for_failures(self, tmp_path):
        tc = TestCase(module="lead", title="Create lead")
        result = TestResult(
            test_case_id=tc.id,
            status=TestStatus.FAILED,
            error_message="Expected 201 but got 500",
        )
        summary = TestRunSummary(suite_id="s1", total=1, failed=1, results=[result])

        reporter = BugReporter()
        reporter.reports_dir = tmp_path
        bugs = reporter.generate_reports(summary, [tc])

        assert len(bugs) == 1
        assert "FAILED" in bugs[0].title
        assert bugs[0].module == "lead"

    def test_no_reports_for_passing_tests(self, tmp_path):
        tc = TestCase(module="lead", title="Create lead")
        result = TestResult(test_case_id=tc.id, status=TestStatus.PASSED)
        summary = TestRunSummary(suite_id="s1", total=1, passed=1, results=[result])

        reporter = BugReporter()
        reporter.reports_dir = tmp_path
        bugs = reporter.generate_reports(summary, [tc])

        assert len(bugs) == 0


# ── Agent Command ────────────────────────────────────────────────────────────

class TestAgentCommand:
    def test_full_run_command(self):
        cmd = AgentCommand(action="full-run", modules=["lead", "account"])
        assert cmd.action == "full-run"
        assert len(cmd.modules) == 2

    def test_default_empty_modules(self):
        cmd = AgentCommand(action="generate")
        assert cmd.modules == []
