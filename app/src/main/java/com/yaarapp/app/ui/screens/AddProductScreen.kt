package com.yaarapp.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yaarapp.app.data.CityRepository
import com.yaarapp.app.data.ProductCategories
import com.yaarapp.app.util.ImageStorage
import com.yaarapp.app.viewmodel.YaarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(viewModel: YaarViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    var mode by remember { mutableStateOf<String?>(null) }
    var immoType by remember { mutableStateOf<String?>(null) }
    if (mode == null) {
        ChoiceScreen(onBack, { mode = "PRODUCT" }, { mode = "IMMO" })
        return
    }
    if (mode == "IMMO" && immoType == null) {
        ChoiceScreen(onBack, { immoType = "IMMO_SALE" }, { immoType = "IMMO_RENT" }, title = "Annonces immobilières", first = "Je veux mettre en vente", second = "Je veux mettre en location")
        return
    }
    if (mode == "PRODUCT") ProductForm(viewModel, onBack, onSaved) else ImmoForm(viewModel, immoType!!, onBack, onSaved)
}

@Composable private fun ChoiceScreen(onBack: () -> Unit, onFirst: () -> Unit, onSecond: () -> Unit, title: String = "Que voulez-vous faire ?", first: String = "Mettre en vente un produit", second: String = "Annonces immobilières") {
    Scaffold(topBar={TopAppBar(title={Text(title)}, navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour")}})}) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(24.dp), verticalArrangement=Arrangement.spacedBy(16.dp), horizontalAlignment=Alignment.CenterHorizontally) {
            Spacer(Modifier.height(30.dp)); Button(onClick=onFirst, Modifier.fillMaxWidth(), shape=RoundedCornerShape(14.dp)){Text(first)}
            OutlinedButton(onClick=onSecond, Modifier.fillMaxWidth(), shape=RoundedCornerShape(14.dp)){Text(second)}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ProductForm(viewModel:YaarViewModel,onBack:()->Unit,onSaved:()->Unit){
    val context=LocalContext.current; val user by viewModel.currentUser.collectAsStateWithLifecycle(); var image by remember{mutableStateOf<Uri?>(null)}; var name by remember{mutableStateOf("")}; var desc by remember{mutableStateOf("")}; var price by remember{mutableStateOf("")}; var cat by remember{mutableStateOf<String?>(null)}; var expanded by remember{mutableStateOf(false)}; var showCities by remember{mutableStateOf(false)}; var cities by remember{mutableStateOf(setOf<String>())}; val error by viewModel.addProductError.collectAsStateWithLifecycle(); val picker=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){if(it!=null)image=it}; LaunchedEffect(user?.city){user?.city?.let{cities=setOf(it)}}
    Scaffold(topBar={TopAppBar(title={Text("Ajouter un produit")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour")}})}){pad->Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp)){
        PhotoBox(image,"Ajouter une photo"){picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}; OutlinedTextField(name,{name=it},label={Text("Nom du produit")},Modifier.fillMaxWidth().padding(top=12.dp)); ExposedDropdownMenuBox(expanded,{expanded=!expanded}){OutlinedTextField(cat?:"",{},readOnly=true,label={Text("Catégorie")},modifier=Modifier.fillMaxWidth().menuAnchor()); DropdownMenu(expanded,{expanded=false}){ProductCategories.all.forEach{DropdownMenuItem({Text(it)},{cat=it;expanded=false})}}}; OutlinedTextField(price,{price=it.filter(Char::isDigit)},label={Text("Prix (FCFA)")},modifier=Modifier.fillMaxWidth().padding(top=8.dp)); OutlinedTextField(desc,{desc=it},label={Text("Description")},minLines=3,modifier=Modifier.fillMaxWidth().padding(top=8.dp)); if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error,Modifier.padding(top=8.dp)); Button(onClick={showCities=true},enabled=image!=null&&name.isNotBlank()&&desc.isNotBlank()&&price.isNotBlank()&&cat!=null,modifier=Modifier.fillMaxWidth().padding(top=18.dp)){Text("Publier le produit")}
    }}
    if(showCities){AlertDialog(onDismissRequest={showCities=false},title={Text("Villes de visibilité")},text={Column(Modifier.verticalScroll(rememberScrollState())){CityRepository.citiesFor(user!!.country).forEach{c->Row(Modifier.fillMaxWidth().clickable{if(c in cities&&c!=user?.city)cities-=c else if(cities.size<6)cities+=c},verticalAlignment=Alignment.CenterVertically){Checkbox(c in cities,{checked->if(checked)cities+=c else cities-=c});Text(c)}}}},confirmButton={TextButton(onClick={val path=ImageStorage.saveToInternalStorage(context,image!!);if(path!=null){viewModel.addProduct(name.trim(),desc.trim(),price.toDoubleOrNull()?:0.0,path,cat!!,cities.toList(),onSuccess= {showCities=false;onSaved()})}}){Text("Publier")}},dismissButton={TextButton(onClick={showCities=false}){Text("Annuler")}})}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ImmoForm(viewModel:YaarViewModel,type:String,onBack:()->Unit,onSaved:()->Unit){val context=LocalContext.current;var first by remember{mutableStateOf<Uri?>(null)};var second by remember{mutableStateOf<Uri?>(null)};var title by remember{mutableStateOf("")};var desc by remember{mutableStateOf("")};val error by viewModel.addProductError.collectAsStateWithLifecycle();val pick=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()){u->if(u!=null){if(first==null)first=u else second=u}};Scaffold(topBar={TopAppBar(title={Text(if(type=="IMMO_RENT")"Nouvelle annonce — Location" else "Nouvelle annonce — Vente")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Retour")}})}){pad->Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp)){Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){PhotoBox(first,"Photo 1"){pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))};PhotoBox(second,"Photo 2"){if(first!=null)pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}};Text("2 photos maximum",style=MaterialTheme.typography.labelSmall,modifier=Modifier.padding(top=6.dp));OutlinedTextField(title,{title=it},label={Text("Titre de l'annonce")},modifier=Modifier.fillMaxWidth().padding(top=14.dp));OutlinedTextField(desc,{desc=it},label={Text("Description — prix, caractéristiques, localisation, etc.")},minLines=7,modifier=Modifier.fillMaxWidth().padding(top=8.dp));if(error!=null)Text(error!!,color=MaterialTheme.colorScheme.error,Modifier.padding(top=8.dp));Button(onClick={val p1=ImageStorage.saveToInternalStorage(context,first!!);val p2=second?.let{ImageStorage.saveToInternalStorage(context,it)};if(p1!=null)viewModel.addImmoListing(title.trim(),desc.trim(),p1,p2,type){onSaved()}},enabled=first!=null&&title.isNotBlank()&&desc.isNotBlank(),modifier=Modifier.fillMaxWidth().padding(top=18.dp)){Text("Publier l'annonce")}}}}

@Composable private fun PhotoBox(uri:Uri?,label:String,onClick:()->Unit){Box(Modifier.size(150.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick=onClick),contentAlignment=Alignment.Center){if(uri!=null)AsyncImage(uri,label,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)else Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Filled.AddAPhoto,null);Text(label)}}}
