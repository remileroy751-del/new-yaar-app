package com.yaarapp.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yaarapp.app.data.CityRepository
import com.yaarapp.app.data.ProductCategories
import com.yaarapp.app.util.ImageStorage
import com.yaarapp.app.viewmodel.YaarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(viewModel: YaarViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    var pickedImageUri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var selectedCities by remember { mutableStateOf<Set<String>>(emptySet()) }
    val error by viewModel.addProductError.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val cities = user?.let { CityRepository.citiesFor(it.country) }.orEmpty()

    LaunchedEffect(user?.city) {
        user?.city?.let { selectedCities = setOf(it) }
    }

    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) pickedImageUri = uri }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Ajouter un produit") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour") } })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentAlignment = Alignment.Center) {
                if (pickedImageUri != null) AsyncImage(model = pickedImageUri, contentDescription = "Photo du produit", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary); Text("Ajouter une photo (format 1:1 conseillé)", modifier = Modifier.padding(top = 8.dp)) }
            }
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nom du produit") }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedTextField(value = category ?: "", onValueChange = {}, readOnly = true, label = { Text("Catégorie") }, placeholder = { Text("Sélectionner une catégorie") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor())
                DropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) { ProductCategories.all.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryMenuExpanded = false }) } }
            }
            OutlinedTextField(value = price, onValueChange = { price = it.filter(Char::isDigit) }, label = { Text("Prix (FCFA)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))

            Text("Visibilité du produit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
            Text("Votre ville est sélectionnée automatiquement. Vous pouvez ajouter gratuitement jusqu'à 5 autres villes du même pays.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            user?.city?.let { city -> Text("Ville principale : $city", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp)) }

            if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { showCityPicker = true }, enabled = pickedImageUri != null && category != null && name.isNotBlank() && description.isNotBlank() && price.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 20.dp), shape = RoundedCornerShape(14.dp)) { Text("Publier le produit") }
        }
    }

    if (showCityPicker) {
        AlertDialog(
            onDismissRequest = { showCityPicker = false },
            title = { Text("Où votre produit doit-il être visible ?") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Sélectionnez jusqu'à 5 villes supplémentaires. Votre ville est toujours incluse.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
                    cities.forEach { city ->
                        val selected = city in selectedCities
                        val isHome = city == user?.city
                        Row(modifier = Modifier.fillMaxWidth().clickable(enabled = !isHome) { if (selected) selectedCities = selectedCities - city else if (selectedCities.size < 6) selectedCities = selectedCities + city }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selected, onCheckedChange = { checked ->
                                if (!isHome) {
                                    if (checked && selectedCities.size < 6) selectedCities = selectedCities + city
                                    else if (!checked) selectedCities = selectedCities - city
                                }
                            }, enabled = isHome || selected || selectedCities.size < 6)
                            Text(city)
                            if (isHome) Text("  (ma ville)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pickedImageUri ?: return@TextButton
                    val selectedCategory = category ?: return@TextButton
                    val savedPath = ImageStorage.saveToInternalStorage(context, uri) ?: return@TextButton
                    viewModel.addProduct(name.trim(), description.trim(), price.toDoubleOrNull() ?: 0.0, savedPath, selectedCategory, selectedCities.toList()) { showCityPicker = false; onSaved() }
                }) { Text("Confirmer et publier") }
            },
            dismissButton = { TextButton(onClick = { showCityPicker = false }) { Text("Annuler") } }
        )
    }
}
