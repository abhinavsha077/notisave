package com.echo.inbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.inbox.core.model.InstalledAppInfo
import com.echo.inbox.data.repository.AppSourceRepository
import com.echo.inbox.data.settings.SettingsRepository
import com.echo.inbox.platform.InstalledAppProvider
import com.echo.inbox.platform.NotificationAccessManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class OnboardingUiState(
    val step: Int = 0,
    val apps: List<InstalledAppInfo> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val confirmedSensitivePackages: Set<String> = emptySet(),
    val query: String = "",
    val loadingApps: Boolean = true,
    val notificationAccessGranted: Boolean = false,
    val restrictedSettingsBlocked: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

class OnboardingViewModel(
    private val installedAppProvider: InstalledAppProvider,
    private val appSourceRepository: AppSourceRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationAccessManager: NotificationAccessManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadApps()
        refreshNotificationAccess()
    }

    fun next() = _uiState.update { it.copy(step = (it.step + 1).coerceAtMost(3), error = null) }
    fun previous() = _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(0), error = null) }
    fun setQuery(value: String) = _uiState.update { it.copy(query = value) }

    fun togglePackage(packageName: String) {
        _uiState.update { state ->
            val selected = state.selectedPackages.toMutableSet()
            val confirmedSensitive = state.confirmedSensitivePackages.toMutableSet()
            if (packageName in selected) {
                selected.remove(packageName)
                confirmedSensitive.remove(packageName)
            } else {
                val app = state.apps.firstOrNull { it.packageName == packageName }
                if (app?.sensitive == true && packageName !in confirmedSensitive) {
                    return@update state.copy(
                        error = "Confirm the sensitive application warning before enabling capture.",
                    )
                }
                selected.add(packageName)
            }
            state.copy(
                selectedPackages = selected,
                confirmedSensitivePackages = confirmedSensitive,
                error = null,
            )
        }
    }

    fun confirmSensitivePackage(packageName: String) {
        _uiState.update { state ->
            val app = state.apps.firstOrNull { it.packageName == packageName }
            if (app?.sensitive != true) return@update state
            state.copy(
                selectedPackages = state.selectedPackages + packageName,
                confirmedSensitivePackages = state.confirmedSensitivePackages + packageName,
                error = null,
            )
        }
    }

    fun selectRecommended() {
        _uiState.update { state ->
            state.copy(
                selectedPackages = state.apps
                    .filter { app -> !app.sensitive && app.category.name in RECOMMENDED_CATEGORIES }
                    .mapTo(mutableSetOf()) { it.packageName },
                confirmedSensitivePackages = emptySet(),
            )
        }
    }

    fun clearSelection() = _uiState.update {
        it.copy(selectedPackages = emptySet(), confirmedSensitivePackages = emptySet(), error = null)
    }

    fun refreshNotificationAccess() {
        _uiState.update {
            it.copy(
                notificationAccessGranted = notificationAccessManager.isGranted(),
                restrictedSettingsBlocked = notificationAccessManager.isRestrictedSettingsBlocked(),
            )
        }
    }

    fun complete() {
        val state = _uiState.value
        if (!state.notificationAccessGranted) {
            _uiState.update { it.copy(error = "Grant notification access before finishing setup.") }
            return
        }
        if (state.selectedPackages.isEmpty()) {
            _uiState.update { it.copy(error = "Select at least one application to capture.") }
            return
        }
        val sensitivePackages = state.apps
            .filter { it.sensitive }
            .mapTo(mutableSetOf()) { it.packageName }
        val unconfirmedSensitive = state.selectedPackages
            .intersect(sensitivePackages)
            .minus(state.confirmedSensitivePackages)
        if (unconfirmedSensitive.isNotEmpty()) {
            _uiState.update {
                it.copy(error = "Confirm every sensitive application before finishing setup.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            try {
                appSourceRepository.applyOnboardingSelection(state.apps, state.selectedPackages)
                settingsRepository.setOnboardingComplete(true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(saving = false, error = error.message ?: "Setup could not be saved.")
                }
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val apps = withContext(Dispatchers.IO) { installedAppProvider.loadLaunchableApps() }
                appSourceRepository.initializeInstalledApps(apps)
                val selected = appSourceRepository.currentApps()
                    .filter { it.captureEnabled }
                    .mapTo(mutableSetOf()) { it.packageName }
                val sensitivePackages = apps
                    .filter { it.sensitive }
                    .mapTo(mutableSetOf()) { it.packageName }
                _uiState.update {
                    it.copy(
                        apps = apps,
                        selectedPackages = selected,
                        // A previously persisted enabled source was already explicitly selected.
                        confirmedSensitivePackages = selected.intersect(sensitivePackages),
                        loadingApps = false,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        loadingApps = false,
                        error = error.message ?: "Installed applications could not be loaded.",
                    )
                }
            }
        }
    }

    companion object {
        private val RECOMMENDED_CATEGORIES = setOf("MESSAGING", "SOCIAL", "WORK", "EMAIL")
    }
}
