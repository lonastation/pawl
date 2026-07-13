package com.linn.pawl.ui.recycle

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linn.pawl.data.local.RecycledMediaEntity
import com.linn.pawl.data.model.DuplicateGroupKey
import com.linn.pawl.ui.theme.AppWhite
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.util.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecyclingStationScreen(
    uiState: RecyclingStationViewModel.UiState,
    trashFilePath: (RecycledMediaEntity) -> String,
    onFilterChange: (RecycleFilter) -> Unit,
    onToggleSelection: (String) -> Unit,
    onRestoreSelected: () -> Unit,
    onPermanentlyDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val showActions = uiState.selectedIds.isNotEmpty() && !uiState.isBusy

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Recycling Station",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
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
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filter == RecycleFilter.All,
                        onClick = { onFilterChange(RecycleFilter.All) },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = uiState.filter == RecycleFilter.Images,
                        onClick = { onFilterChange(RecycleFilter.Images) },
                        label = { Text("Images") }
                    )
                    FilterChip(
                        selected = uiState.filter == RecycleFilter.Videos,
                        onClick = { onFilterChange(RecycleFilter.Videos) },
                        label = { Text("Videos") }
                    )
                }

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    uiState.visibleItems.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Recycling station is empty",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                bottom = if (showActions) 88.dp else 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (uiState.filter == RecycleFilter.All) {
                                if (uiState.images.isNotEmpty()) {
                                    item {
                                        SectionHeader("Images (${uiState.images.size})")
                                    }
                                    items(uiState.images, key = { it.id }) { item ->
                                        RecycledMediaCard(
                                            item = item,
                                            trashPath = trashFilePath(item),
                                            selected = item.id in uiState.selectedIds,
                                            onToggleSelection = { onToggleSelection(item.id) }
                                        )
                                    }
                                }
                                if (uiState.videos.isNotEmpty()) {
                                    item {
                                        SectionHeader("Videos (${uiState.videos.size})")
                                    }
                                    items(uiState.videos, key = { it.id }) { item ->
                                        RecycledMediaCard(
                                            item = item,
                                            trashPath = trashFilePath(item),
                                            selected = item.id in uiState.selectedIds,
                                            onToggleSelection = { onToggleSelection(item.id) }
                                        )
                                    }
                                }
                            } else {
                                items(uiState.visibleItems, key = { it.id }) { item ->
                                    RecycledMediaCard(
                                        item = item,
                                        trashPath = trashFilePath(item),
                                        selected = item.id in uiState.selectedIds,
                                        onToggleSelection = { onToggleSelection(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showActions) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestoreSelected,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = !uiState.isBusy
                    ) {
                        Text("Restore (${uiState.selectedIds.size})", fontSize = 16.sp)
                    }
                    Button(
                        onClick = onPermanentlyDeleteSelected,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium,
                        enabled = !uiState.isBusy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Delete (${uiState.selectedIds.size})", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun RecycledMediaCard(
    item: RecycledMediaEntity,
    trashPath: String,
    selected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrashThumbnail(
                path = trashPath,
                isVideo = item.mediaType == DuplicateGroupKey.MEDIA_VIDEO,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.mediaType == DuplicateGroupKey.MEDIA_IMAGE) "Image" else "Video",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatFileSize(item.sizeBytes),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelection() }
            )
        }
    }
}

@Composable
private fun TrashThumbnail(
    path: String,
    isVideo: Boolean,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, path, isVideo) {
        value = withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) return@withContext null
                if (isVideo) {
                    ThumbnailUtils.createVideoThumbnail(file, Size(144, 144), null)
                } else {
                    val options = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, options)
                    options.inSampleSize = calculateInSampleSize(options, 144, 144)
                    options.inJustDecodeBounds = false
                    BitmapFactory.decodeFile(file.absolutePath, options)
                }
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
            Text(if (isVideo) "▶" else "🖼", fontSize = 20.sp)
        }
    }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        var halfHeight = height / 2
        var halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

private val previewRecycledItems = listOf(
    RecycledMediaEntity(
        id = "img-1",
        mediaType = DuplicateGroupKey.MEDIA_IMAGE,
        originalMediaId = 1L,
        displayName = "photo.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 2_500_000L,
        width = 4032,
        height = 3024,
        durationMs = 0L,
        originalPath = "/storage/emulated/0/DCIM/Camera/photo.jpg",
        relativePath = "DCIM/Camera/",
        trashFileName = "img-1.jpg",
        dateTaken = 1_700_000_000_000L,
        recycledAt = 1_710_000_000_000L
    ),
    RecycledMediaEntity(
        id = "img-2",
        mediaType = DuplicateGroupKey.MEDIA_IMAGE,
        originalMediaId = 2L,
        displayName = "photo_copy.jpg",
        mimeType = "image/jpeg",
        sizeBytes = 2_400_000L,
        width = 4032,
        height = 3024,
        durationMs = 0L,
        originalPath = "/storage/emulated/0/Pictures/photo_copy.jpg",
        relativePath = "Pictures/",
        trashFileName = "img-2.jpg",
        dateTaken = 1_700_000_100_000L,
        recycledAt = 1_710_000_100_000L
    ),
    RecycledMediaEntity(
        id = "vid-1",
        mediaType = DuplicateGroupKey.MEDIA_VIDEO,
        originalMediaId = 3L,
        displayName = "clip.mp4",
        mimeType = "video/mp4",
        sizeBytes = 18_000_000L,
        width = 1920,
        height = 1080,
        durationMs = 12_500L,
        originalPath = "/storage/emulated/0/DCIM/Camera/clip.mp4",
        relativePath = "DCIM/Camera/",
        trashFileName = "vid-1.mp4",
        dateTaken = 1_700_000_200_000L,
        recycledAt = 1_710_000_200_000L
    )
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RecyclingStationScreenPreview() {
    PawlTheme {
        RecyclingStationScreen(
            uiState = RecyclingStationViewModel.UiState(
                items = previewRecycledItems,
                selectedIds = setOf("img-2")
            ),
            trashFilePath = { "" },
            onFilterChange = {},
            onToggleSelection = {},
            onRestoreSelected = {},
            onPermanentlyDeleteSelected = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Empty")
@Composable
private fun RecyclingStationScreenEmptyPreview() {
    PawlTheme {
        RecyclingStationScreen(
            uiState = RecyclingStationViewModel.UiState(),
            trashFilePath = { "" },
            onFilterChange = {},
            onToggleSelection = {},
            onRestoreSelected = {},
            onPermanentlyDeleteSelected = {}
        )
    }
}
