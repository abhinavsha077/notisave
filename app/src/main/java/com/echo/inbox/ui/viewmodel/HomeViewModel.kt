package com.echo.inbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.inbox.data.local.AppSourceEntity
import com.echo.inbox.data.local.CaptureHealthEntity
import com.echo.inbox.data.local.ConversationSummaryRow
import com.echo.inbox.data.repository.CaptureHealthRepository
import com.echo.inbox.data.repository.NotificationRepository
import com.echo.inbox.platform.NotificationAccessManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HomeFilter(val label: String) {
    ALL("All"),
    MESSAGES("Messages"),
    SOCIAL("Social"),
    EMAIL("Email"),
    SHOPPING("Shopping"),
    WORK("Work"),
    SAVED("Saved"),
}

data class HomeUiState(
    val conversations: List<ConversationSummaryRow> = emptyList(),
    val apps: List<AppSourceEntity> = emptyList(),
    val filter: HomeFilter = HomeFilter.ALL,
    val accessGranted: Boolean = false,
    val health: CaptureHealthEntity = CaptureHealthEntity(),
)

class HomeViewModel(
    repository: NotificationRepository,
    captureHealthRepository: CaptureHealthRepository,
    private val notificationAccessManager: NotificationAccessManager,
) : ViewModel() {
    private val selectedFilter = MutableStateFlow(HomeFilter.ALL)
    private val accessGranted = MutableStateFlow(notificationAccessManager.isGranted())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.conversations,
        repository.appsWithEvents,
        captureHealthRepository.health,
        selectedFilter,
        accessGranted,
    ) { conversations, apps, health, filter, granted ->
        val filteredConversations = conversations.filter { conversation ->
            when (filter) {
                HomeFilter.ALL -> true
                HomeFilter.MESSAGES -> conversation.appCategory == "MESSAGING"
                HomeFilter.SOCIAL -> conversation.appCategory == "SOCIAL"
                HomeFilter.EMAIL -> conversation.appCategory == "EMAIL"
                HomeFilter.SHOPPING -> conversation.appCategory == "SHOPPING"
                HomeFilter.WORK -> conversation.appCategory == "WORK"
                HomeFilter.SAVED -> conversation.isPinned
            }
        }
        val filteredApps = apps.filter { app ->
            when (filter) {
                HomeFilter.ALL -> true
                HomeFilter.MESSAGES -> app.category == "MESSAGING"
                HomeFilter.SOCIAL -> app.category == "SOCIAL"
                HomeFilter.EMAIL -> app.category == "EMAIL"
                HomeFilter.SHOPPING -> app.category == "SHOPPING"
                HomeFilter.WORK -> app.category == "WORK"
                HomeFilter.SAVED -> true
            }
        }
        HomeUiState(
            conversations = filteredConversations,
            apps = filteredApps,
            filter = filter,
            accessGranted = granted,
            health = health,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setFilter(filter: HomeFilter) {
        selectedFilter.value = filter
    }

    fun refreshAccess() {
        accessGranted.value = notificationAccessManager.isGranted()
    }
}
