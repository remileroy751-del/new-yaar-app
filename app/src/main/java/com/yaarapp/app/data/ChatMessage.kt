package com.yaarapp.app.data

data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatConversation(
    val id: String,
    val buyerUid: String,
    val sellerUid: String,
    val productRemoteId: String?,
    val productName: String,
    val productPrice: Double,
    val shopName: String,
    val participants: List<String>
)
