package com.yaarapp.app.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.util.KkiapayHtmlBuilder
import com.yaarapp.app.viewmodel.YaarViewModel

/**
 * Écran de paiement Kkiapay : charge le widget officiel Kkiapay (cdn.kkiapay.me/k.js) dans
 * une WebView. Utilisé pour "Publier plus de produits" (changement de forfait) et pour
 * "Promouvoir mes produits" (mise en avant payante d'un produit) — le montant et la
 * description affichés dépendent de [YaarViewModel.pendingPayment].
 *
 * ⚠️ Nécessite d'avoir renseigné votre clé publique Kkiapay dans
 * `util/KkiapayConfig.kt` avant de fonctionner réellement (voir ce fichier).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KkiapayCheckoutScreen(
    viewModel: YaarViewModel,
    onCancel: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val payment by viewModel.pendingPayment.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(true) }

    val onSuccessState = rememberUpdatedState(onSuccess)
    val onCancelState = rememberUpdatedState(onCancel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paiement Kkiapay", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Annuler")
                    }
                }
            )
        }
    ) { padding ->
        val currentPayment = payment
        if (currentPayment == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Aucun paiement en cours.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(currentPayment.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${currentPayment.amountFcfa} FCFA",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Ouverture du paiement sécurisé...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(
                            KkiapayBridge(
                                onSuccess = {
                                    Toast.makeText(context, "Paiement réussi !", Toast.LENGTH_SHORT).show()
                                    onSuccessState.value()
                                },
                                onFailed = {
                                    Toast.makeText(context, "Paiement échoué. Réessayez.", Toast.LENGTH_SHORT).show()
                                },
                                onClosed = { onCancelState.value() }
                            ),
                            "Android"
                        )
                        loadDataWithBaseURL(
                            "https://kkiapay.me",
                            KkiapayHtmlBuilder.build(
                                amountFcfa = currentPayment.amountFcfa,
                                description = currentPayment.description,
                                whatsappForReceipt = user?.whatsappNumber.orEmpty()
                            ),
                            "text/html",
                            "UTF-8",
                            null
                        )
                        isLoading = false
                    }
                }
            )
        }
    }
}

/**
 * Pont JavaScript ↔ Kotlin pour recevoir les évènements du widget Kkiapay.
 * Les appels @JavascriptInterface arrivent sur un thread secondaire : on les fait
 * repasser sur le thread principal avant de toucher l'UI / la navigation Compose.
 */
private class KkiapayBridge(
    private val onSuccess: () -> Unit,
    private val onFailed: () -> Unit,
    private val onClosed: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("JavascriptInterface")
    @JavascriptInterface
    fun onPaymentSuccess(responseJson: String) {
        mainHandler.post { onSuccess() }
    }

    @JavascriptInterface
    fun onPaymentFailed(responseJson: String) {
        mainHandler.post { onFailed() }
    }

    @JavascriptInterface
    fun onWidgetClosed() {
        mainHandler.post { onClosed() }
    }
}
