package com.echo.inbox.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.inbox.core.model.UserSettings
import com.echo.inbox.data.local.AppSourceEntity
import com.echo.inbox.data.local.DataStatsRow
import com.echo.inbox.data.repository.AppSourceRepository
import com.echo.inbox.data.repository.NotificationRepository
import com.echo.inbox.data.settings.SettingsRepository
import com.echo.inbox.export.JsonExportWriter
import com.echo.inbox.security.DeviceAuthenticationCapability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class PrivacyUiState(
    val settings: UserSettings = UserSettings(),
    val apps: List<AppSourceEntity> = emptyList(),
    val stats: DataStatsRow = DataStatsRow(0, 0, 0, 0),
    val databaseBytes: Long = 0,
    val authenticationAvailable: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
)

private data class PrivacyOperationState(
    val stats: DataStatsRow,
    val databaseBytes: Long,
    val busy: Boolean,
    val message: String?,
)

class PrivacyViewModel(
    private val settingsRepository: SettingsRepository,
    private val appSourceRepository: AppSourceRepository,
    private val notificationRepository: NotificationRepository,
    private val exportWriter: JsonExportWriter,
    private val deviceAuthenticationCapability: DeviceAuthenticationCapability,
) : ViewModel() {
    private val stats = MutableStateFlow(DataStatsRow(0, 0, 0, 0))
    private val databaseBytes = MutableStateFlow(0L)
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val operationState = combine(stats, databaseBytes, busy, message) { currentStats, bytes, isBusy, currentMessage ->
        PrivacyOperationState(currentStats, bytes, isBusy, currentMessage)
    }

    val uiState: StateFlow<PrivacyUiState> = combine(
        settingsRepository.settings,
        appSourceRepository.allApps,
        operationState,
    ) { settings, apps, operation ->
        PrivacyUiState(
            settings = settings,
            apps = apps,
            stats = operation.stats,
            databaseBytes = operation.databaseBytes,
            authenticationAvailable = deviceAuthenticationCapability.canAuthenticate(),
            busy = operation.busy,
            message = operation.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrivacyUiState())

    init {
        refreshStats()
    }

    fun setCaptureEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch { appSourceRepository.setCaptureEnabled(packageName, enabled) }
    }

    fun setCapturePaused(paused: Boolean) {
        viewModelScope.launch { settingsRepository.setCapturePaused(paused) }
    }

    fun setCaptureUnknownApps(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setCaptureUnknownApps(enabled) }
    }

    fun setRetentionDays(days: Int) = runBusy("Retention policy updated.") {
        settingsRepository.setRetentionDays(days)
        if (days > 0) {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
            notificationRepository.deleteOlderThan(cutoff)
        }
    }

    fun setBiometricLock(enabled: Boolean) {
        if (enabled && !deviceAuthenticationCapability.canAuthenticate()) {
            message.value = "Set a secure device PIN, pattern, password, or supported biometric before enabling app lock."
            return
        }
        viewModelScope.launch { settingsRepository.setBiometricLockEnabled(enabled) }
    }

    fun setScreenshotProtection(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setScreenshotProtectionEnabled(enabled) }
    }

    fun setOtpRedaction(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setRedactOtpEnabled(enabled) }
    }

    fun deleteAppData(packageName: String) = runBusy("Stored data for the app was deleted.") {
        notificationRepository.deleteAppData(packageName)
    }

    fun clearCapturedData() = runBusy("All captured notification data was deleted.") {
        notificationRepository.clearCapturedData()
    }

    fun export(uri: Uri) = runBusy("Export completed.") {
        exportWriter.write(uri)
    }

    fun clearMessage() {
        message.value = null
    }

    fun refreshStats() {
        viewModelScope.launch {
            stats.value = withContext(Dispatchers.IO) { notificationRepository.stats() }
            databaseBytes.value = withContext(Dispatchers.IO) { notificationRepository.databaseBytes() }
        }
    }

    private fun runBusy(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            busy.value = true
            message.value = null
            try {
                withContext(Dispatchers.IO) { action() }
                message.value = successMessage
                refreshStats()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                message.value = error.message ?: "The operation failed."
            } finally {
                busy.value = false
            }
        }
    }
}
