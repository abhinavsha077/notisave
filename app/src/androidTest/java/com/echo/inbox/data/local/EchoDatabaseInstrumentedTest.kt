package com.echo.inbox.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.echo.inbox.data.repository.SearchQueryBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EchoDatabaseInstrumentedTest {
    private lateinit var database: EchoDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, EchoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun roomSchemaFtsSearchAndCascadeWorkTogether() = runBlocking {
        val app = AppSourceEntity(
            packageName = "com.example.chat",
            displayName = "Example Chat",
            category = "MESSAGING",
            captureEnabled = true,
            sensitive = false,
            firstSeenAt = 1,
            lastSeenAt = 1,
        )
        database.appSourceDao().upsert(app)
        val conversationId = database.conversationDao().insertIgnore(
            ConversationEntity(
                appPackage = app.packageName,
                stableKey = "thread-1",
                displayName = "Rahul",
                subtitle = null,
                lastEventAt = 100,
                confidence = "HIGH",
            ),
        )
        val snapshotId = database.snapshotDao().insertIgnore(
            NotificationSnapshotEntity(
                conversationId = conversationId,
                dedupKey = "snapshot-1",
                systemKey = "system-key",
                packageName = app.packageName,
                notificationId = 42,
                notificationTag = null,
                groupKey = null,
                channelId = "messages",
                postedAt = 90,
                sourceWhen = 90,
                capturedAt = 100,
                removedAt = null,
                removalReason = null,
                title = "Rahul",
                text = "Hello there",
                bigText = null,
                textLinesJson = "[]",
                conversationTitle = "Rahul",
                styleType = "MESSAGING",
                category = "msg",
                flags = 0,
                isGroupSummary = false,
                isSummaryOnly = false,
                contentHash = "content-1",
                parserVersion = 2,
                normalizedMessagesJson = "[]",
                rawMetadataJson = "{}",
            ),
        )
        val messageId = database.messageDao().insertIgnore(
            MessageEventEntity(
                conversationId = conversationId,
                sourceSnapshotId = snapshotId,
                eventKey = "event-1",
                sender = "Rahul",
                body = "Hello there",
                sourceTimestamp = 95,
                capturedAt = 100,
                eventType = "MESSAGE",
                attachmentType = "NONE",
                occurrenceOrdinal = 0,
                isHistoricMessage = false,
                isPossibleEdit = false,
            ),
        )
        database.searchDao().insert(
            MessageSearchFtsEntity(
                rowId = messageId,
                appName = "Example Chat",
                conversationName = "Rahul",
                sender = "Rahul",
                body = "Hello there",
            ),
        )

        val results = database.searchDao().search(
            SearchQueryBuilder.build("Hel", app.packageName, since = null),
        ).first()
        assertEquals(listOf(messageId), results.map { it.messageId })
        assertEquals(95L, results.single().sourceTimestamp)
        assertEquals(
            listOf(messageId),
            database.searchDao().search(
                SearchQueryBuilder.build("Example", app.packageName, since = null),
            ).first().map { it.messageId },
        )

        database.searchDao().deleteRows(listOf(messageId))
        database.conversationDao().delete(conversationId)
        assertEquals(0, database.messageDao().count())
        assertEquals(0, database.snapshotDao().count())
        assertTrue(
            database.searchDao().search(
                SearchQueryBuilder.build("Hel", app.packageName, since = null),
            ).first().isEmpty(),
        )
    }
}
