package com.echo.inbox.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationLifecycleTest {
    @Test
    fun matchingPostTimeWithoutRemovalIsSameLifecycle() {
        assertTrue(NotificationLifecycle.isSameActiveLifecycle(100, null, 100))
    }

    @Test
    fun removalOrDifferentPostTimeStartsANewLifecycle() {
        assertFalse(NotificationLifecycle.isSameActiveLifecycle(100, 150, 100))
        assertFalse(NotificationLifecycle.isSameActiveLifecycle(100, null, 101))
        assertFalse(NotificationLifecycle.isSameActiveLifecycle(null, null, 100))
    }
}
