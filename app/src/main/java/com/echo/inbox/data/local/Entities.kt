package com.echo.inbox.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "app_sources")
data class AppSourceEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val category: String,
    val captureEnabled: Boolean,
    val sensitive: Boolean,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val eventCount: Long = 0,
)

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = AppSourceEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["appPackage"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["appPackage"]),
        Index(value = ["appPackage", "stableKey"], unique = true),
        Index(value = ["lastEventAt"]),
    ],
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appPackage: String,
    val stableKey: String,
    val displayName: String,
    val subtitle: String?,
    val lastEventAt: Long,
    val eventCount: Long = 0,
    val unreadCount: Long = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val confidence: String,
)

@Entity(
    tableName = "notification_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["systemKey"]),
        Index(value = ["capturedAt"]),
        Index(value = ["dedupKey"], unique = true),
    ],
)
data class NotificationSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val dedupKey: String,
    val systemKey: String,
    val packageName: String,
    val notificationId: Int,
    val notificationTag: String?,
    val groupKey: String?,
    val channelId: String?,
    val postedAt: Long,
    val sourceWhen: Long,
    val capturedAt: Long,
    val removedAt: Long?,
    val removalReason: Int?,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val textLinesJson: String,
    val conversationTitle: String?,
    val styleType: String,
    val category: String?,
    val flags: Int,
    val isGroupSummary: Boolean,
    val isSummaryOnly: Boolean,
    val contentHash: String,
    val parserVersion: Int,
    val normalizedMessagesJson: String,
    val rawMetadataJson: String,
)

@Entity(
    tableName = "message_events",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = NotificationSnapshotEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceSnapshotId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["sourceSnapshotId"]),
        Index(value = ["capturedAt"]),
        Index(value = ["eventKey"], unique = true),
    ],
)
data class MessageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val sourceSnapshotId: Long,
    val eventKey: String,
    val sender: String?,
    val body: String,
    val sourceTimestamp: Long?,
    val capturedAt: Long,
    val eventType: String,
    val attachmentType: String,
    val occurrenceOrdinal: Int,
    val isHistoricMessage: Boolean,
    val isPossibleEdit: Boolean,
    val isRead: Boolean = false,
    val mediaPath: String? = null,
    val isDeletedBySender: Boolean = false,
)

@Fts4
@Entity(tableName = "message_search_fts")
data class MessageSearchFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowId: Long,
    val appName: String,
    val conversationName: String,
    val sender: String,
    val body: String,
)

@Entity(tableName = "capture_health")
data class CaptureHealthEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val listenerConnected: Boolean = false,
    val lastConnectedAt: Long? = null,
    val lastDisconnectedAt: Long? = null,
    val lastCallbackAt: Long? = null,
    val lastSuccessfulWriteAt: Long? = null,
    val lastTestCapturedAt: Long? = null,
    val pendingQueueCount: Int = 0,
    val parserFailureCount: Long = 0,
    val databaseFailureCount: Long = 0,
    val droppedEventCount: Long = 0,
    val lastErrorCode: String? = null,
    val lastErrorAt: Long? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
