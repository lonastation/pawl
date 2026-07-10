package com.linn.pawl.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linn.pawl.ui.theme.AppRed
import com.linn.pawl.ui.theme.PawlTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isVideoScanning: Boolean,
    onRegenerateVideoClick: () -> Unit,
    videoFingerprintCount: Int,
    isImageScanning: Boolean,
    onRegenerateImageClick: () -> Unit,
    imageFingerprintCount: Int,
    modifier: Modifier = Modifier
) {
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
        val regenerateButtonColors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppRed,
            disabledContentColor = AppRed.copy(alpha = 0.38f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Video",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cached fingerprints: $videoFingerprintCount",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Clear cached fingerprints and rescan all videos.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRegenerateVideoClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isVideoScanning,
                colors = regenerateButtonColors,
                border = BorderStroke(
                    1.dp,
                    if (!isVideoScanning) AppRed else AppRed.copy(alpha = 0.38f)
                ),
            ) {
                Text("Regenerate Video Fingerprints", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Image",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Cached fingerprints: $imageFingerprintCount",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Clear cached fingerprints and rescan all images.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRegenerateImageClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = !isImageScanning,
                colors = regenerateButtonColors,
                border = BorderStroke(
                    1.dp,
                    if (!isImageScanning) AppRed else AppRed.copy(alpha = 0.38f)
                ),
            ) {
                Text("Regenerate Image Fingerprints", fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun SettingsScreenPreview() {
    PawlTheme {
        SettingsScreen(
            isVideoScanning = false,
            onRegenerateVideoClick = {},
            videoFingerprintCount = 42,
            isImageScanning = false,
            onRegenerateImageClick = {},
            imageFingerprintCount = 128
        )
    }
}

@Preview(showBackground = true, heightDp = 800, name = "Scanning")
@Composable
private fun SettingsScreenScanningPreview() {
    PawlTheme {
        SettingsScreen(
            isVideoScanning = true,
            onRegenerateVideoClick = {},
            videoFingerprintCount = 42,
            isImageScanning = true,
            onRegenerateImageClick = {},
            imageFingerprintCount = 128
        )
    }
}
