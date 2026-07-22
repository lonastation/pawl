package com.linn.pawl.ui.scanlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linn.pawl.R
import com.linn.pawl.data.model.DuplicateGroupKey
import com.linn.pawl.data.model.ScanLogEntry
import com.linn.pawl.data.model.ScanLogLevel
import com.linn.pawl.ui.theme.AppBrown
import com.linn.pawl.ui.theme.AppLightBrown
import com.linn.pawl.ui.theme.AppWhite
import com.linn.pawl.ui.theme.PawlTheme
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
                title = {
                    Text(
                        text = stringResource(R.string.brand_name),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.offset(y = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onClearLogs,
                        enabled = uiState.totalCount > 0,
                        modifier = Modifier.offset(y = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = stringResource(R.string.scan_logs_clear)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets(top = 18.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val filterChipColors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AppBrown,
                selectedLabelColor = AppWhite,
                labelColor = AppBrown,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filter == ScanLogFilter.All,
                    onClick = { onFilterChange(ScanLogFilter.All) },
                    label = { Text(stringResource(R.string.scan_logs_filter_all)) },
                    colors = filterChipColors,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.filter == ScanLogFilter.All,
                        borderColor = AppLightBrown,
                        selectedBorderColor = AppBrown,
                    )
                )
                FilterChip(
                    selected = uiState.filter == ScanLogFilter.Errors,
                    onClick = { onFilterChange(ScanLogFilter.Errors) },
                    label = { Text(stringResource(R.string.scan_logs_filter_errors)) },
                    colors = filterChipColors,
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = uiState.filter == ScanLogFilter.Errors,
                        borderColor = AppLightBrown,
                        selectedBorderColor = AppBrown,
                    )
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

private val previewScanLogs = listOf(
    ScanLogEntry(
        id = 1L,
        timestamp = 1_720_000_000_000L,
        mediaType = DuplicateGroupKey.MEDIA_IMAGE,
        level = ScanLogLevel.ERROR,
        path = "/storage/emulated/0/DCIM/Camera/IMG_corrupt.jpg",
        message = "Skipped image while extracting signature",
        detail = "java.io.IOException: broken JPEG\n\tat com.linn.pawl.service.ImageScanner.extractSignature(ImageScanner.kt:140)"
    ),
    ScanLogEntry(
        id = 2L,
        timestamp = 1_720_000_100_000L,
        mediaType = DuplicateGroupKey.MEDIA_VIDEO,
        level = ScanLogLevel.WARN,
        path = "/storage/emulated/0/Movies/Downloads/clip_incomplete.mp4",
        message = "Failed to extract video frame hashes",
        detail = "java.lang.RuntimeException: setDataSource failed\n\tat android.media.MediaMetadataRetriever.setDataSource(MediaMetadataRetriever.java:200)"
    ),
    ScanLogEntry(
        id = 3L,
        timestamp = 1_720_000_200_000L,
        mediaType = DuplicateGroupKey.MEDIA_IMAGE,
        level = ScanLogLevel.ERROR,
        path = null,
        message = "Image scan aborted",
        detail = "java.lang.IllegalStateException: Content resolver query failed\n\tat com.linn.pawl.ui.image.ImageScannerViewModel.runScan(ImageScannerViewModel.kt:105)"
    ),
    ScanLogEntry(
        id = 4L,
        timestamp = 1_720_000_300_000L,
        mediaType = DuplicateGroupKey.MEDIA_VIDEO,
        level = ScanLogLevel.INFO,
        path = "/storage/emulated/0/DCIM/Camera/VID_20260315.mp4",
        message = "Video scan aborted",
        detail = null
    ),
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ScanLogsScreenPreview() {
    PawlTheme {
        ScanLogsScreen(
            uiState = ScanLogsViewModel.UiState(
                logs = previewScanLogs,
                filter = ScanLogFilter.All,
                totalCount = previewScanLogs.size
            ),
            onBack = {},
            onFilterChange = {},
            onToggleExpanded = {},
            onClearLogs = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Empty")
@Composable
private fun ScanLogsScreenEmptyPreview() {
    PawlTheme {
        ScanLogsScreen(
            uiState = ScanLogsViewModel.UiState(),
            onBack = {},
            onFilterChange = {},
            onToggleExpanded = {},
            onClearLogs = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Errors filter")
@Composable
private fun ScanLogsScreenErrorsPreview() {
    val errors = previewScanLogs.filter { it.level == ScanLogLevel.ERROR }
    PawlTheme {
        ScanLogsScreen(
            uiState = ScanLogsViewModel.UiState(
                logs = errors,
                filter = ScanLogFilter.Errors,
                totalCount = previewScanLogs.size
            ),
            onBack = {},
            onFilterChange = {},
            onToggleExpanded = {},
            onClearLogs = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Expanded detail")
@Composable
private fun ScanLogsScreenExpandedPreview() {
    PawlTheme {
        ScanLogsScreen(
            uiState = ScanLogsViewModel.UiState(
                logs = previewScanLogs,
                filter = ScanLogFilter.All,
                expandedIds = setOf(1L),
                totalCount = previewScanLogs.size
            ),
            onBack = {},
            onFilterChange = {},
            onToggleExpanded = {},
            onClearLogs = {},
        )
    }
}
