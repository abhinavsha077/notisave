package com.echo.inbox.core.util

import com.echo.inbox.core.model.NormalizedMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderedMessageDiffTest {
    @Test
    fun cumulativeUpdateAddsOnlyNewTailMessage() {
        val previous = listOf(message("One", 1), message("Two", 2), message("Three", 3))
        val current = previous + message("Four", 4)

        val added = OrderedMessageDiff.diff(previous, current)

        assertEquals(listOf("Four"), added.map { it.message.body })
        assertEquals(3, added.single().currentIndex)
    }

    @Test
    fun identicalMessagesRemainDistinctOccurrences() {
        val first = message("OK", 1_000)
        val second = message("OK", 1_000)

        val added = OrderedMessageDiff.diff(listOf(first), listOf(first, second))

        assertEquals(1, added.size)
        assertEquals("OK", added.single().message.body)
        assertEquals(1, added.single().occurrenceOrdinal)
    }

    @Test
    fun identicalMessagesWithMissingTimestampsStillPreserveMultiplicity() {
        val ok = NormalizedMessage(sender = "Priya", body = "OK", sourceTimestamp = null)

        val added = OrderedMessageDiff.diff(listOf(ok), listOf(ok, ok))

        assertEquals(1, added.size)
        assertEquals(1, added.single().occurrenceOrdinal)
    }

    @Test
    fun slidingWindowAddsOnlyMessagesOutsideOverlap() {
        val all = (1..8).map { message("Message $it", it.toLong()) }
        val previous = all.take(5)
        val current = all.drop(2)

        val added = OrderedMessageDiff.diff(previous, current)

        assertEquals(listOf("Message 6", "Message 7", "Message 8"), added.map { it.message.body })
    }

    @Test
    fun exactReplayAddsNothing() {
        val snapshot = listOf(message("One", 1), message("Two", 2))
        assertTrue(OrderedMessageDiff.diff(snapshot, snapshot).isEmpty())
    }

    @Test
    fun sameSenderAndTimestampWithChangedBodyIsPossibleEdit() {
        val old = message("Draft", 5_000)
        val edited = message("Final", 5_000)

        val added = OrderedMessageDiff.diff(listOf(old), listOf(edited))

        assertEquals(1, added.size)
        assertTrue(added.single().possibleEdit)
    }

    @Test
    fun sameBodyFromDifferentSenderIsNewMessage() {
        val added = OrderedMessageDiff.diff(
            listOf(message("OK", 1_000, "Rahul")),
            listOf(message("OK", 1_000, "Priya")),
        )

        assertEquals(1, added.size)
        assertFalse(added.single().possibleEdit)
    }

    private fun message(body: String, time: Long, sender: String = "Rahul") =
        NormalizedMessage(sender = sender, body = body, sourceTimestamp = time)
}
