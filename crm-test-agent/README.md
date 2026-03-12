# CRM AI Test Agent

An AI-powered testing agent for the CRM SaaS platform. Uses **OpenAI GPT-4o** to generate comprehensive test cases, **Playwright** for browser-based UI testing, and **Allure** for beautifully detailed reports.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CRM Test Agent                           │
│                                                             │
│  ┌──────────────┐   ┌───────────────┐   ┌──────────────┐  │
│  │  CRM Module  │──▶│  AI Test Case │──▶│  Playwright   │  │
│  │  Definitions │   │  Generator    │   │  Script Gen   │  │
│  └──────────────┘   └───────────────┘   └──────┬───────┘  │
│                            │                     │          │
│                            ▼                     ▼          │
│                    ┌──────────────┐   ┌──────────────────┐ │
│                    │  API Test    │   │  UI Test         │ │
│                    │  Engine      │   │  Executor        │ │
│                    └──────┬───────┘   └────────┬─────────┘ │
│                           │                     │           │
│                           ▼                     ▼           │
│                    ┌───────────────────────────────────┐    │
│                    │  Bug Reporter + Allure Reports    │    │
│                    └───────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## CRM Modules Covered

| Module | API Tests | UI Tests | Features |
|--------|-----------|----------|----------|
| **Lead** | CRUD, search, filter | Create form, data grid, status filter | Lead scoring, conversion, duplicate detection |
| **Account** | CRUD, hierarchy | Create form, data grid | Parent-child, related contacts/opps |
| **Contact** | CRUD, link to account | Create form, data grid | Merge duplicates, activity history |
| **Opportunity** | CRUD, stage transitions | Kanban board, deal cards | Pipeline management, forecasting |
| **Activity** | CRUD tasks/calls/meetings | Activity list, calendar | Reminders, completion tracking |
| **Pipeline** | Stage management | Drag-and-drop board | Conversion rates, weighted values |
| **Reports** | Data aggregation | Charts, dashboards | Export CSV, filtered views |
| **User** | Auth (login/register) | Login form, settings | JWT, RBAC, profile management |
| **Email** | Log email activities | Email composer | Templates, tracking |

## Quick Start

```bash
# 1. Install dependencies
pip install -r requirements.txt
playwright install chromium

# 2. Configure environment
cp .env.example .env
# Edit .env with your OpenAI API key and CRM URLs

# 3. Run the full test pipeline
python -m src.agent full-run

# 4. Run specific modules
python -m src.agent generate --modules lead account
python -m src.agent execute --modules lead --types api

# 5. Generate Allure report
python -m src.agent report
```

## CLI Commands

| Command | Description |
|---------|-------------|
| `python -m src.agent generate` | Generate test cases via AI for all modules |
| `python -m src.agent generate -m lead contact` | Generate for specific modules |
| `python -m src.agent execute` | Execute all generated tests |
| `python -m src.agent execute -t api` | Execute only API tests |
| `python -m src.agent execute -t ui` | Execute only UI/Playwright tests |
| `python -m src.agent report` | Generate Allure HTML report |
| `python -m src.agent full-run` | Generate → Execute → Report |

## Running Unit Tests

```bash
pytest tests/ -v
```

## Project Structure

```
crm-test-agent/
├── src/
│   ├── config.py              # Pydantic Settings from .env
│   ├── models.py              # Domain models (TestCase, BugReport, etc.)
│   ├── crm_modules.py         # 9 CRM module definitions
│   ├── ai_test_generator.py   # OpenAI-powered test generation
│   ├── playwright_generator.py # Jinja2 → executable test scripts
│   ├── api_test_engine.py     # Direct API test execution via httpx
│   ├── ui_test_executor.py    # Playwright test runner via pytest
│   ├── bug_reporter.py        # Bug reports + Jira + Allure
│   └── agent.py               # Main orchestrator + CLI
├── tests/
│   └── test_agent_core.py     # Unit tests (mocked AI)
├── generated_tests/           # AI-generated test scripts (auto-created)
├── conftest.py                # Shared pytest fixtures
├── pytest.ini                 # pytest configuration
├── requirements.txt           # Python dependencies
├── .env                       # Environment variables
└── README.md
```

## Tech Stack

- **Python 3.11+**
- **OpenAI GPT-4o** — AI test-case generation
- **Playwright** — Browser automation for UI tests
- **pytest** — Test runner
- **Allure** — Rich test reports
- **httpx** — Async-capable HTTP client for API tests
- **Pydantic v2** — Data models and settings
- **Jinja2** — Script template rendering
- **Rich** — Beautiful CLI output
- **Jira** (optional) — Bug ticket creation
