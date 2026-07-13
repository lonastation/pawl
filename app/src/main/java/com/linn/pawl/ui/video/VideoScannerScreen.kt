package com.linn.pawl.ui.video

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.linn.pawl.ui.theme.AppWhite
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.util.formatAspectRatio
import com.linn.pawl.ui.util.formatDuration
import com.linn.pawl.ui.util.formatFileSize
import com.linn.pawl.ui.util.formatPathForDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScannerScreen(
    modifier: Modifier = Modifier,
    uiState: VideoScannerViewModel.UiState,
    listState: LazyListState = rememberLazyListState(),
    onScanClick: () -> Unit,
    onToggleSelection: (Long) -> Unit = {},
    onVideoClick: (VideoFile) -> Unit = {},
    onIgnoreGroup: (DuplicateGroup) -> Unit = {},
    onDeleteSelected: () -> Unit = {},
) {
    val showDeleteButton = uiState.selectedVideoIds.isNotEmpty()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VM-LIKE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = 8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets(top = 18.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onScanClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !uiState.isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning... ${uiState.scannedCount}/${uiState.totalVideos}")
                    } else {
                        Text("Start Scanning", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.totalVideos > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = AppWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Total", fontSize = 12.sp)
                                Text(
                                    "${uiState.totalVideos}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Group", fontSize = 12.sp)
                                Text(
                                    "${uiState.duplicateGroups.size}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Duplicate", fontSize = 12.sp)
                                Text(
                                    "${uiState.totalDuplicates}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = if (showDeleteButton) 72.dp else 0.dp
                    )
                ) {
                    items(uiState.duplicateGroups) { group ->
                        VideoDuplicateGroupCard(
                            group = group,
                            selectedVideoIds = uiState.selectedVideoIds,
                            onToggleSelection = onToggleSelection,
                            onVideoClick = onVideoClick,
                            onIgnoreGroup = { onIgnoreGroup(group) }
                        )
                    }
                }
            }

            if (showDeleteButton) {
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Move to trash (${uiState.selectedVideoIds.size})",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VideoDuplicateGroupCard(
    group: DuplicateGroup,
    selectedVideoIds: Set<Long> = emptySet(),
    onToggleSelection: (Long) -> Unit = {},
    onVideoClick: (VideoFile) -> Unit = {},
    onIgnoreGroup: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duplicate Group",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${group.videos.size}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    TextButton(onClick = onIgnoreGroup) {
                        Text("Ignore")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            group.videos.forEach { video ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoThumbnail(
                        contentUri = video.contentUri,
                        modifier = Modifier
                            .size(width = 72.dp, height = 128.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onVideoClick(video) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatPathForDisplay(video.path),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatFileSize(video.size),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatAspectRatio(video.width, video.height),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(video.duration),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Checkbox(
                            checked = video.mediaId in selectedVideoIds,
                            onCheckedChange = { onToggleSelection(video.mediaId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnail(
    contentUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, contentUri) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.loadThumbnail(contentUri, Size(144, 256), null)
            } catch (_: Exception) {
                null
            }
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text("🎬", fontSize = 20.sp)
        }
    }
}

private val previewDuplicateGroup = DuplicateGroup(
    videos = listOf(
        VideoFile(
            mediaId = 1L,
            contentUri = "content://media/external/video/media/1".toUri(),
            path = "/storage/emulated/0/DCIM/Camera/Screenshots/vacation_clip.mp4",
            name = "vacation_clip.mp4",
            size = 125_829_120L,
            duration = 62_000L,
            width = 1080,
            height = 1920
        ),
        VideoFile(
            mediaId = 2L,
            contentUri = "content://media/external/video/media/2".toUri(),
            path = "/storage/emulated/0/Movies/Downloads/vacation_clip (1).mp4",
            name = "vacation_clip (1).mp4",
            size = 125_800_000L,
            duration = 62_100L,
            width = 1080,
            height = 1920
        )
    )
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun VideoScannerScreenPreview() {
    PawlTheme {
        VideoScannerScreen(
            uiState = VideoScannerViewModel.UiState(
                totalVideos = 128,
                totalDuplicates = 3,
                duplicateGroups = listOf(previewDuplicateGroup)
            ),
            onScanClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Scanning")
@Composable
private fun VideoScannerScreenScanningPreview() {
    PawlTheme {
        VideoScannerScreen(
            uiState = VideoScannerViewModel.UiState(
                isScanning = true,
                totalVideos = 128,
                scannedCount = 42
            ),
            onScanClick = {}
        )
    }
}
