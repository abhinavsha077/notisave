package com.echo.inbox.core.util

object OtpRedactor {
    private val otpContext = Regex(
        pattern = "(?i)\\b(otp|one[- ]?time|verification|security|login|auth(?:entication)?|passcode|code)\\b",
    )
    private val code = Regex("(?<!\\d)\\d{4,8}(?!\\d)")

    /** Redacts codes only when the same value also carries verification-related wording. */
    fun redact(text: String, enabled: Boolean): String = redact(
        text = text,
        enabled = enabled,
        contextHasOtp = hasOtpContext(text),
    )

    /**
     * Redacts a field using context detected across the complete notification. This covers common
     * payloads where a title says "Verification code" while the numeric code is in a separate
     * text field.
     */
    fun redact(text: String, enabled: Boolean, contextHasOtp: Boolean): String {
        if (!enabled || !contextHasOtp) return text
        return code.replace(text) { match -> "•".repeat(match.value.length) }
    }

    fun hasOtpContext(text: String): Boolean = otpContext.containsMatchIn(text)
}
