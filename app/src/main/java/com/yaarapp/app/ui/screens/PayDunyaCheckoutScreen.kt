@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yaarapp.app.viewmodel.YaarViewModel

/**
 * Écran de transition de paiement.
 *
 * Les intégrations de paiement restent présentes dans le projet pour une activation
 * ultérieure, mais aucun paiement réel n'est lancé avant la date commerciale annoncée.
 */
@Composable
fun PayDunyaCheckoutScreen(
    viewModel: YaarViewModel,
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    val payment = viewModel.pendingPayment.value
    var showDisabledDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paiement") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Paiement sécurisé",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            payment?.let {
                Text(it.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
                Text("${it.amountFcfa} FCFA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            }
            Text(
                "Les paiements seront activés le 01/11/2026.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 18.dp)
            )
            Text(
                "Vous pouvez préparer votre forfait, mais aucun débit ne sera effectué avant l'activation officielle des paiements.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Button(
                onClick = { showDisabledDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
            ) { Text("Payer") }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) { Text("Annuler") }
        }
    }

    if (showDisabledDialog) {
        AlertDialog(
            onDismissRequest = { showDisabledDialog = false },
            title = { Text("Paiements momentanément indisponibles") },
            text = { Text("Les paiements seront activés le 01/11/2026") },
            confirmButton = { TextButton(onClick = { showDisabledDialog = false }) { Text("Compris") } }
        )
    }
}
