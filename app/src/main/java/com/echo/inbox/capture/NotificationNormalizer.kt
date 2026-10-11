package com.echo.inbox.capture

import com.echo.inbox.core.model.AttachmentType
import com.echo.inbox.core.model.MessageEventType
import com.echo.inbox.core.model.NormalizedMessage
import com.echo.inbox.core.model.NormalizedNotification
import com.echo.inbox.core.model.NotificationStyleType
import com.echo.inbox.core.model.RawMessage
import com.echo.inbox.core.model.RawNotificationSnapshot
import com.echo.inbox.core.util.ContentIdentityHasher
import com.echo.inbox.core.util.JsonCodec
import com.echo.inbox.core.util.OtpRedactor

class NotificationNormalizer(
    private val identityHasher: ContentIdentityHasher = ContentIdentityHasher.SHA256,
) {
    fun normalize(sourceRaw: RawNotificationSnapshot, redactOtp: Boolean): NormalizedNotification {
        val style = detectStyle(sourceRaw)
        val contextHasOtp = redactOtp && readableSourceFields(sourceRaw).any(OtpRedactor::hasOtpContext)
        val structured = (sourceRaw.historicMessages + sourceRaw.messages)
            .takeLast(MAX_MESSAGE_ITEMS)
            .mapNotNull { it.toNormalized(redactOtp, contextHasOtp) }

        val fallbackMessages = when {
            structured.isNotEmpty() -> structured
            sourceRaw.textLines.isNotEmpty() -> sourceRaw.textLines
                .takeLast(MAX_MESSAGE_ITEMS)
                .mapNotNull { line ->
                    parseLine(line, sourceRaw.title, redactOtp, contextHasOtp, sourceRaw.mediaPath)
                }
            !sourceRaw.bigText.isNullOrBlank() -> {
                val lines = sourceRaw.bigText.lines().map { it.trim() }.filter { it.isNotBlank() }
                if (lines.size > 1) {
                    lines.takeLast(MAX_MESSAGE_ITEMS).mapNotNull { line ->
                        parseLine(line, sourceRaw.title, redactOtp, contextHasOtp, sourceRaw.mediaPath)
                    }
                } else {
                    listOf(fallback(sourceRaw.bigText, sourceRaw.title, redactOtp, contextHasOtp, sourceRaw.mediaPath))
                }
            }
            !sourceRaw.text.isNullOrBlank() -> listOf(
                fallback(sourceRaw.text, sourceRaw.title, redactOtp, contextHasOtp, sourceRaw.mediaPath),
            )
            !sourceRaw.summaryText.isNullOrBlank() -> listOf(
                NormalizedMessage(
                    sender = redactField(sourceRaw.title, redactOtp, contextHasOtp),
                    body = OtpRedactor.redact(sourceRaw.summaryText, redactOtp, contextHasOtp),
                    sourceTimestamp = sourceRaw.sourceWhen.takeIf { it > 0 },
                    eventType = MessageEventType.SUMMARY,
                    identityHash = identity(
                        sender = sourceRaw.title,
                        body = sourceRaw.summaryText,
                        timestamp = sourceRaw.sourceWhen.takeIf { it > 0 },
                        mimeType = null,
                        eventType = MessageEventType.SUMMARY,
                    ),
                ),
            )
            else -> emptyList()
        }

        val messages = fallbackMessages
            .filter { it.body.isNotBlank() || !it.mediaPath.isNullOrBlank() }
            .takeLast(MAX_MESSAGE_ITEMS)
        val isSummaryOnly = messages.isNotEmpty() && messages.all { message ->
            message.eventType == MessageEventType.SUMMARY || looksLikeSummary(message.body)
        }
        val finalMessages = if (isSummaryOnly) {
            messages.map { it.copy(eventType = MessageEventType.SUMMARY) }
        } else {
            messages
        }

        // From this point onward, the original readable source values must not cross the storage
        // boundary. Structured message bodies and attachment URIs are stripped from the retained
        // snapshot because their redacted normalized representation is already captured above.
        val storedRaw = sanitizeForStorage(sourceRaw, redactOtp, contextHasOtp)
        val resolution = ConversationKeyResolver.resolve(
            source = sourceRaw,
            display = storedRaw,
            identityHasher = identityHasher,
        )
        val canonical = buildString {
            // Length-prefix every field so delimiter characters inside app-controlled text cannot
            // create an ambiguous snapshot identity. capturedAt is intentionally excluded: an
            // exact repost in the same Android notification lifecycle remains a duplicate.
            appendCanonical("snapshot-v$PARSER_VERSION")
            appendCanonical(storedRaw.systemKey)
            appendCanonical(storedRaw.packageName)
            appendCanonical(storedRaw.notificationId)
            appendCanonical(storedRaw.notificationTag)
            appendCanonical(storedRaw.groupKey)
            appendCanonical(storedRaw.channelId)
            appendCanonical(storedRaw.shortcutId)
            appendCanonical(storedRaw.locusId)
            appendCanonical(storedRaw.postedAt)
            appendCanonical(storedRaw.sourceWhen)
            appendCanonical(storedRaw.title)
            appendCanonical(storedRaw.text)
            appendCanonical(storedRaw.bigText)
            appendCanonical(storedRaw.subText)
            appendCanonical(storedRaw.summaryText)
            appendCanonical(storedRaw.conversationTitle)
            appendCanonical(JsonCodec.encodeStringList(storedRaw.textLines))
            appendCanonical(storedRaw.template)
            appendCanonical(storedRaw.category)
            appendCanonical(storedRaw.flags)
            appendCanonical(storedRaw.isGroupSummary)
            appendCanonical(storedRaw.isOngoing)
            appendCanonical(storedRaw.userId)
            appendCanonical(storedRaw.mediaPath)
            finalMessages.forEach { message ->
                appendCanonical(message.sender)
                appendCanonical(message.body)
                appendCanonical(message.sourceTimestamp)
                appendCanonical(message.attachmentType.name)
                appendCanonical(message.historic)
                appendCanonical(message.eventType.name)
                appendCanonical(message.identityHash)
                appendCanonical(message.mediaPath)
                appendCanonical(message.isDeletedBySender)
            }
        }

        return NormalizedNotification(
            raw = storedRaw,
            styleType = style,
            resolution = resolution,
            messages = finalMessages,
            isSummaryOnly = isSummaryOnly,
            contentHash = identityHasher.digest(canonical),
            parserVersion = PARSER_VERSION,
        )
    }

    private fun detectStyle(raw: RawNotificationSnapshot): NotificationStyleType = when {
        raw.messages.isNotEmpty() || raw.historicMessages.isNotEmpty() -> NotificationStyleType.MESSAGING
        raw.template?.endsWith("MessagingStyle", ignoreCase = true) == true -> NotificationStyleType.MESSAGING
        raw.textLines.isNotEmpty() -> NotificationStyleType.INBOX
        raw.template?.endsWith("InboxStyle", ignoreCase = true) == true -> NotificationStyleType.INBOX
        !raw.bigText.isNullOrBlank() -> NotificationStyleType.BIG_TEXT
        raw.template?.endsWith("BigTextStyle", ignoreCase = true) == true -> NotificationStyleType.BIG_TEXT
        !raw.text.isNullOrBlank() || !raw.title.isNullOrBlank() -> NotificationStyleType.BASIC
        else -> NotificationStyleType.UNKNOWN
    }

    private fun RawMessage.toNormalized(
        redactOtp: Boolean,
        contextHasOtp: Boolean,
    ): NormalizedMessage? {
        val sourceBody = body.trim()
        val cleanBody = OtpRedactor.redact(sourceBody, redactOtp, contextHasOtp)
        if (cleanBody.isBlank() && dataUri.isNullOrBlank() && mediaPath.isNullOrBlank()) return null
        val sourceSender = sender?.trim()?.takeIf { it.isNotBlank() }
        val cleanSender = redactField(sourceSender, redactOtp, contextHasOtp)
        val sourceTimestamp = timestamp?.takeIf { it > 0 }
        val resolvedAttachmentType = when {
            dataMimeType != null -> attachmentType(dataMimeType)
            mediaPath != null -> AttachmentType.IMAGE
            else -> AttachmentType.NONE
        }
        return NormalizedMessage(
            sender = cleanSender,
            body = cleanBody.ifBlank { attachmentLabel(dataMimeType) },
            sourceTimestamp = sourceTimestamp,
            attachmentType = resolvedAttachmentType,
            historic = historic,
            mediaPath = mediaPath,
            isDeletedBySender = isDeletedBySender,
            identityHash = identity(
                sender = sourceSender,
                body = sourceBody.ifBlank { dataUri ?: mediaPath.orEmpty() },
                timestamp = sourceTimestamp,
                mimeType = dataMimeType,
                eventType = MessageEventType.MESSAGE,
            ),
        )
    }

    private fun parseLine(
        line: String,
        defaultSender: String?,
        redactOtp: Boolean,
        contextHasOtp: Boolean,
        mediaPath: String? = null,
    ): NormalizedMessage? {
        val cleaned = line.trim()
        if (cleaned.isBlank()) return null
        val separator = cleaned.indexOf(':')
        val (sourceSender, sourceBody) = if (separator in 1..40 && separator < cleaned.length - 1 && !cleaned.substring(0, separator).contains('\n') && !cleaned.startsWith("http:", ignoreCase = true) && !cleaned.startsWith("https:", ignoreCase = true)) {
            val prefix = cleaned.substring(0, separator).trim()
            val rest = cleaned.substring(separator + 1).trim()
            if (prefix.isNotEmpty() && rest.isNotEmpty()) {
                prefix to rest
            } else {
                defaultSender to cleaned
            }
        } else {
            defaultSender to cleaned
        }

        return NormalizedMessage(
            sender = redactField(sourceSender, redactOtp, contextHasOtp),
            body = OtpRedactor.redact(sourceBody, redactOtp, contextHasOtp),
            sourceTimestamp = null,
            attachmentType = if (mediaPath != null) AttachmentType.IMAGE else AttachmentType.NONE,
            mediaPath = mediaPath,
            identityHash = identity(
                sender = sourceSender,
                body = sourceBody,
                timestamp = null,
                mimeType = null,
                eventType = MessageEventType.MESSAGE,
            ),
        )
    }

    private fun fallback(
        body: String,
        sender: String?,
        redactOtp: Boolean,
        contextHasOtp: Boolean,
        mediaPath: String? = null,
    ): NormalizedMessage {
        val sourceSender = sender?.trim()?.takeIf { it.isNotBlank() }
        val sourceBody = body.trim()

        val separator = sourceBody.indexOf(':')
        val (resolvedSender, resolvedBody) = if (separator in 1..40 && separator < sourceBody.length - 1 && !sourceBody.substring(0, separator).contains('\n') && !sourceBody.startsWith("http:", ignoreCase = true) && !sourceBody.startsWith("https:", ignoreCase = true)) {
            val prefix = sourceBody.substring(0, separator).trim()
            val rest = sourceBody.substring(separator + 1).trim()
            if (prefix.isNotEmpty() && rest.isNotEmpty()) {
                prefix to rest
            } else {
                sourceSender to sourceBody
            }
        } else {
            sourceSender to sourceBody
        }

        val eventType = if (looksLikeSummary(resolvedBody)) {
            MessageEventType.SUMMARY
        } else {
            MessageEventType.MESSAGE
        }
        return NormalizedMessage(
            sender = redactField(resolvedSender, redactOtp, contextHasOtp),
            body = OtpRedactor.redact(resolvedBody, redactOtp, contextHasOtp),
            sourceTimestamp = null,
            attachmentType = if (mediaPath != null) AttachmentType.IMAGE else AttachmentType.NONE,
            eventType = eventType,
            mediaPath = mediaPath,
            identityHash = identity(
                sender = resolvedSender,
                body = resolvedBody,
                timestamp = null,
                mimeType = null,
                eventType = eventType,
            ),
        )
    }

    private fun sanitizeForStorage(
        raw: RawNotificationSnapshot,
        redactOtp: Boolean,
        contextHasOtp: Boolean,
    ): RawNotificationSnapshot = raw.copy(
        title = redactField(raw.title, redactOtp, contextHasOtp),
        text = redactField(raw.text, redactOtp, contextHasOtp),
        bigText = redactField(raw.bigText, redactOtp, contextHasOtp),
        subText = redactField(raw.subText, redactOtp, contextHasOtp),
        summaryText = redactField(raw.summaryText, redactOtp, contextHasOtp),
        conversationTitle = redactField(raw.conversationTitle, redactOtp, contextHasOtp),
        textLines = raw.textLines.map { OtpRedactor.redact(it, redactOtp, contextHasOtp) },
        messages = emptyList(),
        historicMessages = emptyList(),
    )

    private fun redactField(value: String?, redactOtp: Boolean, contextHasOtp: Boolean): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return OtpRedactor.redact(trimmed, redactOtp, contextHasOtp)
    }

    private fun readableSourceFields(raw: RawNotificationSnapshot): Sequence<String> = sequence {
        raw.title?.let { yield(it) }
        raw.text?.let { yield(it) }
        raw.bigText?.let { yield(it) }
        raw.subText?.let { yield(it) }
        raw.summaryText?.let { yield(it) }
        raw.conversationTitle?.let { yield(it) }
        raw.textLines.forEach { yield(it) }
        raw.messages.forEach { yield(it.body) }
        raw.historicMessages.forEach { yield(it.body) }
    }

    private fun attachmentLabel(mimeType: String?): String = when (attachmentType(mimeType)) {
        AttachmentType.IMAGE -> "Photo attachment"
        AttachmentType.VIDEO -> "Video attachment"
        AttachmentType.AUDIO -> "Audio attachment"
        AttachmentType.FILE -> "File attachment"
        AttachmentType.UNKNOWN -> "Media attachment"
        AttachmentType.NONE -> "Message"
    }

    private fun attachmentType(mimeType: String?): AttachmentType {
        val type = mimeType?.trim()?.lowercase().orEmpty()
        return when {
            type.startsWith("image/") -> AttachmentType.IMAGE
            type.startsWith("video/") -> AttachmentType.VIDEO
            type.startsWith("audio/") -> AttachmentType.AUDIO
            type.isNotBlank() -> AttachmentType.FILE
            else -> AttachmentType.NONE
        }
    }

    private fun looksLikeSummary(body: String): Boolean = SUMMARY_REGEX.matches(body.trim())

    private fun StringBuilder.appendCanonical(value: Any?) {
        val encoded = value?.toString().orEmpty()
        append(encoded.length)
        append(':')
        append(encoded)
        append(';')
    }

    private fun identity(
        sender: String?,
        body: String,
        timestamp: Long?,
        mimeType: String?,
        eventType: MessageEventType,
    ): String = identityHasher.digest(
        listOf(
            sender.orEmpty().trim(),
            body.trim(),
            timestamp?.toString().orEmpty(),
            mimeType.orEmpty().trim(),
            eventType.name,
        ).joinToString("\u001f"),
    )

    private companion object {
        const val PARSER_VERSION = 5
        const val MAX_MESSAGE_ITEMS = 64
        val SUMMARY_REGEX = Regex(
            "(?i)^(?:you have\\s+)?\\d+\\s+(?:new\\s+)?(?:messages?|notifications?|chats?|updates?)(?:\\s+from.*)?$",
        )
    }
}
