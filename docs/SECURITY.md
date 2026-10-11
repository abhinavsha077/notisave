# Security design

## Data classification

Treat every captured title, sender, body, summary, and source metadata value as highly sensitive. Notifications may contain private messages, verification codes, financial alerts, medical information, email previews, work data, and addresses.

## Controls implemented

### No network capability

The main application does not declare `android.permission.INTERNET` and includes no networking, ad, analytics, remote-config, or crash-reporting library. The fixture app is also network-free.

### Encryption at rest

Room is backed by SQLCipher. `DatabaseKeyManager` creates a random 256-bit passphrase and stores only an AES-GCM-wrapped form in private preferences. The wrapping key is non-exportable in Android Keystore.

### Opaque message identity

OTP redaction can make different messages look identical. Production builds HMAC the unredacted identity material with a separate non-exportable Android Keystore HMAC key. Only the digest and redacted body enter the encrypted database. The digest is not exported.

### Backup exclusion

`android:allowBackup="false"`, `android:fullBackupContent="false"`, and data-extraction rules exclude root, files, databases, and preferences from cloud backup and device transfer.

### UI protection

- Optional biometric or device-credential lock.
- Lock enablement is blocked when no secure authenticator exists.
- Once enabled, the UI lock fails closed if the user later removes all secure authenticators.
- The in-memory foreground unlock token is cleared whenever the task leaves the foreground.
- API-specific authenticator combinations avoid unsupported Android 9/10 behavior.
- Screenshot and recent-task preview protection uses `FLAG_SECURE` and defaults on.

### Sensitive-source consent

Sensitive applications are disabled by default. Enabling one during onboarding requires a second app-specific affirmative confirmation after the general notification-access disclosure. The same confirmation is required when enabling a sensitive source later in Privacy Center.

### Permission minimization

The only ordinary runtime permission is `POST_NOTIFICATIONS`, used solely for a user-triggered local health test. Notification access is an explicit special-access grant. No SMS, contact, call-log, location, microphone, camera, accessibility, broad storage, package-all, or Internet permission is requested.

### Logging

Production source contains no `android.util.Log` calls and must never print captured fields, raw extras, encryption material, sender names, or message bodies. Health diagnostics store bounded error codes only.

### Export

Export is initiated through the Storage Access Framework. The UI warns that JSON is readable and not encrypted by Echo Inbox. Export includes readable message events but excludes raw snapshots, HMAC identities, key material, and app settings.

## Secure development rules

1. Never add Internet capability without a separate product and threat-model review.
2. Never add notification body fields to logs, analytics, crash reports, or exception messages.
3. Never persist `PendingIntent`, `RemoteInput`, binder objects, `RemoteViews`, or temporary content URIs.
4. Preserve explicit sensitive-app confirmation.
5. Keep capture disabled for newly discovered sensitive apps.
6. Update the privacy policy and Play Data Safety form whenever data behavior changes.
7. Add a migration and migration test before changing a shipped Room schema.
8. Run `./scripts/verify_all.sh` and the full Gradle build before release.
9. Review merged manifests, not only source manifests.
10. Test release/R8 builds on physical devices.

## Reporting a vulnerability

Do not file a public issue containing captured notification data, keys, exports, or reproduction screenshots with real private content. Use the private security contact configured by the project owner before publication.

## Storage-boundary redaction

Readable notification fields are context-redacted before any snapshot is inserted. Structured source messages and attachment URIs are not retained in the raw snapshot after their normalized representation is produced. Snapshot and message identities use an Android Keystore HMAC in production so low-entropy values such as OTPs cannot be tested against an ordinary unsalted digest.

## Dependency logging

SQLCipher's Java client uses Logcat by default. Echo Inbox installs `net.zetetic.database.NoopTarget` before loading or opening SQLCipher. Release validation must still inspect Logcat for JNI/core diagnostics and verify that no captured content, identity input, passphrase, or export data appears.
