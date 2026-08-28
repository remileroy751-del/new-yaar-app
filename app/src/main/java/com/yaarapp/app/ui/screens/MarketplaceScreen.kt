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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.Product
import com.yaarapp.app.ui.components.CategoryChipsRow
import com.yaarapp.app.ui.components.ProductCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    viewModel: com.yaarapp.app.viewmodel.YaarViewModel,
    onProductClick: (Product) -> Unit,
    onCartClick: () -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val categories = viewModel.categories
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartItemCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yaar-App", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Filled.Search, contentDescription = "Rechercher", tint = Color.White)
                    }
                    IconButton(onClick = onCartClick) {
                        if (cartCount > 0) {
                            BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = "Panier", tint = Color.White)
                            }
                        } else {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = "Panier", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        // Les catégories (liste fixe) restent toujours visibles en haut de la page
        // d'accueil, même s'il n'y a encore aucun produit publié.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                CategoryChipsRow(
                    categories = categories,
                    selected = selectedCategory,
                    onSelect = { viewModel.selectCategory(it) },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (products.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            if (selectedCategory != null) "Aucun produit dans cette catégorie pour le moment."
                            else "Aucun produit en vente pour le moment."
                        )
                    }
                }
            } else {
                items(products, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onProductClick(product) })
                }
            }
        }
    }
}
