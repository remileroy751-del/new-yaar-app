package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.Product
import com.yaarapp.app.ui.components.ProductCard
import com.yaarapp.app.viewmodel.YaarViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: YaarViewModel,
    onBack: () -> Unit,
    onProductClick: (Product) -> Unit
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sameCity by viewModel.searchResultsSameCity.collectAsStateWithLifecycle()
    val otherCitiesResults by viewModel.searchResultsOtherCities.collectAsStateWithLifecycle()
    val otherCitiesList by viewModel.otherCitiesInCountry.collectAsStateWithLifecycle()
    val selectedOtherCities by viewModel.selectedOtherCities.collectAsStateWithLifecycle()
    val showOtherCitiesPicker by viewModel.showOtherCitiesPicker.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Rechercher un produit...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (query.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Tapez le nom d'un produit pour lancer la recherche.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Scaffold
        }

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
                Text(
                    if (user != null) "Résultats à ${user!!.city}" else "Résultats",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (sameCity.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Aucun résultat dans votre ville pour le moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(sameCity, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { onProductClick(product) })
                }
            }

            item(span = { GridItemSpan(2) }) {
                TextButton(onClick = { viewModel.toggleOtherCitiesPicker() }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (showOtherCitiesPicker) "Masquer les autres villes"
                        else "Afficher les produits disponibles dans d'autres villes"
                    )
                }
            }

            if (showOtherCitiesPicker) {
                item(span = { GridItemSpan(2) }) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        otherCitiesList.forEach { city ->
                            FilterChip(
                                selected = city in selectedOtherCities,
                                onClick = { viewModel.toggleOtherCity(city) },
                                label = { Text(city) }
                            )
                        }
                    }
                }

                if (selectedOtherCities.isNotEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            "Résultats dans les autres villes sélectionnées",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    if (otherCitiesResults.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                "Aucun résultat dans ces villes.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(otherCitiesResults, key = { it.id }) { product ->
                            ProductCard(product = product, onClick = { onProductClick(product) })
                        }
                    }
                }
            }
        }
    }
}
