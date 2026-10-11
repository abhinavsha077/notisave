package com.echo.inbox.fixture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import kotlinx.coroutines.delay

class NotificationScenarioManager(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)
    private val systemManager = context.getSystemService(NotificationManager::class.java)

    init {
        createChannels()
    }

    fun postSimpleText() {
        notify(
            id = SIMPLE_ID,
            builder = baseBuilder()
                .setContentTitle("Alice")
                .setContentText("A simple notification captured by Echo Inbox")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("A simple notification captured by Echo Inbox. Open the logger to inspect the stored snapshot."),
                ),
        )
    }

    suspend fun postCumulativeEightMessages() {
        val sender = Person.Builder().setName("Rahul").setKey("fixture-rahul").build()
        val localUser = Person.Builder().setName("You").setKey("fixture-local-user").build()
        val messages = listOf("One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight")
        val startedAt = System.currentTimeMillis()

        messages.indices.forEach { endIndex ->
            val style = NotificationCompat.MessagingStyle(localUser)
                .setConversationTitle("Rahul · cumulative eight")
                .setGroupConversation(false)

            messages.take(endIndex + 1).forEachIndexed { index, body ->
                style.addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        body,
                        startedAt + index * 1_000L,
                        sender,
                    ),
                )
            }

            notify(
                id = CUMULATIVE_ID,
                builder = baseBuilder()
                    .setContentTitle("Rahul")
                    .setContentText(messages[endIndex])
                    .setStyle(style)
                    .setShortcutId("fixture-rahul-thread")
                    .setOnlyAlertOnce(endIndex > 0),
            )
            delay(650)
        }
    }

    suspend fun postRepeatedIdenticalMessages() {
        val sender = Person.Builder().setName("Priya").setKey("fixture-priya").build()
        val localUser = Person.Builder().setName("You").build()
        val baseTime = System.currentTimeMillis()

        repeat(2) { repeatIndex ->
            val style = NotificationCompat.MessagingStyle(localUser)
                .setConversationTitle("Priya · duplicate text")
                .setGroupConversation(false)
            repeat(repeatIndex + 1) { occurrence ->
                style.addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        "OK",
                        baseTime + occurrence * 1_000L,
                        sender,
                    ),
                )
            }
            notify(
                id = REPEATED_ID,
                builder = baseBuilder()
                    .setContentTitle("Priya")
                    .setContentText("OK")
                    .setStyle(style)
                    .setShortcutId("fixture-priya-thread")
                    .setOnlyAlertOnce(repeatIndex > 0),
            )
            delay(900)
        }
    }

    suspend fun postHistoricRollover() {
        val sender = Person.Builder().setName("Historic Sender").setKey("fixture-historic").build()
        val localUser = Person.Builder().setName("You").setKey("fixture-local-user").build()
        val baseTime = System.currentTimeMillis()
        val first = NotificationCompat.MessagingStyle.Message("First", baseTime, sender)

        notify(
            id = HISTORIC_ROLLOVER_ID,
            builder = baseBuilder()
                .setContentTitle("Historic rollover")
                .setContentText("First")
                .setStyle(
                    NotificationCompat.MessagingStyle(localUser)
                        .setConversationTitle("Historic rollover")
                        .setGroupConversation(false)
                        .addMessage(first),
                )
                .setShortcutId("fixture-historic-rollover"),
        )
        delay(900)

        notify(
            id = HISTORIC_ROLLOVER_ID,
            builder = baseBuilder()
                .setContentTitle("Historic rollover")
                .setContentText("Second")
                .setStyle(
                    NotificationCompat.MessagingStyle(localUser)
                        .setConversationTitle("Historic rollover")
                        .setGroupConversation(false)
                        .addHistoricMessage(first)
                        .addMessage(
                            NotificationCompat.MessagingStyle.Message(
                                "Second",
                                baseTime + 1_000L,
                                sender,
                            ),
                        ),
                )
                .setShortcutId("fixture-historic-rollover")
                .setOnlyAlertOnce(true),
        )
    }

    suspend fun postSlidingWindowMessages() {
        val sender = Person.Builder().setName("Office Team").setKey("fixture-office").build()
        val localUser = Person.Builder().setName("You").build()
        val messages = (1..8).map { "Window message $it" }
        val startedAt = System.currentTimeMillis()

        val windows = listOf(messages.take(5), messages.drop(2))
        windows.forEachIndexed { updateIndex, window ->
            val style = NotificationCompat.MessagingStyle(localUser)
                .setConversationTitle("Office Team · sliding window")
                .setGroupConversation(true)
            window.forEach { body ->
                val originalIndex = messages.indexOf(body)
                style.addMessage(
                    NotificationCompat.MessagingStyle.Message(
                        body,
                        startedAt + originalIndex * 1_000L,
                        sender,
                    ),
                )
            }
            notify(
                id = SLIDING_ID,
                builder = baseBuilder()
                    .setContentTitle("Office Team")
                    .setContentText(window.last())
                    .setStyle(style)
                    .setShortcutId("fixture-office-thread")
                    .setOnlyAlertOnce(updateIndex > 0),
            )
            delay(1_000)
        }
    }

    fun postInboxStyle() {
        val lines = listOf(
            "Anita: Meeting at 3 PM",
            "Dev: I pushed the new build",
            "Karan: Reviewing it now",
            "Anita: Please add the release notes",
        )
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle("Project group · 4 messages")
            .setSummaryText("Fixture InboxStyle")
        lines.forEach { line -> style.addLine(line) }

        notify(
            id = INBOX_ID,
            builder = baseBuilder()
                .setContentTitle("Project group")
                .setContentText(lines.last())
                .setStyle(style),
        )
    }

    fun postBigText() {
        val body = "This intentionally long notification validates BigTextStyle extraction, expanded text storage, Unicode, emoji ✅, and multiline rendering.\nSecond line: नमस्ते from the fixture application."
        notify(
            id = BIG_TEXT_ID,
            builder = baseBuilder()
                .setContentTitle("Long-form alert")
                .setContentText("This intentionally long notification validates…")
                .setStyle(NotificationCompat.BigTextStyle().bigText(body)),
        )
    }

    fun postSummaryOnly() {
        notify(
            id = SUMMARY_ONLY_ID,
            builder = baseBuilder()
                .setContentTitle("Fixture Messenger")
                .setContentText("8 new messages")
                .setNumber(8),
        )
    }

    fun postOtp() {
        notify(
            id = OTP_ID,
            builder = baseBuilder()
                .setContentTitle("Fixture Bank")
                .setContentText("Your verification code is 482913. Do not share it with anyone."),
        )
    }

    suspend fun postTwoRedactedOtpUpdates() {
        listOf("482913", "739204").forEachIndexed { index, code ->
            notify(
                id = OTP_UPDATE_ID,
                builder = baseBuilder()
                    .setContentTitle("Fixture Bank · OTP update")
                    .setContentText("Your verification code is $code. Do not share it with anyone.")
                    .setOnlyAlertOnce(index > 0),
            )
            delay(900)
        }
    }

    suspend fun postReusedNotificationKey() {
        val post = {
            notify(
                id = REUSED_KEY_ID,
                builder = baseBuilder()
                    .setContentTitle("Reused notification key")
                    .setContentText("Identical content posted in a new lifecycle"),
            )
        }
        post()
        delay(900)
        manager.cancel(REUSED_KEY_ID)
        delay(900)
        post()
    }

    fun postGroupedNotifications() {
        val groupKey = "fixture-group"
        listOf(
            GROUP_CHILD_A_ID to ("Shipping" to "Your parcel is out for delivery"),
            GROUP_CHILD_B_ID to ("Payments" to "Your receipt is ready"),
        ).forEach { (id, content) ->
            notify(
                id = id,
                builder = baseBuilder()
                    .setContentTitle(content.first)
                    .setContentText(content.second)
                    .setGroup(groupKey),
            )
        }
        notify(
            id = GROUP_SUMMARY_ID,
            builder = baseBuilder()
                .setContentTitle("Fixture updates")
                .setContentText("2 new notifications")
                .setGroup(groupKey)
                .setGroupSummary(true)
                .setStyle(
                    NotificationCompat.InboxStyle()
                        .addLine("Shipping: Your parcel is out for delivery")
                        .addLine("Payments: Your receipt is ready"),
                ),
        )
    }

    suspend fun postRapidBurst(count: Int = 100) {
        repeat(count.coerceIn(1, 1_000)) { index ->
            notify(
                id = BURST_START_ID + index,
                builder = baseBuilder()
                    .setContentTitle("Burst test")
                    .setContentText("Notification ${index + 1} of $count")
                    .setWhen(System.currentTimeMillis()),
            )
            if (index % 20 == 19) delay(25)
        }
    }

    fun cancelScenarioNotifications() {
        listOf(
            SIMPLE_ID,
            CUMULATIVE_ID,
            REPEATED_ID,
            HISTORIC_ROLLOVER_ID,
            SLIDING_ID,
            INBOX_ID,
            BIG_TEXT_ID,
            SUMMARY_ONLY_ID,
            OTP_ID,
            OTP_UPDATE_ID,
            REUSED_KEY_ID,
            GROUP_CHILD_A_ID,
            GROUP_CHILD_B_ID,
            GROUP_SUMMARY_ID,
        ).forEach { id -> manager.cancel(id) }
        repeat(1_000) { manager.cancel(BURST_START_ID + it) }
    }

    private fun createChannels() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Echo fixture scenarios",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Controlled notifications for testing Echo Inbox capture and reconstruction"
        }
        systemManager.createNotificationChannel(channel)
    }

    private fun baseBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_fixture)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setWhen(System.currentTimeMillis() - SystemClock.elapsedRealtime() % 50L)

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        manager.notify(id, builder.build())
    }

    companion object {
        private const val CHANNEL_ID = "echo_fixture_scenarios"
        private const val SIMPLE_ID = 1_001
        private const val CUMULATIVE_ID = 2_001
        private const val REPEATED_ID = 2_002
        private const val SLIDING_ID = 2_003
        private const val HISTORIC_ROLLOVER_ID = 2_004
        private const val INBOX_ID = 3_001
        private const val BIG_TEXT_ID = 3_002
        private const val SUMMARY_ONLY_ID = 3_003
        private const val OTP_ID = 3_004
        private const val OTP_UPDATE_ID = 3_005
        private const val REUSED_KEY_ID = 3_006
        private const val GROUP_CHILD_A_ID = 4_001
        private const val GROUP_CHILD_B_ID = 4_002
        private const val GROUP_SUMMARY_ID = 4_099
        private const val BURST_START_ID = 10_000
    }
}
