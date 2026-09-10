package com.yaarapp.app.data

import com.yaarapp.app.supabase.SupabaseModule
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.Serializable

@Serializable
data class PayDunyaCreateRequest(
    val purpose: String,
    val amountFcfa: Int,
    val description: String,
    val productId: String? = null,
    val shopId: String? = null,
    val expositions: Int? = null,
    val durationDays: Int? = null
)

@Serializable
data class PayDunyaCreateResponse(
    val paymentId: String,
    val token: String,
    val checkoutUrl: String
)

@Serializable
data class PayDunyaCheckRequest(val paymentId: String)

@Serializable
data class PayDunyaCheckResponse(val status: String, val paymentId: String? = null, val message: String? = null)

class PayDunyaService {
    private val client get() = SupabaseModule.client

    suspend fun createPayment(request: PayDunyaCreateRequest): PayDunyaCreateResponse =
        client.functions.invoke("paydunya-create-invoice", body = request).body()

    suspend fun checkPayment(paymentId: String): PayDunyaCheckResponse =
        client.functions.invoke("paydunya-check-payment", body = PayDunyaCheckRequest(paymentId)).body()
}
