# Changelog

## 0.1.0 — initial source implementation

- Added local-only notification listener and bounded capture pipeline.
- Added event-sourced snapshots and ordered conversation reconstruction.
- Added lifecycle-safe deduplication, current-to-historic rollover handling, and removal handling.
- Added SQLCipher/Room storage, FTS4 search, retention, deletion, and bounded streaming JSON export.
- Added OTP redaction with Android Keystore HMAC identities.
- Added Compose onboarding, inbox, search, conversations, Privacy Center, Settings, lock, and Capture Health.
- Added deterministic fixture application and automated verification suites.
- Added API-26-safe `NotificationCompat` extraction for current and historic `MessagingStyle` messages.
- Pinned the encrypted persistence compatibility stack to Room 2.6.1, AndroidX SQLite 2.6.2, and SQLCipher 4.17.0.
- Disabled SQLCipher Java-client Logcat output before native library loading.
- Hardened startup privacy settings, queue telemetry, notification-key reuse, OTP storage boundaries, FTS cleanup, fail-closed foreground locking, and sensitive-source consent.
- Added dependency compatibility notes, CI, release guidance, and a reproducible build/validation report.
