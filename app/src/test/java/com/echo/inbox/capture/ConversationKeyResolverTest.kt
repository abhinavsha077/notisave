package com.echo.inbox.capture

import com.echo.inbox.core.model.ConversationConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConversationKeyResolverTest {
    @Test
    fun shortcutIdProducesHighConfidenceStableConversation() {
        val first = ConversationKeyResolver.resolve(
            rawSnapshot(shortcutId = "rahul-thread", title = "Rahul"),
        )
        val updatedTitle = ConversationKeyResolver.resolve(
            rawSnapshot(shortcutId = "rahul-thread", title = "Rahul (2)"),
        )

        assertEquals(ConversationConfidence.HIGH, first.confidence)
        assertEquals(first.stableKey, updatedTitle.stableKey)
    }

    @Test
    fun packageNameSeparatesOtherwiseIdenticalThreads() {
        val one = ConversationKeyResolver.resolve(
            rawSnapshot(packageName = "com.example.one", shortcutId = "thread"),
        )
        val two = ConversationKeyResolver.resolve(
            rawSnapshot(packageName = "com.example.two", shortcutId = "thread"),
        )

        assertNotEquals(one.stableKey, two.stableKey)
    }

    @Test
    fun titleFallbackIsLowConfidence() {
        val resolution = ConversationKeyResolver.resolve(rawSnapshot(title = "Family Group"))
        assertEquals(ConversationConfidence.LOW, resolution.confidence)
        assertEquals("Family Group", resolution.displayName)
    }
}
