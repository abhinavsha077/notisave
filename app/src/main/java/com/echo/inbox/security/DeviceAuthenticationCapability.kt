package com.echo.inbox.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager

/**
 * Centralizes the authenticator combination used by both capability checks and BiometricPrompt.
 * API 28-29 reject BIOMETRIC_STRONG | DEVICE_CREDENTIAL, so those releases use weak biometrics
 * with the device credential fallback. The database encryption key itself is not gated by this
 * setting; this lock protects access to the app UI.
 */
class DeviceAuthenticationCapability(private val context: Context) {
    fun canAuthenticate(): Boolean {
        val result = BiometricManager.from(context).canAuthenticate(allowedAuthenticators())
        if (result == BiometricManager.BIOMETRIC_SUCCESS) return true

        // Some pre-30 devices cannot fully report credential capability through BiometricManager.
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            context.getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
    }

    companion object {
        fun allowedAuthenticators(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        }
    }
}
