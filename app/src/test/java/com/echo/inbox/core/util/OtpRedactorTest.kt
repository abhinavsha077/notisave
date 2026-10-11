package com.echo.inbox.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpRedactorTest {
    @Test
    fun redactsNumericCodeWhenOtpContextExists() {
        assertEquals(
            "Your verification code is ••••••. Do not share it.",
            OtpRedactor.redact("Your verification code is 482913. Do not share it.", true),
        )
    }

    @Test
    fun leavesOrdinaryNumbersUntouchedWithoutOtpContext() {
        assertEquals(
            "Invoice 482913 is ready",
            OtpRedactor.redact("Invoice 482913 is ready", true),
        )
    }

    @Test
    fun redactsCodeWhenContextLivesInAnotherField() {
        assertEquals(
            "••••••",
            OtpRedactor.redact("482913", enabled = true, contextHasOtp = true),
        )
    }

    @Test
    fun disabledSettingLeavesCodeUntouched() {
        assertEquals(
            "OTP 482913",
            OtpRedactor.redact("OTP 482913", false),
        )
    }
}
