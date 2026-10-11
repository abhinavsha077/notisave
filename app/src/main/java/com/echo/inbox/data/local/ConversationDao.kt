package com.echo.inbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations WHERE appPackage = :appPackage AND stableKey = :stableKey LIMIT 1")
    suspend fun find(appPackage: String, stableKey: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun get(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun observe(id: Long): Flow<ConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: ConversationEntity): Long

    @Query(
        """
        UPDATE conversations
        SET displayName = :displayName,
            subtitle = :subtitle,
            lastEventAt = MAX(lastEventAt, :at),
            eventCount = eventCount + :addedCount,
            unreadCount = unreadCount + :addedCount,
            confidence = :confidence
        WHERE id = :id
        """,
    )
    suspend fun updateActivity(
        id: Long,
        displayName: String,
        subtitle: String?,
        at: Long,
        addedCount: Long,
        confidence: String,
    ): Int

    @Query(
        """
        SELECT
            c.id AS id,
            c.appPackage AS appPackage,
            a.displayName AS appName,
            a.category AS appCategory,
            c.displayName AS displayName,
            c.subtitle AS subtitle,
            c.lastEventAt AS lastEventAt,
            c.eventCount AS eventCount,
            c.unreadCount AS unreadCount,
            c.isPinned AS isPinned,
            c.confidence AS confidence,
            (SELECT m.body FROM message_events m WHERE m.conversationId = c.id ORDER BY m.capturedAt DESC, m.id DESC LIMIT 1) AS latestBody,
            (SELECT m.sender FROM message_events m WHERE m.conversationId = c.id ORDER BY m.capturedAt DESC, m.id DESC LIMIT 1) AS latestSender,
            (SELECT m.eventType FROM message_events m WHERE m.conversationId = c.id ORDER BY m.capturedAt DESC, m.id DESC LIMIT 1) AS latestEventType
        FROM conversations c
        INNER JOIN app_sources a ON a.packageName = c.appPackage
        WHERE c.isArchived = 0 AND (SELECT COUNT(*) FROM message_events m WHERE m.conversationId = c.id) > 0
        ORDER BY c.isPinned DESC, c.lastEventAt DESC
        """,
    )
    fun observeSummaries(): Flow<List<ConversationSummaryRow>>

    @Query(
        """
        SELECT
            c.id AS id,
            c.appPackage AS appPackage,
            a.displayName AS appName,
            a.category AS appCategory,
            c.displayName AS displayName,
            c.subtitle AS subtitle,
            c.lastEventAt AS lastEventAt,
            c.eventCount AS eventCount,
            c.unreadCount AS unreadCount,
            c.isPinned AS isPinned,
            c.confidence AS confidence,
            (SELECT m.body FROM message_events m WHERE m.conversationId = c.id ORDER BY m.capturedAt DESC, m.id DESC LIMIT 1) AS latestBody,
            (SELECT m.sender FROM message_events m WHERE m.conversationId = c.id ORDER BY m.capturedAt DESC, m.id DESC LIMIT 1) AS latestSender,
            (SELECT m.eventType FROM message_events m WHERE m.conversationId = c.id ORDER BY m.capturedAt DESC, m.id DESC LIMIT 1) AS latestEventType
        FROM conversations c
        INNER JOIN app_sources a ON a.packageName = c.appPackage
        WHERE c.appPackage = :packageName AND c.isArchived = 0 AND (SELECT COUNT(*) FROM message_events m WHERE m.conversationId = c.id) > 0
        ORDER BY c.isPinned DESC, c.lastEventAt DESC
        """,
    )
    fun observeSummariesForApp(packageName: String): Flow<List<ConversationSummaryRow>>

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :id")
    suspend fun markRead(id: Long): Int

    @Query("UPDATE conversations SET isPinned = NOT isPinned WHERE id = :id")
    suspend fun togglePinned(id: Long): Int

    @Query("UPDATE conversations SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean): Int

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM conversations WHERE appPackage = :packageName")
    suspend fun deleteForApp(packageName: String): Int

    @Query("DELETE FROM conversations WHERE eventCount = 0 OR id NOT IN (SELECT DISTINCT conversationId FROM message_events)")
    suspend fun deleteEmpty(): Int
}
