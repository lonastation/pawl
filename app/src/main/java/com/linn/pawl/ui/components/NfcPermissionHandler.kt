import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linn.pawl.ui.viewmodels.NfcCardViewModel

@Composable
fun NfcPermissionHandler(
    viewModel: NfcCardViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val nfcStatus by viewModel.nfcStatus.collectAsState()

    when (nfcStatus) {
        NfcStatus.NOT_SUPPORTED -> {
            ErrorScreen(
                title = "NFC Not Supported",
                message = "This device does not support NFC functionality."
            )
        }

        NfcStatus.DISABLED -> {
            NfcDisabledPrompt(
                onEnableClick = {
                    context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                }
            )
        }

        NfcStatus.ENABLED -> {
            content()
        }
    }
}

@Composable
fun NfcDisabledPrompt(
    onEnableClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "NFC is Disabled",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please enable NFC to use this app",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onEnableClick,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text("Enable NFC")
        }
    }
}

@Composable
fun ErrorScreen(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
} 