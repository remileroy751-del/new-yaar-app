package com.yaarapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yaarapp.app.nav.YaarNavHost
import com.yaarapp.app.ui.theme.YaarAppTheme
import com.yaarapp.app.viewmodel.YaarViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as YaarApplication
        val factory = YaarViewModelFactory(app.repository)

        setContent {
            YaarAppTheme {
                Surface(
                    // enableEdgeToEdge() fait dessiner l'app sous le clavier (IME) : sans
                    // imePadding() ici, aucun écran ne remonte automatiquement quand le
                    // clavier s'ouvre, et les champs de saisie du bas (prix, description...)
                    // se retrouvent cachés derrière. Appliqué une seule fois ici, ça corrige
                    // le problème sur TOUS les écrans (Ajouter un produit, Inscription, etc.)
                    // sans avoir à le répéter partout.
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    YaarNavHost(viewModelFactory = factory)
                }
            }
        }
    }
}

