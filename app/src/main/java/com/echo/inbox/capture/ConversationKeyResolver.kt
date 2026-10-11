package com.echo.inbox.capture

import com.echo.inbox.core.model.ConversationConfidence
import com.echo.inbox.core.model.ConversationResolution
import com.echo.inbox.core.model.RawNotificationSnapshot
import com.echo.inbox.core.util.ContentIdentityHasher

object ConversationKeyResolver {
    fun resolve(
        source: RawNotificationSnapshot,
        display: RawNotificationSnapshot = source,
        identityHasher: ContentIdentityHasher = ContentIdentityHasher.SHA256,
    ): ConversationResolution {
        val sourceTitle = clean(source.conversationTitle)
            ?: clean(source.title)
            ?: clean(source.subText)
            ?: "Unknown source"
        val displayTitle = clean(display.conversationTitle)
            ?: clean(display.title)
            ?: clean(display.subText)
            ?: "Unknown source"

        val candidates = listOfNotNull(
            source.shortcutId?.takeIf { it.isNotBlank() }
                ?.let { Candidate("shortcut:$it", ConversationConfidence.HIGH) },
            source.locusId?.takeIf { it.isNotBlank() }
                ?.let { Candidate("locus:$it", ConversationConfidence.HIGH) },
            clean(source.conversationTitle)
                ?.let { Candidate("conversation:${it.lowercase()}", ConversationConfidence.HIGH) },
            source.groupKey?.takeIf { it.isNotBlank() }
                ?.let { Candidate("group:${it.lowercase()}", ConversationConfidence.MEDIUM) },
            source.notificationTag?.takeIf { it.isNotBlank() }
                ?.let { Candidate("tag:${it.lowercase()}", ConversationConfidence.MEDIUM) },
            sourceTitle.takeIf { it != "Unknown source" }
                ?.let { Candidate("title:${it.lowercase()}", ConversationConfidence.LOW) },
            Candidate("notification:${source.notificationId}", ConversationConfidence.LOW),
        )

        val candidate = candidates.first()
        val stableMaterial = "conversation\u001f${source.packageName}\u001f${candidate.value}"
        return ConversationResolution(
            stableKey = identityHasher.digest(stableMaterial),
            displayName = displayTitle,
            subtitle = clean(display.subText)?.takeUnless { it == displayTitle },
            confidence = candidate.confidence,
        )
    }

    private fun clean(value: String?): String? = value
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf { it.isNotBlank() }

    private data class Candidate(val value: String, val confidence: ConversationConfidence)
}
