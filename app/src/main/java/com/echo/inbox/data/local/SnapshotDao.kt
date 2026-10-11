package com.echo.inbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM notification_snapshots WHERE systemKey = :systemKey ORDER BY capturedAt DESC, id DESC LIMIT 1")
    suspend fun latestForSystemKey(systemKey: String): NotificationSnapshotEntity?

    @Query(
        """
        SELECT * FROM notification_snapshots
        WHERE systemKey = :systemKey
          AND postedAt = :postedAt
          AND isSummaryOnly = 0
          AND normalizedMessagesJson != '[]'
        ORDER BY capturedAt DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun latestNonSummaryForLifecycle(
        systemKey: String,
        postedAt: Long,
    ): NotificationSnapshotEntity?

    @Query(
        """
        SELECT * FROM notification_snapshots
        WHERE conversationId = :conversationId
          AND isSummaryOnly = 0
          AND normalizedMessagesJson != '[]'
        ORDER BY capturedAt DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun latestNonSummaryForConversation(
        conversationId: Long,
    ): NotificationSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: NotificationSnapshotEntity): Long

    @Query(
        """
        UPDATE notification_snapshots
        SET removedAt = :removedAt, removalReason = :reason
        WHERE id = (
            SELECT id FROM notification_snapshots
            WHERE systemKey = :systemKey AND postedAt = :postedAt
            ORDER BY capturedAt DESC, id DESC
            LIMIT 1
        )
        """,
    )
    suspend fun markLatestRemoved(
        systemKey: String,
        postedAt: Long,
        removedAt: Long,
        reason: Int?,
    ): Int

    @Query("DELETE FROM notification_snapshots WHERE capturedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("DELETE FROM notification_snapshots WHERE packageName = :packageName")
    suspend fun deleteForApp(packageName: String): Int

    @Query("SELECT COUNT(*) FROM notification_snapshots")
    suspend fun count(): Long
}
