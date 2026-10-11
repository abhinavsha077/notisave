package com.echo.inbox.data.local

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MaintenanceDao {
    @Query(
        """
        SELECT
            (SELECT COUNT(*) FROM app_sources) AS appCount,
            (SELECT COUNT(*) FROM conversations) AS conversationCount,
            (SELECT COUNT(*) FROM message_events) AS messageCount,
            (SELECT COUNT(*) FROM notification_snapshots) AS snapshotCount
        """,
    )
    suspend fun stats(): DataStatsRow

    @Query("SELECT id FROM message_events WHERE conversationId = :conversationId")
    suspend fun messageIdsForConversation(conversationId: Long): List<Long>

    @Query("DELETE FROM message_events")
    suspend fun deleteAllMessages(): Int

    @Query("DELETE FROM notification_snapshots")
    suspend fun deleteAllSnapshots(): Int

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations(): Int

    @Query("UPDATE app_sources SET eventCount = 0")
    suspend fun resetAllAppCounts(): Int

    @Query("UPDATE app_sources SET eventCount = 0 WHERE packageName = :packageName")
    suspend fun resetAppCount(packageName: String): Int

    @Query(
        """
        UPDATE conversations
        SET eventCount = (SELECT COUNT(*) FROM message_events m WHERE m.conversationId = conversations.id),
            unreadCount = (SELECT COUNT(*) FROM message_events m WHERE m.conversationId = conversations.id AND m.isRead = 0),
            lastEventAt = COALESCE(
                (SELECT MAX(m.capturedAt) FROM message_events m WHERE m.conversationId = conversations.id),
                lastEventAt
            )
        """,
    )
    suspend fun recalculateConversations(): Int

    @Query("DELETE FROM conversations WHERE id NOT IN (SELECT DISTINCT conversationId FROM message_events)")
    suspend fun deleteEmptyConversations(): Int

    @Query(
        """
        UPDATE app_sources
        SET eventCount = COALESCE((
            SELECT SUM(c.eventCount) FROM conversations c WHERE c.appPackage = app_sources.packageName
        ), 0)
        """,
    )
    suspend fun recalculateAppCounts(): Int
}
