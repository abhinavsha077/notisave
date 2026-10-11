# NotiSave (Echo Inbox)

NotiSave (Echo Inbox) is a privacy-first, local-only Android notification saver and archive. It records notification content from applications the user explicitly enables, preserves repeated notification updates as immutable snapshots, and reconstructs message-style notifications into readable chronological conversations with media attachment preservation and delete-recovery support.

The repository contains two Android applications:

- **`app`** — the Echo Inbox application.
- **`fixture`** — a deterministic notification generator for compatibility and regression testing.

## What is implemented

- Four-step trust-focused onboarding and prominent notification-access disclosure.
- Explicit per-application capture selection, including sensitive-app warnings.
- `NotificationListenerService` capture with a bounded, non-blocking, single-writer queue.
- API-26-safe extraction of `MessagingStyle` and historic messages through `NotificationCompat`, plus `InboxStyle`, `BigTextStyle`, basic text, grouping metadata, channels, shortcuts, locus IDs, and removal reasons.
- Event-sourced immutable notification snapshots.
- Ordered LCS-based message reconstruction that preserves repeated identical text.
- Notification lifecycle separation using key plus original post time.
- Summary-only detection instead of inventing content Android did not expose.
- OTP redaction with Android Keystore-backed opaque identities, so different codes remain distinct without storing or exporting the original code.
- SQLCipher-encrypted Room database with Android Keystore-wrapped passphrase and SQLCipher Java-client Logcat output disabled before open.
- FTS4 search across app name, conversation, sender, and message body, with app and date filters.
- Inbox, application, conversation, search, Privacy Center, Settings, and Capture Health screens in Jetpack Compose/Material 3.
- Global pause, app switches, retention, immediate cleanup, app/conversation/all-data deletion, readable JSON export, biometric/device-credential lock, and screenshot protection.
- No account, ad SDK, analytics SDK, crash-reporting SDK, cloud sync, accessibility service, SMS/contact/call-log permission, broad storage permission, or `INTERNET` permission.
- Daily WorkManager retention cleanup that waits for persisted settings before deleting anything.
- Listener health diagnostics, OEM guidance, rebind action, and user-triggered local capture test.
- Unit tests, Android instrumentation tests, a dependency-free JVM core harness, SQLite contract tests, static privacy checks, wrapper bootstrap tests, and CI.

## Important limitations

Echo Inbox receives only what another app exposes through Android notifications. It does **not** read WhatsApp, Instagram, Telegram, email, or other apps' private databases. It cannot recover:

- notifications posted before notification access was granted;
- content hidden by the source app or Android privacy settings;
- message bodies when the notification exposes only a summary such as “8 new messages”;
- notifications missed while the listener was disconnected or the device vendor blocked background operation.

A reconstructed timeline is therefore a notification archive, not an authoritative copy of the source conversation.

## Toolchain

- Android Gradle Plugin 8.13.2
- Gradle 8.13
- Kotlin 2.3.10
- KSP 2.3.9 (KSP2)
- Compose BOM 2026.06.00
- Room 2.6.1 (SQLCipher compatibility pin)
- AndroidX SQLite 2.6.2
- SQLCipher for Android 4.17.0
- `compileSdk` / `targetSdk` 36
- `minSdk` 26
- Java 17 bytecode

The exact dependency versions are centralized in `gradle/libs.versions.toml`.

## Open in Android Studio

1. Install a recent Android Studio with JDK 17 or newer.
2. Install Android SDK Platform 36 and a compatible Build Tools package.
3. Open the repository root.
4. Let Gradle sync.
5. Build both debug applications:

```bash
./gradlew :app:assembleDebug :fixture:assembleDebug
```

On Windows:

```bat
gradlew.bat :app:assembleDebug :fixture:assembleDebug
```

Expected APK locations:

```text
app/build/outputs/apk/debug/app-debug.apk
fixture/build/outputs/apk/debug/fixture-debug.apk
```

The helper performs the same build after checking for an Android SDK:

```bash
./scripts/build_debug.sh
```

## First-device workflow

1. Install `app-debug.apk` and `fixture-debug.apk`.
2. Open Echo Inbox and read the disclosure.
3. Select **Echo Fixture** and any real applications you want to test.
4. Grant Android Notification Access.
5. Finish setup.
6. Open Echo Fixture and run **Eight cumulative messages**.
7. Open the reconstructed conversation in Echo Inbox and verify messages One through Eight appear once and in order.
8. Run **Two identical messages**, **Historic message rollover**, **Two redacted OTP updates**, **Reused notification key**, **Sliding history window**, and **Grouped children + summary**.
9. Open **Capture Health** and run the local capture test.

## Verification without an Android SDK

The following suite is intentionally runnable in a lightweight source environment:

```bash
./scripts/verify_all.sh
```

It executes:

- dependency-free Kotlin reconstruction tests;
- executable SQLite/FTS/cascade contract tests;
- manifest privacy allowlist checks;
- broad project audit;
- Kotlin grammar scan;
- local fake-distribution wrapper bootstrap and checksum test.

A real Android build remains the source of truth for Android/AndroidX type resolution, Room code generation, resource linking, lint, R8, and APK packaging.

## Gradle wrapper note

This generated repository includes a small source-auditable bootstrap JAR because the execution environment could not download binary ZIP/JAR artifacts directly. It accepts only local files or Gradle's official distribution hosts, verifies the pinned SHA-256, prevents ZIP path traversal, serializes extraction with a file lock, and then delegates to Gradle.

Its source is in:

```text
gradle/wrapper-src/org/gradle/wrapper/GradleWrapperMain.java
```

After the first successful Gradle download, replace it with Gradle's stock wrapper:

```bash
./scripts/regenerate_official_wrapper.sh
```

## Repository map

```text
app/src/main/java/com/echo/inbox/
├── capture/       listener, extractor, normalizer, lifecycle, diff pipeline
├── core/          models and pure algorithms
├── data/          Room, DAOs, repositories, DataStore settings
├── export/        user-selected JSON export
├── platform/      Android settings, app metadata, local test notification
├── security/      SQLCipher key wrapping, HMAC identity, biometric support
├── ui/            Compose screens, components, navigation, ViewModels
└── worker/        scheduled retention cleanup

fixture/           controlled notification scenarios
scripts/           build and verification entry points
docs/              architecture, security, privacy, testing, and release docs
tools/core-harness dependency-free JVM test harness
```

## Security and privacy

Read `docs/SECURITY.md` and `docs/THREAT_MODEL.md` before modifying capture, export, logging, backup, or permissions. The release manifest must remain local-only unless the product is deliberately redesigned and re-reviewed.

## Development status

This is a complete source implementation and validation package, not a claim that every OEM/source-app combination is already certified. Notification payloads change across app and Android versions. Read `BUILD_REPORT.md`, then run the fixture suite and the real-device matrix in `docs/TEST_MATRIX.md` before public release.

## Compatibility pins that must not be upgraded blindly

The encrypted persistence stack currently uses Room 2.6.1, AndroidX SQLite 2.6.2, and SQLCipher for Android 4.17.0. Room 2.6.1 is intentional: newer Room 2.8.x code can call a read-only transaction method not implemented by the current SQLCipher support database. Message extraction also deliberately uses AndroidX `NotificationCompat` rather than the API-30-only framework bundle helper so the API 26 minimum remains valid.

See `docs/DEPENDENCY_NOTES.md` for the evidence, upgrade gate, and required device tests.

## License

Apache License 2.0. See `LICENSE`.
