package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Profil local d'un compte Yaar-App.
 *
 * Le mot de passe n'est JAMAIS stocké ici ni dans Firestore : il est géré
 * par Firebase Authentication (Email/Password). L'adresse e-mail technique
 * utilisée par Firebase est dérivée du numéro WhatsApp et n'est pas affichée.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firstName: String,
    val country: Country,
    val city: String,
    val whatsappNumber: String,
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    /** UID Firebase Authentication. Null uniquement pour les anciens comptes avant sécurisation. */
    val firebaseUid: String? = null
)
