package com.linn.pawl

import NfcPermissionHandler
import NfcStatus
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.linn.pawl.ui.AppViewModelProvider
import com.linn.pawl.ui.screens.NfcCardListScreen
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.viewmodels.NfcCardViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NfcCardViewModel by viewModels {
        AppViewModelProvider.Factory
    }

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            PawlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NfcPermissionHandler {
                        NfcCardListScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNfcState()
    }

    private fun checkNfcState() {
        when {
            nfcAdapter == null -> {
                // Device doesn't support NFC
                viewModel.updateNfcStatus(NfcStatus.NOT_SUPPORTED)
            }

            !nfcAdapter!!.isEnabled -> {
                // NFC is not enabled
                viewModel.updateNfcStatus(NfcStatus.DISABLED)
            }

            else -> {
                // NFC is available and enabled
                viewModel.updateNfcStatus(NfcStatus.ENABLED)
                nfcAdapter?.enableForegroundDispatch(
                    this,
                    getPendingIntent(),
                    null,
                    null
                )
            }
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }
}