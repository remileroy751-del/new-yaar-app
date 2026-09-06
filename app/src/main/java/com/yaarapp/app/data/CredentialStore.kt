package com.yaarapp.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stockage local des identifiants de connexion.
 *
 * L'adresse e-mail est conservée en clair pour pouvoir la préremplir.
 * Le mot de passe, lorsqu'il est mémorisé, est chiffré avec une clé AES
 * conservée dans Android Keystore. Le mot de passe n'est jamais écrit en clair
 * dans SharedPreferences/DataStore.
 */
class CredentialStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getEmail(): String = prefs.getString(KEY_EMAIL, "").orEmpty()

    fun saveEmail(email: String) {
        prefs.edit().putString(KEY_EMAIL, email.trim().lowercase()).apply()
    }

    fun getRememberedPassword(): String? {
        val encoded = prefs.getString(KEY_PASSWORD, null) ?: return null
        return runCatching { decrypt(encoded) }.getOrNull()
    }

    fun savePassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, encrypt(password)).apply()
    }

    fun clearPassword() {
        prefs.edit().remove(KEY_PASSWORD).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val iv = cipher.iv
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val parts = value.split(':', limit = 2)
        require(parts.size == 2)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    companion object {
        private const val PREFS_NAME = "yaar_saved_login"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password_encrypted"
        private const val KEY_ALIAS = "yaar_app_login_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
