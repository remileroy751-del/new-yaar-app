package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.AdPricing
import com.yaarapp.app.viewmodel.YaarViewModel
import kotlin.math.roundToInt

/**
 * Étape 2 de "Promouvoir mes produits" : le vendeur règle deux curseurs — le nombre
 * d'expositions souhaité et le nombre de jours de la campagne — et le prix se calcule
 * automatiquement (20 FCFA par exposition), avant l'ouverture du paiement Kkiapay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureAdCampaignScreen(
    viewModel: YaarViewModel,
    onBack: () -> Unit,
    onConfirmed: () -> Unit
) {
    val product by viewModel.productToPromote.collectAsStateWithLifecycle()

    var expositions by remember { mutableFloatStateOf(100f) }
    var days by remember { mutableFloatStateOf(10f) }

    val expositionsInt = expositions.roundToInt()
    val daysInt = days.roundToInt()
    val price = AdPricing.priceFor(expositionsInt)
    val perDay = AdPricing.expositionsPerDay(expositionsInt, daysInt)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurer la campagne") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            product?.let {
                Text("Produit : ${it.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                "Votre produit apparaîtra en tête des résultats \"Acheter\" à chaque ouverture de l'application par un acheteur, jusqu'à épuisement des expositions ou fin de la période choisie.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )

            Text(
                "Nombre d'expositions : $expositionsInt",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = expositions,
                onValueChange = { expositions = it },
                valueRange = AdPricing.MIN_EXPOSITIONS.toFloat()..AdPricing.MAX_EXPOSITIONS.toFloat(),
                steps = (AdPricing.MAX_EXPOSITIONS - AdPricing.MIN_EXPOSITIONS) / AdPricing.EXPOSITION_STEP - 1
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${AdPricing.MIN_EXPOSITIONS}", style = MaterialTheme.typography.labelSmall)
                Text("${AdPricing.MAX_EXPOSITIONS}", style = MaterialTheme.typography.labelSmall)
            }

            Text(
                "Durée de la campagne : $daysInt jour(s)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp)
            )
            Slider(
                value = days,
                onValueChange = { days = it },
                valueRange = AdPricing.MIN_DAYS.toFloat()..AdPricing.MAX_DAYS.toFloat(),
                steps = AdPricing.MAX_DAYS - AdPricing.MIN_DAYS - 1
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${AdPricing.MIN_DAYS} jours", style = MaterialTheme.typography.labelSmall)
                Text("${AdPricing.MAX_DAYS} jours", style = MaterialTheme.typography.labelSmall)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "$price FCFA",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "$expositionsInt expositions × ${AdPricing.PRICE_PER_EXPOSITION_FCFA} FCFA — soit environ $perDay expositions/jour sur $daysInt jours",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Button(
                onClick = {
                    viewModel.requestAdCampaign(expositionsInt, daysInt)
                    onConfirmed()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuer — $price FCFA")
            }
        }
    }
}
