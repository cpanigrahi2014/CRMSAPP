"""
CRM Test Agent — main orchestrator.

Workflow:
  1. Read CRM module definitions
  2. Generate test cases via OpenAI
  3. Convert to executable scripts (Playwright / API)
  4. Execute tests (API engine + UI executor)
  5. Validate results, generate bug reports
  6. Produce Allure report
"""
from __future__ import annotations

import logging
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Optional

from rich.console import Console
from rich.panel import Panel
from rich.table import Table

from src.config import settings
from src.models import (
    AgentCommand,
    AgentResponse,
    TestRunSummary,
    TestSuite,
    TestType,
)
from src.crm_modules import get_all_modules, get_module, ALL_MODULES
from src.ai_test_generator import AITestGenerator
from src.playwright_generator import PlaywrightScriptGenerator
from src.api_test_engine import APITestEngine
from src.ui_test_executor import UITestExecutor
from src.bug_reporter import BugReporter

logger = logging.getLogger(__name__)
console = Console()


class CRMTestAgent:
    """
    Main agent that orchestrates the full AI-powered testing pipeline.
    """

    def __init__(self):
        self.generator = AITestGenerator()
        self.script_gen = PlaywrightScriptGenerator()
        self.api_engine = APITestEngine()
        self.ui_executor = UITestExecutor()
        self.bug_reporter = BugReporter()

    # ── high-level commands ──────────────────────────────────────────

    def run(self, command: AgentCommand) -> AgentResponse:
        """Dispatch a command from the user / scheduler."""
        action = command.action.lower()

        if action == "generate":
            return self._handle_generate(command)
        elif action == "execute":
            return self._handle_execute(command)
        elif action == "report":
            return self._handle_report()
        elif action in ("full-run", "full_run", "run"):
            return self._handle_full_run(command)
        else:
            return AgentResponse(success=False, message=f"Unknown action: {action}")

    # ── generate ─────────────────────────────────────────────────────

    def _handle_generate(self, cmd: AgentCommand) -> AgentResponse:
        modules = self._resolve_modules(cmd.modules)
        test_types = cmd.test_types or []

        suites: list[TestSuite] = []
        for mod in modules:
            console.print(f"[bold cyan]Generating tests for {mod.display_name}...[/]")
            suite = self.generator.generate_test_suite(mod, test_types)
            suites.append(suite)

            # Write scripts
            self.script_gen.generate_scripts(suite)
            console.print(f"  ✔ {len(suite.test_cases)} test cases generated")

        total = sum(len(s.test_cases) for s in suites)
        return AgentResponse(
            success=True,
            message=f"Generated {total} test cases across {len(suites)} modules",
            suites_generated=len(suites),
        )

    # ── execute ──────────────────────────────────────────────────────

    def _handle_execute(self, cmd: AgentCommand) -> AgentResponse:
        console.print("[bold yellow]Executing tests...[/]")

        # API tests (direct execution)
        api_summary = TestRunSummary(suite_id="api-direct")
        if not cmd.test_types or TestType.API in cmd.test_types:
            modules = self._resolve_modules(cmd.modules)
            for mod in modules:
                console.print(f"  [cyan]API tests for {mod.display_name}[/]")
                suite = self.generator.generate_test_suite(mod, [TestType.API])
                result = self.api_engine.run_suite(suite)
                api_summary.total += result.total
                api_summary.passed += result.passed
                api_summary.failed += result.failed
                api_summary.errors += result.errors
                api_summary.results.extend(result.results)
                api_summary.duration_ms += result.duration_ms

        # UI tests (via pytest subprocess)
        ui_summary = TestRunSummary(suite_id="ui-pytest")
        if not cmd.test_types or TestType.UI in cmd.test_types:
            ui_summary = self.ui_executor.run_all_generated()

        # Combine
        combined = TestRunSummary(
            suite_id="combined",
            total=api_summary.total + ui_summary.total,
            passed=api_summary.passed + ui_summary.passed,
            failed=api_summary.failed + ui_summary.failed,
            errors=api_summary.errors + ui_summary.errors,
            skipped=api_summary.skipped + ui_summary.skipped,
            duration_ms=api_summary.duration_ms + ui_summary.duration_ms,
            results=api_summary.results + ui_summary.results,
            started_at=api_summary.started_at or ui_summary.started_at,
            finished_at=datetime.utcnow(),
        )

        self._print_summary(combined)

        return AgentResponse(
            success=combined.failed == 0 and combined.errors == 0,
            message=f"Executed {combined.total} tests: {combined.passed} passed, {combined.failed} failed, {combined.errors} errors",
            tests_executed=combined.total,
            run_summary=combined,
        )

    # ── report ───────────────────────────────────────────────────────

    def _handle_report(self) -> AgentResponse:
        """Generate the Allure report from collected results."""
        allure_dir = Path(settings.allure_results_dir)
        report_dir = Path(settings.report_output_dir) / "allure-report"

        try:
            subprocess.run(
                ["allure", "generate", str(allure_dir), "-o", str(report_dir), "--clean"],
                check=True,
                capture_output=True,
                text=True,
            )
            console.print(f"[bold green]Allure report generated → {report_dir}[/]")
            return AgentResponse(success=True, message=f"Report at {report_dir}", report_path=str(report_dir))
        except FileNotFoundError:
            logger.warning("Allure CLI not found — report not generated")
            return AgentResponse(success=False, message="Allure CLI not installed. Install with: npm install -g allure-commandline")
        except subprocess.CalledProcessError as exc:
            return AgentResponse(success=False, message=f"Allure generation failed: {exc.stderr[:300]}")

    # ── full run ─────────────────────────────────────────────────────

    def _handle_full_run(self, cmd: AgentCommand) -> AgentResponse:
        console.print(Panel("[bold magenta]CRM AI Test Agent — Full Run[/]", expand=False))

        # Step 1: Generate
        gen_resp = self._handle_generate(cmd)
        if not gen_resp.success:
            return gen_resp

        # Step 2: Execute
        exec_resp = self._handle_execute(cmd)

        # Step 3: Bug reports
        bugs_count = 0
        if exec_resp.run_summary:
            # Gather test cases from all suites for bug reporting
            all_cases = []
            for mod in self._resolve_modules(cmd.modules):
                suite = self.generator.generate_test_suite(mod)
                all_cases.extend(suite.test_cases)

            bugs = self.bug_reporter.generate_reports(exec_resp.run_summary, all_cases)
            bugs_count = len(bugs)

            if bugs and settings.jira_url:
                self.bug_reporter.file_jira_tickets(bugs)

        # Step 4: Allure report
        self._handle_report()

        return AgentResponse(
            success=exec_resp.success,
            message=f"Full run complete: {exec_resp.tests_executed} tests, {bugs_count} bugs filed",
            suites_generated=gen_resp.suites_generated,
            tests_executed=exec_resp.tests_executed,
            bugs_filed=bugs_count,
            run_summary=exec_resp.run_summary,
        )

    # ── helpers ──────────────────────────────────────────────────────

    def _resolve_modules(self, names: list[str]):
        if not names:
            return get_all_modules()
        resolved = []
        for n in names:
            try:
                resolved.append(get_module(n))
            except KeyError:
                logger.warning("Unknown module: %s — skipping", n)
        return resolved or get_all_modules()

    @staticmethod
    def _print_summary(summary: TestRunSummary):
        table = Table(title="Test Execution Summary")
        table.add_column("Metric", style="bold")
        table.add_column("Value", justify="right")
        table.add_row("Total", str(summary.total))
        table.add_row("Passed", f"[green]{summary.passed}[/]")
        table.add_row("Failed", f"[red]{summary.failed}[/]")
        table.add_row("Errors", f"[yellow]{summary.errors}[/]")
        table.add_row("Skipped", str(summary.skipped))
        table.add_row("Duration", f"{summary.duration_ms:.0f} ms")
        console.print(table)


# ── CLI entry-point ──────────────────────────────────────────────────────────

def main():
    """CLI interface for the CRM Test Agent."""
    import argparse

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s — %(message)s",
    )

    parser = argparse.ArgumentParser(description="CRM AI Test Agent")
    parser.add_argument(
        "action",
        choices=["generate", "execute", "report", "full-run"],
        help="Action to perform",
    )
    parser.add_argument(
        "--modules", "-m",
        nargs="*",
        default=[],
        help="CRM modules to test (default: all)",
    )
    parser.add_argument(
        "--types", "-t",
        nargs="*",
        default=[],
        help="Test types: api, ui, e2e, smoke, regression",
    )
    args = parser.parse_args()

    test_types = [TestType(t) for t in args.types] if args.types else []

    cmd = AgentCommand(
        action=args.action,
        modules=args.modules,
        test_types=test_types,
    )

    agent = CRMTestAgent()
    response = agent.run(cmd)

    console.print()
    if response.success:
        console.print(f"[bold green]✔ {response.message}[/]")
    else:
        console.print(f"[bold red]✘ {response.message}[/]")

    return 0 if response.success else 1


if __name__ == "__main__":
    sys.exit(main())
