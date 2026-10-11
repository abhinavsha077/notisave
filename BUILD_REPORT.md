# Echo Inbox build and validation report

**Snapshot date:** 2026-08-17 (Asia/Kolkata)  
**Application version:** 0.1.0  
**Main package:** `com.echo.inbox`  
**Fixture package:** `com.echo.inbox.fixture`  
**Source status:** End-to-end implementation complete; Android SDK build and physical-device certification remain release gates.

## Delivered source

The repository contains a complete Android Studio project with two installable application modules:

| Module | Purpose |
|---|---|
| `app` | Local-only notification archive, reconstruction engine, encrypted storage, search, privacy controls, health diagnostics, and Compose UI. |
| `fixture` | Deterministic notification generator for cumulative messages, duplicate text, sliding windows, OTP redaction, key reuse, grouped notifications, summaries, Unicode, and burst load. |

The main implementation includes:

- Android `NotificationListenerService` capture and listener lifecycle handling;
- bounded, non-blocking, single-writer processing with conflated health telemetry;
- API-26-safe current and historic `MessagingStyle` extraction through AndroidX, plus InboxStyle, BigText, basic text, grouping, and removal metadata;
- immutable event-sourced notification snapshots;
- ordered LCS-based reconstruction that preserves repeated identical messages and sliding notification windows;
- lifecycle-safe notification-key reuse and removal scoping;
- summary-only detection without inventing hidden message bodies;
- context-aware OTP redaction and Android Keystore HMAC identities;
- SQLCipher-encrypted Room persistence with a Keystore-wrapped random database passphrase;
- FTS4 search across app, conversation, sender, and body, with app/date filters;
- per-app capture controls, sensitive-app defaults with second-step consent, pause, retention, deletion, and bounded user-selected JSON export;
- fail-closed biometric/device-credential gate, foreground relocking, and optional screenshot/recent-task protection;
- Compose Material 3 onboarding, inbox, app detail, timeline, search, Privacy Center, Settings, Capture Health, and lock screens;
- scheduled retention cleanup, local capture test, OEM guidance, and listener diagnostics;
- release shrinking configuration, backup exclusion, privacy-safe manifests, CI, tests, security documentation, and release documentation.

## Privacy posture verified in source

The main manifest does not request `INTERNET`, accessibility, SMS, contacts, call log, broad storage, location, microphone, camera, or `QUERY_ALL_PACKAGES`. Backup and device transfer are disabled. The only ordinary runtime permission is `POST_NOTIFICATIONS`, used for the explicit local capture-health test; notification access remains a user-granted Android special access.

The source contains no ad, analytics, remote-config, crash-reporting, or networking SDK. Production source contains no `android.util.Log` calls. Readable snapshot fields are redacted before persistence, structured source messages and temporary attachment URIs are discarded from stored raw snapshots, and opaque identities are omitted from export.

## Source inventory at freeze

| Measure | Count |
|---|---:|
| Repository files | 137 |
| Kotlin/Kotlin-script files | 88 |
| Kotlin/Kotlin-script lines | 8,023 |
| Android/XML resources and manifests | 13 |
| Kotlin unit/instrumentation/harness files | 11 |

## Verification performed in this environment

The following command completed successfully:

```bash
./scripts/verify_all.sh
```

Results:

| Verification | Result |
|---|---:|
| Pure Kotlin reconstruction harness | 17 / 17 passed |
| Executable SQLite/FTS/cascade/export contract | 16 / 16 passed |
| Privacy-manifest allowlist and backup rules | 7 / 7 passed |
| Project, dependency, consent, storage-boundary, locking, export, identity, and concurrency audit | 35 / 35 passed |
| Kotlin/KTS grammar scan | Passed |
| Checksum-verifying wrapper bootstrap test | Passed |
| Combined lightweight suite | `ALL_LIGHTWEIGHT_CHECKS_PASSED` |

The checks specifically cover cumulative eight-message reconstruction, repeated identical text, current-to-historic rollover, sliding windows, edits, summaries, structured-message precedence, contextual OTP redaction, distinct redacted OTP identities, sanitized snapshot persistence, conversation identity, lifecycle reuse, attachment type, FTS prefix and metadata search, removal scoping, retention cascades, orphan prevention, foreign-key integrity, permission allowlists, backup exclusion, bounded payloads, bounded queueing, conflated telemetry, SQLCipher logging suppression, compatibility pins, and source completeness.

Additional package-time checks passed for shell syntax, Python bytecode compilation, workflow YAML parsing, XML parsing, and wrapper Java compilation. ZIP integrity and the external SHA-256 checksum are verified after the repository is frozen into its delivery archive.

## What could not be executed here

This container has OpenJDK 21 and a local Kotlin/JVM compiler, but it has no Android SDK, `sdkmanager`, system Gradle installation, populated Gradle dependency cache, or network access for Maven/Google dependency resolution. `ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset.

The build helper was invoked and correctly stopped before Gradle execution:

```text
./scripts/build_debug.sh
exit status: 2
Set ANDROID_HOME or ANDROID_SDK_ROOT to an Android SDK containing API 36.
```

Consequently, this report does **not** claim completion of:

- Android/AndroidX symbol and API resolution;
- Gradle dependency resolution;
- KSP and Room generated-code compilation;
- AAPT2 resource linking;
- Android lint;
- R8 shrinking/obfuscation;
- native SQLCipher loading and ABI packaging;
- APK/AAB assembly or signing;
- emulator or physical-device installation;
- OEM and real-source-application certification.

No APK or AAB is included. Producing one without the Android build passing would be misleading.

## Required Android build gate

On a machine with Android SDK Platform 36, JDK 17 or newer, and network/dependency cache access, run:

```bash
./scripts/verify_all.sh
./gradlew clean \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :fixture:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

Expected debug artifacts:

```text
app/build/outputs/apk/debug/app-debug.apk
fixture/build/outputs/apk/debug/fixture-debug.apk
```

Before release, additionally run:

```bash
./gradlew :app:bundleRelease :app:assembleRelease
```

Then execute every critical fixture on API 26 and API 36, test encrypted close/reopen and upgrade installation, inspect the merged release manifest and Logcat, and complete the physical-device and source-app matrix in `docs/TEST_MATRIX.md`.

## Compatibility decisions

- `NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(...)` is used instead of the API-30-only framework bundle helper so the API 26 minimum remains valid.
- The encrypted database stack is intentionally pinned to Room 2.6.1, AndroidX SQLite 2.6.2, and SQLCipher for Android 4.17.0. Upgrading Room is blocked until the newer read-only transaction interface is supported and verified on-device.
- SQLCipher Java-client logging is routed to `NoopTarget` before native library loading.
- KSP 2.3.9 runs in KSP2 mode; DAO writes return explicit row IDs/counts.
- The included wrapper is a source-auditable checksum-verifying bootstrap. Replace it with Gradle's official generated wrapper after the first successful trusted Gradle download using `./scripts/regenerate_official_wrapper.sh`.

See `docs/DEPENDENCY_NOTES.md` for the upgrade evidence and regression gate.

## Release assessment

The source package is suitable for Android Studio sync, Android SDK compilation, automated CI, fixture testing, and continued product development. It should be treated as a **release candidate source implementation**, not as a Play-ready binary, until the Android build, instrumentation tests, R8 release build, security review, accessibility review, OEM matrix, and real-source-app compatibility matrix pass.
