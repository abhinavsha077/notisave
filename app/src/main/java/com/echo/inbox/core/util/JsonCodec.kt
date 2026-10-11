package com.echo.inbox.core.util

import com.echo.inbox.core.model.AttachmentType
import com.echo.inbox.core.model.MessageEventType
import com.echo.inbox.core.model.NormalizedMessage
import org.json.JSONArray
import org.json.JSONObject

object JsonCodec {
    fun encodeStringList(values: List<String>): String = JSONArray(values).toString()

    fun decodeStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList(array.length()) {
                repeat(array.length()) { index -> add(array.optString(index)) }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeMessages(messages: List<NormalizedMessage>): String {
        val array = JSONArray()
        messages.forEach { message ->
            array.put(
                JSONObject()
                    .put("sender", message.sender)
                    .put("body", message.body)
                    .put("timestamp", message.sourceTimestamp)
                    .put("attachment", message.attachmentType.name)
                    .put("historic", message.historic)
                    .put("eventType", message.eventType.name)
                    .put("identityHash", message.identityHash),
            )
        }
        return array.toString()
    }

    fun decodeMessages(value: String?): List<NormalizedMessage> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList(array.length()) {
                repeat(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    add(
                        NormalizedMessage(
                            sender = item.optString("sender").takeIf { it.isNotBlank() && it != "null" },
                            body = item.optString("body"),
                            sourceTimestamp = item.optLong("timestamp").takeIf { item.has("timestamp") && !item.isNull("timestamp") },
                            attachmentType = enumValueOrDefault(item.optString("attachment"), AttachmentType.NONE),
                            historic = item.optBoolean("historic"),
                            eventType = enumValueOrDefault(item.optString("eventType"), MessageEventType.MESSAGE),
                            identityHash = item.optString("identityHash")
                                .takeIf { it.isNotBlank() && it != "null" },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeMetadata(values: Map<String, Any?>): String {
        val json = JSONObject()
        values.forEach { (key, value) -> json.put(key, value) }
        return json.toString()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback
}
