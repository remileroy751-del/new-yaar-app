package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.CertificationStatus
import com.yaarapp.app.data.maxProducts
import com.yaarapp.app.viewmodel.YaarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: YaarViewModel,
    onLoggedOut: () -> Unit,
    onCertifyShop: () -> Unit = {},
    onMyAds: () -> Unit = {}
) {
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val shop by viewModel.myShop.collectAsStateWithLifecycle()
    val activeAdCampaigns by viewModel.activeAdCampaigns.collectAsStateWithLifecycle()
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showFinalDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mon profil", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                user?.firstName ?: "",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                user?.let { "${it.country.labelWithFlag} · ${it.city}" } ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row1(icon = Icons.Filled.Storefront, label = "Numéro WhatsApp", value = user?.whatsappNumber ?: "")
                    if (shop != null) {
                        Row1(icon = Icons.Filled.Storefront, label = "Ma boutique", value = shop!!.name)
                        Row1(icon = Icons.Filled.Storefront, label = "Produits actifs autorisés", value = "${shop!!.maxProducts}")
                        Row1(
                            icon = Icons.Filled.VerifiedUser,
                            label = "Certification",
                            value = shop!!.certificationStatus.label
                        )
                    } else {
                        Text(
                            "Vous n'avez pas encore de boutique. Rendez-vous dans l'onglet \"Ma boutique\" pour en créer une.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row1(
                    icon = Icons.Filled.Notifications,
                    label = "Recevoir des notifications",
                    trailing = {
                        Switch(
                            checked = user?.notificationsEnabled ?: true,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                        )
                    }
                )
            }

            if (shop != null) {
                // Le bouton "Ma Publicité" ne s'affiche que tant qu'au moins une
                // campagne publicitaire est active ; il disparaît automatiquement à la fin.
                if (activeAdCampaigns.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onMyAds,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null)
                        Text(" Ma Publicité (${activeAdCampaigns.size} en cours)", modifier = Modifier.padding(start = 6.dp))
                    }
                }

                if (shop!!.certificationStatus == CertificationStatus.NONE) {
                    OutlinedButton(
                        onClick = onCertifyShop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null)
                        Text(" Certifié ma boutique", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }

            OutlinedButton(
                onClick = { password = ""; deleteError = null; showPasswordDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = null)
                Text(" Supprimer mon compte", modifier = Modifier.padding(start = 6.dp))
            }

            OutlinedButton(
                onClick = { viewModel.logout { onLoggedOut() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Text(" Se déconnecter", modifier = Modifier.padding(start = 6.dp))
            }
        }

        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { if (!deleting) showPasswordDialog = false },
                title = { Text("Supprimer mon compte") },
                text = {
                    Column {
                        Text("Saisissez le mot de passe de votre compte pour continuer.")
                        OutlinedTextField(
                            value = password,
                            onValueChange = { if (it.length <= 6 && it.all(Char::isLetterOrDigit)) password = it },
                            label = { Text("Mot de passe") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) } },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                        deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
                    }
                },
                confirmButton = {
                    Button(enabled = password.length == 6 && !deleting, onClick = { showPasswordDialog = false; showFinalDialog = true }) { Text("Continuer") }
                },
                dismissButton = { TextButton(enabled = !deleting, onClick = { showPasswordDialog = false }) { Text("Annuler") } }
            )
        }

        if (showFinalDialog) {
            AlertDialog(
                onDismissRequest = { if (!deleting) showFinalDialog = false },
                title = { Text("Confirmation définitive") },
                text = { Text("Votre compte sera supprimé avec toutes vos données.") },
                confirmButton = {
                    Button(enabled = !deleting, onClick = {
                        deleting = true
                        viewModel.deleteAccount(password,
                            onSuccess = { deleting = false; showFinalDialog = false; password = ""; onLoggedOut() },
                            onError = { deleting = false; showFinalDialog = false; deleteError = it; showPasswordDialog = true }
                        )
                    }) { Text(if (deleting) "Patientez…" else "Confirmer") }
                },
                dismissButton = { TextButton(enabled = !deleting, onClick = { showFinalDialog = false }) { Text("Annuler") } }
            )
        }
    }
}

@Composable
private fun Row1(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String = "",
    trailing: (@Composable () -> Unit)? = null
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        if (trailing != null) {
            trailing()
        } else {
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
