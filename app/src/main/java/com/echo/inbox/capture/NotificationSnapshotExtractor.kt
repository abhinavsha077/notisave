package com.echo.inbox.capture

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.echo.inbox.core.model.RawMessage
import com.echo.inbox.core.model.RawNotificationSnapshot

import android.graphics.Bitmap
import android.graphics.drawable.Icon
import com.echo.inbox.platform.MediaStorageManager

class NotificationSnapshotExtractor(
    private val mediaStorageManager: MediaStorageManager? = null,
) {
    fun extract(sbn: StatusBarNotification): RawNotificationSnapshot {
        val notification = sbn.notification
        val extras = runCatching { notification.extras ?: Bundle.EMPTY }.getOrDefault(Bundle.EMPTY)
        val messagingStyle = runCatching {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        }.getOrNull()

        val picturePath = extractPicture(extras)

        return RawNotificationSnapshot(
            systemKey = sbn.key,
            packageName = sbn.packageName,
            notificationId = sbn.id,
            notificationTag = sbn.tag.bounded(IDENTIFIER_LIMIT),
            groupKey = sbn.groupKey.bounded(IDENTIFIER_LIMIT),
            channelId = notification.channelId.bounded(IDENTIFIER_LIMIT),
            shortcutId = notification.shortcutId.bounded(IDENTIFIER_LIMIT),
            locusId = if (Build.VERSION.SDK_INT >= 29) {
                notification.locusId?.id.bounded(IDENTIFIER_LIMIT)
            } else {
                null
            },
            postedAt = sbn.postTime,
            sourceWhen = notification.`when`,
            capturedAt = System.currentTimeMillis(),
            title = extras.charSequence(Notification.EXTRA_TITLE),
            text = extras.charSequence(Notification.EXTRA_TEXT),
            bigText = extras.charSequence(Notification.EXTRA_BIG_TEXT),
            subText = extras.charSequence(Notification.EXTRA_SUB_TEXT),
            summaryText = extras.charSequence(Notification.EXTRA_SUMMARY_TEXT),
            conversationTitle = extras.charSequence(Notification.EXTRA_CONVERSATION_TITLE)
                ?: messagingStyle?.conversationTitle?.toString()?.trim()
                    ?.takeIf { it.isNotBlank() }
                    .bounded(TEXT_LIMIT),
            textLines = extras.charSequenceArray(Notification.EXTRA_TEXT_LINES),
            messages = extractMessages(messagingStyle?.messages.orEmpty(), historic = false, defaultMediaPath = picturePath),
            historicMessages = extractMessages(
                messagingStyle?.historicMessages.orEmpty(),
                historic = true,
                defaultMediaPath = null,
            ),
            template = extras.charSequence("android.template", IDENTIFIER_LIMIT),
            category = notification.category.bounded(IDENTIFIER_LIMIT),
            flags = notification.flags,
            isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            userId = sbn.user.extractUserId(),
            mediaPath = picturePath,
        )
    }

    private fun extractPicture(extras: Bundle): String? {
        val manager = mediaStorageManager ?: return null
        return runCatching {
            @Suppress("DEPRECATION")
            val bitmap = extras.getParcelable<Bitmap>(Notification.EXTRA_PICTURE)
                ?: extras.getParcelable<Bitmap>(Notification.EXTRA_LARGE_ICON_BIG)
            if (bitmap != null) return@runCatching manager.saveBitmap(bitmap)

            val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_PICTURE_ICON, Icon::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_PICTURE_ICON)
            }
            if (icon != null) return@runCatching manager.saveIcon(icon)
            null
        }.getOrNull()
    }

    private fun extractMessages(
        messages: List<NotificationCompat.MessagingStyle.Message>,
        historic: Boolean,
        defaultMediaPath: String?,
    ): List<RawMessage> = messages
        .takeLast(MESSAGE_LIMIT)
        .mapIndexed { index, message ->
            val dataUriString = message.dataUri?.toString()
            val mediaPath = if (!dataUriString.isNullOrBlank()) {
                mediaStorageManager?.saveUri(dataUriString) ?: defaultMediaPath
            } else if (index == messages.lastIndex) {
                defaultMediaPath
            } else {
                null
            }

            RawMessage(
                sender = message.person?.name?.toString().bounded(SENDER_LIMIT),
                body = message.text?.toString().orEmpty().bounded(TEXT_LIMIT).orEmpty(),
                timestamp = message.timestamp.takeIf { it > 0 },
                dataMimeType = message.dataMimeType.bounded(MIME_LIMIT),
                dataUri = dataUriString.bounded(URI_LIMIT),
                historic = historic,
                mediaPath = mediaPath,
            )
        }

    private fun Bundle.charSequence(key: String, limit: Int = TEXT_LIMIT): String? = runCatching {
        getCharSequence(key)?.toString()?.trim()?.takeIf { it.isNotBlank() }.bounded(limit)
    }.getOrNull()

    private fun Bundle.charSequenceArray(key: String): List<String> = runCatching {
        getCharSequenceArray(key)
            ?.takeLast(MESSAGE_LIMIT)
            ?.mapNotNull { value ->
                value?.toString()?.trim()?.takeIf { it.isNotBlank() }.bounded(TEXT_LIMIT)
            }
            .orEmpty()
    }.getOrDefault(emptyList())

    private fun android.os.UserHandle.extractUserId(): Int = runCatching {
        android.os.UserHandle::class.java.getMethod("getIdentifier").invoke(this) as Int
    }.getOrElse { hashCode() }

    private fun String?.bounded(limit: Int): String? = this?.take(limit)

    private companion object {
        const val TEXT_LIMIT = 32_768
        const val MESSAGE_LIMIT = 200
        const val SENDER_LIMIT = 1_024
        const val IDENTIFIER_LIMIT = 2_048
        const val URI_LIMIT = 4_096
        const val MIME_LIMIT = 256
    }
}
