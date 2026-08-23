package com.yaarapp.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Compte utilisateur.
 *
 * NOTE IMPORTANTE : pour cette version de démonstration, les comptes sont stockés
 * uniquement en local (base Room) avec un mot de passe simplement haché. Cela permet
 * de tester tout le parcours (inscription, connexion, boutique, achats) sans backend.
 * Pour une mise en production réelle où plusieurs utilisateurs doivent voir les mêmes
 * boutiques/produits depuis des téléphones différents, il faudra remplacer cette couche
 * par un vrai service en ligne — voir /BACKEND_FIREBASE.md pour la marche à suivre.
 *
 * whatsappNumber est l'identifiant de connexion (unique) : il est stocké au format
 * "00" + indicatif pays + numéro local (ex : "0022890000000" pour un numéro togolais),
 * afin de faciliter l'envoi automatique de messages WhatsApp vers ce numéro.
 */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firstName: String,
    val sex: Sex,
    val country: Country,
    val city: String,
    val whatsappNumber: String,
    val passwordHash: String,
    /**
     * Reçoit ou non les notifications de l'application (ex : "je suis intéressé" sur un
     * produit de sa boutique). Réglable dans "Mon profil". Servira à filtrer l'envoi des
     * notifications push une fois le backend Firebase branché (Firebase Cloud Messaging).
     */
    val notificationsEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
