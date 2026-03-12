"""
Bug Reporter — generates structured BugReport objects from failed tests,
optionally creates Jira tickets, and writes Allure-compatible artefacts.
"""
from __future__ import annotations

import json
import logging
from datetime import datetime
from pathlib import Path
from typing import Optional

from src.config import settings
from src.models import (
    BugReport,
    Priority,
    Severity,
    TestCase,
    TestResult,
    TestRunSummary,
    TestStatus,
)

logger = logging.getLogger(__name__)


class BugReporter:
    """Creates bug reports from failed test results."""

    def __init__(self):
        self.reports_dir = Path(settings.report_output_dir)
        self.reports_dir.mkdir(parents=True, exist_ok=True)

    # ── public API ───────────────────────────────────────────────────

    def generate_reports(
        self,
        run_summary: TestRunSummary,
        test_cases: list[TestCase],
    ) -> list[BugReport]:
        """
        For every failed / errored result create a BugReport.
        Returns all generated bug reports.
        """
        tc_map = {tc.id: tc for tc in test_cases}
        bugs: list[BugReport] = []

        for result in run_summary.results:
            if result.status not in (TestStatus.FAILED, TestStatus.ERROR):
                continue

            tc = tc_map.get(result.test_case_id)
            bug = self._create_bug_report(result, tc)
            bugs.append(bug)

        if bugs:
            self._write_bug_json(bugs)
            self._write_allure_attachments(bugs)
            logger.info("Generated %d bug reports → %s", len(bugs), self.reports_dir)

        return bugs

    # ── Jira integration (optional) ──────────────────────────────────

    def file_jira_tickets(self, bugs: list[BugReport]) -> list[BugReport]:
        """
        Create Jira issues for each bug (only if Jira is configured).
        Updates the bug objects with ticket keys.
        """
        if not settings.jira_url or not settings.jira_api_token:
            logger.info("Jira not configured — skipping ticket creation")
            return bugs

        try:
            from jira import JIRA

            jira = JIRA(
                server=settings.jira_url,
                basic_auth=(settings.jira_email, settings.jira_api_token),
            )

            for bug in bugs:
                issue = jira.create_issue(
                    project=settings.jira_project_key,
                    summary=bug.title,
                    description=self._format_jira_description(bug),
                    issuetype={"name": "Bug"},
                    priority={"name": self._jira_priority(bug.priority)},
                )
                bug.jira_ticket_key = issue.key
                logger.info("Created Jira ticket %s for bug '%s'", issue.key, bug.title)

                # Attach screenshots
                for ss in bug.screenshot_paths:
                    p = Path(ss)
                    if p.exists():
                        jira.add_attachment(issue=issue, attachment=str(p))

        except Exception as exc:
            logger.error("Jira integration failed: %s", exc)

        return bugs

    # ── internals ────────────────────────────────────────────────────

    def _create_bug_report(self, result: TestResult, tc: Optional[TestCase]) -> BugReport:
        title_prefix = f"[{tc.module.upper()}]" if tc else "[CRM]"
        tc_title = tc.title if tc else result.test_case_id

        steps = []
        if tc:
            for s in tc.steps:
                steps.append(f"{s.order}. {s.action} → {s.target} (value: {s.value})")

        screenshots = [result.screenshot_path] if result.screenshot_path else []

        return BugReport(
            title=f"{title_prefix} {tc_title} — FAILED",
            module=tc.module if tc else "unknown",
            severity=tc.severity if tc else Severity.MAJOR,
            priority=tc.priority if tc else Priority.P1,
            description=f"AI-generated test failed.\n\nError:\n{result.error_message}",
            steps_to_reproduce=steps or result.logs,
            expected_behavior=tc.expected_result if tc else "",
            actual_behavior=result.error_message or "Test failed (see logs)",
            environment=f"URL: {settings.crm_base_url}, Browser: {settings.browser}, Headless: {settings.headless}",
            screenshot_paths=screenshots,
            test_case_id=result.test_case_id,
            test_result=result,
        )

    def _write_bug_json(self, bugs: list[BugReport]) -> Path:
        path = self.reports_dir / f"bugs_{datetime.utcnow().strftime('%Y%m%d_%H%M%S')}.json"
        data = [bug.model_dump(mode="json") for bug in bugs]
        path.write_text(json.dumps(data, indent=2, default=str), encoding="utf-8")
        return path

    def _write_allure_attachments(self, bugs: list[BugReport]) -> None:
        """Write bug reports as Allure environment/attachments."""
        allure_dir = Path(settings.allure_results_dir)
        allure_dir.mkdir(parents=True, exist_ok=True)

        # environment.properties
        env_file = allure_dir / "environment.properties"
        env_file.write_text(
            f"CRM_URL={settings.crm_base_url}\n"
            f"Browser={settings.browser}\n"
            f"Headless={settings.headless}\n"
            f"Test_Admin={settings.test_admin_email}\n"
            f"Bugs_Found={len(bugs)}\n",
            encoding="utf-8",
        )

        # categories.json for Allure
        categories = [
            {
                "name": "API Failures",
                "matchedStatuses": ["failed"],
                "messageRegex": ".*status_code.*",
            },
            {
                "name": "UI Failures",
                "matchedStatuses": ["failed"],
                "messageRegex": ".*Locator.*|.*timeout.*",
            },
            {
                "name": "Infrastructure Errors",
                "matchedStatuses": ["broken"],
                "messageRegex": ".*Connection.*|.*ECONNREFUSED.*",
            },
        ]
        cat_file = allure_dir / "categories.json"
        cat_file.write_text(json.dumps(categories, indent=2), encoding="utf-8")

    @staticmethod
    def _format_jira_description(bug: BugReport) -> str:
        lines = [
            f"*Module:* {bug.module}",
            f"*Severity:* {bug.severity.value}",
            f"*Priority:* {bug.priority.value}",
            "",
            "h3. Description",
            bug.description,
            "",
            "h3. Steps to Reproduce",
        ]
        for step in bug.steps_to_reproduce:
            lines.append(f"# {step}")
        lines += [
            "",
            "h3. Expected Behavior",
            bug.expected_behavior,
            "",
            "h3. Actual Behavior",
            bug.actual_behavior,
            "",
            f"h3. Environment",
            bug.environment,
        ]
        return "\n".join(lines)

    @staticmethod
    def _jira_priority(priority: Priority) -> str:
        return {
            Priority.P0: "Highest",
            Priority.P1: "High",
            Priority.P2: "Medium",
            Priority.P3: "Low",
        }.get(priority, "Medium")
