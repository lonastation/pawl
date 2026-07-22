package com.linn.pawl.ui.scanlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linn.pawl.R
import com.linn.pawl.data.model.ScanLogEntry
import com.linn.pawl.data.model.ScanLogLevel
import com.linn.pawl.ui.util.formatDateTime
import com.linn.pawl.ui.util.formatPathForDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanLogsScreen(
    uiState: ScanLogsViewModel.UiState,
    onBack: () -> Unit,
    onFilterChange: (ScanLogFilter) -> Unit,
    onToggleExpanded: (Long) -> Unit,
    onClearLogs: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.scan_logs_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onClearLogs,
                        enabled = uiState.totalCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.scan_logs_clear)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == ScanLogFilter.All,
                    onClick = { onFilterChange(ScanLogFilter.All) },
                    label = { Text(stringResource(R.string.scan_logs_filter_all)) }
                )
                FilterChip(
                    selected = uiState.filter == ScanLogFilter.Errors,
                    onClick = { onFilterChange(ScanLogFilter.Errors) },
                    label = { Text(stringResource(R.string.scan_logs_filter_errors)) }
                )
            }

            if (uiState.logs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.scan_logs_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.logs, key = { it.id }) { entry ->
                        ScanLogRow(
                            entry = entry,
                            expanded = entry.id in uiState.expandedIds,
                            onClick = { onToggleExpanded(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanLogRow(
    entry: ScanLogEntry,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val levelColor = when (entry.level) {
        ScanLogLevel.ERROR -> MaterialTheme.colorScheme.error
        ScanLogLevel.WARN -> MaterialTheme.colorScheme.tertiary
        ScanLogLevel.INFO -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = entry.level.name,
                color = levelColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = formatDateTime(entry.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.message,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = entry.mediaType,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        entry.path?.let { path ->
            Text(
                text = formatPathForDisplay(path),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (expanded && !entry.detail.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = entry.detail,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (!expanded && !entry.detail.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.scan_logs_tap_details),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
