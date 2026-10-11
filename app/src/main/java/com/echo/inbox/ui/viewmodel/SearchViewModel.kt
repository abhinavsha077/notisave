package com.echo.inbox.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echo.inbox.data.local.AppSourceEntity
import com.echo.inbox.data.local.SearchResultRow
import com.echo.inbox.data.repository.NotificationRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.concurrent.TimeUnit

data class SearchUiState(
    val query: String = "",
    val selectedPackage: String? = null,
    val periodDays: Int? = null,
    val apps: List<AppSourceEntity> = emptyList(),
    val results: List<SearchResultRow> = emptyList(),
)

@OptIn(FlowPreview::class)
class SearchViewModel(private val repository: NotificationRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val selectedPackage = MutableStateFlow<String?>(null)
    private val periodDays = MutableStateFlow<Int?>(null)

    private val results = combine(
        query.debounce(250),
        selectedPackage,
        periodDays,
    ) { text, packageName, days -> Triple(text, packageName, days) }
        .flatMapLatest { (text, packageName, days) ->
            val since = days?.let { System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it.toLong()) }
            repository.search(text, packageName, since)
        }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        selectedPackage,
        periodDays,
        repository.appsWithEvents,
        results,
    ) { text, packageName, days, apps, searchResults ->
        SearchUiState(text, packageName, days, apps, searchResults)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(value: String) { query.value = value }
    fun setPackage(value: String?) { selectedPackage.value = value }
    fun setPeriodDays(value: Int?) { periodDays.value = value }
}
