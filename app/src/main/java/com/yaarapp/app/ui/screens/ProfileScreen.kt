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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                onClick = { viewModel.logout { onLoggedOut() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null)
                Text(" Se déconnecter", modifier = Modifier.padding(start = 6.dp))
            }
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
