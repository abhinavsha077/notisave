package com.echo.inbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.inbox.core.model.ThemeMode
import com.echo.inbox.core.model.UserSettings
import com.echo.inbox.data.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setCaptureUnknown(enabled: Boolean) {
        viewModelScope.launch { repository.setCaptureUnknownApps(enabled) }
    }

    fun resetOnboarding() {
        viewModelScope.launch { repository.setOnboardingComplete(false) }
    }
}
