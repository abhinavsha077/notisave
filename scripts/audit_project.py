#!/usr/bin/env python3
"""Static project audit that does not require Gradle or an Android SDK."""
from __future__ import annotations

import hashlib
import re
import sys
import tomllib
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FAILURES: list[str] = []
PASSES: list[str] = []


def check(condition: bool, message: str) -> None:
    (PASSES if condition else FAILURES).append(message)


def text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def files_under(*parts: str) -> list[Path]:
    root = ROOT.joinpath(*parts)
    return [path for path in root.rglob("*") if path.is_file()]


def main() -> int:
    required = [
        "settings.gradle.kts",
        "build.gradle.kts",
        "gradle/libs.versions.toml",
        "gradle/wrapper/gradle-wrapper.properties",
        "gradle/wrapper/gradle-wrapper.jar",
        "gradle/wrapper-src/org/gradle/wrapper/GradleWrapperMain.java",
        "gradlew",
        "gradlew.bat",
        "app/build.gradle.kts",
        "app/src/main/AndroidManifest.xml",
        "fixture/build.gradle.kts",
        "fixture/src/main/AndroidManifest.xml",
        "scripts/run_core_harness.sh",
        "scripts/test_sql_contract.py",
        "README.md",
        "BUILD_REPORT.md",
        "CHANGELOG.md",
        "LICENSE",
        ".github/workflows/android.yml",
        "docs/ARCHITECTURE.md",
        "docs/SECURITY.md",
        "docs/THREAT_MODEL.md",
        "docs/DATA_MODEL.md",
        "docs/DEPENDENCY_NOTES.md",
        "docs/TEST_MATRIX.md",
        "docs/RELEASE_GUIDE.md",
        "docs/PLAY_STORE_CHECKLIST.md",
    ]
    missing = [path for path in required if not (ROOT / path).is_file()]
    check(not missing, f"required project files exist ({len(required)} checked); missing={missing}")

    # Every XML file must parse.
    xml_files = [path for path in ROOT.rglob("*.xml") if "/build/" not in path.as_posix()]
    xml_errors: list[str] = []
    for path in xml_files:
        try:
            ET.parse(path)
        except ET.ParseError as error:
            xml_errors.append(f"{path.relative_to(ROOT)}: {error}")
    check(not xml_errors, f"all {len(xml_files)} XML resources parse; errors={xml_errors}")

    source_files = [
        path for path in ROOT.rglob("*")
        if path.is_file()
        and path.suffix in {".kt", ".kts", ".java", ".xml", ".toml"}
        and "/build/" not in path.as_posix()
    ]
    combined = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in source_files)
    check(not re.search(r"\b(?:TODO|FIXME|NotImplementedError)\b", combined), "no TODO/FIXME/NotImplemented stubs")

    app_code = "\n".join(
        path.read_text(encoding="utf-8", errors="replace")
        for path in files_under("app", "src", "main")
    )
    check("android.util.Log" not in app_code and "printStackTrace" not in app_code, "production app does not log captured content")
    check("WebView" not in app_code and "HttpURLConnection" not in app_code and "OkHttp" not in app_code and "Retrofit" not in app_code, "production app contains no network client")

    catalog = tomllib.loads(text("gradle/libs.versions.toml"))
    modules = {item.get("module", "") for item in catalog.get("libraries", {}).values()}
    banned_dependency_fragments = ("okhttp", "retrofit", "firebase", "analytics", "crashlytics", "ads")
    banned_modules = sorted(module for module in modules if any(fragment in module.lower() for fragment in banned_dependency_fragments))
    check(not banned_modules, f"dependency catalog contains no ad, analytics, crash, or network SDKs; found={banned_modules}")

    versions = catalog.get("versions", {})
    required_versions = {
        "agp": "8.13.2",
        "kotlin": "2.3.10",
        "ksp": "2.3.9",
        "composeBom": "2026.06.00",
        "room": "2.6.1",
    }
    mismatches = {key: versions.get(key) for key, expected in required_versions.items() if versions.get(key) != expected}
    check(not mismatches, f"verified toolchain coordinates are pinned; mismatches={mismatches}")

    manifest = text("app/src/main/AndroidManifest.xml")
    check("android.permission.INTERNET" not in manifest, "main manifest declares no Internet permission")
    check("android.permission.QUERY_ALL_PACKAGES" not in manifest, "main manifest avoids broad package visibility")
    check("android:allowBackup=\"false\"" in manifest, "Android backup is disabled")
    check("android:usesCleartextTraffic=\"false\"" in manifest, "cleartext traffic is disabled")

    processor = text("app/src/main/java/com/echo/inbox/capture/CaptureProcessor.kt")
    check("settingsRepository.current()" in processor, "capture waits for persisted privacy settings")
    settings_repository = text("app/src/main/java/com/echo/inbox/data/settings/SettingsRepository.kt")
    check(
        "loadedSettings = loaded" in settings_repository
        and "return checkNotNull(loadedSettings)" in settings_repository,
        "privacy settings publish a real loaded snapshot before background work resumes",
    )
    check("NotificationLifecycle.isSameActiveLifecycle" in processor, "notification key reuse is lifecycle-aware")
    check("AndroidKeystoreIdentityHasher" in text("app/src/main/java/com/echo/inbox/di/AppContainer.kt"), "production message identity uses Android Keystore HMAC")
    main_activity = text("app/src/main/java/com/echo/inbox/MainActivity.kt")
    check(
        "settings.biometricLockEnabled && !unlocked" in main_activity
        and "settings.biometricLockEnabled && biometricAuthenticator.canAuthenticate()" not in main_activity,
        "enabled UI lock fails closed if device authentication becomes unavailable",
    )
    check(
        "if (!isChangingConfigurations)" in main_activity
        and "container.settingsRepository.settings.value.biometricLockEnabled" not in main_activity,
        "foreground unlock state is cleared without depending on a startup settings placeholder",
    )
    onboarding_vm = text("app/src/main/java/com/echo/inbox/ui/viewmodel/OnboardingViewModel.kt")
    onboarding_screen = text("app/src/main/java/com/echo/inbox/ui/screens/OnboardingScreen.kt")
    check(
        "confirmedSensitivePackages" in onboarding_vm
        and "unconfirmedSensitive" in onboarding_vm
        and "confirmSensitivePackage" in onboarding_vm
        and "Capture notifications from" in onboarding_screen,
        "sensitive sources require a second affirmative onboarding confirmation",
    )

    normalizer = text("app/src/main/java/com/echo/inbox/capture/NotificationNormalizer.kt")
    check("const val PARSER_VERSION = 5" in normalizer, "stored normalized-message parser version is current")
    check("identityHash" in normalizer and "OtpRedactor.redact" in normalizer, "redaction and opaque identity are both applied")
    identity_section = normalizer[normalizer.index("private fun identity("):]
    check(
        "historic: Boolean" not in identity_section and "historic.toString()" not in identity_section,
        "message identity remains stable when Android moves an item into historic messages",
    )
    check(
        "sanitizeForStorage" in normalizer
        and "messages = emptyList()" in normalizer
        and "contentHash = identityHasher.digest(canonical)" in normalizer
        and "appendCanonical(storedRaw.shortcutId)" in normalizer
        and "appendCanonical(storedRaw.subText)" in normalizer,
        "only redacted snapshot text and unambiguous keyed content identities cross the storage boundary",
    )

    extractor = text("app/src/main/java/com/echo/inbox/capture/NotificationSnapshotExtractor.kt")
    check(
        "TEXT_LIMIT = 32_768" in extractor and "MESSAGE_LIMIT = 200" in extractor,
        "notification payload size is bounded before queueing and LCS diffing",
    )
    check(
        "NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification" in extractor
        and "Notification.MessagingStyle.Message.getMessagesFromBundleArray" not in extractor,
        "MessagingStyle extraction is compatible with the API 26 minimum",
    )

    database_factory = text("app/src/main/java/com/echo/inbox/data/local/DatabaseFactory.kt")
    check(
        "Logger.setTarget(NoopTarget())" in database_factory,
        "SQLCipher Java-client Logcat output is disabled before database open",
    )

    entities = text("app/src/main/java/com/echo/inbox/data/local/Entities.kt")
    check("@Fts4" in entities and "conversationName" in entities and "appName" in entities, "FTS covers app, conversation, sender, and body")
    check("onDelete = ForeignKey.CASCADE" in entities, "Room entities use cascading lifecycle cleanup")
    export_writer = text("app/src/main/java/com/echo/inbox/export/JsonExportWriter.kt")
    message_dao = text("app/src/main/java/com/echo/inbox/data/local/MessageDao.kt")
    check(
        "EXPORT_PAGE_SIZE = 500" in export_writer
        and "exportUpperBoundId" in export_writer
        and "exportPage" in export_writer
        and 'openOutputStream(uri, "wt")' in export_writer
        and "maxIdForExport" in message_dao,
        "readable export is truncated safely and streamed in bounded stable pages",
    )

    queue = text("app/src/main/java/com/echo/inbox/capture/CaptureCoordinator.kt")
    check("QUEUE_CAPACITY = 4_096" in queue and "trySend" in queue, "capture callbacks use a bounded non-blocking queue")
    check(
        queue.count("Channel.CONFLATED") >= 2 and "unreportedDrops" in queue,
        "queue depth and drop telemetry are conflated instead of spawning per-event workers",
    )

    worker = text("app/src/main/java/com/echo/inbox/worker/RetentionCleanupWorker.kt")
    check("settingsRepository.current()" in worker, "retention waits for persisted settings")
    scheduler = text("app/src/main/java/com/echo/inbox/worker/RetentionScheduler.kt")
    check(
        "setRequiresStorageNotLow(true)" not in scheduler,
        "storage-reclaiming retention remains eligible when device storage is low",
    )

    wrapper_properties = text("gradle/wrapper/gradle-wrapper.properties")
    checksum_match = re.search(r"distributionSha256Sum=([0-9a-f]{64})", wrapper_properties)
    check(bool(checksum_match), "Gradle distribution has a pinned SHA-256 checksum")
    wrapper_jar = ROOT / "gradle/wrapper/gradle-wrapper.jar"
    check(wrapper_jar.stat().st_size > 1_000, f"wrapper bootstrap JAR is present ({wrapper_jar.stat().st_size} bytes)")

    kotlin_files = list(ROOT.rglob("*.kt")) + list(ROOT.rglob("*.kts"))
    kotlin_lines = sum(path.read_text(encoding="utf-8", errors="replace").count("\n") + 1 for path in kotlin_files)
    test_files = [path for path in kotlin_files if "/src/test/" in path.as_posix() or "/src/androidTest/" in path.as_posix() or "/tools/core-harness/" in path.as_posix()]
    check(len(test_files) >= 10, f"test coverage structure exists ({len(test_files)} Kotlin test/harness files)")

    for message in PASSES:
        print(f"PASS: {message}")
    for message in FAILURES:
        print(f"FAIL: {message}", file=sys.stderr)

    print(f"AUDIT_COUNTS files={sum(1 for p in ROOT.rglob('*') if p.is_file())} kotlin_files={len(kotlin_files)} kotlin_lines={kotlin_lines} xml_files={len(xml_files)}")
    if FAILURES:
        print(f"PROJECT_AUDIT_FAILED ({len(FAILURES)} failures)", file=sys.stderr)
        return 1
    print(f"PROJECT_AUDIT_PASSED ({len(PASSES)} checks)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
