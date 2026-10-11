package com.echo.inbox.capture

import com.echo.inbox.core.model.MessageEventType
import com.echo.inbox.core.model.NotificationStyleType
import com.echo.inbox.core.model.RawMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationNormalizerTest {
    private val normalizer = NotificationNormalizer()

    @Test
    fun structuredMessagesArePreferredOverFallbackText() {
        val raw = rawSnapshot(
            text = "Latest fallback",
            conversationTitle = "Rahul",
            template = "android.app.Notification\$MessagingStyle",
            messages = listOf(
                RawMessage("Rahul", "One", 1_000, null, null, false),
                RawMessage("Rahul", "Two", 2_000, null, null, false),
            ),
        )

        val result = normalizer.normalize(raw, redactOtp = true)

        assertEquals(NotificationStyleType.MESSAGING, result.styleType)
        assertEquals(listOf("One", "Two"), result.messages.map { it.body })
        assertFalse(result.isSummaryOnly)
    }

    @Test
    fun historicalMessagesAppearBeforeCurrentMessages() {
        val result = normalizer.normalize(
            rawSnapshot(
                historicMessages = listOf(RawMessage("A", "Historic", 1_000, null, null, true)),
                messages = listOf(RawMessage("A", "Current", 2_000, null, null, false)),
            ),
            redactOtp = true,
        )

        assertEquals(listOf("Historic", "Current"), result.messages.map { it.body })
        assertTrue(result.messages.first().historic)
    }

    @Test
    fun movingAMessageFromCurrentToHistoricDoesNotChangeItsIdentity() {
        val previous = normalizer.normalize(
            rawSnapshot(messages = listOf(RawMessage("A", "First", 1_000, null, null, false))),
            redactOtp = true,
        )
        val current = normalizer.normalize(
            rawSnapshot(
                historicMessages = listOf(RawMessage("A", "First", 1_000, null, null, true)),
                messages = listOf(RawMessage("A", "Second", 2_000, null, null, false)),
            ),
            redactOtp = true,
        )

        val additions = com.echo.inbox.core.util.OrderedMessageDiff.diff(previous.messages, current.messages)
        assertEquals(listOf("Second"), additions.map { it.message.body })
    }

    @Test
    fun summaryTextIsMarkedAsSummaryOnly() {
        val result = normalizer.normalize(
            rawSnapshot(text = "8 new messages"),
            redactOtp = true,
        )

        assertTrue(result.isSummaryOnly)
        assertEquals(MessageEventType.SUMMARY, result.messages.single().eventType)
    }

    @Test
    fun inboxStyleParsesSenderPrefix() {
        val result = normalizer.normalize(
            rawSnapshot(textLines = listOf("Anita: Meeting at 3", "Dev: Build is ready")),
            redactOtp = true,
        )

        assertEquals(NotificationStyleType.INBOX, result.styleType)
        assertEquals(listOf("Anita", "Dev"), result.messages.map { it.sender })
        assertEquals(listOf("Meeting at 3", "Build is ready"), result.messages.map { it.body })
    }

    @Test
    fun otpRedactionAffectsStoredMessageAndHash() {
        val raw = rawSnapshot(text = "Your login code is 482913")
        val redacted = normalizer.normalize(raw, redactOtp = true)
        val plain = normalizer.normalize(raw, redactOtp = false)

        assertEquals("Your login code is ••••••", redacted.messages.single().body)
        assertEquals("Your login code is 482913", plain.messages.single().body)
        assertNotEquals(redacted.contentHash, plain.contentHash)
    }

    @Test
    fun differentOtpValuesRetainDistinctOpaqueIdentitiesAfterRedaction() {
        val first = normalizer.normalize(rawSnapshot(text = "Your login code is 482913"), redactOtp = true)
        val second = normalizer.normalize(rawSnapshot(text = "Your login code is 739204"), redactOtp = true)

        assertEquals(first.messages.single().body, second.messages.single().body)
        assertNotEquals(first.messages.single().identityHash, second.messages.single().identityHash)
        assertEquals(1, com.echo.inbox.core.util.OrderedMessageDiff.diff(first.messages, second.messages).size)
    }

    @Test
    fun storageSnapshotContainsOnlyContextAwareRedactedText() {
        val result = normalizer.normalize(
            rawSnapshot(
                title = "Verification code",
                text = "482913",
                bigText = "Use 482913 to continue",
                summaryText = "Code 482913",
                textLines = listOf("482913"),
                messages = listOf(RawMessage("Auth", "482913", 1_000, null, null, false)),
            ),
            redactOtp = true,
        )

        assertEquals("••••••", result.messages.single().body)
        assertEquals("••••••", result.raw.text)
        assertEquals("Use •••••• to continue", result.raw.bigText)
        assertEquals("Code ••••••", result.raw.summaryText)
        assertEquals(listOf("••••••"), result.raw.textLines)
        assertTrue(result.raw.messages.isEmpty())
        assertTrue(result.raw.historicMessages.isEmpty())
        assertFalse(result.raw.toString().contains("482913"))
    }

    @Test
    fun disabledRedactionKeepsReadableSnapshotText() {
        val result = normalizer.normalize(
            rawSnapshot(title = "Verification code", text = "482913"),
            redactOtp = false,
        )

        assertEquals("482913", result.raw.text)
        assertEquals("482913", result.messages.single().body)
    }

    @Test
    fun conversationIdentityMetadataChangesSnapshotHash() {
        val first = normalizer.normalize(
            rawSnapshot(text = "Same body", shortcutId = "thread-one"),
            redactOtp = true,
        )
        val second = normalizer.normalize(
            rawSnapshot(text = "Same body", shortcutId = "thread-two"),
            redactOtp = true,
        )

        assertNotEquals(first.contentHash, second.contentHash)
    }

    @Test
    fun sameInputProducesStableHash() {
        val raw = rawSnapshot(text = "Stable content")
        assertEquals(
            normalizer.normalize(raw, true).contentHash,
            normalizer.normalize(raw, true).contentHash,
        )
    }

    @Test
    fun groupSenderPrefixIsParsedAndStrippedFromBody() {
        val raw = rawSnapshot(
            title = "Samaj",
            text = "Dewansh 2: 💟 Sticker",
        )
        val normalized = normalizer.normalize(raw, redactOtp = true)
        assertEquals(1, normalized.messages.size)
        val message = normalized.messages.single()
        assertEquals("Dewansh 2", message.sender)
        assertEquals("💟 Sticker", message.body)
    }

    @Test
    fun textLinesMultiPostNotificationIsCleanlyParsed() {
        val raw = rawSnapshot(
            packageName = "com.reddit.frontpage",
            title = "Reddit",
            textLines = listOf(
                "WTFisAI: China has killed the GPU mafia.",
                "OpenAI: PLEASE HELP!!",
            ),
        )
        val normalized = normalizer.normalize(raw, redactOtp = true)
        assertEquals(2, normalized.messages.size)
        assertEquals("WTFisAI", normalized.messages[0].sender)
        assertEquals("China has killed the GPU mafia.", normalized.messages[0].body)
        assertEquals("OpenAI", normalized.messages[1].sender)
        assertEquals("PLEASE HELP!!", normalized.messages[1].body)
    }
}
