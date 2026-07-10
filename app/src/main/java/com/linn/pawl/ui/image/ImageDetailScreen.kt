package com.linn.pawl.ui.image

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.util.formatAspectRatio
import com.linn.pawl.ui.util.formatDateTime
import com.linn.pawl.ui.util.formatFileSize
import com.linn.pawl.ui.util.formatPathForDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(
    image: ImageFile,
    onBack: () -> Unit
) {
    val aspectRatio = if (image.width > 0 && image.height > 0) {
        image.width.toFloat() / image.height
    } else {
        1f
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = image.name,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                )
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
            ImagePreview(
                contentUri = image.contentUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(8.dp))
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailInfoRow(label = "File Name", value = image.name)
                DetailInfoRow(label = "File Path", value = formatPathForDisplay(image.path))
                DetailInfoRow(label = "File Size", value = formatFileSize(image.size))
                DetailInfoRow(label = "Created", value = formatDateTime(image.dateCreated))
                DetailInfoRow(
                    label = "Dimensions",
                    value = "${image.width} × ${image.height}"
                )
                DetailInfoRow(
                    label = "Aspect Ratio",
                    value = formatAspectRatio(image.width, image.height)
                )
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
private fun ImagePreview(
    contentUri: Uri,
    modifier: Modifier = Modifier
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
        return
    }

    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, contentUri) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.loadThumbnail(contentUri, Size(1024, 1024), null)
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
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text("🖼", fontSize = 32.sp)
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ImageDetailScreenPreview() {
    PawlTheme {
        ImageDetailScreen(
            image = ImageFile(
                mediaId = 1L,
                contentUri = "content://media/external/images/media/1".toUri(),
                path = "/storage/emulated/0/DCIM/Camera/photo.jpg",
                name = "photo.jpg",
                size = 2_500_000L,
                width = 4032,
                height = 3024,
                dateCreated = 1_718_000_000_000L
            ),
            onBack = {}
        )
    }
}
