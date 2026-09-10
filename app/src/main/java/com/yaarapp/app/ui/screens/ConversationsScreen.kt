@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.yaarapp.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yaarapp.app.data.ChatConversation
import com.yaarapp.app.viewmodel.YaarViewModel

@Composable
fun ConversationsScreen(
    viewModel: YaarViewModel,
    onOpen: (String) -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentUid by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Discussions", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        if (conversations.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null)
                Text("Aucune discussion pour le moment", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                Text("Quand vous échangez avec un vendeur ou un acheteur, la discussion apparaîtra ici.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    val otherName = if (conversation.buyerUid == currentUid?.firebaseUid) conversation.shopName else conversation.buyerName
                    Card(onClick = { onOpen(conversation.id) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(otherName.ifBlank { "Discussion" }, fontWeight = FontWeight.Bold)
                                Text(conversation.productName, style = MaterialTheme.typography.bodyMedium)
                                if (conversation.lastMessage.isNotBlank()) {
                                    Text(conversation.lastMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                            Text("${conversation.productPrice.toLong()} FCFA", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
