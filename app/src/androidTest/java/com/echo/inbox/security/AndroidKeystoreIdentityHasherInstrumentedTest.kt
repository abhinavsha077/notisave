package com.echo.inbox.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreIdentityHasherInstrumentedTest {
    @Test
    fun hmacIdentityIsStableOpaqueAndContentSensitive() {
        val hasher = AndroidKeystoreIdentityHasher()
        val first = hasher.digest("Your login code is 482913")
        val same = hasher.digest("Your login code is 482913")
        val different = hasher.digest("Your login code is 739204")

        assertEquals(first, same)
        assertNotEquals(first, different)
        assertFalse(first.contains("482913"))
        assertEquals(64, first.length)
    }
}
