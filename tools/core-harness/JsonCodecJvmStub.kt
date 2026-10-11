package com.echo.inbox.core.util

import com.echo.inbox.core.model.NormalizedMessage

/** JVM-only deterministic encoder used by the dependency-free core harness. */
object JsonCodec {
    fun encodeStringList(values: List<String>): String = values.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
    ) { value -> value.replace("\\", "\\\\").replace(",", "\\,") }

    fun encodeMessages(messages: List<NormalizedMessage>): String = messages.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
    ) { message ->
        listOf(
            message.sender.orEmpty(),
            message.body,
            message.sourceTimestamp?.toString().orEmpty(),
            message.attachmentType.name,
            message.historic.toString(),
            message.eventType.name,
            message.identityHash.orEmpty(),
        ).joinToString("|") { it.replace("\\", "\\\\").replace("|", "\\|") }
    }
}
