package com.echo.inbox.capture

import android.content.Context
import androidx.room.withTransaction
import com.echo.inbox.core.model.NormalizedNotification
import com.echo.inbox.core.model.UserSettings
import com.echo.inbox.core.util.Hashing
import com.echo.inbox.core.util.JsonCodec
import com.echo.inbox.core.util.NotificationLifecycle
import com.echo.inbox.core.util.OrderedMessageDiff
import com.echo.inbox.data.local.AppSourceEntity
import com.echo.inbox.data.local.ConversationEntity
import com.echo.inbox.data.local.EchoDatabase
import com.echo.inbox.data.local.MessageEventEntity
import com.echo.inbox.data.local.MessageSearchFtsEntity
import com.echo.inbox.data.local.NotificationSnapshotEntity
import com.echo.inbox.data.repository.CaptureHealthRepository
import com.echo.inbox.data.settings.SettingsRepository
import com.echo.inbox.platform.AppMetadataResolver
import com.echo.inbox.platform.NotificationTestManager
import kotlinx.coroutines.CancellationException

class CaptureProcessor(
    private val context: Context,
    private val database: EchoDatabase,
    private val normalizer: NotificationNormalizer,
    private val metadataResolver: AppMetadataResolver,
    private val settingsRepository: SettingsRepository,
    private val healthRepository: CaptureHealthRepository,
) {
    private val appDao = database.appSourceDao()
    private val conversationDao = database.conversationDao()
    private val snapshotDao = database.snapshotDao()
    private val messageDao = database.messageDao()
    private val searchDao = database.searchDao()

    suspend fun process(command: CaptureCommand) {
        when (command) {
            is CaptureCommand.Posted -> processPosted(command.snapshot)
            is CaptureCommand.Removed -> processRemoved(command)
        }
    }

    private suspend fun processPosted(raw: com.echo.inbox.core.model.RawNotificationSnapshot) {
        healthRepository.callbackReceived(raw.capturedAt)

        if (raw.packageName == context.packageName) {
            if (raw.notificationId == NotificationTestManager.TEST_NOTIFICATION_ID) {
                healthRepository.testCaptured(raw.capturedAt)
            }
            return
        }

        val settings = settingsRepository.current()
        if (settings.capturePaused) return

        val metadata = metadataResolver.resolve(raw.packageName)
        val source = ensureSource(raw.packageName, metadata, raw.capturedAt, settings)
        if (!source.captureEnabled && !(settings.captureUnknownApps && !metadata.sensitive)) return

        val normalized = try {
            normalizer.normalize(raw, settings.redactOtpEnabled)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            healthRepository.parserFailure("PARSER_NORMALIZATION_FAILURE")
            return
        }

        try {
            database.withTransaction {
                persist(normalized, source)
            }
            healthRepository.successfulWrite()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            healthRepository.databaseFailure("DATABASE_CAPTURE_WRITE_FAILURE")
        }
    }

    private suspend fun ensureSource(
        packageName: String,
        metadata: AppMetadataResolver.Metadata,
        at: Long,
        settings: UserSettings,
    ): AppSourceEntity {
        val existing = appDao.get(packageName)
        if (existing != null) {
            appDao.updateSeenMetadata(
                packageName = packageName,
                displayName = metadata.displayName,
                category = metadata.category.name,
                sensitive = metadata.sensitive,
                lastSeenAt = at,
            )
            return existing.copy(
                displayName = metadata.displayName,
                category = metadata.category.name,
                sensitive = metadata.sensitive,
                lastSeenAt = at,
            )
        }

        val entity = AppSourceEntity(
            packageName = packageName,
            displayName = metadata.displayName,
            category = metadata.category.name,
            captureEnabled = settings.captureUnknownApps && !metadata.sensitive,
            sensitive = metadata.sensitive,
            firstSeenAt = at,
            lastSeenAt = at,
        )
        appDao.insertIgnore(entity)
        return appDao.get(packageName) ?: entity
    }

    private suspend fun persist(normalized: NormalizedNotification, source: AppSourceEntity) {
        val raw = normalized.raw
        val latestSnapshot = snapshotDao.latestForSystemKey(raw.systemKey)
        val sameLifecycle = NotificationLifecycle.isSameActiveLifecycle(
            previousPostedAt = latestSnapshot?.postedAt,
            previousRemovedAt = latestSnapshot?.removedAt,
            currentPostedAt = raw.postedAt,
        )

        // If content is identical to the active notification in status bar, skip processing entirely
        if (sameLifecycle && latestSnapshot?.contentHash == normalized.contentHash) {
            return
        }

        val conversation = getOrCreateConversation(normalized)
        val dedupKey = Hashing.sha256("${raw.systemKey}|${raw.postedAt}|${normalized.contentHash}")
        val snapshotId = snapshotDao.insertIgnore(
            NotificationSnapshotEntity(
                conversationId = conversation.id,
                dedupKey = dedupKey,
                systemKey = raw.systemKey,
                packageName = raw.packageName,
                notificationId = raw.notificationId,
                notificationTag = raw.notificationTag,
                groupKey = raw.groupKey,
                channelId = raw.channelId,
                postedAt = raw.postedAt,
                sourceWhen = raw.sourceWhen,
                capturedAt = raw.capturedAt,
                removedAt = null,
                removalReason = null,
                title = raw.title,
                text = raw.text,
                bigText = raw.bigText,
                textLinesJson = JsonCodec.encodeStringList(raw.textLines),
                conversationTitle = raw.conversationTitle,
                styleType = normalized.styleType.name,
                category = raw.category,
                flags = raw.flags,
                isGroupSummary = raw.isGroupSummary,
                isSummaryOnly = normalized.isSummaryOnly,
                contentHash = normalized.contentHash,
                parserVersion = normalized.parserVersion,
                normalizedMessagesJson = JsonCodec.encodeMessages(normalized.messages),
                rawMetadataJson = JsonCodec.encodeMetadata(
                    mapOf(
                        "shortcutId" to raw.shortcutId,
                        "locusId" to raw.locusId,
                        "template" to raw.template,
                        "subText" to raw.subText,
                        "summaryText" to raw.summaryText,
                        "isOngoing" to raw.isOngoing,
                        "userId" to raw.userId,
                    ),
                ),
            ),
        )
        if (snapshotId == -1L) return

        // If this is a group summary notification that only contains summary text (e.g. "X new messages"),
        // skip inserting duplicate message rows to avoid cluttering chat conversations
        if (raw.isGroupSummary && normalized.isSummaryOnly) {
            return
        }

        // Find baseline snapshot from the same system key or conversation to diff against
        val baselineSnapshot = when {
            latestSnapshot != null && sameLifecycle && latestSnapshot.conversationId == conversation.id -> {
                if (latestSnapshot.isSummaryOnly) {
                    snapshotDao.latestNonSummaryForConversation(conversation.id)
                } else {
                    latestSnapshot
                }
            }
            else -> snapshotDao.latestNonSummaryForConversation(conversation.id)
        }

        val previousMessages = JsonCodec.decodeMessages(baselineSnapshot?.normalizedMessagesJson)
        val rawAdditions = OrderedMessageDiff.diff(previousMessages, normalized.messages)
        val recentMessages = messageDao.recentForConversation(conversation.id, limit = 50)
        val recentEventKeys = recentMessages.map { it.eventKey }.toSet()

        // Fallback: If this conversation has 0 recorded messages, force inserting all normalized messages
        val additions = if (rawAdditions.isEmpty() && recentMessages.isEmpty() && normalized.messages.isNotEmpty()) {
            normalized.messages.mapIndexed { idx, msg ->
                com.echo.inbox.core.util.OrderedMessageDiff.AddedMessage(
                    message = msg,
                    currentIndex = idx,
                    occurrenceOrdinal = 0,
                    possibleEdit = false,
                )
            }
        } else {
            rawAdditions
        }

        var insertedCount = 0L

        additions.forEach { addition ->
            val message = addition.message
            if (message.body.isBlank() && message.mediaPath.isNullOrBlank()) return@forEach

            // In-place Media Update: If this message brings an image/sticker for a recent message that had no media,
            // update that message in-place instead of creating a duplicate row!
            if (!message.mediaPath.isNullOrBlank()) {
                val existingWithoutMedia = recentMessages.firstOrNull { recent ->
                    recent.mediaPath.isNullOrBlank() &&
                        recent.body.trim() == message.body.trim() &&
                        (recent.sender?.trim() == message.sender?.trim() || message.sender.isNullOrBlank()) &&
                        Math.abs(recent.capturedAt - raw.capturedAt) < 180_000L
                }
                if (existingWithoutMedia != null) {
                    messageDao.updateMediaPath(existingWithoutMedia.id, message.mediaPath)
                    return@forEach
                }
            }

            val isDeletedNotice = DELETED_MESSAGE_REGEX.containsMatchIn(message.body.trim())
            if (isDeletedNotice) {
                // Sender revoked their message (e.g. WhatsApp "Delete for Everyone").
                // Mark the most recent message in this conversation as deletedBySender while keeping its text!
                messageDao.markLatestDeletedBySender(conversation.id)
                if (recentMessages.isNotEmpty()) {
                    return@forEach
                }
            }

            // Stable event key derived from conversation identity and message content
            val eventKey = Hashing.sha256(
                "msg|${conversation.id}|${message.sender.orEmpty().trim()}|${message.body.trim()}|${message.sourceTimestamp ?: ""}|${message.mediaPath ?: ""}|${addition.occurrenceOrdinal}",
            )

            // Check if this exact event was already recorded
            if (eventKey in recentEventKeys) return@forEach

            // For notifications without a source timestamp, deduplicate if identical body + sender was recorded very recently
            if (message.sourceTimestamp == null && addition.occurrenceOrdinal == 0) {
                val duplicateRecent = recentMessages.any { recent ->
                    recent.body.trim() == message.body.trim() &&
                        recent.sender?.trim() == message.sender?.trim() &&
                        Math.abs(recent.capturedAt - raw.capturedAt) < 180_000L
                }
                if (duplicateRecent) return@forEach
            }

            val insertedId = messageDao.insertIgnore(
                MessageEventEntity(
                    conversationId = conversation.id,
                    sourceSnapshotId = snapshotId,
                    eventKey = eventKey,
                    sender = message.sender,
                    body = message.body,
                    sourceTimestamp = message.sourceTimestamp,
                    capturedAt = raw.capturedAt + addition.currentIndex,
                    eventType = message.eventType.name,
                    attachmentType = message.attachmentType.name,
                    occurrenceOrdinal = addition.occurrenceOrdinal,
                    isHistoricMessage = message.historic,
                    isPossibleEdit = addition.possibleEdit,
                    mediaPath = message.mediaPath,
                    isDeletedBySender = message.isDeletedBySender || isDeletedNotice,
                ),
            )
            if (insertedId != -1L) {
                insertedCount++
                searchDao.insert(
                    MessageSearchFtsEntity(
                        rowId = insertedId,
                        appName = source.displayName,
                        conversationName = normalized.resolution.displayName,
                        sender = message.sender.orEmpty(),
                        body = message.body,
                    ),
                )
            }
        }

        conversationDao.updateActivity(
            id = conversation.id,
            displayName = normalized.resolution.displayName,
            subtitle = normalized.resolution.subtitle,
            at = raw.capturedAt,
            addedCount = insertedCount,
            confidence = normalized.resolution.confidence.name,
        )
        appDao.incrementEventCount(source.packageName, insertedCount, raw.capturedAt)
    }

    private suspend fun getOrCreateConversation(normalized: NormalizedNotification): ConversationEntity {
        val raw = normalized.raw
        conversationDao.find(raw.packageName, normalized.resolution.stableKey)?.let { return it }
        val newEntity = ConversationEntity(
            appPackage = raw.packageName,
            stableKey = normalized.resolution.stableKey,
            displayName = normalized.resolution.displayName,
            subtitle = normalized.resolution.subtitle,
            lastEventAt = raw.capturedAt,
            confidence = normalized.resolution.confidence.name,
        )
        val insertedId = conversationDao.insertIgnore(newEntity)
        return if (insertedId != -1L) {
            newEntity.copy(id = insertedId)
        } else {
            checkNotNull(conversationDao.find(raw.packageName, normalized.resolution.stableKey))
        }
    }

    private suspend fun processRemoved(command: CaptureCommand.Removed) {
        healthRepository.callbackReceived(command.removedAt)
        try {
            snapshotDao.markLatestRemoved(
                systemKey = command.systemKey,
                postedAt = command.postedAt,
                removedAt = command.removedAt,
                reason = command.reason,
            )
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            healthRepository.databaseFailure("DATABASE_REMOVAL_WRITE_FAILURE")
        }
    }

    private companion object {
        val DELETED_MESSAGE_REGEX = Regex(
            "(?i)(?:this message was deleted|you deleted this message|this message has been deleted|message was deleted|message deleted)",
        )
    }
}
