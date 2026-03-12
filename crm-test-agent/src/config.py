"""
Configuration — loads .env and exposes typed settings via Pydantic.
"""
from __future__ import annotations

from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


_ROOT = Path(__file__).resolve().parent.parent  # crm-test-agent/


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=str(_ROOT / ".env"),
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # ── Environment ──────────────────────────────────────────────────
    env: str = "development"

    # ── CRM under test ───────────────────────────────────────────────
    crm_base_url: str = "http://localhost:3000"
    crm_api_base: str = "http://localhost:3000/api/v1"

    auth_service_url: str = "http://localhost:9081"
    lead_service_url: str = "http://localhost:9082"
    account_service_url: str = "http://localhost:9083"
    contact_service_url: str = "http://localhost:9084"
    opportunity_service_url: str = "http://localhost:9085"
    activity_service_url: str = "http://localhost:9086"

    # ── Test credentials ─────────────────────────────────────────────
    test_admin_email: str = "admin@crm.test"
    test_admin_password: str = "Admin@12345"
    test_tenant_id: str = "default"

    # ── OpenAI ───────────────────────────────────────────────────────
    openai_api_key: str = ""
    openai_model: str = "gpt-4o"

    # ── Playwright ───────────────────────────────────────────────────
    headless: bool = True
    browser: str = "chromium"
    slow_mo: int = 0
    screenshot_on_failure: bool = True
    video_on_failure: bool = False

    # ── Bug tracking ─────────────────────────────────────────────────
    jira_url: str = ""
    jira_email: str = ""
    jira_api_token: str = ""
    jira_project_key: str = "CRM"

    # ── Reporting ────────────────────────────────────────────────────
    allure_results_dir: str = "./allure-results"
    report_output_dir: str = "./reports"

    # ── Derived paths ────────────────────────────────────────────────
    @property
    def root_dir(self) -> Path:
        return _ROOT

    @property
    def screenshots_dir(self) -> Path:
        d = _ROOT / "screenshots"
        d.mkdir(exist_ok=True)
        return d

    @property
    def generated_tests_dir(self) -> Path:
        d = _ROOT / "generated_tests"
        d.mkdir(exist_ok=True)
        return d


settings = Settings()
