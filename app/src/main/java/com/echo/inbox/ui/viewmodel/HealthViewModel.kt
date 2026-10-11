package com.echo.inbox.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.inbox.data.local.CaptureHealthEntity
import com.echo.inbox.data.repository.CaptureHealthRepository
import com.echo.inbox.platform.NotificationAccessManager
import com.echo.inbox.platform.NotificationTestManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private fun manufacturerGuidance(manufacturer: String): String = when (manufacturer.lowercase()) {
    "xiaomi", "redmi", "poco" ->
        "Allow autostart and set battery use to No restrictions if captures stop after the app is closed."
    "oppo", "realme", "oneplus" ->
        "Allow background activity and autostart in the device's app-management settings."
    "vivo", "iqoo" ->
        "Enable autostart and unrestricted background power use for reliable capture."
    "samsung" ->
        "Remove Echo Inbox from Sleeping apps if the listener disconnects repeatedly."
    else ->
        "Keep notification access enabled. If captures stop, review this app's battery and background restrictions."
}

data class HealthUiState(
    val notificationAccessGranted: Boolean = false,
    val health: CaptureHealthEntity = CaptureHealthEntity(),
    val manufacturer: String = Build.MANUFACTURER,
    val guidance: String = manufacturerGuidance(Build.MANUFACTURER),
    val actionMessage: String? = null,
)

class HealthViewModel(
    captureHealthRepository: CaptureHealthRepository,
    private val notificationAccessManager: NotificationAccessManager,
    private val notificationTestManager: NotificationTestManager,
) : ViewModel() {
    private val accessGranted = MutableStateFlow(notificationAccessManager.isGranted())
    private val actionMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HealthUiState> = combine(
        accessGranted,
        captureHealthRepository.health,
        actionMessage,
    ) { granted, health, message ->
        HealthUiState(
            notificationAccessGranted = granted,
            health = health,
            actionMessage = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HealthUiState())

    fun refresh() {
        accessGranted.value = notificationAccessManager.isGranted()
    }

    fun requestRebind() {
        notificationAccessManager.requestRebind()
        actionMessage.value = "Listener rebind requested."
    }

    fun postTest() {
        notificationTestManager.postTest()
            .onSuccess {
                actionMessage.value = "Test notification posted. Detection should appear below."
            }
            .onFailure {
                actionMessage.value = "The test notification could not be posted. Check notification permission."
            }
    }

    fun notificationPermissionDenied() {
        actionMessage.value = "Notification permission was not granted, so the local test was not posted."
    }

    fun clearActionMessage() {
        actionMessage.value = null
    }
}
