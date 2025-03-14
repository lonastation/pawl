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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
    val cards by viewModel.cards.collectAsState()
    val hasDefaultCard by viewModel.hasDefaultCard.collectAsState()
    val defaultCard by viewModel.defaultCard.collectAsState()
    val logs by viewModel.logs.collectAsState()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Cards section (top half)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Default card section
            if (!hasDefaultCard && cards.isNotEmpty()) {
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
                        Text(
                            text = "No Default Card Set",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Please select a default card from the list below",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            defaultCard?.let { card ->
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
                        Text(
                            text = "Default Card",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NfcCardContent(card = card)
                    }
                }
            }

            // Other cards
            cards.filter { it.id != defaultCard?.id }.forEach { card ->
                NfcCardItem(
                    card = card,
                    onSetDefault = { viewModel.setDefaultCard(card.id) }
                )
            }
        }

        // Logs section (bottom half)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Text(
                text = "NFC Tap Logs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(
                    items = logs,
                    key = { log -> log.id }  // Using the unique ID as the key
                ) { log ->
                    NfcLogItem(log = log)
                }
            }
        }
    }
}

@Composable
fun NfcCardItem(
    card: NfcCard,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    NfcCardContent(card = card)
                }
                TextButton(onClick = onSetDefault) {
                    Text("Set as Default")
                }
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
            text = card.id,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Type: ${card.type}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (card.description.isNotEmpty()) {
            Text(
                text = card.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "Last read: ${
                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(
                    Date(card.lastReadTime)
                )
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