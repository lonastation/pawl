package com.linn.pawl

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PawlTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NfcCardListScreen(viewModel = viewModel)
                }
            }
        }
    }
}