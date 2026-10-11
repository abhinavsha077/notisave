# Dependency compatibility notes

Last reviewed: 2026-08-17

This project intentionally does not select every newest library independently. A notification archive has a stricter requirement: the Android API level, Room-generated code, SQLite support interface, SQLCipher driver, and native library must work as one tested stack.

## Notification parsing

`Notification.MessagingStyle.Message.getMessagesFromBundleArray(...)` is a framework helper introduced in Android 11 / API 30. Echo Inbox supports Android 8 / API 26, so the capture service must not call that helper directly.

`NotificationSnapshotExtractor` instead uses:

```kotlin
NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
```

AndroidX performs the framework/compat conversion and exposes current and historic messages across the supported API range. `scripts/audit_project.py` fails if the API-30-only framework call is reintroduced.

Primary reference:

- <https://developer.android.com/reference/kotlin/androidx/core/app/NotificationCompat.MessagingStyle>

## Room, AndroidX SQLite, and SQLCipher

The encrypted database stack is pinned to:

```text
Room                      2.6.1
androidx.sqlite           2.6.2
SQLCipher for Android     4.17.0
```

SQLCipher's current Android integration documentation specifies SQLCipher 4.17.0 with AndroidX SQLite 2.6.2 and `SupportOpenHelperFactory`. It also documents that Java-client logging goes to Logcat by default; `DatabaseFactory` installs `NoopTarget` before loading or opening SQLCipher.

Room is deliberately pinned to 2.6.1. An open SQLCipher issue reports that Room 2.8.4 invokes `SupportSQLiteDatabase.beginTransactionReadOnly()`, which SQLCipher's support-database implementation does not implement, causing a runtime crash. The same report identifies Room 2.6.1 as the working line. Until SQLCipher implements the newer transaction API or an upstream compatibility release is verified, upgrading Room is a blocked security/reliability change rather than routine maintenance.

Primary references:

- <https://github.com/sqlcipher/sqlcipher-android#application-integration>
- <https://github.com/sqlcipher/sqlcipher-android/issues/81>

## KSP and generated Room code

The project uses KSP 2.3.9 in KSP2 mode. DAO write methods return an explicit row count or row ID instead of implicit `Unit`; this avoids a class of KSP2/Room signature-processing failures and makes write outcomes observable during debugging.

Any dependency upgrade must pass all of the following on a clean machine:

```bash
./scripts/verify_all.sh
./gradlew clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :fixture:assembleDebug
./gradlew :app:connectedDebugAndroidTest
```

The connected test is mandatory for database-stack upgrades because source-only checks cannot prove JNI loading, Room code generation, encrypted open/reopen, FTS behavior, or device ABI packaging.

## Upgrade checklist

Before changing Room, SQLCipher, AndroidX SQLite, Kotlin, KSP, AGP, or the Compose BOM:

1. Read the release notes and open compatibility issues for the entire stack, not only the package being upgraded.
2. Build from an empty Gradle cache.
3. Inspect the generated Room schema and generated DAO code.
4. Run the encrypted Room instrumentation test on API 26 and the latest target API.
5. Create data with the previous release, install the candidate over it, and verify encrypted reopen and migration.
6. Run search, retention, cascade deletion, export, and key-reuse fixture scenarios.
7. Inspect the merged release manifest and dependency tree.
8. Run R8 release assembly and install the resulting APK.
9. Confirm Logcat contains no captured title, sender, body, OTP, URI, or database passphrase.
