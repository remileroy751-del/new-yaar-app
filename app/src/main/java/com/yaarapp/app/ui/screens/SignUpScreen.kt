package com.yaarapp.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yaarapp.app.viewmodel.YaarViewModel

/**
 * Inscription volontairement minimale : pays et ville (déjà choisis à l'étape
 * précédente), prénom, et numéro WhatsApp — c'est tout, aucun mot de passe (voir la
 * note de sécurité dans data/User.kt).
 */
@Composable
fun SignUpScreen(
    viewModel: YaarViewModel,
    onSignedUp: () -> Unit,
    onGoToLogin: () -> Unit,
    onEditLocation: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var localWhatsapp by remember { mutableStateOf("") }
    val error by viewModel.authError.collectAsState()
    val country by viewModel.onboardingCountry.collectAsState()
    val city by viewModel.onboardingCity.collectAsState()
    val prefix = "00${country?.callingCode ?: ""}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Créer un compte",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${country?.labelWithFlag ?: ""} · ${city ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                TextButton(onClick = onEditLocation) { Text("Modifier") }
            }
        }

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Nom ou prénom") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        Text(
            "Numéro WhatsApp",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Indicatif du pays : fixe, non modifiable, dérivé automatiquement du pays
            // choisi à l'étape précédente.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(prefix, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            }
            OutlinedTextField(
                value = localWhatsapp,
                onValueChange = { localWhatsapp = it.filter { c -> c.isDigit() } },
                placeholder = { Text("90000000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Filled.Chat,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 2.dp, end = 6.dp)
            )
            Text(
                "De préférence, utilisez votre numéro WhatsApp : c'est celui-ci que les " +
                    "clients et vendeurs utiliseront pour ouvrir directement une discussion " +
                    "avec vous.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        if (error != null) {
            Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Button(
            onClick = {
                viewModel.signUp(
                    firstName = firstName.trim(),
                    localWhatsappNumber = localWhatsapp.trim()
                ) { onSignedUp() }
            },
            enabled = firstName.isNotBlank() && localWhatsapp.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Créer mon compte")
        }

        TextButton(
            onClick = {
                viewModel.clearAuthError()
                onGoToLogin()
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("J'ai déjà un compte, me connecter")
        }
    }
}
