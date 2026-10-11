#!/bin/sh

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P) || exit 1
JAVA_EXE="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if [ -z "$JAVA_HOME" ]; then
    JAVA_EXE=java
fi
if ! command -v "$JAVA_EXE" >/dev/null 2>&1; then
    echo "Java 17 or newer is required. Set JAVA_HOME or add java to PATH." >&2
    exit 1
fi
exec "$JAVA_EXE" -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
