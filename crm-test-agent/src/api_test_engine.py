"""
API Test Engine — executes API-level test cases directly (without pytest)
using httpx, and returns TestResult objects.
"""
from __future__ import annotations

import json
import logging
import time
from datetime import datetime
from typing import Any

import httpx

from src.config import settings
from src.models import (
    Assertion,
    TestCase,
    TestResult,
    TestRunSummary,
    TestStatus,
    TestSuite,
    TestType,
)

logger = logging.getLogger(__name__)


class APITestEngine:
    """
    Directly executes API test cases against the CRM backend.
    Useful for fast feedback loops without spawning pytest subprocesses.
    """

    def __init__(self):
        self._token: str = ""
        self._client = httpx.Client(timeout=30.0)

    # ── authentication ───────────────────────────────────────────────

    def authenticate(self) -> str:
        """Obtain a JWT token from the auth service."""
        url = f"{settings.auth_service_url}/api/v1/auth/login"
        payload = {
            "email": settings.test_admin_email,
            "password": settings.test_admin_password,
            "tenantId": settings.test_tenant_id,
        }
        try:
            resp = self._client.post(url, json=payload)
            if resp.status_code == 200:
                data = resp.json()
                self._token = data.get("token") or data.get("accessToken") or ""
                logger.info("Authenticated as %s", settings.test_admin_email)
            else:
                logger.warning("Auth failed: %d — %s", resp.status_code, resp.text[:200])
        except httpx.HTTPError as exc:
            logger.error("Auth request failed: %s", exc)
        return self._token

    @property
    def _headers(self) -> dict[str, str]:
        h: dict[str, str] = {"Content-Type": "application/json"}
        if self._token:
            h["Authorization"] = f"Bearer {self._token}"
        return h

    # ── execute suite ────────────────────────────────────────────────

    def run_suite(self, suite: TestSuite) -> TestRunSummary:
        """Execute every API test case in the suite and collect results."""
        if not self._token:
            self.authenticate()

        api_cases = [tc for tc in suite.test_cases if tc.test_type in (TestType.API, TestType.REGRESSION)]
        if not api_cases:
            api_cases = suite.test_cases  # fall back to all

        summary = TestRunSummary(suite_id=suite.id, started_at=datetime.utcnow())

        for tc in api_cases:
            result = self._execute_case(tc)
            summary.results.append(result)
            if result.status == TestStatus.PASSED:
                summary.passed += 1
            elif result.status == TestStatus.FAILED:
                summary.failed += 1
            elif result.status == TestStatus.ERROR:
                summary.errors += 1
            else:
                summary.skipped += 1
            summary.total += 1
            summary.duration_ms += result.duration_ms

        summary.finished_at = datetime.utcnow()
        logger.info(
            "Suite %s done: %d total, %d passed, %d failed, %d errors",
            suite.id,
            summary.total,
            summary.passed,
            summary.failed,
            summary.errors,
        )
        return summary

    # ── single test case ─────────────────────────────────────────────

    def _execute_case(self, tc: TestCase) -> TestResult:
        result = TestResult(test_case_id=tc.id, started_at=datetime.utcnow())
        t0 = time.perf_counter()

        try:
            for step in tc.steps:
                self._execute_step(step.action, step.target, step.value, step.expected, result)

            # If we got here without explicit assertion failures, mark as passed
            if not any(not a.passed for a in result.assertions):
                result.status = TestStatus.PASSED
            else:
                result.status = TestStatus.FAILED

        except Exception as exc:
            result.status = TestStatus.ERROR
            result.error_message = str(exc)
            import traceback
            result.stack_trace = traceback.format_exc()
            logger.error("Test %s errored: %s", tc.title, exc)

        result.duration_ms = (time.perf_counter() - t0) * 1000
        result.finished_at = datetime.utcnow()
        return result

    # ── step executor ────────────────────────────────────────────────

    def _execute_step(
        self,
        action: str,
        target: str,
        value: str,
        expected: str,
        result: TestResult,
    ) -> None:
        action_upper = action.upper()

        # Parse value as JSON body if possible
        body: Any = None
        if value:
            try:
                body = json.loads(value)
            except json.JSONDecodeError:
                body = value

        url = self._resolve_url(target)

        if "POST" in action_upper:
            resp = self._client.post(url, json=body, headers=self._headers)
        elif "PUT" in action_upper or "PATCH" in action_upper:
            resp = self._client.put(url, json=body, headers=self._headers)
        elif "DELETE" in action_upper:
            resp = self._client.delete(url, headers=self._headers)
        elif "GET" in action_upper:
            resp = self._client.get(url, headers=self._headers)
        else:
            result.logs.append(f"Unknown action: {action}")
            return

        result.logs.append(f"{action} {url} → {resp.status_code}")

        # Auto-assert on expected status
        if expected:
            try:
                expected_status = int(expected)
                assertion = Assertion(
                    field="status_code",
                    expected=expected_status,
                    actual=resp.status_code,
                    passed=resp.status_code == expected_status,
                    message=f"Expected {expected_status}, got {resp.status_code}",
                )
                result.assertions.append(assertion)
            except ValueError:
                # expected is a text description — log it
                result.logs.append(f"Expected: {expected}")

    def _resolve_url(self, target: str) -> str:
        """Turn a relative path into a full URL."""
        if target.startswith("http"):
            return target

        # Map /api/v1/leads → crm_api_base + /leads etc.
        if target.startswith("/api/v1/auth"):
            return f"{settings.auth_service_url}{target}"
        return f"{settings.crm_api_base}{target.removeprefix('/api/v1')}"

    # ── cleanup ──────────────────────────────────────────────────────

    def close(self):
        self._client.close()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()
