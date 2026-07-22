package com.linn.pawl.ui.scanlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linn.pawl.data.model.ScanLogEntry
import com.linn.pawl.data.model.ScanLogLevel
import com.linn.pawl.data.repository.ScanLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScanLogFilter {
    All,
    Errors
}

@HiltViewModel
class ScanLogsViewModel @Inject constructor(
    private val scanLogRepository: ScanLogRepository
) : ViewModel() {

    private val filter = MutableStateFlow(ScanLogFilter.All)
    private val expandedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<UiState> = combine(
        scanLogRepository.observeLogs(),
        filter,
        expandedIds
    ) { logs, currentFilter, expanded ->
        val visible = when (currentFilter) {
            ScanLogFilter.All -> logs
            ScanLogFilter.Errors -> logs.filter { it.level == ScanLogLevel.ERROR }
        }
        UiState(
            logs = visible,
            filter = currentFilter,
            expandedIds = expanded,
            totalCount = logs.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState()
    )

    fun setFilter(value: ScanLogFilter) {
        filter.value = value
    }

    fun toggleExpanded(id: Long) {
        val current = expandedIds.value
        expandedIds.value = if (id in current) current - id else current + id
    }

    fun clearLogs() {
        viewModelScope.launch {
            scanLogRepository.clear()
            expandedIds.value = emptySet()
        }
    }

    data class UiState(
        val logs: List<ScanLogEntry> = emptyList(),
        val filter: ScanLogFilter = ScanLogFilter.All,
        val expandedIds: Set<Long> = emptySet(),
        val totalCount: Int = 0
    )
}
