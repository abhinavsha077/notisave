#!/usr/bin/env python3
"""Executable contract tests for Echo Inbox's Room/SQLite data model.

This intentionally uses the stdlib sqlite3 engine so it can run without an Android SDK. It checks
SQL syntax, foreign-key cascades, manual FTS maintenance, retention behavior, bounded export
pagination, and the two search query shapes used by SearchQueryBuilder.
"""
from __future__ import annotations

import sqlite3
import sys
from contextlib import closing


SCHEMA = r"""
PRAGMA foreign_keys = ON;

CREATE TABLE app_sources (
    packageName TEXT NOT NULL PRIMARY KEY,
    displayName TEXT NOT NULL,
    category TEXT NOT NULL,
    captureEnabled INTEGER NOT NULL,
    sensitive INTEGER NOT NULL,
    firstSeenAt INTEGER NOT NULL,
    lastSeenAt INTEGER NOT NULL,
    eventCount INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    appPackage TEXT NOT NULL,
    stableKey TEXT NOT NULL,
    displayName TEXT NOT NULL,
    subtitle TEXT,
    lastEventAt INTEGER NOT NULL,
    eventCount INTEGER NOT NULL DEFAULT 0,
    unreadCount INTEGER NOT NULL DEFAULT 0,
    isPinned INTEGER NOT NULL DEFAULT 0,
    isArchived INTEGER NOT NULL DEFAULT 0,
    confidence TEXT NOT NULL,
    FOREIGN KEY(appPackage) REFERENCES app_sources(packageName) ON DELETE CASCADE
);
CREATE UNIQUE INDEX index_conversations_appPackage_stableKey
    ON conversations(appPackage, stableKey);
CREATE INDEX index_conversations_appPackage ON conversations(appPackage);
CREATE INDEX index_conversations_lastEventAt ON conversations(lastEventAt);

CREATE TABLE notification_snapshots (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    conversationId INTEGER NOT NULL,
    dedupKey TEXT NOT NULL,
    systemKey TEXT NOT NULL,
    packageName TEXT NOT NULL,
    notificationId INTEGER NOT NULL,
    notificationTag TEXT,
    groupKey TEXT,
    channelId TEXT,
    postedAt INTEGER NOT NULL,
    sourceWhen INTEGER NOT NULL,
    capturedAt INTEGER NOT NULL,
    removedAt INTEGER,
    removalReason INTEGER,
    title TEXT,
    text TEXT,
    bigText TEXT,
    textLinesJson TEXT NOT NULL,
    conversationTitle TEXT,
    styleType TEXT NOT NULL,
    category TEXT,
    flags INTEGER NOT NULL,
    isGroupSummary INTEGER NOT NULL,
    isSummaryOnly INTEGER NOT NULL,
    contentHash TEXT NOT NULL,
    parserVersion INTEGER NOT NULL,
    normalizedMessagesJson TEXT NOT NULL,
    rawMetadataJson TEXT NOT NULL,
    FOREIGN KEY(conversationId) REFERENCES conversations(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX index_notification_snapshots_dedupKey
    ON notification_snapshots(dedupKey);
CREATE INDEX index_notification_snapshots_conversationId
    ON notification_snapshots(conversationId);
CREATE INDEX index_notification_snapshots_systemKey
    ON notification_snapshots(systemKey);
CREATE INDEX index_notification_snapshots_capturedAt
    ON notification_snapshots(capturedAt);

CREATE TABLE message_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    conversationId INTEGER NOT NULL,
    sourceSnapshotId INTEGER NOT NULL,
    eventKey TEXT NOT NULL,
    sender TEXT,
    body TEXT NOT NULL,
    sourceTimestamp INTEGER,
    capturedAt INTEGER NOT NULL,
    eventType TEXT NOT NULL,
    attachmentType TEXT NOT NULL,
    occurrenceOrdinal INTEGER NOT NULL,
    isHistoricMessage INTEGER NOT NULL,
    isPossibleEdit INTEGER NOT NULL,
    isRead INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(conversationId) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY(sourceSnapshotId) REFERENCES notification_snapshots(id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX index_message_events_eventKey ON message_events(eventKey);
CREATE INDEX index_message_events_conversationId ON message_events(conversationId);
CREATE INDEX index_message_events_sourceSnapshotId ON message_events(sourceSnapshotId);
CREATE INDEX index_message_events_capturedAt ON message_events(capturedAt);

CREATE VIRTUAL TABLE message_search_fts USING fts4(appName, conversationName, sender, body);

CREATE TABLE capture_health (
    id INTEGER NOT NULL PRIMARY KEY,
    listenerConnected INTEGER NOT NULL DEFAULT 0,
    lastConnectedAt INTEGER,
    lastDisconnectedAt INTEGER,
    lastCallbackAt INTEGER,
    lastSuccessfulWriteAt INTEGER,
    lastTestCapturedAt INTEGER,
    pendingQueueCount INTEGER NOT NULL DEFAULT 0,
    parserFailureCount INTEGER NOT NULL DEFAULT 0,
    databaseFailureCount INTEGER NOT NULL DEFAULT 0,
    droppedEventCount INTEGER NOT NULL DEFAULT 0,
    lastErrorCode TEXT,
    lastErrorAt INTEGER
);
"""


SEARCH_SELECT = r"""
SELECT
    m.id AS messageId,
    m.conversationId AS conversationId,
    a.packageName AS appPackage,
    a.displayName AS appName,
    c.displayName AS conversationName,
    m.sender AS sender,
    m.body AS body,
    m.sourceTimestamp AS sourceTimestamp,
    m.capturedAt AS capturedAt,
    m.eventType AS eventType
FROM message_search_fts
INNER JOIN message_events m ON m.id = message_search_fts.rowid
INNER JOIN conversations c ON c.id = m.conversationId
INNER JOIN app_sources a ON a.packageName = c.appPackage
WHERE message_search_fts MATCH ?
  AND a.packageName = ?
  AND m.capturedAt >= ?
ORDER BY m.capturedAt DESC, m.id DESC
LIMIT ?
"""


EXPORT_PAGE_SELECT = r"""
SELECT
    m.id AS messageId,
    a.packageName AS appPackage,
    a.displayName AS appName,
    c.displayName AS conversationName,
    m.sender AS sender,
    m.body AS body,
    m.sourceTimestamp AS sourceTimestamp,
    m.capturedAt AS capturedAt,
    m.eventType AS eventType,
    m.attachmentType AS attachmentType,
    m.isHistoricMessage AS isHistoricMessage,
    m.isPossibleEdit AS isPossibleEdit
FROM message_events m
INNER JOIN conversations c ON c.id = m.conversationId
INNER JOIN app_sources a ON a.packageName = c.appPackage
WHERE m.id <= ?
  AND (m.capturedAt > ? OR (m.capturedAt = ? AND m.id > ?))
ORDER BY m.capturedAt ASC, m.id ASC
LIMIT ?
"""


BLANK_SEARCH_SELECT = r"""
SELECT
    m.id AS messageId,
    m.conversationId AS conversationId,
    a.packageName AS appPackage,
    a.displayName AS appName,
    c.displayName AS conversationName,
    m.sender AS sender,
    m.body AS body,
    m.sourceTimestamp AS sourceTimestamp,
    m.capturedAt AS capturedAt,
    m.eventType AS eventType
FROM message_events m
INNER JOIN conversations c ON c.id = m.conversationId
INNER JOIN app_sources a ON a.packageName = c.appPackage
WHERE 1 = 1
ORDER BY m.capturedAt DESC, m.id DESC
LIMIT ?
"""


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)
    print(f"PASS: {message}")


def insert_fixture(db: sqlite3.Connection) -> tuple[int, int, int]:
    db.execute(
        "INSERT INTO app_sources VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        ("com.example.chat", "Example Chat", "MESSAGING", 1, 0, 1, 1, 2),
    )
    conversation_id = db.execute(
        """INSERT INTO conversations
           (appPackage, stableKey, displayName, subtitle, lastEventAt, eventCount,
            unreadCount, isPinned, isArchived, confidence)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        ("com.example.chat", "chat:rahul", "Rahul", None, 250, 2, 2, 0, 0, "HIGH"),
    ).lastrowid
    old_snapshot_id = db.execute(
        """INSERT INTO notification_snapshots
           (conversationId, dedupKey, systemKey, packageName, notificationId, notificationTag,
            groupKey, channelId, postedAt, sourceWhen, capturedAt, removedAt, removalReason,
            title, text, bigText, textLinesJson, conversationTitle, styleType, category, flags,
            isGroupSummary, isSummaryOnly, contentHash, parserVersion, normalizedMessagesJson,
            rawMetadataJson)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        (
            conversation_id, "dedup-old", "system-key", "com.example.chat", 42, None,
            None, "messages", 100, 100, 100, None, None, "Rahul", "Hello there", None,
            "[]", None, "MESSAGING", "msg", 0, 0, 0, "hash-old", 1, "[]", "{}",
        ),
    ).lastrowid
    new_snapshot_id = db.execute(
        """INSERT INTO notification_snapshots
           (conversationId, dedupKey, systemKey, packageName, notificationId, notificationTag,
            groupKey, channelId, postedAt, sourceWhen, capturedAt, removedAt, removalReason,
            title, text, bigText, textLinesJson, conversationTitle, styleType, category, flags,
            isGroupSummary, isSummaryOnly, contentHash, parserVersion, normalizedMessagesJson,
            rawMetadataJson)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        (
            conversation_id, "dedup-new", "system-key", "com.example.chat", 42, None,
            None, "messages", 200, 200, 200, None, None, "Rahul", "Second message", None,
            "[]", None, "MESSAGING", "msg", 0, 0, 0, "hash-new", 1, "[]", "{}",
        ),
    ).lastrowid

    first_id = db.execute(
        """INSERT INTO message_events
           (conversationId, sourceSnapshotId, eventKey, sender, body, sourceTimestamp, capturedAt,
            eventType, attachmentType, occurrenceOrdinal, isHistoricMessage, isPossibleEdit, isRead)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        (
            conversation_id, old_snapshot_id, "event-1", "Rahul", "Hello there", 95, 205,
            "MESSAGE", "NONE", 0, 0, 0, 0,
        ),
    ).lastrowid
    second_id = db.execute(
        """INSERT INTO message_events
           (conversationId, sourceSnapshotId, eventKey, sender, body, sourceTimestamp, capturedAt,
            eventType, attachmentType, occurrenceOrdinal, isHistoricMessage, isPossibleEdit, isRead)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
        (
            conversation_id, new_snapshot_id, "event-2", "Rahul", "Second message", 190, 210,
            "MESSAGE", "NONE", 0, 0, 0, 0,
        ),
    ).lastrowid
    db.execute(
        "INSERT INTO message_search_fts(rowid, appName, conversationName, sender, body) VALUES (?, ?, ?, ?, ?)",
        (first_id, "Example Chat", "Rahul", "Rahul", "Hello there"),
    )
    db.execute(
        "INSERT INTO message_search_fts(rowid, appName, conversationName, sender, body) VALUES (?, ?, ?, ?, ?)",
        (second_id, "Example Chat", "Rahul", "Rahul", "Second message"),
    )
    return int(conversation_id), int(first_id), int(second_id)


def main() -> int:
    with closing(sqlite3.connect(":memory:")) as db:
        db.row_factory = sqlite3.Row
        db.executescript(SCHEMA)
        conversation_id, first_id, second_id = insert_fixture(db)

        rows = db.execute(
            SEARCH_SELECT,
            ('"Hel*"', "com.example.chat", 0, 200),
        ).fetchall()
        check(len(rows) == 1 and rows[0]["messageId"] == first_id, "FTS4 prefix search returns the expected event")
        check(rows[0]["sourceTimestamp"] == 95, "search projections preserve source timestamps")
        app_rows = db.execute(
            SEARCH_SELECT,
            ('"Example*"', "com.example.chat", 0, 200),
        ).fetchall()
        check(len(app_rows) == 2, "FTS search covers application and conversation metadata")

        blank = db.execute(BLANK_SEARCH_SELECT, (200,)).fetchall()
        check([row["messageId"] for row in blank] == [second_id, first_id], "blank search uses captured-time ordering")

        export_max_id = db.execute("SELECT COALESCE(MAX(id), 0) FROM message_events").fetchone()[0]
        new_snapshot_id = db.execute(
            "SELECT id FROM notification_snapshots WHERE dedupKey = 'dedup-new'"
        ).fetchone()[0]
        later_id = db.execute(
            """INSERT INTO message_events
               (conversationId, sourceSnapshotId, eventKey, sender, body, sourceTimestamp, capturedAt,
                eventType, attachmentType, occurrenceOrdinal, isHistoricMessage, isPossibleEdit, isRead)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (
                conversation_id, new_snapshot_id, "event-after-export-start", "Rahul", "Later",
                200, 215, "MESSAGE", "NONE", 0, 0, 0, 0,
            ),
        ).lastrowid
        exported_ids: list[int] = []
        after_captured_at = -(2**63)
        after_id = 0
        while True:
            page = db.execute(
                EXPORT_PAGE_SELECT,
                (export_max_id, after_captured_at, after_captured_at, after_id, 1),
            ).fetchall()
            if not page:
                break
            exported_ids.extend(int(row["messageId"]) for row in page)
            after_captured_at = int(page[-1]["capturedAt"])
            after_id = int(page[-1]["messageId"])
        check(
            exported_ids == [first_id, second_id],
            "bounded keyset export preserves order without duplicates or omissions",
        )
        check(
            later_id not in exported_ids,
            "export upper-bound snapshot excludes notifications captured after export starts",
        )

        db.execute(
            """UPDATE notification_snapshots
               SET removedAt = ?, removalReason = ?
               WHERE id = (
                   SELECT id FROM notification_snapshots
                   WHERE systemKey = ? AND postedAt = ?
                   ORDER BY capturedAt DESC, id DESC LIMIT 1
               )""",
            (220, 2, "system-key", 100),
        )
        removal = db.execute(
            "SELECT dedupKey, removedAt FROM notification_snapshots WHERE removedAt IS NOT NULL"
        ).fetchone()
        check(
            removal["dedupKey"] == "dedup-old" and removal["removedAt"] == 220,
            "removal is scoped to the matching notification lifecycle",
        )
        check(
            db.execute("SELECT removedAt FROM notification_snapshots WHERE dedupKey = 'dedup-new'").fetchone()[0] is None,
            "an old removal cannot close a reused notification key",
        )

        affected = [
            row[0]
            for row in db.execute(
                """SELECT m.id
                   FROM message_events m
                   INNER JOIN notification_snapshots s ON s.id = m.sourceSnapshotId
                   WHERE m.capturedAt < ? OR s.capturedAt < ?""",
                (150, 150),
            )
        ]
        check(affected == [first_id], "retention includes events cascading from an old snapshot")

        placeholders = ",".join("?" for _ in affected)
        db.execute(f"DELETE FROM message_search_fts WHERE rowid IN ({placeholders})", affected)
        db.execute("DELETE FROM notification_snapshots WHERE capturedAt < ?", (150,))
        db.execute("DELETE FROM message_events WHERE capturedAt < ?", (150,))
        check(db.execute("SELECT COUNT(*) FROM message_events WHERE id = ?", (first_id,)).fetchone()[0] == 0, "snapshot retention cascades to its event")
        check(db.execute("SELECT COUNT(*) FROM message_search_fts WHERE rowid = ?", (first_id,)).fetchone()[0] == 0, "retention leaves no orphaned FTS row")
        check(db.execute("SELECT COUNT(*) FROM message_events WHERE id = ?", (second_id,)).fetchone()[0] == 1, "newer events survive retention")

        remaining_ids = [row[0] for row in db.execute("SELECT id FROM message_events WHERE conversationId = ?", (conversation_id,))]
        placeholders = ",".join("?" for _ in remaining_ids)
        if remaining_ids:
            db.execute(f"DELETE FROM message_search_fts WHERE rowid IN ({placeholders})", remaining_ids)
        db.execute("DELETE FROM conversations WHERE id = ?", (conversation_id,))
        check(db.execute("SELECT COUNT(*) FROM message_events").fetchone()[0] == 0, "conversation deletion cascades to messages")
        check(db.execute("SELECT COUNT(*) FROM notification_snapshots").fetchone()[0] == 0, "conversation deletion cascades to snapshots")
        check(db.execute("SELECT COUNT(*) FROM message_search_fts").fetchone()[0] == 0, "conversation deletion leaves no FTS rows")

        integrity = db.execute("PRAGMA foreign_key_check").fetchall()
        check(not integrity, "foreign-key integrity remains valid")

    print("SQL_CONTRACT_PASSED (16 checks)")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"SQL_CONTRACT_FAILED: {error}", file=sys.stderr)
        raise
