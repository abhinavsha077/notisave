package com.echo.inbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.inbox.data.local.ConversationEntity
import com.echo.inbox.data.local.MessageEventEntity
import com.echo.inbox.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConversationUiState(
    val conversation: ConversationEntity? = null,
    val messages: List<MessageEventEntity> = emptyList(),
    val appName: String = "Application",
    val appCategory: String = "OTHER",
)

class ConversationViewModel(
    private val conversationId: Long,
    private val repository: NotificationRepository,
) : ViewModel() {
    val uiState: StateFlow<ConversationUiState> = combine(
        repository.conversation(conversationId),
        repository.messages(conversationId),
        repository.apps,
    ) { conversation, messages, apps ->
        val app = apps.firstOrNull { it.packageName == conversation?.appPackage }
        ConversationUiState(
            conversation = conversation,
            messages = messages,
            appName = app?.displayName ?: conversation?.appPackage.orEmpty(),
            appCategory = app?.category ?: "OTHER",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConversationUiState())

    init {
        viewModelScope.launch { repository.markConversationRead(conversationId) }
    }

    fun togglePinned() {
        viewModelScope.launch { repository.togglePinned(conversationId) }
    }

    fun delete() {
        viewModelScope.launch { repository.deleteConversation(conversationId) }
    }
}
