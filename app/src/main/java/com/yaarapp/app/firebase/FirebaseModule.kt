package com.yaarapp.app.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * Point d'accès unique aux services Firebase utilisés par Yaar-App.
 *
 * Firebase est initialisé explicitement au démarrage de l'application. Cette étape est
 * volontaire : elle évite l'erreur "Default FirebaseApp is not initialized" lorsque
 * l'initialisation automatique n'a pas encore eu lieu au moment où un repository démarre.
 *
 * La configuration elle-même vient de app/google-services.json, traité par le plugin
 * com.google.gms.google-services pendant la compilation.
 */
object FirebaseModule {

    @Volatile
    private var initialized = false

    /**
     * Initialise l'application Firebase par défaut si elle ne l'est pas déjà.
     * Retourne true si Firebase est disponible, false si la configuration générée par
     * google-services.json est absente/invalide.
     */
    @Synchronized
    fun initialize(context: Context): Boolean {
        if (initialized) return true

        val appContext = context.applicationContext
        val existing = FirebaseApp.getApps(appContext)
            .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }

        if (existing != null) {
            initialized = true
            return true
        }

        val app = FirebaseApp.initializeApp(appContext)
        initialized = app != null
        return initialized
    }

    /** Indique si l'application Firebase par défaut est actuellement disponible. */
    fun isInitialized(context: Context): Boolean =
        FirebaseApp.getApps(context.applicationContext)
            .any { it.name == FirebaseApp.DEFAULT_APP_NAME }

    private fun requireDefaultApp(): FirebaseApp = try {
        FirebaseApp.getInstance()
    } catch (e: IllegalStateException) {
        throw IllegalStateException(
            "Firebase n'est pas initialisé. Vérifiez app/google-services.json et le plugin Google Services.",
            e
        )
    }

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance(requireDefaultApp())

    val firestore: FirebaseFirestore
        get() = FirebaseFirestore.getInstance(requireDefaultApp())

    val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance(requireDefaultApp())

    /**
     * Connexion anonyme automatique : chaque installation obtient une identité Firebase
     * stable, suffisante pour satisfaire les règles actuelles request.auth != null.
     */
    suspend fun ensureSignedIn(): String {
        val current = auth.currentUser
        if (current != null) return current.uid

        val result = auth.signInAnonymously().await()
        return result.user?.uid
            ?: throw IllegalStateException("Firebase Auth a réussi mais aucun utilisateur n'a été retourné.")
    }
}
