package com.echo.inbox.data.local

data class ConversationSummaryRow(
    val id: Long,
    val appPackage: String,
    val appName: String,
    val appCategory: String,
    val displayName: String,
    val subtitle: String?,
    val lastEventAt: Long,
    val eventCount: Long,
    val unreadCount: Long,
    val isPinned: Boolean,
    val confidence: String,
    val latestBody: String?,
    val latestSender: String?,
    val latestEventType: String?,
)

data class SearchResultRow(
    val messageId: Long,
    val conversationId: Long,
    val appPackage: String,
    val appName: String,
    val conversationName: String,
    val sender: String?,
    val body: String,
    val sourceTimestamp: Long?,
    val capturedAt: Long,
    val eventType: String,
)

data class ExportMessageRow(
    val messageId: Long,
    val appPackage: String,
    val appName: String,
    val conversationName: String,
    val sender: String?,
    val body: String,
    val sourceTimestamp: Long?,
    val capturedAt: Long,
    val eventType: String,
    val attachmentType: String,
    val isHistoricMessage: Boolean,
    val isPossibleEdit: Boolean,
)

data class DataStatsRow(
    val appCount: Long,
    val conversationCount: Long,
    val messageCount: Long,
    val snapshotCount: Long,
)
