"""
AI Test-Case Generator — uses OpenAI to produce structured TestCase objects
from CRM module definitions.
"""
from __future__ import annotations

import json
import logging
from typing import Optional

from openai import OpenAI

from src.config import settings
from src.models import (
    CRMModuleDefinition,
    Priority,
    Severity,
    TestCase,
    TestStep,
    TestSuite,
    TestType,
)

logger = logging.getLogger(__name__)

# ── System prompt ────────────────────────────────────────────────────────────

SYSTEM_PROMPT = """\
You are an expert QA engineer for a CRM SaaS platform (similar to Salesforce).
Your job is to generate **comprehensive, structured test cases** for a given CRM module.

Rules:
1. Output ONLY valid JSON — an array of test-case objects.
2. Each object MUST have these keys:
   - title (string)
   - description (string)
   - test_type ("api" | "ui" | "e2e" | "smoke" | "regression")
   - severity ("blocker" | "critical" | "major" | "minor" | "trivial")
   - priority ("P0" | "P1" | "P2" | "P3")
   - preconditions (string[])
   - steps (array of {order, action, target, value, expected})
   - expected_result (string)
   - tags (string[])
3. Cover positive, negative, boundary, and edge-case scenarios.
4. Include both API tests (status codes, payload validation) and UI tests (Playwright flows).
5. For API tests, reference the exact endpoint paths.
6. For UI tests, describe user actions (click, type, assert).
7. Do NOT add any text outside the JSON array.
"""


def _build_user_prompt(module: CRMModuleDefinition, test_types: list[TestType], extra_context: str = "") -> str:
    fields_info = "\n".join(
        f"  - {f.name} ({f.field_type}, required={f.required})" + (f" options={f.options}" if f.options else "")
        for f in module.fields
    )
    endpoints_info = "\n".join(
        f"  - {ep.method} {ep.path}  [{ep.description}]  expected_status={ep.expected_status}"
        for ep in module.api_endpoints
    )
    features_info = "\n".join(f"  - {feat}" for feat in module.features)
    selectors_info = "\n".join(
        f"  - {s.element}: {s.selector}" for s in module.ui_selectors
    )
    types_str = ", ".join(t.value for t in test_types) if test_types else "api, ui, e2e, smoke, regression"

    prompt = f"""\
Generate test cases for the **{module.display_name}** module.

Module: {module.name}
Description: {module.description}
UI Route: {module.ui_route}

Fields:
{fields_info or "  (none)"}

API Endpoints:
{endpoints_info or "  (none)"}

UI Selectors:
{selectors_info or "  (none)"}

Features to cover:
{features_info or "  (none)"}

Requested test types: {types_str}

Generate 8-15 test cases covering the above features with a mix of the requested types.
"""
    if extra_context:
        prompt += f"\nAdditional context:\n{extra_context}\n"
    return prompt


# ── Generator class ──────────────────────────────────────────────────────────

class AITestGenerator:
    """Generates TestSuite objects by calling OpenAI with CRM module metadata."""

    def __init__(self, api_key: str | None = None, model: str | None = None):
        self.client = OpenAI(api_key=api_key or settings.openai_api_key)
        self.model = model or settings.openai_model

    # ── public API ───────────────────────────────────────────────────

    def generate_test_suite(
        self,
        module: CRMModuleDefinition,
        test_types: list[TestType] | None = None,
        extra_context: str = "",
    ) -> TestSuite:
        """Generate a full test suite for a single module."""
        test_types = test_types or []
        logger.info("Generating test suite for module=%s types=%s", module.name, test_types)

        raw_cases = self._call_openai(module, test_types, extra_context)
        test_cases = self._parse_cases(raw_cases, module.name)

        suite = TestSuite(
            name=f"{module.display_name} — AI-generated",
            module=module.name,
            test_type=test_types[0] if test_types else TestType.API,
            test_cases=test_cases,
        )
        logger.info("Generated %d test cases for %s", len(test_cases), module.name)
        return suite

    def generate_for_feature(
        self,
        module: CRMModuleDefinition,
        feature: str,
    ) -> list[TestCase]:
        """Generate test cases focused on one specific feature."""
        extra = f"Focus exclusively on this feature: {feature}"
        suite = self.generate_test_suite(module, extra_context=extra)
        return suite.test_cases

    # ── internals ────────────────────────────────────────────────────

    def _call_openai(
        self,
        module: CRMModuleDefinition,
        test_types: list[TestType],
        extra_context: str,
    ) -> str:
        user_prompt = _build_user_prompt(module, test_types, extra_context)
        response = self.client.chat.completions.create(
            model=self.model,
            temperature=0.4,
            response_format={"type": "json_object"},
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
        )
        content = response.choices[0].message.content or "[]"
        logger.debug("OpenAI raw response length: %d", len(content))
        return content

    def _parse_cases(self, raw: str, module_name: str) -> list[TestCase]:
        """Parse the JSON blob returned by OpenAI into TestCase models."""
        try:
            data = json.loads(raw)
        except json.JSONDecodeError:
            logger.error("Failed to parse OpenAI JSON: %s", raw[:300])
            return []

        # handle {"test_cases": [...]} or [...]
        if isinstance(data, dict):
            data = data.get("test_cases") or data.get("tests") or data.get("testCases") or []
        if not isinstance(data, list):
            logger.error("Unexpected data type from OpenAI: %s", type(data))
            return []

        cases: list[TestCase] = []
        for item in data:
            try:
                cases.append(self._item_to_case(item, module_name))
            except Exception as exc:
                logger.warning("Skipping malformed test case: %s — %s", exc, str(item)[:200])
        return cases

    @staticmethod
    def _item_to_case(item: dict, module_name: str) -> TestCase:
        steps = [
            TestStep(
                order=s.get("order", i + 1),
                action=s.get("action", ""),
                target=s.get("target", ""),
                value=str(s.get("value", "")),
                expected=s.get("expected", ""),
            )
            for i, s in enumerate(item.get("steps", []))
        ]
        return TestCase(
            module=module_name,
            title=item.get("title", "Untitled"),
            description=item.get("description", ""),
            test_type=TestType(item.get("test_type", "api")),
            severity=Severity(item.get("severity", "major")),
            priority=Priority(item.get("priority", "P1")),
            preconditions=item.get("preconditions", []),
            steps=steps,
            expected_result=item.get("expected_result", ""),
            tags=item.get("tags", []),
        )
