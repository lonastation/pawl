package com.linn.pawl


import android.Manifest
import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import androidx.activity.result.IntentSenderRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
import com.linn.pawl.ui.VideoDetailScreen
import com.linn.pawl.ui.theme.AppWhite
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.DuplicateGroup
import com.linn.pawl.ui.VideoFile
import com.linn.pawl.ui.VideoScannerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: VideoScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PawlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VideoScannerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoScannerApp(
    viewModel: VideoScannerViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.startScan()
        }
    }

    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedIds = uiState.selectedVideoIds
            viewModel.onVideosDeleted(deletedIds)
        }
    }

    var selectedVideoId by remember { mutableLongStateOf(-1L) }
    val listState = rememberLazyListState()
    val selectedVideo = if (selectedVideoId >= 0) {
        uiState.duplicateGroups
            .flatMap { it.videos }
            .find { it.mediaId == selectedVideoId }
    } else {
        null
    }

    if (selectedVideo != null) {
        BackHandler { selectedVideoId = -1L }
        VideoDetailScreen(
            video = selectedVideo,
            onBack = { selectedVideoId = -1L }
        )
    } else {
        VideoScannerContent(
            uiState = uiState,
            listState = listState,
            onScanClick = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                )
            },
            onToggleSelection = viewModel::toggleVideoSelection,
            onVideoClick = { video -> selectedVideoId = video.mediaId },
            onDeleteSelected = {
                val uris = viewModel.getSelectedVideoUris()
                if (uris.isEmpty()) return@VideoScannerContent
                val intentSender = MediaStore.createDeleteRequest(
                    context.contentResolver,
                    uris
                ).intentSender
                deleteLauncher.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoScannerContent(
    uiState: VideoScannerViewModel.UiState,
    listState: LazyListState = rememberLazyListState(),
    onScanClick: () -> Unit,
    onToggleSelection: (Long) -> Unit = {},
    onVideoClick: (VideoFile) -> Unit = {},
    onDeleteSelected: () -> Unit = {}
) {
    val showDeleteButton = uiState.selectedVideoIds.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VM-LIKE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .offset(y = 8.dp)
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

        // 统计信息
        if (uiState.totalVideos > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = AppWhite
                ),
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
                        Text("${uiState.totalVideos}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Group", fontSize = 12.sp)
                        Text("${uiState.duplicateGroups.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duplicate", fontSize = 12.sp)
                        Text("${uiState.totalDuplicates}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 结果显示
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
                DuplicateGroupCard(
                    group = group,
                    selectedVideoIds = uiState.selectedVideoIds,
                    onToggleSelection = onToggleSelection,
                    onVideoClick = onVideoClick
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
                    text = "Delete (${uiState.selectedVideoIds.size})",
                    fontSize = 18.sp
                )
            }
        }
    }
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedVideoIds: Set<Long> = emptySet(),
    onToggleSelection: (Long) -> Unit = {},
    onVideoClick: (VideoFile) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppWhite
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duplicate Group",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${group.videos.size}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
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

internal fun formatPathForDisplay(path: String): String {
    return path.replace("/", "/\u200B")
}

internal fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}

internal fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "—"
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

internal fun formatDateTime(timestampMs: Long): String {
    if (timestampMs <= 0) return "—"
    val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.US)
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(java.time.Instant.ofEpochMilli(timestampMs))
}

internal fun formatAspectRatio(width: Int, height: Int): String {
    if (width <= 0 || height <= 0) return "—"
    val divisor = gcd(width, height)
    return "${width / divisor}:${height / divisor}"
}

private fun gcd(a: Int, b: Int): Int {
    var x = kotlin.math.abs(a)
    var y = kotlin.math.abs(b)
    while (y != 0) {
        val remainder = x % y
        x = y
        y = remainder
    }
    return x
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun VideoScannerAppPreview() {
    PawlTheme {
        VideoScannerContent(
            uiState = VideoScannerViewModel.UiState(
                totalVideos = 128,
                totalDuplicates = 3,
                duplicateGroups = listOf(
                    previewDuplicateGroup,
                    DuplicateGroup(
                        videos = listOf(
                            VideoFile(
                                mediaId = 3L,
                                contentUri = "content://media/external/video/media/3".toUri(),
                                path = "/storage/emulated/0/Pictures/Screenshots/birthday_party.mp4",
                                name = "birthday_party.mp4",
                                size = 88_900_000L,
                                duration = 45_000L,
                                width = 1920,
                                height = 1080
                            ),
                            VideoFile(
                                mediaId = 4L,
                                contentUri = "content://media/external/video/media/4".toUri(),
                                path = "/storage/emulated/0/DCIM/Camera/birthday_party.mp4",
                                name = "birthday_party.mp4",
                                size = 89_000_000L,
                                duration = 45_000L,
                                width = 1920,
                                height = 1080
                            )
                        )
                    )
                )
            ),
            onScanClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun VideoScannerAppScanningPreview() {
    PawlTheme {
        VideoScannerContent(
            uiState = VideoScannerViewModel.UiState(
                isScanning = true,
                totalVideos = 128,
                scannedCount = 42
            ),
            onScanClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Delete selected")
@Composable
private fun VideoScannerAppDeletePreview() {
    PawlTheme {
        VideoScannerContent(
            uiState = VideoScannerViewModel.UiState(
                totalVideos = 128,
                totalDuplicates = 3,
                duplicateGroups = listOf(previewDuplicateGroup),
                selectedVideoIds = setOf(1L, 2L)
            ),
            onScanClick = {}
        )
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