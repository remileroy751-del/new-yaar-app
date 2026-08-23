package com.yaarapp.app.data

import androidx.room.Entity

@Entity(tableName = "cart_items", primaryKeys = ["userId", "productId"])
data class CartItem(
    val userId: Int,
    val productId: Int,
    val productName: String,
    val price: Double,
    val imageUrl: String,
    val shopId: Int,
    val shopName: String,
    val shopWhatsappNumber: String,
    val quantity: Int = 1
)
