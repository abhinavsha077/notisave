package com.echo.inbox.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.echo.inbox.EchoInboxApplication

class NotificationCaptureService : NotificationListenerService() {
    private val container get() = (application as EchoInboxApplication).container
    private val extractor by lazy { NotificationSnapshotExtractor(container.mediaStorageManager) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        container.captureCoordinator.listenerConnected()
        runCatching { activeNotifications.orEmpty() }
            .getOrDefault(emptyArray())
            .forEach(::capture)
    }

    override fun onListenerDisconnected() {
        container.captureCoordinator.listenerDisconnected()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) capture(sbn)
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        sbn?.let { notification ->
            container.captureCoordinator.removed(notification.key, notification.postTime, reason)
        }
    }

    override fun onDestroy() {
        container.captureCoordinator.listenerDisconnected()
        super.onDestroy()
    }

    private fun capture(sbn: StatusBarNotification) {
        runCatching { extractor.extract(sbn) }
            .onSuccess(container.captureCoordinator::posted)
            .onFailure { container.captureCoordinator.extractionFailed() }
    }
}
