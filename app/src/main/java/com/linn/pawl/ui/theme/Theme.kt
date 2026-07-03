package com.linn.pawl.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = AppBlack,
    onPrimary = AppWhite,
    secondary = AppTextSecondary,
    onSecondary = AppWhite,
    background = AppWhite,
    surface = AppWhite,
    onBackground = AppBlack,
    onSurface = AppBlack,
    onSurfaceVariant = AppTextSecondary,
)

@Composable
fun PawlTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}