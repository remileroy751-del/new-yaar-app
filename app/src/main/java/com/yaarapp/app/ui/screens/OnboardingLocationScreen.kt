package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yaarapp.app.data.Country
import com.yaarapp.app.R
import com.yaarapp.app.viewmodel.YaarViewModel

/**
 * Premier écran vu par un nouvel utilisateur : choix du pays (avec drapeau), puis
 * choix de la ville/région dans la liste alphabétique qui s'actualise automatiquement
 * selon le pays sélectionné. Le bouton "Continuer" mène ensuite au formulaire
 * d'inscription (prénom, sexe, numéro WhatsApp, mot de passe).
 */
@Composable
fun OnboardingLocationScreen(
    viewModel: YaarViewModel,
    onContinue: () -> Unit
) {
    val country by viewModel.onboardingCountry.collectAsState()
    val city by viewModel.onboardingCity.collectAsState()
    val cities by viewModel.onboardingCities.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Bienvenue sur Yaar-App",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Pour commencer, sélectionnez votre pays puis votre ville.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionLabel(icon = Icons.Filled.Public, text = "Pays")
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(Country.values().toList()) { c ->
                SelectableCountryRow(
                    country = c,
                    selected = c == country,
                    onClick = { viewModel.selectOnboardingCountry(c) }
                )
            }

            if (country != null) {
                item {
                    Column(modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) {
                        SectionLabel(icon = Icons.Filled.LocationCity, text = "Ville / Région")
                    }
                }
                items(cities) { c ->
                    SelectableRow(
                        label = c,
                        selected = c == city,
                        onClick = { viewModel.selectOnboardingCity(c) }
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            Button(
                onClick = onContinue,
                enabled = country != null && city != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Continuer")
            }
        }
    }
}

@Composable
private fun SectionLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 6.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
        onClick = onClick
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SelectableCountryRow(country: Country, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 0.dp else 1.dp),
        onClick = onClick
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (country == Country.TOGO) {
                    Image(
                        painter = painterResource(id = R.drawable.flag_togo),
                        contentDescription = "Drapeau du Togo",
                        modifier = Modifier.size(32.dp, 22.dp).padding(end = 8.dp)
                    )
                } else {
                    Text(country.flagEmoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 8.dp))
                }
                Text(country.displayName, style = MaterialTheme.typography.bodyLarge)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
