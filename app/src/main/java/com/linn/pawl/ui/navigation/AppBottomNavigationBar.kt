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
import androidx.compose.ui.res.stringResource
import com.linn.pawl.R
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
            icon = {
                Icon(
                    Icons.Default.VideoLibrary,
                    contentDescription = stringResource(R.string.nav_video)
                )
            },
            label = { Text(stringResource(R.string.nav_video)) },
            colors = navItemColors,
        )
        NavigationBarItem(
            selected = selectedTab == AppTab.Image,
            onClick = onImageTabClick,
            icon = {
                Icon(
                    Icons.Default.Image,
                    contentDescription = stringResource(R.string.nav_image)
                )
            },
            label = { Text(stringResource(R.string.nav_image)) },
            colors = navItemColors,
        )
    }
}
