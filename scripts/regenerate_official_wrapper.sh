#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
cd "$ROOT"
# The included bootstrap JAR is source-auditable and checksum-verifying. This command replaces it
# with Gradle's stock wrapper after the first distribution download.
./gradlew wrapper --gradle-version 8.13 --distribution-type bin
./gradlew wrapper --gradle-version 8.13 --distribution-type bin

echo "Official Gradle wrapper regenerated. Review and commit gradle/wrapper/gradle-wrapper.jar."
