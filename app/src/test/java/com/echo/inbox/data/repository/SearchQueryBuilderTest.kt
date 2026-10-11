package com.echo.inbox.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchQueryBuilderTest {
    @Test
    fun createsPrefixAndExpressionForMultipleTerms() {
        assertEquals(
            "\"hello*\" AND \"rahul*\"",
            SearchQueryBuilder.toFtsExpression("hello rahul"),
        )
    }

    @Test
    fun removesFtsPunctuation() {
        assertEquals(
            "\"hello*\" AND \"world*\"",
            SearchQueryBuilder.toFtsExpression("hello! world?"),
        )
    }

    @Test
    fun queryIncludesOptionalAppAndDateFilters() {
        val query = SearchQueryBuilder.build(
            query = "invoice",
            appPackage = "com.example.mail",
            since = 123L,
            limit = 50,
        )

        assertTrue(query.sql.contains("message_search_fts MATCH ?"))
        assertTrue(query.sql.contains("a.packageName = ?"))
        assertTrue(query.sql.contains("m.capturedAt >= ?"))
        assertTrue(query.sql.endsWith("LIMIT ?"))
    }
}
