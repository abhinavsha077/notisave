package com.echo.inbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: MessageEventEntity): Long

    @Query("SELECT * FROM message_events WHERE conversationId = :conversationId ORDER BY capturedAt ASC, id ASC")
    fun observeForConversation(conversationId: Long): Flow<List<MessageEventEntity>>

    @Query("SELECT * FROM message_events WHERE conversationId = :conversationId ORDER BY capturedAt DESC, id DESC LIMIT :limit")
    suspend fun recentForConversation(conversationId: Long, limit: Int = 100): List<MessageEventEntity>

    @Query("UPDATE message_events SET isRead = 1 WHERE conversationId = :conversationId")
    suspend fun markConversationRead(conversationId: Long): Int

    @Query("UPDATE message_events SET isDeletedBySender = 1 WHERE conversationId = :conversationId AND isDeletedBySender = 0 AND id = (SELECT MAX(id) FROM message_events WHERE conversationId = :conversationId AND isDeletedBySender = 0)")
    suspend fun markLatestDeletedBySender(conversationId: Long): Int

    @Query("UPDATE message_events SET mediaPath = :mediaPath, attachmentType = 'IMAGE' WHERE id = :id")
    suspend fun updateMediaPath(id: Long, mediaPath: String): Int

    @Query("DELETE FROM message_events WHERE conversationId = :conversationId")
    suspend fun deleteForConversation(conversationId: Long): Int

    @Query(
        """
        SELECT m.id FROM message_events m
        INNER JOIN conversations c ON c.id = m.conversationId
        WHERE c.appPackage = :packageName
        """,
    )
    suspend fun idsForApp(packageName: String): List<Long>

    @Query(
        """
        SELECT m.id
        FROM message_events m
        INNER JOIN notification_snapshots s ON s.id = m.sourceSnapshotId
        WHERE m.capturedAt < :cutoff OR s.capturedAt < :cutoff
        """,
    )
    suspend fun idsAffectedByRetention(cutoff: Long): List<Long>

    @Query("DELETE FROM message_events WHERE capturedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("SELECT COUNT(*) FROM message_events")
    suspend fun count(): Long

    @Query("SELECT COALESCE(MAX(id), 0) FROM message_events")
    suspend fun maxIdForExport(): Long

    @Query(
        """
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
        WHERE m.id <= :maxId
          AND (m.capturedAt > :afterCapturedAt OR (m.capturedAt = :afterCapturedAt AND m.id > :afterId))
        ORDER BY m.capturedAt ASC, m.id ASC
        LIMIT :limit
        """,
    )
    suspend fun exportPage(
        maxId: Long,
        afterCapturedAt: Long,
        afterId: Long,
        limit: Int,
    ): List<ExportMessageRow>
}
