package com.echo.inbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.inbox.data.local.AppSourceEntity
import com.echo.inbox.data.local.ConversationSummaryRow
import com.echo.inbox.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AppDetailUiState(
    val packageName: String = "",
    val app: AppSourceEntity? = null,
    val conversations: List<ConversationSummaryRow> = emptyList(),
)

class AppDetailViewModel(
    val packageName: String,
    repository: NotificationRepository,
) : ViewModel() {
    val uiState: StateFlow<AppDetailUiState> = combine(
        repository.apps,
        repository.conversationsForApp(packageName),
    ) { apps, conversations ->
        AppDetailUiState(
            packageName = packageName,
            app = apps.firstOrNull { it.packageName == packageName },
            conversations = conversations,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDetailUiState(packageName = packageName))
}
