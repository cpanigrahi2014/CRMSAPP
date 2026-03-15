"""
Master Runner — CRM Automation Agent
=====================================
Run all 15 modules (UI + API) with summary reporting.

Usage:
    cd crm-test-agent
    python run_automation.py                # Run all modules
    python run_automation.py --api-only     # API tests only
    python run_automation.py --ui-only      # UI tests only
    python run_automation.py --module 3     # Single module (leads)
    python run_automation.py --parallel     # Run API and UI in parallel
"""
import subprocess, sys, argparse, time, pathlib, json

ROOT = pathlib.Path(__file__).resolve().parent
AUTO = ROOT / "automation"

MODULES = {
    1:  ("test_mod01_auth.py",          "Auth Management",              30),
    2:  ("test_mod02_roles.py",         "Role & Permission",            25),
    3:  ("test_mod03_leads.py",         "Lead Management",              60),
    4:  ("test_mod04_contacts.py",      "Contact Management",           40),
    5:  ("test_mod05_accounts.py",      "Account Management",           40),
    6:  ("test_mod06_opportunities.py", "Opportunity / Deal Mgmt",      60),
    7:  ("test_mod07_pipeline.py",      "Pipeline Management",          25),
    8:  ("test_mod08_tasks.py",         "Task & Activity Mgmt",         30),
    9:  ("test_mod09_meetings.py",      "Meeting & Calendar",           20),
    10: ("test_mod10_email.py",         "Email Integration",            20),
    11: ("test_mod11_workflow.py",      "Workflow Automation",           35),
    12: ("test_mod12_ai.py",            "AI CRM Features",              40),
    13: ("test_mod13_csv.py",           "CSV Import / Data Mgmt",       20),
    14: ("test_mod14_reports.py",       "Reports & Dashboards",         20),
    15: ("test_mod15_security.py",      "Security & Compliance",        25),
}

TOTAL_EXPECTED = sum(v[2] for v in MODULES.values())  # 490


def run_module(mod_num: int, extra_args: list[str] | None = None) -> dict:
    filename, label, expected = MODULES[mod_num]
    filepath = AUTO / filename
    if not filepath.exists():
        return {"module": mod_num, "label": label, "status": "MISSING", "passed": 0,
                "failed": 0, "skipped": 0, "total": 0, "duration": 0}

    cmd = [
        sys.executable, "-m", "pytest", str(filepath),
        "-v", "--tb=short", "-q", "--no-header",
    ]
    if extra_args:
        cmd.extend(extra_args)

    start = time.time()
    result = subprocess.run(cmd, capture_output=True, text=True, cwd=str(ROOT))
    duration = time.time() - start

    output = result.stdout + result.stderr
    passed = output.count(" PASSED")
    failed = output.count(" FAILED")
    skipped = output.count(" SKIPPED")
    errors = output.count(" ERROR")
    total = passed + failed + skipped + errors

    status = "PASS" if failed == 0 and errors == 0 else "FAIL"

    return {
        "module": mod_num, "label": label, "status": status,
        "passed": passed, "failed": failed, "skipped": skipped,
        "errors": errors, "total": total, "expected": expected,
        "duration": round(duration, 1), "output": output,
    }


def print_summary(results: list[dict]):
    print("\n" + "=" * 85)
    print("  CRM AUTOMATION AGENT — TEST EXECUTION SUMMARY")
    print("=" * 85)
    print(f"{'#':<4} {'Module':<28} {'Status':<8} {'Pass':<6} {'Fail':<6} {'Skip':<6} {'Time':<8}")
    print("-" * 85)

    total_p = total_f = total_s = total_e = 0
    for r in results:
        total_p += r["passed"]
        total_f += r["failed"]
        total_s += r["skipped"]
        total_e += r.get("errors", 0)
        emoji = "PASS" if r["status"] == "PASS" else ("MISS" if r["status"] == "MISSING" else "FAIL")
        print(f"{r['module']:<4} {r['label']:<28} {emoji:<8} {r['passed']:<6} {r['failed']:<6} "
              f"{r['skipped']:<6} {r['duration']:<8}s")

    grand = total_p + total_f + total_s + total_e
    print("-" * 85)
    print(f"{'TOTAL':<33} {'PASS' if total_f == 0 and total_e == 0 else 'FAIL':<8} "
          f"{total_p:<6} {total_f:<6} {total_s:<6} {sum(r['duration'] for r in results):.1f}s")
    print(f"\nExpected: {TOTAL_EXPECTED} tests   Executed: {grand} tests")
    print("=" * 85)


def main():
    parser = argparse.ArgumentParser(description="CRM Automation Agent Runner")
    parser.add_argument("--api-only", action="store_true", help="Run API tests only")
    parser.add_argument("--ui-only", action="store_true", help="Run UI tests only")
    parser.add_argument("--module", type=int, help="Run single module by number (1-15)")
    parser.add_argument("--headed", action="store_true", help="Run UI tests in headed mode")
    parser.add_argument("--json-report", type=str, help="Save JSON report to file")
    args = parser.parse_args()

    extra: list[str] = []
    if args.api_only:
        extra.extend(["-k", "api"])
    elif args.ui_only:
        extra.extend(["-k", "ui"])
    if args.headed:
        extra.extend(["--headed"])

    modules_to_run = [args.module] if args.module else sorted(MODULES.keys())
    results = []
    print(f"\nRunning {len(modules_to_run)} module(s)…\n")

    for num in modules_to_run:
        label = MODULES[num][1]
        print(f"  [{num:02d}/15] {label} …", end="", flush=True)
        r = run_module(num, extra)
        results.append(r)
        print(f"  {r['status']}  ({r['passed']}p/{r['failed']}f/{r['skipped']}s) in {r['duration']}s")

    print_summary(results)

    if args.json_report:
        safe_results = [{k: v for k, v in r.items() if k != "output"} for r in results]
        pathlib.Path(args.json_report).write_text(json.dumps(safe_results, indent=2))
        print(f"\nJSON report saved to {args.json_report}")

    # Exit with failure if any tests failed
    sys.exit(1 if any(r["status"] == "FAIL" for r in results) else 0)


if __name__ == "__main__":
    main()
