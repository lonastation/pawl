package com.linn.pawl

import NfcPermissionHandler
import NfcStatus
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.linn.pawl.ui.AppViewModelProvider
import com.linn.pawl.ui.screens.NfcCardListScreen
import com.linn.pawl.ui.theme.PawlTheme
import com.linn.pawl.ui.viewmodels.NfcCardViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: NfcCardViewModel by viewModels {
        AppViewModelProvider.Factory
    }

    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }
    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for default card and set appropriate scanning mode
        lifecycleScope.launch {
            viewModel.hasDefaultCard.collect { hasCard ->
                if (!hasCard) {
                    // No default card found, start scanning for new card
                    viewModel.startReadingNewCard()
                }
            }
        }

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
        (application as PawlApplication).currentActivity = this
        checkNfcState()
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            null,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        (application as PawlApplication).currentActivity = null
        nfcAdapter?.disableForegroundDispatch(this)
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
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            // For Android 13 (API 33) and above
            val tag =
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)

            if (viewModel.isReadingNewCard.value) {
                viewModel.handleNewNfcTag(tag)
            } else {
                viewModel.handleNfcTag(tag)
            }
        }
    }
}