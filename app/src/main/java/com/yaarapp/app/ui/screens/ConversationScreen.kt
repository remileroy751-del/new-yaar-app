@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yaarapp.app.ui.screens

import android.content.Context
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.ChatConversation
import com.yaarapp.app.util.WhatsAppHelper
import com.yaarapp.app.viewmodel.YaarViewModel

@Composable
fun ConversationScreen(
    conversationId: String,
    viewModel: YaarViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val conversation by viewModel.conversation(conversationId).collectAsStateWithLifecycle(initialValue = null)
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val messages by viewModel.observeChatMessages(conversationId).collectAsStateWithLifecycle(initialValue = emptyList())
    var text by remember { mutableStateOf("") }

    LaunchedEffect(conversationId) { viewModel.refreshConversations() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversation?.productName ?: "Discussion") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            conversation?.let { c ->
                Card(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(c.productName, style = MaterialTheme.typography.titleMedium)
                        Text("${c.productPrice.toLong()} FCFA · ${c.shopName}", style = MaterialTheme.typography.bodySmall)
                        val otherNumber = if (c.buyerUid == currentUser?.firebaseUid) c.sellerWhatsappNumber else c.buyerWhatsappNumber
                        if (otherNumber.isNotBlank()) {
                            OutlinedButton(
                                onClick = { WhatsAppHelper.continueConversationOnWhatsApp(context, otherNumber, c.productName) },
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Continuer sur WhatsApp") }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.senderUid == currentUser?.firebaseUid) Arrangement.End else Arrangement.Start) {
                        Card(shape = RoundedCornerShape(14.dp)) { Text(message.text, Modifier.padding(12.dp)) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Écrire un message…") },
                    singleLine = false
                )
                IconButton(
                    onClick = {
                        val msg = text.trim()
                        if (msg.isNotEmpty()) viewModel.sendChatMessageInConversation(conversationId, msg) { if (it == null) text = "" }
                    },
                    enabled = text.isNotBlank()
                ) { Icon(Icons.Filled.Send, "Envoyer") }
            }
        }
    }
}
