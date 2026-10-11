#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
KOTLINC=${KOTLINC:-kotlinc}
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT INT TERM

"$KOTLINC" \
  "$ROOT/app/src/main/java/com/echo/inbox/core/model/Models.kt" \
  "$ROOT/app/src/main/java/com/echo/inbox/core/util/Hashing.kt" \
  "$ROOT/app/src/main/java/com/echo/inbox/core/util/ContentIdentityHasher.kt" \
  "$ROOT/app/src/main/java/com/echo/inbox/core/util/OtpRedactor.kt" \
  "$ROOT/app/src/main/java/com/echo/inbox/core/util/OrderedMessageDiff.kt" \
  "$ROOT/app/src/main/java/com/echo/inbox/core/util/NotificationLifecycle.kt" \
  "$ROOT/app/src/main/java/com/echo/inbox/capture/ConversationKeyResolver.kt" \
  "$ROOT/app/src/main/java/com/echo/inbox/capture/NotificationNormalizer.kt" \
  "$ROOT/tools/core-harness/JsonCodecJvmStub.kt" \
  "$ROOT/tools/core-harness/CoreHarness.kt" \
  -include-runtime \
  -d "$WORK/core-harness.jar"

java -jar "$WORK/core-harness.jar"
