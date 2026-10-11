#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd -P)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT INT TERM

PROJECT="$WORK/project"
DIST="$WORK/fake-gradle"
mkdir -p "$PROJECT/gradle/wrapper" "$DIST/gradle-fake/bin" "$WORK/bin"
javac -d "$WORK/bin" "$ROOT/gradle/wrapper-src/org/gradle/wrapper/GradleWrapperMain.java"
jar -cfe "$PROJECT/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain -C "$WORK/bin" .
cp "$ROOT/gradlew" "$PROJECT/"
chmod +x "$PROJECT/gradlew"
cat > "$DIST/gradle-fake/bin/gradle" <<'EOF'
#!/bin/sh
echo "FAKE_GRADLE_OK:$*"
EOF
chmod +x "$DIST/gradle-fake/bin/gradle"
(
  cd "$DIST"
  zip -qr "$WORK/fake-gradle.zip" gradle-fake
)
SHA=$(sha256sum "$WORK/fake-gradle.zip" | awk '{print $1}')
cat > "$PROJECT/gradle/wrapper/gradle-wrapper.properties" <<EOF
distributionUrl=file\:$WORK/fake-gradle.zip
distributionSha256Sum=$SHA
networkTimeout=10000
validateDistributionUrl=true
EOF
OUTPUT=$(cd "$PROJECT" && GRADLE_USER_HOME="$WORK/gradle-home" ./gradlew alpha beta)
printf '%s\n' "$OUTPUT"
printf '%s' "$OUTPUT" | grep -q 'FAKE_GRADLE_OK:alpha beta'
echo "WRAPPER_BOOTSTRAP_TEST_PASSED"
