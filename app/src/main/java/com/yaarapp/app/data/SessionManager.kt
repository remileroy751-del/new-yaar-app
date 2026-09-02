package com.yaarapp.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "yaar_session")

class SessionManager(private val context: Context) {

    private val currentUserIdKey = intPreferencesKey("current_user_id")
    private val acceptedTermsVersionKey = intPreferencesKey("accepted_terms_version")

    /** Version des conditions acceptée sur cet appareil. Incrémenter TERMS_VERSION
     * force une nouvelle lecture/acceptation lorsque les conditions évoluent. */
    val acceptedTermsVersion: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[acceptedTermsVersionKey] ?: 0
    }

    suspend fun acceptTerms(version: Int) {
        context.dataStore.edit { it[acceptedTermsVersionKey] = version }
    }

    val currentUserId: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[currentUserIdKey]?.takeIf { it > 0 }
    }

    suspend fun setCurrentUser(userId: Int) {
        context.dataStore.edit { it[currentUserIdKey] = userId }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.remove(currentUserIdKey) }
    }
}
