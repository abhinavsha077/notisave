package com.echo.inbox.core.util

import com.echo.inbox.core.model.NormalizedMessage

/**
 * Finds message occurrences present in [current] but not in [previous] while preserving order.
 * It deliberately uses sequence matching instead of a Set so two identical "OK" messages remain
 * two independent events.
 */
object OrderedMessageDiff {
    data class AddedMessage(
        val message: NormalizedMessage,
        val currentIndex: Int,
        val occurrenceOrdinal: Int,
        val possibleEdit: Boolean,
    )

    fun diff(previous: List<NormalizedMessage>, current: List<NormalizedMessage>): List<AddedMessage> {
        if (current.isEmpty()) return emptyList()
        if (previous.isEmpty()) return current.mapIndexed { index, message ->
            AddedMessage(
                message = message,
                currentIndex = index,
                occurrenceOrdinal = occurrenceOrdinal(current, index),
                possibleEdit = false,
            )
        }

        val matchedCurrent = longestCommonSubsequenceMatchedIndices(previous, current)
        return current.mapIndexedNotNull { index, message ->
            if (index in matchedCurrent) {
                null
            } else {
                AddedMessage(
                    message = message,
                    currentIndex = index,
                    occurrenceOrdinal = occurrenceOrdinal(current, index),
                    possibleEdit = previous.any { old -> likelyEditedVersion(old, message) },
                )
            }
        }
    }

    private fun longestCommonSubsequenceMatchedIndices(
        previous: List<NormalizedMessage>,
        current: List<NormalizedMessage>,
    ): Set<Int> {
        // A suffix-based table lets us reconstruct matches from the beginning. This intentionally
        // favors the earliest matching current occurrence, so [OK] -> [OK, OK] treats the second
        // item as the newly appended message rather than incorrectly adding the first item.
        val dp = Array(previous.size + 1) { IntArray(current.size + 1) }
        for (i in previous.indices.reversed()) {
            for (j in current.indices.reversed()) {
                dp[i][j] = if (sameIdentity(previous[i], current[j])) {
                    dp[i + 1][j + 1] + 1
                } else {
                    maxOf(dp[i + 1][j], dp[i][j + 1])
                }
            }
        }

        val matched = mutableSetOf<Int>()
        var i = 0
        var j = 0
        while (i < previous.size && j < current.size) {
            when {
                sameIdentity(previous[i], current[j]) -> {
                    matched += j
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> i++
                else -> j++
            }
        }
        return matched
    }

    private fun occurrenceOrdinal(messages: List<NormalizedMessage>, index: Int): Int {
        val target = messages[index]
        return messages.take(index + 1).count { sameIdentity(it, target) } - 1
    }

    private fun sameIdentity(a: NormalizedMessage, b: NormalizedMessage): Boolean {
        if (a.identityHash != null && b.identityHash != null) {
            return a.identityHash == b.identityHash
        }
        return a.sender.normalized() == b.sender.normalized() &&
            a.body.normalized() == b.body.normalized() &&
            normalizedTimestamp(a.sourceTimestamp) == normalizedTimestamp(b.sourceTimestamp) &&
            a.attachmentType == b.attachmentType &&
            a.eventType == b.eventType
    }

    private fun likelyEditedVersion(old: NormalizedMessage, new: NormalizedMessage): Boolean {
        val timestampMatches = old.sourceTimestamp != null && old.sourceTimestamp == new.sourceTimestamp
        val identityChanged = old.identityHash != null &&
            new.identityHash != null &&
            old.identityHash != new.identityHash
        return old.sender.normalized() == new.sender.normalized() &&
            (timestampMatches || old.body.normalized() == new.body.normalized()) &&
            (old.body.normalized() != new.body.normalized() || identityChanged)
    }

    private fun String?.normalized(): String = this.orEmpty().trim().replace(Regex("\\s+"), " ")
    private fun normalizedTimestamp(value: Long?): Long = value ?: 0L
}
