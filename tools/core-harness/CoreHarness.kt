package coreharness

import com.echo.inbox.capture.ConversationKeyResolver
import com.echo.inbox.capture.NotificationNormalizer
import com.echo.inbox.core.model.ConversationConfidence
import com.echo.inbox.core.model.NormalizedMessage
import com.echo.inbox.core.model.RawMessage
import com.echo.inbox.core.model.RawNotificationSnapshot
import com.echo.inbox.core.util.NotificationLifecycle
import com.echo.inbox.core.util.OrderedMessageDiff
import com.echo.inbox.core.util.OtpRedactor

private var passed = 0

fun main() {
    test("cumulative update adds only the tail") {
        val first = (1..7).map { message("Message $it", it.toLong()) }
        val additions = OrderedMessageDiff.diff(first, first + message("Message 8", 8))
        check(additions.map { it.message.body } == listOf("Message 8"))
    }

    test("two identical messages remain two occurrences") {
        val one = NormalizedMessage("Priya", "OK", null)
        val additions = OrderedMessageDiff.diff(listOf(one), listOf(one, one))
        check(additions.size == 1)
        check(additions.single().occurrenceOrdinal == 1)
    }

    test("sliding windows preserve overlap") {
        val all = (1..8).map { message("Window $it", it.toLong()) }
        val additions = OrderedMessageDiff.diff(all.take(5), all.drop(2))
        check(additions.map { it.message.body } == listOf("Window 6", "Window 7", "Window 8"))
    }

    test("edits are flagged") {
        val additions = OrderedMessageDiff.diff(
            listOf(message("Draft", 42)),
            listOf(message("Final", 42)),
        )
        check(additions.single().possibleEdit)
    }

    test("summary-only notification is labelled honestly") {
        val result = NotificationNormalizer().normalize(raw(text = "8 new messages"), true)
        check(result.isSummaryOnly)
        check(result.messages.single().eventType.name == "SUMMARY")
    }

    test("structured messages beat fallback text") {
        val result = NotificationNormalizer().normalize(
            raw(
                text = "latest fallback",
                messages = listOf(
                    RawMessage("Rahul", "One", 1, null, null, false),
                    RawMessage("Rahul", "Two", 2, null, null, false),
                ),
            ),
            true,
        )
        check(result.messages.map { it.body } == listOf("One", "Two"))
    }

    test("current messages can roll into historic without duplication") {
        val normalizer = NotificationNormalizer()
        val previous = normalizer.normalize(
            raw(messages = listOf(RawMessage("A", "First", 1_000, null, null, false))),
            true,
        )
        val current = normalizer.normalize(
            raw(
                historicMessages = listOf(RawMessage("A", "First", 1_000, null, null, true)),
                messages = listOf(RawMessage("A", "Second", 2_000, null, null, false)),
            ),
            true,
        )
        check(OrderedMessageDiff.diff(previous.messages, current.messages).map { it.message.body } == listOf("Second"))
    }

    test("OTP redaction is contextual") {
        check(OtpRedactor.redact("Your login code is 482913", true) == "Your login code is ••••••")
        check(OtpRedactor.redact("Invoice 482913", true) == "Invoice 482913")
    }

    test("different OTPs remain distinct after redaction") {
        val normalizer = NotificationNormalizer()
        val first = normalizer.normalize(raw(text = "Your login code is 482913"), true)
        val second = normalizer.normalize(raw(text = "Your login code is 739204"), true)
        check(first.messages.single().body == second.messages.single().body)
        check(first.messages.single().identityHash != second.messages.single().identityHash)
        check(OrderedMessageDiff.diff(first.messages, second.messages).size == 1)
    }

    test("stored snapshots contain no original OTP source text") {
        val result = NotificationNormalizer().normalize(
            raw(title = "Verification code", text = "482913"),
            true,
        )
        check(result.messages.single().body == "••••••")
        check(result.raw.text == "••••••")
        check(result.raw.messages.isEmpty())
        check(!result.raw.toString().contains("482913"))
    }

    test("cross-field OTP context triggers redaction") {
        check(OtpRedactor.redact("482913", true, contextHasOtp = true) == "••••••")
    }

    test("shortcut IDs are stable and high confidence") {
        val first = ConversationKeyResolver.resolve(raw(shortcutId = "rahul", title = "Rahul"))
        val renamed = ConversationKeyResolver.resolve(raw(shortcutId = "rahul", title = "Rahul (2)"))
        check(first.stableKey == renamed.stableKey)
        check(first.confidence == ConversationConfidence.HIGH)
    }

    test("updates keep the same notification lifecycle") {
        check(NotificationLifecycle.isSameActiveLifecycle(100, null, 100))
        check(!NotificationLifecycle.isSameActiveLifecycle(100, null, 101))
    }

    test("removed keys can be safely reused") {
        check(!NotificationLifecycle.isSameActiveLifecycle(100, 150, 100))
        check(!NotificationLifecycle.isSameActiveLifecycle(null, null, 100))
    }

    test("conversation identity metadata changes the snapshot hash") {
        val normalizer = NotificationNormalizer()
        val first = normalizer.normalize(raw(text = "Same body", shortcutId = "thread-one"), true)
        val second = normalizer.normalize(raw(text = "Same body", shortcutId = "thread-two"), true)
        check(first.contentHash != second.contentHash)
    }

    test("conversation keys remain package scoped") {
        val first = ConversationKeyResolver.resolve(
            raw(packageName = "com.example.one", shortcutId = "rahul", title = "Rahul"),
        )
        val second = ConversationKeyResolver.resolve(
            raw(packageName = "com.example.two", shortcutId = "rahul", title = "Rahul"),
        )
        check(first.stableKey != second.stableKey)
    }

    test("attachments retain their media type") {
        val result = NotificationNormalizer().normalize(
            raw(
                messages = listOf(
                    RawMessage("Rahul", "", 1, "image/jpeg", "content://fixture/image", false),
                ),
            ),
            true,
        )
        check(result.messages.single().body == "Image attachment")
        check(result.messages.single().attachmentType.name == "IMAGE")
    }

    println("CORE_HARNESS_PASSED ($passed checks)")
}

private fun test(name: String, block: () -> Unit) {
    try {
        block()
        passed++
        println("PASS: $name")
    } catch (error: Throwable) {
        System.err.println("FAIL: $name — ${error.message}")
        throw error
    }
}

private fun message(body: String, timestamp: Long) =
    NormalizedMessage(sender = "Rahul", body = body, sourceTimestamp = timestamp)

private fun raw(
    packageName: String = "com.example.chat",
    title: String? = "Rahul",
    text: String? = null,
    shortcutId: String? = null,
    messages: List<RawMessage> = emptyList(),
    historicMessages: List<RawMessage> = emptyList(),
) = RawNotificationSnapshot(
    systemKey = "system-key",
    packageName = packageName,
    notificationId = 42,
    notificationTag = null,
    groupKey = null,
    channelId = "messages",
    shortcutId = shortcutId,
    locusId = null,
    postedAt = 100,
    sourceWhen = 100,
    capturedAt = 101,
    title = title,
    text = text,
    bigText = null,
    subText = null,
    summaryText = null,
    conversationTitle = null,
    textLines = emptyList(),
    messages = messages,
    historicMessages = historicMessages,
    template = null,
    category = "msg",
    flags = 0,
    isGroupSummary = false,
    isOngoing = false,
    userId = 0,
)
