package com.echo.inbox.export

import android.content.Context
import android.net.Uri
import android.util.JsonWriter
import com.echo.inbox.BuildConfig
import com.echo.inbox.data.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter

class JsonExportWriter(
    private val context: Context,
    private val repository: NotificationRepository,
) {
    suspend fun write(uri: Uri) = withContext(Dispatchers.IO) {
        val stream = requireNotNull(context.contentResolver.openOutputStream(uri, "wt")) {
            "Unable to open the selected export destination."
        }
        stream.use { output ->
            JsonWriter(OutputStreamWriter(output, Charsets.UTF_8)).use { writer ->
                writer.setIndent("  ")
                writer.beginObject()
                writer.name("format").value("echo-inbox-export")
                writer.name("formatVersion").value(1)
                writer.name("appVersion").value(BuildConfig.VERSION_NAME)
                writer.name("exportedAt").value(System.currentTimeMillis())
                writer.name("messages")
                writer.beginArray()

                // Freeze the upper row-ID boundary so notifications captured during a long export
                // are not unpredictably appended to the same file. Keyset pagination avoids loading
                // the complete archive into memory and remains efficient for large databases.
                val maxId = repository.exportUpperBoundId()
                var afterCapturedAt = Long.MIN_VALUE
                var afterId = 0L
                while (true) {
                    val page = repository.exportPage(
                        maxId = maxId,
                        afterCapturedAt = afterCapturedAt,
                        afterId = afterId,
                        limit = EXPORT_PAGE_SIZE,
                    )
                    if (page.isEmpty()) break
                    page.forEach { row ->
                        writer.beginObject()
                        writer.name("id").value(row.messageId)
                        writer.name("appPackage").value(row.appPackage)
                        writer.name("appName").value(row.appName)
                        writer.name("conversation").value(row.conversationName)
                        writer.name("sender").value(row.sender)
                        writer.name("body").value(row.body)
                        writer.name("sourceTimestamp").value(row.sourceTimestamp)
                        writer.name("capturedAt").value(row.capturedAt)
                        writer.name("eventType").value(row.eventType)
                        writer.name("attachmentType").value(row.attachmentType)
                        writer.name("historic").value(row.isHistoricMessage)
                        writer.name("possibleEdit").value(row.isPossibleEdit)
                        writer.endObject()
                    }
                    val last = page.last()
                    afterCapturedAt = last.capturedAt
                    afterId = last.messageId
                }

                writer.endArray()
                writer.endObject()
            }
        }
    }

    private companion object {
        const val EXPORT_PAGE_SIZE = 500
    }
}
