#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$ROOT"
./scripts/run_core_harness.sh
./scripts/test_sql_contract.py
./scripts/verify_privacy_manifest.py
./scripts/audit_project.py
./scripts/check_kotlin_grammar.sh
./scripts/test_wrapper_bootstrap.sh
echo "ALL_LIGHTWEIGHT_CHECKS_PASSED"
