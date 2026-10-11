package com.echo.inbox.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.echo.inbox.core.model.ThemeMode
import com.echo.inbox.core.model.UserSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.io.IOException

private val Context.userSettingsDataStore by preferencesDataStore(name = "user_settings")

class SettingsRepository(
    private val context: Context,
    appScope: CoroutineScope,
) {
    private val firstLoad = CompletableDeferred<Unit>()

    @Volatile
    private var loadedSettings: UserSettings? = null

    val settings: StateFlow<UserSettings> = context.userSettingsDataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                firstLoad.completeExceptionally(error)
                throw error
            }
        }
        .map { preferences ->
            UserSettings(
                onboardingComplete = preferences[Keys.ONBOARDING_COMPLETE] ?: false,
                themeMode = preferences[Keys.THEME_MODE]
                    ?.let { value -> ThemeMode.entries.firstOrNull { it.name == value } }
                    ?: ThemeMode.SYSTEM,
                retentionDays = preferences[Keys.RETENTION_DAYS] ?: 90,
                biometricLockEnabled = preferences[Keys.BIOMETRIC_LOCK] ?: false,
                screenshotProtectionEnabled = preferences[Keys.SCREENSHOT_PROTECTION] ?: false,
                redactOtpEnabled = preferences[Keys.REDACT_OTP] ?: true,
                captureUnknownApps = preferences[Keys.CAPTURE_UNKNOWN] ?: true,
                capturePaused = preferences[Keys.CAPTURE_PAUSED] ?: false,
            )
        }
        .onEach { loaded ->
            // Publish the real snapshot before releasing background capture/retention waiters.
            loadedSettings = loaded
            firstLoad.complete(Unit)
        }
        .stateIn(appScope, SharingStarted.Eagerly, UserSettings())

    /** Waits for DataStore's first real snapshot instead of returning the StateFlow placeholder. */
    suspend fun current(): UserSettings {
        firstLoad.await()
        return checkNotNull(loadedSettings) { "DataStore completed without a settings snapshot" }
    }

    suspend fun setOnboardingComplete(value: Boolean) = edit(Keys.ONBOARDING_COMPLETE, value)
    suspend fun setThemeMode(value: ThemeMode) = edit(Keys.THEME_MODE, value.name)
    suspend fun setRetentionDays(value: Int) = edit(Keys.RETENTION_DAYS, value)
    suspend fun setBiometricLockEnabled(value: Boolean) = edit(Keys.BIOMETRIC_LOCK, value)
    suspend fun setScreenshotProtectionEnabled(value: Boolean) = edit(Keys.SCREENSHOT_PROTECTION, value)
    suspend fun setRedactOtpEnabled(value: Boolean) = edit(Keys.REDACT_OTP, value)
    suspend fun setCaptureUnknownApps(value: Boolean) = edit(Keys.CAPTURE_UNKNOWN, value)
    suspend fun setCapturePaused(value: Boolean) = edit(Keys.CAPTURE_PAUSED, value)

    private suspend fun <T> edit(key: androidx.datastore.preferences.core.Preferences.Key<T>, value: T) {
        context.userSettingsDataStore.edit { preferences -> preferences[key] = value }
    }

    private object Keys {
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val RETENTION_DAYS = intPreferencesKey("retention_days")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val REDACT_OTP = booleanPreferencesKey("redact_otp")
        val CAPTURE_UNKNOWN = booleanPreferencesKey("capture_unknown_apps")
        val CAPTURE_PAUSED = booleanPreferencesKey("capture_paused")
    }
}
