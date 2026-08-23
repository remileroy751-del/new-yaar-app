package com.yaarapp.app.util

import java.security.MessageDigest

/**
 * Hachage simple pour la démonstration locale.
 *
 * ⚠️ Pour une mise en production réelle, ne stockez jamais les mots de passe
 * vous-même : utilisez un service d'authentification éprouvé (ex. Firebase
 * Authentication) qui gère le hachage, le salage et la sécurité pour vous.
 */
object PasswordHasher {
    private const val SALT = "yaar-app-static-salt-v1"

    fun hash(rawPassword: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((SALT + rawPassword).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun matches(rawPassword: String, hashedPassword: String): Boolean = hash(rawPassword) == hashedPassword
}
