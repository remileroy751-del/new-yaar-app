package com.yaarapp.app.firebase

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await

/**
 * Point d'accès unique aux services Firebase utilisés par l'app.
 *
 * Ce fichier nécessite que `app/google-services.json` soit présent (voir
 * BACKEND_FIREBASE.md) — sans lui, les dépendances Firebase ne sont pas incluses par
 * Gradle et ce fichier ne compilerait pas. Comme le fichier est maintenant en place,
 * tout est actif.
 */
object FirebaseModule {
    val auth by lazy { Firebase.auth }
    val firestore by lazy { Firebase.firestore }
    val storage by lazy { Firebase.storage }

    /**
     * Connexion anonyme automatique : chaque installation de l'app obtient une identité
     * Firebase stable (sans mot de passe ni numéro à vérifier), ce qui suffit à satisfaire
     * les règles de sécurité Firestore/Storage ("request.auth != null") sans imposer un
     * vrai écran de connexion Firebase en plus du système de compte local existant.
     *
     * Amélioration future possible : remplacer par une vraie connexion par numéro de
     * téléphone (Firebase Auth Phone) pour relier chaque compte local à une identité
     * Firebase persistante et permettre des règles plus strictes par propriétaire.
     */
    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid.orEmpty()
    }
}
