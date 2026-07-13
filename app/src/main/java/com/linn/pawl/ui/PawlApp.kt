package com.linn.pawl.ui

import android.Manifest
import android.app.Activity
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linn.pawl.ui.image.ImageDetailScreen
import com.linn.pawl.ui.image.ImageScannerScreen
import com.linn.pawl.ui.image.ImageScannerViewModel
import com.linn.pawl.ui.navigation.AppTab
import com.linn.pawl.ui.settings.SettingsScreen
import com.linn.pawl.ui.settings.SettingsViewModel
import com.linn.pawl.ui.theme.AppBrown
import com.linn.pawl.ui.theme.AppLightBrown
import com.linn.pawl.ui.theme.AppWhite
import com.linn.pawl.ui.video.VideoDetailScreen
import com.linn.pawl.ui.video.VideoScannerScreen
import com.linn.pawl.ui.video.VideoScannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PawlApp(
    videoViewModel: VideoScannerViewModel,
    imageViewModel: ImageScannerViewModel,
    settingsViewModel: SettingsViewModel
) {
    val videoUiState by videoViewModel.uiState.collectAsStateWithLifecycle()
    val imageUiState by imageViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            videoViewModel.startScan()
        }
    }

    val regenerateVideoPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            videoViewModel.regenerateFingerprintsAndScan()
        }
    }

    var pendingImageScan by remember { mutableStateOf(false) }

    val imagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            if (pendingImageScan) {
                imageViewModel.startSimilarScan()
            }
        }
        pendingImageScan = false
    }

    val regenerateImagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            imageViewModel.regenerateFingerprintsAndScan()
        }
    }

    val context = LocalContext.current

    val videoDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            videoViewModel.onVideosDeleted(videoUiState.selectedVideoIds)
        }
    }

    val imageDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            imageViewModel.onImagesDeleted(imageUiState.selectedImageIds)
        }
    }

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Video) }
    var selectedVideoId by remember { mutableLongStateOf(-1L) }
    var selectedImageId by remember { mutableLongStateOf(-1L) }
    val videoListState = rememberLazyListState()
    val imageListState = rememberLazyListState()

    LaunchedEffect(selectedTab, videoUiState.isScanning, imageUiState.isScanning) {
        if (selectedTab == AppTab.Settings && !videoUiState.isScanning && !imageUiState.isScanning) {
            settingsViewModel.refreshCounts()
        }
    }

    val selectedVideo = if (selectedVideoId >= 0) {
        videoUiState.duplicateGroups
            .flatMap { it.videos }
            .find { it.mediaId == selectedVideoId }
    } else {
        null
    }

    val selectedImage = if (selectedImageId >= 0) {
        imageUiState.duplicateGroups
            .flatMap { it.images }
            .find { it.mediaId == selectedImageId }
    } else {
        null
    }

    val onRegenerateVideoClick: () -> Unit = {
        regenerateVideoPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
    }

    val onRegenerateImageClick: () -> Unit = {
        regenerateImagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
    }

    val requestImageScan: () -> Unit = {
        pendingImageScan = true
        imagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
    }

    if (selectedVideo != null) {
        BackHandler { selectedVideoId = -1L }
        VideoDetailScreen(
            video = selectedVideo,
            onBack = { selectedVideoId = -1L }
        )
    } else if (selectedImage != null) {
        BackHandler { selectedImageId = -1L }
        ImageDetailScreen(
            image = selectedImage,
            onBack = { selectedImageId = -1L }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                val navItemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppWhite,
                    selectedTextColor = AppWhite,
                    unselectedIconColor = AppLightBrown,
                    unselectedTextColor = AppLightBrown,
                    indicatorColor = AppLightBrown.copy(alpha = 0.35f),
                )
                NavigationBar(containerColor = AppBrown) {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.Video,
                        onClick = { selectedTab = AppTab.Video },
                        icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Video") },
                        label = { Text("Video") },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.Image,
                        onClick = { selectedTab = AppTab.Image },
                        icon = { Icon(Icons.Default.Image, contentDescription = "Image") },
                        label = { Text("Image") },
                        colors = navItemColors,
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.Settings,
                        onClick = { selectedTab = AppTab.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Setting") },
                        colors = navItemColors,
                    )
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                AppTab.Video -> VideoScannerScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = videoUiState,
                    listState = videoListState,
                    onScanClick = {
                        videoPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
                    },
                    onToggleSelection = videoViewModel::toggleVideoSelection,
                    onVideoClick = { video -> selectedVideoId = video.mediaId },
                    onIgnoreGroup = videoViewModel::ignoreGroup,
                    onDeleteSelected = {
                        val uris = videoViewModel.getSelectedVideoUris()
                        if (uris.isEmpty()) return@VideoScannerScreen
                        val intentSender = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            uris
                        ).intentSender
                        videoDeleteLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                )
                AppTab.Image -> ImageScannerScreen(
                    modifier = Modifier.padding(innerPadding),
                    uiState = imageUiState,
                    listState = imageListState,
                    onFindSimilarClick = requestImageScan,
                    onToggleSelection = imageViewModel::toggleImageSelection,
                    onImageClick = { image -> selectedImageId = image.mediaId },
                    onIgnoreGroup = imageViewModel::ignoreGroup,
                    onDeleteSelected = {
                        val uris = imageViewModel.getSelectedImageUris()
                        if (uris.isEmpty()) return@ImageScannerScreen
                        val intentSender = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            uris
                        ).intentSender
                        imageDeleteLauncher.launch(
                            IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                )
                AppTab.Settings -> SettingsScreen(
                    modifier = Modifier.padding(innerPadding),
                    isVideoScanning = videoUiState.isScanning,
                    onRegenerateVideoClick = onRegenerateVideoClick,
                    videoFingerprintCount = settingsUiState.videoFingerprintCount,
                    videoIgnoredGroupCount = settingsUiState.videoIgnoredGroupCount,
                    onClearIgnoredVideoGroups = settingsViewModel::clearIgnoredVideoGroups,
                    isImageScanning = imageUiState.isScanning,
                    onRegenerateImageClick = onRegenerateImageClick,
                    imageFingerprintCount = settingsUiState.imageFingerprintCount,
                    imageIgnoredGroupCount = settingsUiState.imageIgnoredGroupCount,
                    onClearIgnoredImageGroups = settingsViewModel::clearIgnoredImageGroups
                )
            }
        }
    }
}
