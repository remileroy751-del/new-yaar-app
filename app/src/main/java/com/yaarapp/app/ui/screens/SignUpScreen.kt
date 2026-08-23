package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.yaarapp.app.data.Sex
import com.yaarapp.app.viewmodel.YaarViewModel

@Composable
fun SignUpScreen(
    viewModel: YaarViewModel,
    onSignedUp: () -> Unit,
    onGoToLogin: () -> Unit,
    onEditLocation: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf<Sex?>(null) }
    var localWhatsapp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val error by viewModel.authError.collectAsState()
    val country by viewModel.onboardingCountry.collectAsState()
    val city by viewModel.onboardingCity.collectAsState()

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
            label = { Text("Prénom") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        Text(
            "Sexe",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 6.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = sex == Sex.F,
                onClick = { sex = Sex.F },
                label = { Text("Féminin") }
            )
            FilterChip(
                selected = sex == Sex.M,
                onClick = { sex = Sex.M },
                label = { Text("Masculin") }
            )
        }

        OutlinedTextField(
            value = localWhatsapp,
            onValueChange = { localWhatsapp = it.filter { c -> c.isDigit() } },
            label = { Text("Numéro WhatsApp") },
            leadingIcon = { Text("  00${country?.callingCode ?: ""}  ", style = MaterialTheme.typography.bodyMedium) },
            placeholder = { Text("90000000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            supportingText = { Text("Sera enregistré comme 00${country?.callingCode ?: ""}$localWhatsapp") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mot de passe") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        if (error != null) {
            Text(
                error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                val chosenSex = sex ?: return@Button
                viewModel.signUp(
                    firstName = firstName.trim(),
                    sex = chosenSex,
                    localWhatsappNumber = localWhatsapp.trim(),
                    password = password
                ) { onSignedUp() }
            },
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
