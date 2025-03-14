package com.linn.pawl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.linn.pawl.data.NfcLogEntity
import com.linn.pawl.model.NfcCard
import com.linn.pawl.ui.viewmodels.NfcCardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NfcCardListScreen(
    viewModel: NfcCardViewModel,
    modifier: Modifier = Modifier
) {
    val defaultCard by viewModel.defaultCard.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isReadingNewCard by viewModel.isReadingNewCard.collectAsState()

    if (isReadingNewCard) {
        ReadNewCardDialog(
            onDismiss = { viewModel.stopReadingNewCard() }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Default Card Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Default NFC Card",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(
                        onClick = { viewModel.startReadingNewCard() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Set new default card"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (defaultCard != null) {
                    NfcCardContent(card = defaultCard!!)
                } else {
                    Text(
                        text = "No default card selected",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // NFC Logs Section
        Text(
            text = "NFC Card Logs",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(logs) { log ->
                NfcLogItem(log = log)
            }
        }
    }
}

@Composable
fun NfcCardContent(
    card: NfcCard,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = card.type,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "ID: ${card.id}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (card.description.isNotEmpty()) {
            Text(
                text = card.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "Last used: ${
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    .format(Date(card.lastReadTime))
            }",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun NfcLogItem(
    log: NfcLogEntity,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Card: ${log.cardId}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Type: ${log.cardType}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
                    .format(Date(log.timestamp)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ReadNewCardDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Reading New Card")
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Hold a NFC card against your device to set it as default",
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 