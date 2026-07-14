package com.linn.pawl.ui

import android.Manifest
import android.app.Activity
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.linn.pawl.ui.image.ImageDetailScreen
import com.linn.pawl.ui.image.ImageScannerScreen
import com.linn.pawl.ui.image.ImageScannerViewModel
import com.linn.pawl.ui.navigation.AppTab
import com.linn.pawl.ui.recycle.RecyclingStationScreen
import com.linn.pawl.ui.recycle.RecyclingStationViewModel
import com.linn.pawl.ui.settings.SettingsScreen
import com.linn.pawl.ui.settings.SettingsViewModel
import com.linn.pawl.ui.theme.AppBrown
import com.linn.pawl.ui.theme.AppLightBrown
import com.linn.pawl.ui.theme.AppWhite
import com.linn.pawl.ui.video.VideoDetailScreen
import com.linn.pawl.ui.video.VideoScannerScreen
import com.linn.pawl.ui.video.VideoScannerViewModel
import com.linn.pawl.util.openManageAllFilesAccessSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PawlApp(
    videoViewModel: VideoScannerViewModel,
    imageViewModel: ImageScannerViewModel,
    settingsViewModel: SettingsViewModel,
    recyclingStationViewModel: RecyclingStationViewModel
) {
    val videoUiState by videoViewModel.uiState.collectAsStateWithLifecycle()
    val imageUiState by imageViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val recycleUiState by recyclingStationViewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            settingsViewModel.refreshAllFilesAccess()
            recyclingStationViewModel.refreshAllFilesAccess()
        }
    }

    var pendingVideoDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingImageDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

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

    val videoDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            recyclingStationViewModel.onMediaStoreDeleteConfirmed()
            videoViewModel.onVideosDeleted(pendingVideoDeleteIds)
        } else {
            recyclingStationViewModel.onMediaStoreDeleteCancelled()
        }
        pendingVideoDeleteIds = emptySet()
    }

    val imageDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            recyclingStationViewModel.onMediaStoreDeleteConfirmed()
            imageViewModel.onImagesDeleted(pendingImageDeleteIds)
        } else {
            recyclingStationViewModel.onMediaStoreDeleteCancelled()
        }
        pendingImageDeleteIds = emptySet()
    }

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Video) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var selectedVideoId by remember { mutableLongStateOf(-1L) }
    var selectedImageId by remember { mutableLongStateOf(-1L) }
    val videoListState = rememberLazyListState()
    val imageListState = rememberLazyListState()

    LaunchedEffect(showSettings, videoUiState.isScanning, imageUiState.isScanning) {
        if (showSettings && !videoUiState.isScanning && !imageUiState.isScanning) {
            settingsViewModel.refreshCounts()
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == AppTab.Recycle) {
            recyclingStationViewModel.load()
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

    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
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
    } else if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            onBack = { showSettings = false },
            isVideoScanning = videoUiState.isScanning,
            onRegenerateVideoClick = onRegenerateVideoClick,
            videoFingerprintCount = settingsUiState.videoFingerprintCount,
            videoIgnoredGroupCount = settingsUiState.videoIgnoredGroupCount,
            onClearIgnoredVideoGroups = settingsViewModel::clearIgnoredVideoGroups,
            isImageScanning = imageUiState.isScanning,
            onRegenerateImageClick = onRegenerateImageClick,
            imageFingerprintCount = settingsUiState.imageFingerprintCount,
            imageIgnoredGroupCount = settingsUiState.imageIgnoredGroupCount,
            onClearIgnoredImageGroups = settingsViewModel::clearIgnoredImageGroups,
            hasAllFilesAccess = settingsUiState.hasAllFilesAccess,
            onRequestAllFilesAccess = { openManageAllFilesAccessSettings(context) }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "VM-LIKE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NavigationDrawerItem(
                        label = { Text("Settings") },
                        selected = false,
                        onClick = {
                            scope.launch {
                                drawerState.close()
                                showSettings = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedIconColor = AppBrown,
                            unselectedTextColor = AppBrown,
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        ) {
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
                            selected = selectedTab == AppTab.Recycle,
                            onClick = { selectedTab = AppTab.Recycle },
                            icon = { Icon(Icons.Default.Recycling, contentDescription = "Recycle") },
                            label = { Text("Recycle") },
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
                            val videos = videoViewModel.getSelectedVideos()
                            if (videos.isEmpty()) return@VideoScannerScreen
                            scope.launch {
                                try {
                                    val staged = recyclingStationViewModel.stageVideosForRecycle(videos)
                                    if (staged.isEmpty()) return@launch
                                    pendingVideoDeleteIds = videos.map { it.mediaId }.toSet()
                                    val intentSender = MediaStore.createDeleteRequest(
                                        context.contentResolver,
                                        staged.map { it.contentUri }
                                    ).intentSender
                                    videoDeleteLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                } catch (_: Exception) {
                                    recyclingStationViewModel.onMediaStoreDeleteCancelled()
                                    pendingVideoDeleteIds = emptySet()
                                }
                            }
                        },
                        onOpenDrawer = openDrawer,
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
                            val images = imageViewModel.getSelectedImages()
                            if (images.isEmpty()) return@ImageScannerScreen
                            scope.launch {
                                try {
                                    val staged = recyclingStationViewModel.stageImagesForRecycle(images)
                                    if (staged.isEmpty()) return@launch
                                    pendingImageDeleteIds = images.map { it.mediaId }.toSet()
                                    val intentSender = MediaStore.createDeleteRequest(
                                        context.contentResolver,
                                        staged.map { it.contentUri }
                                    ).intentSender
                                    imageDeleteLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                } catch (_: Exception) {
                                    recyclingStationViewModel.onMediaStoreDeleteCancelled()
                                    pendingImageDeleteIds = emptySet()
                                }
                            }
                        },
                        onOpenDrawer = openDrawer,
                    )
                    AppTab.Recycle -> RecyclingStationScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = recycleUiState,
                        trashFilePath = recyclingStationViewModel::trashFilePath,
                        onFilterChange = recyclingStationViewModel::setFilter,
                        onToggleSelection = recyclingStationViewModel::toggleSelection,
                        onRestoreSelected = recyclingStationViewModel::restoreSelected,
                        onPermanentlyDeleteSelected = recyclingStationViewModel::permanentlyDeleteSelected,
                        onRequestAllFilesAccess = { openManageAllFilesAccessSettings(context) },
                        onOpenDrawer = openDrawer,
                    )
                }
            }
        }
    }
}
