package com.echo.inbox.data.repository

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

object SearchQueryBuilder {
    fun build(
        query: String,
        appPackage: String?,
        since: Long?,
        limit: Int = 200,
    ): SupportSQLiteQuery {
        val arguments = mutableListOf<Any>()
        val sql = StringBuilder()
        if (query.isBlank()) {
            sql.append(
                """
                SELECT
                    m.id AS messageId,
                    m.conversationId AS conversationId,
                    a.packageName AS appPackage,
                    a.displayName AS appName,
                    c.displayName AS conversationName,
                    m.sender AS sender,
                    m.body AS body,
                    m.sourceTimestamp AS sourceTimestamp,
                    m.capturedAt AS capturedAt,
                    m.eventType AS eventType
                FROM message_events m
                INNER JOIN conversations c ON c.id = m.conversationId
                INNER JOIN app_sources a ON a.packageName = c.appPackage
                WHERE 1 = 1
                """.trimIndent(),
            )
        } else {
            sql.append(
                """
                SELECT
                    m.id AS messageId,
                    m.conversationId AS conversationId,
                    a.packageName AS appPackage,
                    a.displayName AS appName,
                    c.displayName AS conversationName,
                    m.sender AS sender,
                    m.body AS body,
                    m.sourceTimestamp AS sourceTimestamp,
                    m.capturedAt AS capturedAt,
                    m.eventType AS eventType
                FROM message_search_fts
                INNER JOIN message_events m ON m.id = message_search_fts.rowid
                INNER JOIN conversations c ON c.id = m.conversationId
                INNER JOIN app_sources a ON a.packageName = c.appPackage
                WHERE message_search_fts MATCH ?
                """.trimIndent(),
            )
            arguments += toFtsExpression(query)
        }
        if (!appPackage.isNullOrBlank()) {
            sql.append(" AND a.packageName = ?")
            arguments += appPackage
        }
        if (since != null) {
            sql.append(" AND m.capturedAt >= ?")
            arguments += since
        }
        sql.append(" ORDER BY m.capturedAt DESC, m.id DESC LIMIT ?")
        arguments += limit.coerceIn(1, 1000)
        return SimpleSQLiteQuery(sql.toString(), arguments.toTypedArray())
    }

    internal fun toFtsExpression(query: String): String {
        val tokens = query.trim()
            .split(Regex("\\s+"))
            .map { token -> token.replace(Regex("[^\\p{L}\\p{N}_@.+-]"), "") }
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return "\"\""
        return tokens.joinToString(" AND ") { token ->
            // SQLite FTS4 requires the prefix marker inside the quoted term. `"hel"*`
            // matches only the exact token on FTS4, while `"hel*"` correctly matches "hello".
            "\"${token.replace("\"", "\"\"")}*\""
        }
    }
}
