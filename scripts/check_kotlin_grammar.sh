#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
KOTLINC=${KOTLINC:-kotlinc}
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT INT TERM

# Dependencies and android.jar are intentionally absent in this lightweight check. kotlinc will
# report unresolved Android/AndroidX symbols, but it still performs Kotlin grammar parsing.
set +e
TERM=dumb "$KOTLINC" $(find "$ROOT/app/src" "$ROOT/fixture/src" -name '*.kt' -print) \
  -d "$WORK/syntax-only.jar" >"$WORK/diagnostics.log" 2>&1
set -e

if grep -Eiq 'expecting|unexpected tokens|syntax error' "$WORK/diagnostics.log"; then
  grep -Ein 'expecting|unexpected tokens|syntax error' "$WORK/diagnostics.log" >&2
  echo "KOTLIN_GRAMMAR_FAILED" >&2
  exit 1
fi

echo "KOTLIN_GRAMMAR_PASSED (syntax-only; Android type resolution requires Gradle/SDK)"
