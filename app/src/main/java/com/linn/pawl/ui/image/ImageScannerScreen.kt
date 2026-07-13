package com.linn.pawl.ui.image

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
import com.linn.pawl.ui.util.formatFileSize
import com.linn.pawl.ui.util.formatPathForDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageScannerScreen(
    modifier: Modifier = Modifier,
    uiState: ImageScannerViewModel.UiState,
    listState: LazyListState = rememberLazyListState(),
    onFindSimilarClick: () -> Unit,
    onToggleSelection: (Long) -> Unit = {},
    onImageClick: (ImageFile) -> Unit = {},
    onIgnoreGroup: (ImageDuplicateGroup) -> Unit = {},
    onDeleteSelected: () -> Unit = {},
) {
    val showDeleteButton = uiState.selectedImageIds.isNotEmpty()
    val isScanning = uiState.isScanning

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
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
            ) {
                Button(
                    onClick = onFindSimilarClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.tertiary,
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scanning... ${uiState.scannedCount}/${uiState.totalImages}")
                    } else {
                        Text("Find Similar", fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.totalImages > 0) {
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
                                    "${uiState.totalImages}",
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
                        ImageDuplicateGroupCard(
                            group = group,
                            selectedImageIds = uiState.selectedImageIds,
                            onToggleSelection = onToggleSelection,
                            onImageClick = onImageClick,
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
                        text = "Move to trash (${uiState.selectedImageIds.size})",
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ImageDuplicateGroupCard(
    group: ImageDuplicateGroup,
    selectedImageIds: Set<Long> = emptySet(),
    onToggleSelection: (Long) -> Unit = {},
    onImageClick: (ImageFile) -> Unit = {},
    onIgnoreGroup: () -> Unit = {}
) {
    val groupLabel = if (group.images.any { it.isGif }) {
        "Similar Gif(${group.images.size})"
    } else {
        "Similar Image(${group.images.size})"
    }

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
                    text = groupLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onIgnoreGroup) {
                    Text("Ignore")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            group.images.forEach { image ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ImageThumbnail(
                        contentUri = image.contentUri,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onImageClick(image) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = image.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatPathForDisplay(image.path),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatFileSize(image.size),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatAspectRatio(image.width, image.height),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Checkbox(
                            checked = image.mediaId in selectedImageIds,
                            onCheckedChange = { onToggleSelection(image.mediaId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageThumbnail(
    contentUri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, contentUri) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.loadThumbnail(contentUri, Size(144, 144), null)
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
            Text("🖼", fontSize = 20.sp)
        }
    }
}

private val previewImageGroup = ImageDuplicateGroup(
    images = listOf(
        ImageFile(
            mediaId = 1L,
            contentUri = "content://media/external/images/media/1".toUri(),
            path = "/storage/emulated/0/DCIM/Camera/photo.jpg",
            name = "photo.jpg",
            size = 2_500_000L,
            width = 4032,
            height = 3024
        ),
        ImageFile(
            mediaId = 2L,
            contentUri = "content://media/external/images/media/2".toUri(),
            path = "/storage/emulated/0/Pictures/photo_copy.jpg",
            name = "photo_copy.jpg",
            size = 2_500_000L,
            width = 4032,
            height = 3024
        )
    )
)

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ImageScannerScreenPreview() {
    PawlTheme {
        ImageScannerScreen(
            uiState = ImageScannerViewModel.UiState(
                totalImages = 256,
                totalDuplicates = 2,
                duplicateGroups = listOf(previewImageGroup)
            ),
            onFindSimilarClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Scanning similar")
@Composable
private fun ImageScannerScreenScanningPreview() {
    PawlTheme {
        ImageScannerScreen(
            uiState = ImageScannerViewModel.UiState(
                isScanning = true,
                totalImages = 256,
                scannedCount = 80
            ),
            onFindSimilarClick = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "With delete selection")
@Composable
private fun ImageScannerScreenDeletePreview() {
    PawlTheme {
        ImageScannerScreen(
            uiState = ImageScannerViewModel.UiState(
                totalImages = 256,
                totalDuplicates = 2,
                duplicateGroups = listOf(previewImageGroup),
                selectedImageIds = setOf(2L)
            ),
            onFindSimilarClick = {}
        )
    }
}
