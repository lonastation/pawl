package com.linn.pawl.ui

import android.Manifest
import android.app.Activity
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.linn.pawl.R
import com.linn.pawl.ui.image.ImageDetailScreen
import com.linn.pawl.ui.image.ImageScannerScreen
import com.linn.pawl.ui.image.ImageScannerViewModel
import com.linn.pawl.ui.navigation.AppBottomNavigationBar
import com.linn.pawl.ui.navigation.AppRoutes
import com.linn.pawl.ui.navigation.AppTab
import com.linn.pawl.ui.scanlog.ScanLogsScreen
import com.linn.pawl.ui.scanlog.ScanLogsViewModel
import com.linn.pawl.ui.settings.SettingsScreen
import com.linn.pawl.ui.settings.SettingsViewModel
import com.linn.pawl.ui.theme.AppBrown
import com.linn.pawl.ui.theme.AppWhite
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.trash.TrashScreen
import com.linn.pawl.ui.trash.TrashViewModel
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
    trashViewModel: TrashViewModel,
    scanLogsViewModel: ScanLogsViewModel,
) {
    val videoUiState by videoViewModel.uiState.collectAsStateWithLifecycle()
    val imageUiState by imageViewModel.uiState.collectAsStateWithLifecycle()
    val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val trashUiState by trashViewModel.uiState.collectAsStateWithLifecycle()
    val scanLogsUiState by scanLogsViewModel.uiState.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val navController = rememberNavController()

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            settingsViewModel.refreshAllFilesAccess()
            trashViewModel.refreshAllFilesAccess()
        }
    }

    var pendingVideoDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingImageDeleteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pendingImageScan by remember { mutableStateOf(false) }

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
            trashViewModel.onMediaStoreDeleteConfirmed()
            videoViewModel.onVideosDeleted(pendingVideoDeleteIds)
        } else {
            trashViewModel.onMediaStoreDeleteCancelled()
        }
        pendingVideoDeleteIds = emptySet()
    }

    val imageDeleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            trashViewModel.onMediaStoreDeleteConfirmed()
            imageViewModel.onImagesDeleted(pendingImageDeleteIds)
        } else {
            trashViewModel.onMediaStoreDeleteCancelled()
        }
        pendingImageDeleteIds = emptySet()
    }

    val videoListState = rememberLazyListState()
    val imageListState = rememberLazyListState()

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

    val navigateFromDrawer: (String) -> Unit = { route ->
        scope.launch {
            drawerState.close()
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.home(AppRoutes.TAB_VIDEO)
    ) {
        composable(
            route = AppRoutes.HOME,
            arguments = listOf(navArgument("tab") { type = NavType.StringType })
        ) { entry ->
            val selectedTab = AppRoutes.appTabFromRoute(entry.arguments?.getString("tab"))

            val drawerWidth = with(LocalDensity.current) {
                (LocalWindowInfo.current.containerSize.width * 0.78f).toDp()
            }.coerceAtMost(320.dp)
            val drawerContentBlur by animateDpAsState(
                targetValue = if (drawerState.targetValue == DrawerValue.Open) 10.dp else 0.dp,
                label = "drawerContentBlur",
            )
            ModalNavigationDrawer(
                drawerState = drawerState,
                scrimColor = Color.Black.copy(alpha = 0.35f),
                drawerContent = {
                    AppNavigationDrawerContent(
                        modifier = Modifier.width(drawerWidth),
                        recentlyDeletedSelected = false,
                        scanLogsSelected = false,
                        settingsSelected = false,
                        onRecentlyDeletedClick = { navigateFromDrawer(AppRoutes.TRASH) },
                        onScanLogsClick = { navigateFromDrawer(AppRoutes.SCAN_LOGS) },
                        onSettingsClick = { navigateFromDrawer(AppRoutes.SETTINGS) },
                    )
                }
            ) {
                Scaffold(
                    modifier = Modifier.blur(drawerContentBlur),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        AppBottomNavigationBar(
                            selectedTab = selectedTab,
                            onVideoTabClick = {
                                if (selectedTab != AppTab.Video) {
                                    navController.navigate(AppRoutes.home(AppRoutes.TAB_VIDEO)) {
                                        launchSingleTop = true
                                        popUpTo(AppRoutes.HOME) { inclusive = true }
                                    }
                                }
                            },
                            onImageTabClick = {
                                if (selectedTab != AppTab.Image) {
                                    navController.navigate(AppRoutes.home(AppRoutes.TAB_IMAGE)) {
                                        launchSingleTop = true
                                        popUpTo(AppRoutes.HOME) { inclusive = true }
                                    }
                                }
                            },
                        )
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        AppTab.Video -> VideoScannerScreen(
                            modifier = Modifier.padding(innerPadding),
                            uiState = videoUiState,
                            listState = videoListState,
                            onScanClick = {
                                videoPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                                )
                            },
                            onToggleSelection = videoViewModel::toggleVideoSelection,
                            onVideoClick = { video ->
                                navController.navigate(AppRoutes.videoDetail(video.mediaId))
                            },
                            onIgnoreGroup = videoViewModel::ignoreGroup,
                            onDeleteSelected = {
                                val videos = videoViewModel.getSelectedVideos()
                                if (videos.isEmpty()) return@VideoScannerScreen
                                scope.launch {
                                    try {
                                        val staged = trashViewModel.stageVideosForTrash(videos)
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
                                        trashViewModel.onMediaStoreDeleteCancelled()
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
                            onImageClick = { image ->
                                navController.navigate(AppRoutes.imageDetail(image.mediaId))
                            },
                            onIgnoreGroup = imageViewModel::ignoreGroup,
                            onDeleteSelected = {
                                val images = imageViewModel.getSelectedImages()
                                if (images.isEmpty()) return@ImageScannerScreen
                                scope.launch {
                                    try {
                                        val staged = trashViewModel.stageImagesForTrash(images)
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
                                        trashViewModel.onMediaStoreDeleteCancelled()
                                        pendingImageDeleteIds = emptySet()
                                    }
                                }
                            },
                            onOpenDrawer = openDrawer,
                        )
                    }
                }
            }
        }

        composable(AppRoutes.SETTINGS) {
            LaunchedEffect(videoUiState.isScanning, imageUiState.isScanning) {
                if (!videoUiState.isScanning && !imageUiState.isScanning) {
                    settingsViewModel.refreshCounts()
                }
            }
            SettingsScreen(
                onBack = { navController.popBackStack() },
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
        }

        composable(AppRoutes.TRASH) {
            LaunchedEffect(Unit) {
                trashViewModel.load()
            }
            TrashScreen(
                uiState = trashUiState,
                trashFilePath = trashViewModel::trashFilePath,
                onFilterChange = trashViewModel::setFilter,
                onToggleSelection = trashViewModel::toggleSelection,
                onRestoreSelected = trashViewModel::restoreSelected,
                onPermanentlyDeleteSelected = trashViewModel::permanentlyDeleteSelected,
                onPermanentlyDeleteAll = trashViewModel::permanentlyDeleteAll,
                onRequestAllFilesAccess = { openManageAllFilesAccessSettings(context) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(AppRoutes.SCAN_LOGS) {
            ScanLogsScreen(
                uiState = scanLogsUiState,
                onBack = { navController.popBackStack() },
                onFilterChange = scanLogsViewModel::setFilter,
                onToggleExpanded = scanLogsViewModel::toggleExpanded,
                onClearLogs = scanLogsViewModel::clearLogs,
            )
        }

        composable(
            route = AppRoutes.VIDEO_DETAIL,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { entry ->
            val mediaId = entry.arguments?.getLong("mediaId") ?: -1L
            val video = videoUiState.duplicateGroups
                .flatMap { it.videos }
                .find { it.mediaId == mediaId }
            if (video == null) {
                LaunchedEffect(mediaId) { navController.popBackStack() }
            } else {
                VideoDetailScreen(
                    video = video,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = AppRoutes.IMAGE_DETAIL,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType })
        ) { entry ->
            val mediaId = entry.arguments?.getLong("mediaId") ?: -1L
            val image = imageUiState.duplicateGroups
                .flatMap { it.images }
                .find { it.mediaId == mediaId }
            if (image == null) {
                LaunchedEffect(mediaId) { navController.popBackStack() }
            } else {
                ImageDetailScreen(
                    image = image,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun AppNavigationDrawerContent(
    recentlyDeletedSelected: Boolean,
    scanLogsSelected: Boolean,
    settingsSelected: Boolean,
    onRecentlyDeletedClick: () -> Unit,
    onScanLogsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = AppBrown,
        drawerTonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppWhite)
                    .padding(top = 32.dp, bottom = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.vm_pic),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.brand_name),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppBrown,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(AppBrown)
                    .padding(vertical = 16.dp),
            ) {
                AppNavigationDrawerItem(
                    label = stringResource(R.string.nav_recently_deleted),
                    icon = Icons.Default.CatchingPokemon,
                    selected = recentlyDeletedSelected,
                    onClick = onRecentlyDeletedClick,
                )
                AppNavigationDrawerItem(
                    label = stringResource(R.string.nav_scan_logs),
                    icon = Icons.Default.Description,
                    selected = scanLogsSelected,
                    onClick = onScanLogsClick,
                )
                AppNavigationDrawerItem(
                    label = stringResource(R.string.nav_settings),
                    icon = Icons.Default.Settings,
                    selected = settingsSelected,
                    onClick = onSettingsClick,
                )
            }
        }
    }
}

@Composable
private fun AppNavigationDrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) AppBrown else AppWhite
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) AppWhite else AppBrown)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 28.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 16.sp,
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 600)
@Composable
private fun AppNavigationDrawerContentPreview() {
    PawlTheme {
        AppNavigationDrawerContent(
            recentlyDeletedSelected = false,
            scanLogsSelected = false,
            settingsSelected = false,
            onRecentlyDeletedClick = {},
            onScanLogsClick = {},
            onSettingsClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 600, name = "Recently deleted selected")
@Composable
private fun AppNavigationDrawerContentSelectedPreview() {
    PawlTheme {
        AppNavigationDrawerContent(
            recentlyDeletedSelected = true,
            scanLogsSelected = false,
            settingsSelected = false,
            onRecentlyDeletedClick = {},
            onScanLogsClick = {},
            onSettingsClick = {},
        )
    }
}
