package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.flowOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.Product
import com.yaarapp.app.data.Shop
import com.yaarapp.app.viewmodel.YaarViewModel

@Composable
fun ChatScreen(
    product: Product,
    shop: Shop,
    viewModel: YaarViewModel,
    onBack: () -> Unit
) {
    val conversationId = viewModel.chatConversationId(product, shop)
    var text by remember { mutableStateOf("") }
    val messageFlow = remember(conversationId) { conversationId?.let { viewModel.observeChatMessages(it) } ?: flowOf(emptyList()) }
    val messages by messageFlow.collectAsState(initial = emptyList())
    val currentUid by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discussion — ${shop.name}") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text("${product.price.toLong()} FCFA · ${shop.city}", style = MaterialTheme.typography.bodySmall)
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.senderUid == currentUid?.firebaseUid) Arrangement.End else Arrangement.Start) {
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Text(message.text, Modifier.padding(12.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Écrire au fournisseur…") },
                    singleLine = false
                )
                IconButton(
                    onClick = {
                        val msg = text.trim()
                        if (msg.isNotEmpty()) {
                            viewModel.sendChatMessage(product, shop, msg) { error ->
                                if (error == null) text = ""
                            }
                        }
                    },
                    enabled = text.isNotBlank()
                ) { Icon(Icons.Filled.Send, "Envoyer") }
            }
        }
    }
}
