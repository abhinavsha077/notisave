#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
if [ -z "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]; then
  echo "Set ANDROID_HOME or ANDROID_SDK_ROOT to an Android SDK containing API 36." >&2
  exit 2
fi
cd "$ROOT"
./gradlew --stacktrace :app:assembleDebug :fixture:assembleDebug
printf '\nMain APK: %s\n' "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
printf 'Fixture APK: %s\n' "$ROOT/fixture/build/outputs/apk/debug/fixture-debug.apk"
