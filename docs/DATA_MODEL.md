# Data model

## `app_sources`

One row per observed or launchable package. Stores display metadata, sensitivity classification, capture switch, first/last seen times, and denormalized event count. Disabled sources may exist without captured content.

## `conversations`

Groups snapshots/events by package-scoped stable key. Stores display name, subtitle, confidence, activity counters, unread count, pin/archive state, and last event time.

## `notification_snapshots`

Immutable meaningful states of an Android notification. Important fields include:

- `systemKey`, `postedAt`, ID, tag, group, channel;
- notification/source/capture timestamps;
- title/text/big text/lines/conversation title;
- style, category, flags, summary markers;
- normalized ordered messages as JSON;
- parser version and content hash;
- removal timestamp and reason.

`dedupKey` is unique and includes notification key, lifecycle post time, and normalized content hash.

## `message_events`

Newly observed message occurrences. It stores readable body, sender, source timestamp, capture timestamp, type, attachment type, occurrence ordinal, historic/edit markers, and read state. It references both conversation and source snapshot with cascading foreign keys.

## `message_search_fts`

Manual FTS4 index keyed by `message_events.id` as `rowid`. Indexed columns are app name, conversation name, sender, and body. FTS rows are explicitly deleted before message cascades.

## `capture_health`

Singleton operational state: listener connection, callback/write/test times, queue depth, parser/database/drop counts, and a bounded error code.

## Time semantics

- `postedAt`: Android's original notification post time; stable for updates to one active lifecycle.
- `sourceWhen`: the source notification's `when` value.
- `sourceTimestamp`: a structured message timestamp when exposed.
- `capturedAt`: when Echo Inbox observed and persisted the snapshot/event.
- retention uses `capturedAt`, not an untrusted source timestamp.
