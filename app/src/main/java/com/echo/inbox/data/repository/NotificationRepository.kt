package com.echo.inbox.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.echo.inbox.data.local.AppSourceEntity
import com.echo.inbox.data.local.ConversationEntity
import com.echo.inbox.data.local.ConversationSummaryRow
import com.echo.inbox.data.local.DataStatsRow
import com.echo.inbox.data.local.EchoDatabase
import com.echo.inbox.data.local.ExportMessageRow
import com.echo.inbox.data.local.MessageEventEntity
import com.echo.inbox.data.local.SearchResultRow
import com.echo.inbox.data.local.DatabaseFactory
import kotlinx.coroutines.flow.Flow

class NotificationRepository(
    private val context: Context,
    private val database: EchoDatabase,
) {
    private val appDao = database.appSourceDao()
    private val conversationDao = database.conversationDao()
    private val snapshotDao = database.snapshotDao()
    private val messageDao = database.messageDao()
    private val searchDao = database.searchDao()
    private val maintenanceDao = database.maintenanceDao()

    val conversations: Flow<List<ConversationSummaryRow>> = conversationDao.observeSummaries()
    val apps: Flow<List<AppSourceEntity>> = appDao.observeAll()
    val appsWithEvents: Flow<List<AppSourceEntity>> = appDao.observeWithEvents()

    fun conversationsForApp(packageName: String): Flow<List<ConversationSummaryRow>> =
        conversationDao.observeSummariesForApp(packageName)

    fun conversation(id: Long): Flow<ConversationEntity?> = conversationDao.observe(id)

    fun messages(id: Long): Flow<List<MessageEventEntity>> = messageDao.observeForConversation(id)

    fun search(query: String, appPackage: String?, since: Long?): Flow<List<SearchResultRow>> =
        searchDao.search(SearchQueryBuilder.build(query, appPackage, since))

    suspend fun markConversationRead(id: Long) {
        database.withTransaction {
            conversationDao.markRead(id)
            messageDao.markConversationRead(id)
        }
    }

    suspend fun togglePinned(id: Long) = conversationDao.togglePinned(id)

    suspend fun deleteConversation(id: Long) {
        database.withTransaction {
            val messageIds = maintenanceDao.messageIdsForConversation(id)
            if (messageIds.isNotEmpty()) searchDao.deleteRows(messageIds)
            conversationDao.delete(id)
            maintenanceDao.recalculateAppCounts()
        }
    }

    suspend fun deleteAppData(packageName: String) {
        database.withTransaction {
            val messageIds = messageDao.idsForApp(packageName)
            if (messageIds.isNotEmpty()) searchDao.deleteRows(messageIds)
            conversationDao.deleteForApp(packageName)
            maintenanceDao.resetAppCount(packageName)
        }
    }

    suspend fun clearCapturedData() {
        database.withTransaction {
            searchDao.clear()
            maintenanceDao.deleteAllMessages()
            maintenanceDao.deleteAllSnapshots()
            maintenanceDao.deleteAllConversations()
            maintenanceDao.resetAllAppCounts()
        }
    }

    suspend fun deleteOlderThan(cutoff: Long): Int = database.withTransaction {
        // Snapshot deletion cascades to its message rows. Remove every affected FTS row first,
        // including messages whose source timestamp is newer than an old snapshot timestamp.
        val messageIds = messageDao.idsAffectedByRetention(cutoff)
        if (messageIds.isNotEmpty()) searchDao.deleteRows(messageIds)
        snapshotDao.deleteOlderThan(cutoff)
        val deleted = messageDao.deleteOlderThan(cutoff)
        maintenanceDao.recalculateConversations()
        maintenanceDao.deleteEmptyConversations()
        maintenanceDao.recalculateAppCounts()
        deleted
    }

    suspend fun stats(): DataStatsRow = maintenanceDao.stats()

    fun databaseBytes(): Long {
        val main = context.getDatabasePath(DatabaseFactory.DATABASE_NAME)
        return listOf(main, java.io.File(main.path + "-wal"), java.io.File(main.path + "-shm"))
            .filter { it.exists() }
            .sumOf { it.length() }
    }

    suspend fun exportUpperBoundId(): Long = messageDao.maxIdForExport()

    suspend fun exportPage(
        maxId: Long,
        afterCapturedAt: Long,
        afterId: Long,
        limit: Int,
    ): List<ExportMessageRow> = messageDao.exportPage(
        maxId = maxId,
        afterCapturedAt = afterCapturedAt,
        afterId = afterId,
        limit = limit,
    )
}
