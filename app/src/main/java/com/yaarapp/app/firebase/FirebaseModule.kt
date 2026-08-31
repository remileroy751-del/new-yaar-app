package com.yaarapp.app.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/** Accès centralisé à Firebase Authentication, Firestore et Storage. */
object FirebaseModule {
    @Volatile private var initialized = false

    @Synchronized
    fun initialize(context: Context): Boolean {
        if (initialized) return true
        val appContext = context.applicationContext
        val existing = FirebaseApp.getApps(appContext).firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
        if (existing != null) {
            initialized = true
            return true
        }
        val app = FirebaseApp.initializeApp(appContext)
        initialized = app != null
        return initialized
    }

    private fun requireDefaultApp(): FirebaseApp = try {
        FirebaseApp.getInstance()
    } catch (e: IllegalStateException) {
        throw IllegalStateException(
            "Firebase n'est pas initialisé. Vérifiez app/google-services.json et le plugin Google Services.", e
        )
    }

    val auth: FirebaseAuth get() = FirebaseAuth.getInstance(requireDefaultApp())
    val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance(requireDefaultApp())
    val storage: FirebaseStorage get() = FirebaseStorage.getInstance(requireDefaultApp())

    /** Adresse technique Firebase correspondant au numéro WhatsApp. Aucun e-mail personnel n'est exposé. */
    fun authEmailForWhatsapp(whatsappNumber: String): String =
        whatsappNumber.filter { it.isDigit() } + "@login.yaar-app.com"

    fun isValidPassword(password: String): Boolean =
        password.matches(Regex("^[A-Za-z0-9]{6}$"))

    suspend fun createEmailPasswordAccount(whatsappNumber: String, password: String): String {
        require(isValidPassword(password)) { "Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement." }
        val result = auth.createUserWithEmailAndPassword(authEmailForWhatsapp(whatsappNumber), password).await()
        return result.user?.uid ?: error("Firebase n'a pas retourné l'identifiant du compte.")
    }

    /** Transforme l'ancien compte anonyme de l'appareil en compte permanent sans changer son UID. */
    suspend fun linkAnonymousAccount(whatsappNumber: String, password: String): String {
        require(isValidPassword(password)) { "Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement." }
        val current = auth.currentUser ?: signInAnonymouslyUser()
        val credential = EmailAuthProvider.getCredential(authEmailForWhatsapp(whatsappNumber), password)
        val result = current.linkWithCredential(credential).await()
        return result.user?.uid ?: error("Impossible de sécuriser le compte Firebase.")
    }

    suspend fun signInWithWhatsappPassword(whatsappNumber: String, password: String): String {
        require(isValidPassword(password)) { "Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement." }
        val result = auth.signInWithEmailAndPassword(authEmailForWhatsapp(whatsappNumber), password).await()
        return result.user?.uid ?: error("Firebase n'a pas retourné l'identifiant du compte.")
    }

    suspend fun signOut() { auth.signOut() }

    suspend fun signInAnonymouslyUser(): String {
        val current = auth.currentUser
        if (current != null) return current.uid
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Firebase Auth anonyme n'a retourné aucun utilisateur.")
    }

    suspend fun ensureSignedIn(): String = signInAnonymouslyUser()
}
