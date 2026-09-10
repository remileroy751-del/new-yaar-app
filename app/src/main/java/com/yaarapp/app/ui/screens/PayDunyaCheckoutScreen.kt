@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yaarapp.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yaarapp.app.data.PayDunyaCreateRequest
import com.yaarapp.app.viewmodel.YaarViewModel
import kotlinx.coroutines.delay

@Composable
fun PayDunyaCheckoutScreen(
    viewModel: YaarViewModel,
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val payment = viewModel.pendingPayment.value
    var checkoutUrl by remember { mutableStateOf<String?>(null) }
    var paymentId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var completed by remember { mutableStateOf(false) }

    LaunchedEffect(payment) {
        if (payment == null) { loading = false; return@LaunchedEffect }
        runCatching {
            viewModel.createPayDunyaPayment()
        }.onSuccess { result ->
            paymentId = result.first
            checkoutUrl = result.second
            loading = false
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(result.second)))
        }.onFailure {
            error = it.message ?: "Impossible de préparer le paiement."
            loading = false
        }
    }

    LaunchedEffect(paymentId, completed) {
        val id = paymentId ?: return@LaunchedEffect
        while (!completed) {
            delay(5000)
            viewModel.checkPayDunyaPayment(id) { status ->
                if (status == "COMPLETED") { completed = true; onSuccess() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paiement sécurisé") },
                navigationIcon = { androidx.compose.material3.IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Paiement PayDunya", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            payment?.let { Text("${it.amountFcfa} FCFA", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
            Text("Vous pouvez reprendre le paiement dans votre navigateur, puis revenir ici pour vérifier automatiquement la confirmation.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
            if (loading) CircularProgressIndicator(Modifier.padding(top = 20.dp))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
            checkoutUrl?.let { url ->
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                ) { Text("Ouvrir PayDunya") }
            }
            if (paymentId != null && !completed) {
                Button(
                    onClick = {
                        checking = true
                        viewModel.checkPayDunyaPayment(paymentId!!) { status -> checking = false; if (status == "COMPLETED") onSuccess() }
                    },
                    enabled = !checking,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) { Text(if (checking) "Vérification…" else "J'ai terminé le paiement — vérifier") }
            }
            Text("Le paiement des forfaits et campagnes est désormais géré par PayDunya.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
