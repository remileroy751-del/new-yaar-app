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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yaarapp.app.viewmodel.YaarViewModel

/** Étapes 2 et 3 du parcours d'inscription : identité puis mot de passe. */
@Composable
fun SignUpScreen(
    viewModel: YaarViewModel,
    onSignedUp: () -> Unit,
    onGoToLogin: () -> Unit,
    onEditLocation: () -> Unit
) {
    var step by remember { mutableStateOf(2) }
    var firstName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var localWhatsapp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    var showRememberDialog by remember { mutableStateOf(false) }
    var pendingEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }
    val error by viewModel.authError.collectAsState()
    val country by viewModel.onboardingCountry.collectAsState()
    val city by viewModel.onboardingCity.collectAsState()
    val prefix = "00${country?.callingCode ?: ""}"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Créer un compte", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Étape $step sur 3", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${country?.labelWithFlag ?: ""} · ${city ?: ""}", fontWeight = FontWeight.Medium)
                TextButton(onClick = onEditLocation) { Text("Modifier") }
            }
        }

        if (step == 2) {
            Text(
                "Utilisez une adresse e-mail valide pour créer et sécuriser votre compte Yaar-App.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Adresse e-mail") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("Prénom") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            Text("Numéro WhatsApp", style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.fillMaxHeight().padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(prefix, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = localWhatsapp,
                    onValueChange = { localWhatsapp = it.filter(Char::isDigit) },
                    placeholder = { Text("90000000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 2.dp, end = 6.dp))
                Text("Votre numéro WhatsApp sert uniquement à permettre aux clients de vous contacter dans Yaar-App.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            Button(
                onClick = {
                    viewModel.clearAuthError()
                    if (firstName.isNotBlank() && viewModel.isValidEmail(email) && localWhatsapp.isNotBlank()) step = 3
                },
                enabled = firstName.isNotBlank() && viewModel.isValidEmail(email) && localWhatsapp.isNotBlank() && country != null && city != null,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Continuer") }
        } else {
            Text("Choisissez un mot de passe de 6 caractères. Lettres et chiffres sont autorisés.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { if (it.length <= 6 && it.all(Char::isLetterOrDigit)) password = it },
                label = { Text("Mot de passe (6 caractères)") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (passwordVisible) "Cacher" else "Afficher") } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
            OutlinedTextField(
                value = confirmation,
                onValueChange = { if (it.length <= 6 && it.all(Char::isLetterOrDigit)) confirmation = it },
                label = { Text("Confirmer le mot de passe") },
                singleLine = true,
                visualTransformation = if (confirmationVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { IconButton(onClick = { confirmationVisible = !confirmationVisible }) { Icon(if (confirmationVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (confirmationVisible) "Cacher" else "Afficher") } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            )
            if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            Button(
                onClick = {
                    if (password != confirmation) return@Button
                    viewModel.signUp(firstName.trim(), email.trim(), localWhatsapp.trim(), password) {
                        pendingEmail = email.trim()
                        pendingPassword = password
                        showRememberDialog = true
                    }
                },
                enabled = password.length == 6 && confirmation == password && password.matches(Regex("^[A-Za-z0-9]{6}$")),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Créer mon compte") }
            TextButton(onClick = { viewModel.clearAuthError(); step = 2 }) { Text("Retour") }
        }

        TextButton(onClick = { viewModel.clearAuthError(); onGoToLogin() }, modifier = Modifier.padding(top = 8.dp)) { Text("J'ai déjà un compte, me connecter") }
    }

    if (showRememberDialog) {
        AlertDialog(
            onDismissRequest = { /* L'utilisateur doit choisir Oui ou Non. */ },
            title = { Text("Se souvenir de mon mot de passe ?") },
            text = {
                Text(
                    "Votre adresse e-mail sera toujours mémorisée sur ce téléphone. Si vous choisissez Oui, votre mot de passe sera conservé de manière chiffrée pour éviter de le ressaisir à chaque connexion."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveLoginCredentials(pendingEmail, pendingPassword, true)
                        pendingPassword = ""
                        showRememberDialog = false
                        onSignedUp()
                    }
                ) { Text("Oui") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.saveLoginCredentials(pendingEmail, null, false)
                        pendingPassword = ""
                        showRememberDialog = false
                        onSignedUp()
                    }
                ) { Text("Non") }
            }
        )
    }
}
