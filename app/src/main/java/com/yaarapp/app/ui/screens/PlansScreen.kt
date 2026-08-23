package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yaarapp.app.data.ShopLimits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    alreadyUpgraded: Boolean,
    onBack: () -> Unit,
    onSubscribe: () -> Unit = {}
) {
    val newTotal = ShopLimits.FREE_PRODUCTS + ShopLimits.EXTRA_PACK_PRODUCTS

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publier plus de produits") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)) {
            Text(
                "Le forfait gratuit permet de publier jusqu'à ${ShopLimits.FREE_PRODUCTS} produits actifs à la fois.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)) {
                    Text(
                        "${ShopLimits.FREE_PRODUCTS} → $newTotal produits actifs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${ShopLimits.EXTRA_PACK_PRICE_FCFA} FCFA — paiement unique",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    FeatureRow("Ajoutez ${ShopLimits.EXTRA_PACK_PRODUCTS} produits actifs supplémentaires")
                    FeatureRow("Aucun engagement ni abonnement — un seul paiement")
                    FeatureRow("Capacité conservée définitivement")

                    if (alreadyUpgraded) {
                        Row(
                            modifier = Modifier.padding(top = 16.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                " Vous avez déjà débloqué cette capacité.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = onSubscribe,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Souscrire — ${ShopLimits.EXTRA_PACK_PRICE_FCFA} FCFA")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(text: String) {
    Row(
        modifier = Modifier.padding(top = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
