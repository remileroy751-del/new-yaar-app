package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Compte utilisateur.
 *
 * NOTE IMPORTANTE : pour cette version, l'inscription est volontairement minimale —
 * pays, ville, prénom et numéro WhatsApp, sans mot de passe. L'identifiant de connexion
 * est le numéro WhatsApp lui-même : quiconque saisit un numéro déjà enregistré est
 * connecté à ce compte, sans vérification supplémentaire. C'est un choix assumé pour
 * réduire la friction à l'inscription, mais ce n'est PAS un mécanisme de sécurité — à
 * remplacer par une vraie vérification (ex. code OTP envoyé par SMS/WhatsApp, ou
 * Firebase Auth par numéro de téléphone) avant toute mise en production sérieuse.
 *
 * whatsappNumber est stocké au format "00" + indicatif pays + numéro local (ex :
 * "0022890000000" pour un numéro togolais), afin de faciliter l'envoi automatique de
 * messages vers ce numéro.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firstName: String,
    val country: Country,
    val city: String,
    val whatsappNumber: String,
    /**
     * Reçoit ou non les notifications de l'application (ex : "je suis intéressé" sur un
     * produit de sa boutique). Réglable dans "Mon profil". Servira à filtrer l'envoi des
     * notifications push une fois le backend Firebase branché (Firebase Cloud Messaging).
     */
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
