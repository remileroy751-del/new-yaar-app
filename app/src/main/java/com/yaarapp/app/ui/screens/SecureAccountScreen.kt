package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.yaarapp.app.firebase.FirebaseModule
import com.yaarapp.app.viewmodel.YaarViewModel

/** Écran affiché une seule fois aux comptes créés avec l'ancienne version anonyme. */
@Composable
fun SecureAccountScreen(viewModel: YaarViewModel, onDone: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var confirmationVisible by remember { mutableStateOf(false) }
    val error by viewModel.authError.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Sécurisez votre compte", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Bonjour ${user?.firstName ?: ""}. Définissez votre mot de passe pour conserver votre compte, votre boutique et vos produits même après un changement de téléphone.", modifier = Modifier.padding(top = 12.dp))
        Text("6 caractères : lettres et chiffres uniquement.", modifier = Modifier.fillMaxWidth().padding(top = 16.dp), color = MaterialTheme.colorScheme.primary)
        OutlinedTextField(value = password, onValueChange = { if (it.length <= 6 && it.all(Char::isLetterOrDigit)) password = it }, label = { Text("Nouveau mot de passe") }, singleLine = true, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        OutlinedTextField(value = confirmation, onValueChange = { if (it.length <= 6 && it.all(Char::isLetterOrDigit)) confirmation = it }, label = { Text("Confirmer") }, singleLine = true, visualTransformation = if (confirmationVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { confirmationVisible = !confirmationVisible }) { Icon(if (confirmationVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null) } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
        Button(onClick = { viewModel.secureLegacyAccount(password) { onDone() } }, enabled = password.length == 6 && password == confirmation && FirebaseModule.isValidPassword(password), modifier = Modifier.fillMaxWidth().padding(top = 20.dp), shape = RoundedCornerShape(14.dp)) { Text("Sécuriser mon compte") }
    }
}
