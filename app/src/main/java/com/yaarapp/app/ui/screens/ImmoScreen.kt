package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.Product
import com.yaarapp.app.ui.components.ProductCard
import com.yaarapp.app.viewmodel.YaarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmoScreen(viewModel: YaarViewModel, onListingClick: (Product) -> Unit) {
    val listings by viewModel.immoListings.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    var saleSelected = remember { androidx.compose.runtime.mutableStateOf(true) }
    val filtered = listings
        .filter { user == null || it.country == user?.country }
        .filter { it.listingType == if (saleSelected.value) "IMMO_SALE" else "IMMO_RENT" }
        .sortedWith(compareByDescending<Product> { it.city == user?.city }.thenByDescending { it.createdAt })

    Scaffold(topBar = { TopAppBar(title = { Text("Immo") }) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = saleSelected.value, onClick = { saleSelected.value = true }, label = { Text("Vente") })
                        FilterChip(selected = !saleSelected.value, onClick = { saleSelected.value = false }, label = { Text("Location") })
                    }
                    Text(
                        if (user != null) "Annonces de votre pays, avec votre ville prioritaire" else "Annonces immobilières",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            if (filtered.isEmpty()) {
                item(span = { GridItemSpan(2) }) { Text("Aucune annonce disponible pour le moment.", modifier = Modifier.padding(top = 24.dp)) }
            } else {
                items(filtered, key = { it.id }) { listing ->
                    ProductCard(product = listing, own = listing.ownerUid != null && listing.ownerUid == user?.firebaseUid, onClick = { onListingClick(listing) })
                }
            }
        }
    }
}
