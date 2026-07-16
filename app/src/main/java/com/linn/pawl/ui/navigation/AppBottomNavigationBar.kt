package com.linn.pawl.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.linn.pawl.ui.theme.AppBrown
import com.linn.pawl.ui.theme.AppLightBrown
import com.linn.pawl.ui.theme.AppWhite

@Composable
fun AppBottomNavigationBar(
    selectedTab: AppTab,
    onVideoTabClick: () -> Unit,
    onImageTabClick: () -> Unit,
) {
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
            onClick = onVideoTabClick,
            icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Video") },
            label = { Text("Video") },
            colors = navItemColors,
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.Image,
            onClick = onImageTabClick,
            icon = { Icon(Icons.Default.Image, contentDescription = "Image") },
            label = { Text("Image") },
            colors = navItemColors,
        )
    }
}
