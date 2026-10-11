# Architecture

## Goals

Echo Inbox is designed around four invariants:

1. A notification listener callback must return quickly and never perform database work on Android's callback thread.
2. A newer notification update must not overwrite an earlier readable message.
3. Disabled or globally paused sources must not persist notification content.
4. Capture, search, retention, and deletion must preserve database and FTS integrity.

## Runtime flow

```text
Source application
      │
      ▼
NotificationListenerService
      │ primitive snapshot copy
      ▼
Bounded Channel<CaptureCommand> (4,096)
      │ single writer
      ▼
CaptureProcessor
      ├── waits for persisted DataStore settings
      ├── resolves source app and capture policy
      ├── normalizes notification styles
      ├── resolves conversation identity
      ├── separates notification lifecycles
      ├── deduplicates exact snapshots
      ├── runs ordered message diff
      └── writes one Room transaction
               │
               ├── app_sources
               ├── conversations
               ├── notification_snapshots
               ├── message_events
               ├── message_search_fts
               └── capture_health
```

## Layer boundaries

### Platform capture

`NotificationSnapshotExtractor` copies supported primitive and string values from `StatusBarNotification` and `Notification.extras`. It never retains `Notification`, `Bundle`, `PendingIntent`, binder, bitmap, or `RemoteViews` objects.

`NotificationCaptureService` owns no mutable history. It delegates posted and removed callbacks to `CaptureCoordinator`.

### Coordination

`CaptureCoordinator` uses a bounded `Channel` and one consumer. The listener never suspends on database work. A pathological overflow is counted and surfaced in Capture Health instead of creating unbounded coroutines.

### Normalization

`NotificationNormalizer` converts structured and fallback notification representations into a common ordered list of `NormalizedMessage` objects.

Priority:

1. historic plus current `MessagingStyle` messages extracted through `NotificationCompat` across API 26+;
2. `EXTRA_TEXT_LINES`;
3. `EXTRA_BIG_TEXT`;
4. `EXTRA_TEXT`;
5. summary text.

It marks generic counts as `SUMMARY` and does not infer missing message bodies.

### Conversation resolution

`ConversationKeyResolver` ranks identifiers:

1. shortcut ID;
2. locus ID;
3. explicit conversation title;
4. group key plus title;
5. notification tag plus title;
6. normalized title;
7. notification ID fallback.

The source package is always part of the hashed stable material, so two apps cannot merge into one conversation.

### Notification lifecycle

Android may update one active notification repeatedly and may later reuse the same notification key. Echo Inbox treats snapshots as the same active lifecycle only when:

- the original `postTime` matches; and
- the previously stored lifecycle has no removal timestamp.

Removal callbacks are also scoped by key plus post time. A delayed removal from an older lifecycle cannot close a newer notification that reused the key.

### Ordered diff

`OrderedMessageDiff` reconstructs an LCS of previous and current ordered messages. A set is intentionally not used; `[OK] -> [OK, OK]` must add the second occurrence.

Each normalized message carries an opaque content identity. Production identities are HMAC-SHA-256 values generated with a non-exportable Android Keystore key. This allows two differently sourced OTPs to remain separate even when both display as bullets after redaction.

### Persistence

Room is opened through SQLCipher. The SQLCipher passphrase is random and wrapped with an AES-GCM key in Android Keystore. The passphrase is never hard-coded.

The database is event sourced:

- `notification_snapshots` records meaningful notification states;
- `message_events` records only newly observed ordered events;
- FTS is maintained manually in the same transaction.

### Search

FTS4 indexes:

- application display name;
- conversation display name;
- sender;
- body.

`SearchQueryBuilder` sanitizes user tokens and places the FTS4 prefix marker inside each quoted term. Optional package and captured-time filters are parameterized.

### Retention and deletion

FTS4 is not a Room external-content table, so its rows are explicitly removed before foreign-key cascades delete messages. Retention first selects every message that will disappear directly or through an old snapshot, removes those FTS row IDs, deletes snapshots/messages, then recalculates counters and removes empty conversations.

### UI state

Screens use ViewModels and immutable `StateFlow` state. The UI does not access DAOs directly.

## Process startup

DataStore-backed settings expose `current()`, which waits for the first persisted snapshot. Capture and retention use this API rather than the initial placeholder `StateFlow` value. This prevents startup races from bypassing pause or retention choices.

## Deliberate non-features

The MVP does not include:

- cloud or cross-device sync;
- remote dashboards;
- hidden operation;
- automatic replies;
- accessibility actions;
- media downloading;
- source-app database access;
- AI processing;
- network clients.


## Android-version compatibility boundary

The listener never invokes the framework `getMessagesFromBundleArray` helper because that API exists only from API 30. `NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification` is the single extraction boundary for structured messages. Raw payloads are bounded before entering the queue, then normalized and context-redacted before persistence.

## Encrypted database compatibility boundary

Room 2.6.1 is pinned with AndroidX SQLite 2.6.2 and SQLCipher 4.17.0. This avoids the newer Room read-only transaction call that the current SQLCipher support implementation does not provide. `DatabaseFactory` disables SQLCipher Java-client logging, loads the native library, obtains a Keystore-wrapped random passphrase, and supplies `SupportOpenHelperFactory` to Room. See `DEPENDENCY_NOTES.md`.
