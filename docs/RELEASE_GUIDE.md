# Release guide

## 1. Prepare environment

- JDK 17 or newer.
- Android SDK Platform 36.
- Current Android Studio compatible with the pinned AGP/Kotlin versions.
- A protected signing key stored outside the repository.

## 2. Verify source

Read `DEPENDENCY_NOTES.md` before changing Room, AndroidX SQLite, SQLCipher, Kotlin, KSP, AGP, or the Compose BOM.


```bash
./scripts/verify_all.sh
./gradlew clean :app:testDebugUnitTest :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
```

## 3. Build debug and fixture

```bash
./gradlew :app:assembleDebug :fixture:assembleDebug
```

Install both and execute the complete fixture matrix.

## 4. Configure signing

Do not commit passwords or keystores. Supply release signing through local Gradle properties, environment variables, or CI secrets, then add an explicit `signingConfig` in a private/release configuration.

## 5. Build release

```bash
./gradlew :app:bundleRelease :app:assembleRelease
```

Inspect:

- merged release manifest;
- dependency report;
- R8 mapping and warnings;
- APK/AAB signing;
- package ID, version code, and version name;
- native SQLCipher libraries for required ABIs;
- Room 2.6.1 / AndroidX SQLite 2.6.2 / SQLCipher 4.17.0 compatibility pin;
- SQLCipher Java-client logging remains routed to `NoopTarget`;
- no API-30-only framework message parser is present.

## 6. Test release build

Use the release APK/AAB-derived APK, not only debug. Verify capture after reboot, listener rebind, encryption, search, deletion, retention, export, biometric lock, screenshot protection, and at least the critical fixture scenarios.

## 7. Compliance

Review the prominent disclosure, privacy policy, Play Data Safety form, target-SDK requirement, store claims, screenshots, and support contact against the exact release.

## 8. Rollout

Use internal testing, then closed testing, then a staged production rollout. Monitor only privacy-safe aggregate Play console signals. Do not add remote content telemetry to diagnose notification parsing.
