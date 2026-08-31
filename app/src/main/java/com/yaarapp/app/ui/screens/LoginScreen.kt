package com.yaarapp.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.yaarapp.app.R
import com.yaarapp.app.viewmodel.YaarViewModel

@Composable
fun LoginScreen(viewModel: YaarViewModel, onLoggedIn: () -> Unit, onGoToSignUp: () -> Unit) {
    var whatsapp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val error by viewModel.authError.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painter = painterResource(id = R.drawable.logo_icon), contentDescription = "Yaar-App", modifier = Modifier.size(96.dp))
        Text("Connexion", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 20.dp))
        OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it.filter(Char::isDigit) }, label = { Text("Numéro WhatsApp (ex : 0022890000000)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password, onValueChange = { if (it.length <= 6 && it.all(Char::isLetterOrDigit)) password = it }, label = { Text("Mot de passe") }, singleLine = true, visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = if (visible) "Cacher" else "Afficher") } }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
        if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = { viewModel.login(whatsapp.trim(), password) { onLoggedIn() } }, enabled = whatsapp.isNotBlank() && password.length == 6, modifier = Modifier.fillMaxWidth().padding(top = 20.dp), shape = RoundedCornerShape(14.dp)) { Text("Se connecter") }
        TextButton(onClick = { viewModel.clearAuthError(); onGoToSignUp() }, modifier = Modifier.padding(top = 8.dp)) { Text("Pas encore de compte ? Créer un compte") }
    }
}
