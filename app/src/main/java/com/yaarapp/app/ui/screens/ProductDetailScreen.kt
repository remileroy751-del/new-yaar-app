package com.yaarapp.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yaarapp.app.data.Product
import com.yaarapp.app.data.Shop
import com.yaarapp.app.util.ImageStorage
import com.yaarapp.app.util.WhatsAppHelper
import com.yaarapp.app.viewmodel.YaarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    viewModel: YaarViewModel,
    onBack: () -> Unit,
    onViewShop: (Int) -> Unit = {},
    onChatSupplier: (Product, Shop) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var product by remember { mutableStateOf<Product?>(null) }
    var shop by remember { mutableStateOf<Shop?>(null) }
    var interestSent by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        val p = viewModel.getProduct(productId)
        product = p
        shop = p?.let { viewModel.getShop(it.shopId) }
        interestSent = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") } }
            )
        }
    ) { padding ->
        val p = product
        if (p == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) { Text("Chargement...", modifier = Modifier.padding(24.dp)) }
            return@Scaffold
        }

        val isOwnListing = currentUser?.firebaseUid != null && currentUser?.firebaseUid == p.ownerUid
        val isImmo = p.listingType == "IMMO_SALE" || p.listingType == "IMMO_RENT"

        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            AsyncImage(
                model = ImageStorage.resolveImageModel(context, p.imageUrl),
                contentDescription = p.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
            p.secondImageUrl?.takeIf { it.isNotBlank() }?.let { second ->
                AsyncImage(
                    model = ImageStorage.resolveImageModel(context, second),
                    contentDescription = "Deuxième photo",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 8.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Text(p.category, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                Text(p.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                Text(
                    if (!isImmo) "${p.price.toLong()} FCFA" else if (p.listingType == "IMMO_RENT") "Location" else "Vente",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                shop?.let {
                    Text("Publié par ${it.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.padding(top = 4.dp))
                }
                Text("Disponible à ${p.city}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                OutlinedButton(onClick = { onViewShop(p.shopId) }, modifier = Modifier.padding(top = 10.dp), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(" Voir la boutique", modifier = Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelMedium)
                }

                Text(p.description, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))

                if (isOwnListing) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(
                            if (isImmo) "Vous ne pouvez pas acheter ou louer votre propre bien." else "Vous ne pouvez pas acheter votre propre produit.",
                            modifier = Modifier.padding(14.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val discussionsEnabled = p.internalDiscussionEnabled || p.whatsappDiscussionEnabled
                if (!isOwnListing && discussionsEnabled) {
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (p.internalDiscussionEnabled) {
                            Button(onClick = { shop?.let { onChatSupplier(p, it) } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Discuter ici") }
                        }
                        if (p.whatsappDiscussionEnabled) {
                            OutlinedButton(onClick = { shop?.let { WhatsAppHelper.discussProduct(context, p, it) } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Discuter sur WhatsApp") }
                        }
                    }
                } else if (!isOwnListing && !discussionsEnabled) {
                    Text("Le vendeur a désactivé les discussions pour ce produit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), modifier = Modifier.padding(top = 16.dp))
                }

                if (!isImmo && !isOwnListing) {
                    OutlinedButton(
                        onClick = {
                            if (!interestSent) {
                                shop?.let { viewModel.expressInterest(p, it) }
                                interestSent = true
                                Toast.makeText(context, "Le vendeur a été averti de votre intérêt.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !interestSent,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.FavoriteBorder, contentDescription = null)
                        Text(if (interestSent) " Le vendeur a été averti" else " Je suis intéressé", modifier = Modifier.padding(start = 4.dp))
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { shop?.let { viewModel.addToCart(p, it) } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                            Text(" Ajouter au panier", modifier = Modifier.padding(start = 4.dp))
                        }
                        Button(
                            onClick = { shop?.let { WhatsAppHelper.orderProduct(context, p, it) } },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Filled.ShoppingCartCheckout, contentDescription = null)
                            Text(" Acheter", modifier = Modifier.padding(start = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
