package com.linn.pawl

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.linn.pawl.ui.PawlApp
import com.linn.pawl.ui.image.ImageScannerViewModel
import com.linn.pawl.ui.settings.SettingsViewModel
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.video.VideoScannerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val videoViewModel: VideoScannerViewModel by viewModels()
    private val imageViewModel: ImageScannerViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            PawlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PawlApp(
                        videoViewModel = videoViewModel,
                        imageViewModel = imageViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
