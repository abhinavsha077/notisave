package com.echo.inbox.core.util

/**
 * Decides whether two snapshots belong to the same active Android notification lifecycle.
 *
 * Android may reuse a notification key after the earlier notification is removed. The original
 * post time is stable while an active notification is updated, so a matching [postedAt] plus an
 * unset removal time is the safest lifecycle boundary available to a listener.
 */
object NotificationLifecycle {
    fun isSameActiveLifecycle(
        previousPostedAt: Long?,
        previousRemovedAt: Long?,
        currentPostedAt: Long,
    ): Boolean = previousPostedAt != null &&
        previousPostedAt == currentPostedAt &&
        previousRemovedAt == null
}
