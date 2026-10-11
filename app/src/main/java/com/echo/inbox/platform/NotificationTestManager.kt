package com.echo.inbox.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.echo.inbox.R

class NotificationTestManager(private val context: Context) {
    fun postTest(): Result<Unit> = runCatching {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                TEST_CHANNEL_ID,
                "Capture health test",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Temporary local notifications used to verify Echo Inbox capture."
            },
        )
        val notification = NotificationCompat.Builder(context, TEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_test)
            .setContentTitle("Echo Inbox capture test")
            .setContentText("If the listener is healthy, this test will be detected locally.")
            .setAutoCancel(true)
            .setTimeoutAfter(10_000)
            .build()
        manager.notify(TEST_NOTIFICATION_ID, notification)
    }

    companion object {
        const val TEST_NOTIFICATION_ID = 90_901
        private const val TEST_CHANNEL_ID = "capture_health_test"
    }
}
