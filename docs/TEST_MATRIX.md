# Test matrix

## Automated checks

### Dependency-free core harness

```bash
./scripts/run_core_harness.sh
```

Covers cumulative updates, duplicate identical messages, current-to-historic rollover, sliding windows, edits, summary-only payloads, structured precedence, OTP redaction, redacted identity separation, conversation keys, lifecycle reuse, and attachments.

### SQLite contract

```bash
./scripts/test_sql_contract.py
```

Covers FTS4 prefix behavior, metadata search, projections, ordering, bounded keyset export, stable export upper bounds, lifecycle-scoped removal, retention cascades, orphan prevention, conversation cascades, and foreign-key integrity.

### Privacy and static audit

```bash
./scripts/verify_privacy_manifest.py
./scripts/audit_project.py
./scripts/check_kotlin_grammar.sh
./scripts/test_wrapper_bootstrap.sh
```

### Gradle tests

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

### Instrumentation tests

```bash
./gradlew :app:connectedDebugAndroidTest
```

Instrumentation verifies the generated Room schema/FTS/cascades and Android Keystore HMAC behavior.

## Fixture scenarios

| Scenario | Expected result |
|---|---|
| Eight cumulative messages | Exactly One through Eight, once each, ordered |
| Two identical messages | Two independent `OK` events |
| Historic message rollover | Previously current “First” is not duplicated when it becomes historic; only “Second” is added |
| Sliding history window | Only messages 6–8 added by second update |
| Two redacted OTP updates | Two redacted events remain distinct |
| Reused notification key | Identical content captured in two lifecycles |
| Summary only | Warning/summary event, no invented bodies |
| InboxStyle | Sender-prefixed lines parsed in order |
| Long Unicode | Emoji, Hindi, newline, and long layout remain readable |
| Grouped children + summary | Children and summary retained without silent overwrites |
| Rapid burst ×100 | Queue/write health remains stable; no crash |

## Physical-device matrix

Test at least:

- Pixel / AOSP Android 13, 14, 15, and 16;
- Samsung One UI;
- Xiaomi/Redmi/Poco HyperOS or MIUI;
- Oppo/Realme/OnePlus ColorOS/OxygenOS;
- Vivo/iQOO Funtouch OS;
- one low-memory device.

## Source-app compatibility

Record app version, Android version, notification preview settings, and observed payload behavior for:

- WhatsApp and WhatsApp Business;
- Instagram;
- Telegram;
- Signal;
- Messenger;
- Google Messages;
- Gmail and Outlook;
- Slack and Teams;
- shopping/delivery apps.

## Lifecycle scenarios

- grant/revoke/regrant Notification Access;
- reboot;
- app update;
- source-app update;
- listener rebind;
- app force-stop and reopen;
- battery saver and OEM autostart restrictions;
- user/profile changes;
- database with 100,000+ events;
- nearly full storage;
- timezone and clock changes;
- app lock with and without enrolled authenticator;
- export cancellation and write failure;
- retention from 365 to 7 days;
- delete app, conversation, and all data;
- release build with R8.

## Release acceptance

- No captured content in Logcat, test reports, or exception messages.
- Main merged manifest has no Internet or forbidden sensitive permission.
- Eight-message and duplicate-message fixture scenarios pass.
- Redacted OTP identities remain distinct.
- Old removal cannot close a reused key.
- No orphan FTS rows after every deletion path.
- Listener disconnection is visible in Capture Health.
- Search remains responsive at the target data volume.
- TalkBack, font scaling, RTL, dark theme, and rotation are usable.


## Dependency-stack regression gate

Run after any Room, AndroidX SQLite, SQLCipher, Kotlin, KSP, AGP, or target-SDK change:

- API 26 physical/emulated device: create encrypted database, close process, reopen, search, delete, and export.
- API 36 physical/emulated device: repeat the same flow.
- Upgrade install: create data with the prior release, install candidate without clearing data, reopen and verify counts.
- Release/R8 APK: repeat database open and notification capture using the minified build.
- Logcat review: confirm no title, sender, message, OTP, URI, key material, or passphrase appears.
- NotificationCompat extraction: run current and historic MessagingStyle fixtures on API 26 and API 36.
