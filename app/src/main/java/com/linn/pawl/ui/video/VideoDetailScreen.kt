package com.linn.pawl.ui.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.util.formatAspectRatio
import com.linn.pawl.ui.util.formatDateTime
import com.linn.pawl.ui.util.formatDuration
import com.linn.pawl.ui.util.formatFileSize
import com.linn.pawl.ui.util.formatPathForDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDetailScreen(
    video: VideoFile,
    onBack: () -> Unit
) {
    val aspectRatio = if (video.width > 0 && video.height > 0) {
        video.width.toFloat() / video.height
    } else {
        16f / 9f
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = video.name,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(y = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.offset(y = 8.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = WindowInsets(top = 18.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VideoPlayer(
                contentUri = video.contentUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailInfoRow(label = "File Name", value = video.name)
                DetailInfoRow(label = "File Path", value = formatPathForDisplay(video.path))
                DetailInfoRow(label = "File Size", value = formatFileSize(video.size))
                DetailInfoRow(label = "Created", value = formatDateTime(video.dateCreated))
                DetailInfoRow(
                    label = "Aspect Ratio",
                    value = formatAspectRatio(video.width, video.height)
                )
                DetailInfoRow(label = "Duration", value = formatDuration(video.duration))
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun VideoPlayer(
    contentUri: Uri,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(Color.Black))
        return
    }

    val context = LocalContext.current
    val exoPlayer = remember(contentUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(contentUri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(contentUri) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun VideoDetailScreenPreview() {
    PawlTheme {
        VideoDetailScreen(
            video = VideoFile(
                mediaId = 1L,
                contentUri = "content://media/external/video/media/1".toUri(),
                path = "/storage/emulated/0/DCIM/Camera/Screenshots/vacation_clip.mp4",
                name = "vacation_clip.mp4",
                size = 125_829_120L,
                duration = 62_000L,
                width = 1080,
                height = 1920,
                dateCreated = 1_718_000_000_000L
            ),
            onBack = {}
        )
    }
}
