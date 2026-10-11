package com.echo.inbox.capture

import com.echo.inbox.core.model.RawNotificationSnapshot

sealed interface CaptureCommand {
    data class Posted(val snapshot: RawNotificationSnapshot) : CaptureCommand
    data class Removed(
        val systemKey: String,
        val postedAt: Long,
        val removedAt: Long,
        val reason: Int?,
    ) : CaptureCommand
}
