"""
UI Test Executor — runs generated Playwright tests via pytest subprocess
and collects results + screenshots.
"""
from __future__ import annotations

import json
import logging
import subprocess
import sys
from datetime import datetime
from pathlib import Path

from src.config import settings
from src.models import TestResult, TestRunSummary, TestStatus

logger = logging.getLogger(__name__)


class UITestExecutor:
    """
    Executes generated Playwright-Python test files using pytest + allure.
    """

    def __init__(self):
        self.results_dir = Path(settings.allure_results_dir)
        self.results_dir.mkdir(parents=True, exist_ok=True)
        self.screenshots_dir = settings.screenshots_dir
        self.screenshots_dir.mkdir(parents=True, exist_ok=True)

    # ── public API ───────────────────────────────────────────────────

    def run_test_file(self, test_file: Path, markers: list[str] | None = None) -> TestRunSummary:
        """
        Run a single generated test file through pytest and return a summary.
        """
        if not test_file.exists():
            logger.error("Test file not found: %s", test_file)
            return TestRunSummary(suite_id="unknown", total=0)

        cmd = self._build_pytest_command(test_file, markers)
        logger.info("Running: %s", " ".join(cmd))

        started = datetime.utcnow()
        proc = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            cwd=str(settings.root_dir),
            timeout=300,
        )
        finished = datetime.utcnow()

        logger.info("pytest exit code: %d", proc.returncode)
        if proc.stdout:
            logger.debug("STDOUT:\n%s", proc.stdout[-2000:])
        if proc.stderr:
            logger.debug("STDERR:\n%s", proc.stderr[-2000:])

        summary = self._parse_pytest_output(proc.stdout, proc.returncode)
        summary.started_at = started
        summary.finished_at = finished
        return summary

    def run_all_generated(self, output_dir: Path | None = None, markers: list[str] | None = None) -> TestRunSummary:
        """
        Run all generated test files in the output directory.
        """
        test_dir = output_dir or settings.generated_tests_dir
        test_files = sorted(test_dir.glob("test_*.py"))
        if not test_files:
            logger.warning("No generated test files in %s", test_dir)
            return TestRunSummary(suite_id="all", total=0)

        combined = TestRunSummary(suite_id="all", started_at=datetime.utcnow())

        for tf in test_files:
            summary = self.run_test_file(tf, markers)
            combined.total += summary.total
            combined.passed += summary.passed
            combined.failed += summary.failed
            combined.errors += summary.errors
            combined.skipped += summary.skipped
            combined.duration_ms += summary.duration_ms
            combined.results.extend(summary.results)

        combined.finished_at = datetime.utcnow()
        return combined

    # ── internals ────────────────────────────────────────────────────

    def _build_pytest_command(self, test_file: Path, markers: list[str] | None) -> list[str]:
        cmd = [
            sys.executable, "-m", "pytest",
            str(test_file),
            f"--alluredir={self.results_dir}",
            "-v",
            "--tb=short",
            f"--screenshot={'on' if settings.screenshot_on_failure else 'off'}",
            f"--output={self.screenshots_dir}",
        ]

        if settings.headless:
            cmd.append("--headed" if not settings.headless else "--browser-channel=chromium")

        if markers:
            marker_expr = " or ".join(markers)
            cmd.extend(["-m", marker_expr])

        # json report for machine parsing
        json_report = self.results_dir / "report.json"
        cmd.extend([f"--json-report", f"--json-report-file={json_report}"])

        return cmd

    def _parse_pytest_output(self, stdout: str, exit_code: int) -> TestRunSummary:
        """
        Parse pytest short summary into a TestRunSummary.
        Also try to read the json-report if available.
        """
        summary = TestRunSummary(suite_id="pytest")

        # Try JSON report first
        json_report = self.results_dir / "report.json"
        if json_report.exists():
            try:
                data = json.loads(json_report.read_text(encoding="utf-8"))
                summary_data = data.get("summary", {})
                summary.passed = summary_data.get("passed", 0)
                summary.failed = summary_data.get("failed", 0)
                summary.errors = summary_data.get("error", 0)
                summary.skipped = summary_data.get("skipped", 0)
                summary.total = summary.passed + summary.failed + summary.errors + summary.skipped
                summary.duration_ms = data.get("duration", 0) * 1000

                for t in data.get("tests", []):
                    result = TestResult(
                        test_case_id=t.get("nodeid", ""),
                        status=self._map_status(t.get("outcome", "")),
                        duration_ms=t.get("duration", 0) * 1000,
                        error_message=self._extract_error(t),
                    )
                    summary.results.append(result)
                return summary
            except Exception as exc:
                logger.warning("Could not parse json report: %s", exc)

        # Fallback: parse stdout
        for line in stdout.splitlines():
            line = line.strip()
            if "passed" in line or "failed" in line or "error" in line:
                import re
                m = re.search(r"(\d+) passed", line)
                if m:
                    summary.passed = int(m.group(1))
                m = re.search(r"(\d+) failed", line)
                if m:
                    summary.failed = int(m.group(1))
                m = re.search(r"(\d+) error", line)
                if m:
                    summary.errors = int(m.group(1))
                m = re.search(r"(\d+) skipped", line)
                if m:
                    summary.skipped = int(m.group(1))

        summary.total = summary.passed + summary.failed + summary.errors + summary.skipped
        return summary

    @staticmethod
    def _map_status(outcome: str) -> TestStatus:
        return {
            "passed": TestStatus.PASSED,
            "failed": TestStatus.FAILED,
            "error": TestStatus.ERROR,
            "skipped": TestStatus.SKIPPED,
        }.get(outcome, TestStatus.PENDING)

    @staticmethod
    def _extract_error(test_data: dict) -> str:
        call = test_data.get("call", {})
        if call.get("crash"):
            return call["crash"].get("message", "")
        if call.get("longrepr"):
            return str(call["longrepr"])[:500]
        return ""
