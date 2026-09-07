package com.yaarapp.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yaarapp.app.ui.theme.YaarAppTheme

/**
 * Écran affiché à la place d'une fermeture brutale de l'application (voir
 * [YaarApplication.installCrashGuard]). Montre le détail technique exact de l'erreur,
 * copiable en un clic, pour permettre un diagnostic précis sans avoir besoin d'adb/logcat.
 */
class CrashActivity : ComponentActivity() {

    companion object {
        const val EXTRA_STACK_TRACE = "stack_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE)
            ?: "Détail de l'erreur indisponible."

        setContent {
            YaarAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CrashScreen(
                        stackTrace = stackTrace,
                        onCopy = { copyToClipboard(stackTrace) },
                        onRestart = { restartApp() }
                    )
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Yaar-App crash", text))
        Toast.makeText(this, "Détails copiés. Vous pouvez les coller pour les envoyer.", Toast.LENGTH_LONG).show()
    }

    private fun restartApp() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(restartIntent)
        finish()
        // Cette activité tourne dans un process séparé (":crash_report") dédié à
        // l'affichage de l'erreur : le fermer ici ne relance PAS l'application, qui
        // redémarre proprement via l'intent ci-dessus dans son propre process.
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}

@Composable
private fun CrashScreen(stackTrace: String, onCopy: () -> Unit, onRestart: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "Oups, une erreur inattendue est survenue",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "L'application a rencontré un problème. Copiez le détail ci-dessous et envoyez-le pour qu'il soit corrigé, puis redémarrez l'application.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                stackTrace,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                Text("Copier les détails")
            }
            Button(onClick = onRestart, modifier = Modifier.weight(1f)) {
                Text("Redémarrer l'application")
            }
        }
    }
}
