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

    /**
     * Normalise tous les formats historiques utilisés par Yaar-App vers le même
     * identifiant international. Ainsi 00228..., 228... et +228... désignent
     * le même compte Firebase.
     */
    fun normalizeWhatsapp(whatsappNumber: String): String {
        var digits = whatsappNumber.filter { it.isDigit() }
        while (digits.startsWith("00")) digits = digits.removePrefix("00")
        val knownCountryCodes = listOf("228", "229", "226", "225", "223", "227", "221")
        if (knownCountryCodes.none { digits.startsWith(it) }) {
            // Format local : on ne peut pas deviner le pays sur l'écran de connexion.
            // On conserve donc le numéro tel quel pour les comptes déjà créés.
            return digits
        }
        return "00$digits"
    }

    /** Adresse technique Firebase correspondant au numéro WhatsApp. */
    fun authEmailForWhatsapp(whatsappNumber: String): String =
        normalizeWhatsapp(whatsappNumber) + "@login.yaar-app.com"

    /**
     * Adresses historiques à essayer lors d'une connexion, notamment pour les
     * comptes créés avant la normalisation des numéros.
     */
    fun legacyAuthEmailsForWhatsapp(whatsappNumber: String): List<String> {
        val digits = whatsappNumber.filter { it.isDigit() }
        val without00 = digits.removePrefix("00")
        val candidates = linkedSetOf(
            authEmailForWhatsapp(whatsappNumber),
            without00 + "@login.yaar-app.com",
            digits + "@login.yaar-app.com"
        )
        return candidates.toList()
    }

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
        // Ne jamais mélanger FirebaseUser et String ici : signInAnonymouslyUser()
        // retourne l'UID, alors que linkWithCredential() appartient à FirebaseUser.
        val current = auth.currentUser ?: auth.signInAnonymously().await().user
            ?: error("Impossible d'obtenir l'utilisateur Firebase anonyme.")
        val credential = EmailAuthProvider.getCredential(authEmailForWhatsapp(whatsappNumber), password)
        val result = current.linkWithCredential(credential).await()
        return result.user?.uid ?: error("Impossible de sécuriser le compte Firebase.")
    }

    suspend fun signInWithWhatsappPassword(whatsappNumber: String, password: String): String {
        require(isValidPassword(password)) { "Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement." }
        var lastError: Exception? = null
        for (email in legacyAuthEmailsForWhatsapp(whatsappNumber)) {
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                return result.user?.uid ?: error("Firebase n'a pas retourné l'identifiant du compte.")
            } catch (e: Exception) {
                lastError = e
                val code = (e as? com.google.firebase.auth.FirebaseAuthException)?.errorCode.orEmpty().lowercase()
                // Si le mot de passe est réellement faux, inutile de multiplier les tentatives.
                if (code.contains("wrong-password") || code.contains("invalid-credential") ||
                    code.contains("invalid-login-credentials")) continue
            }
        }
        throw lastError ?: IllegalStateException("Impossible de se connecter à Firebase.")
    }

    suspend fun reauthenticateWithPassword(whatsappNumber: String, password: String) {
        require(isValidPassword(password)) { "Le mot de passe doit contenir exactement 6 caractères, lettres et chiffres uniquement." }
        val user = auth.currentUser ?: throw IllegalStateException("Compte Firebase introuvable.")
        val credential = EmailAuthProvider.getCredential(authEmailForWhatsapp(whatsappNumber), password)
        user.reauthenticate(credential).await()
    }

    suspend fun deleteCurrentAuthUser() {
        auth.currentUser?.delete()?.await() ?: throw IllegalStateException("Compte Firebase introuvable.")
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
