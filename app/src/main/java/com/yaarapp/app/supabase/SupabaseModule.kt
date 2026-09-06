package com.yaarapp.app.supabase

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/** Configuration Supabase de Yaar-App. La publishable key est conçue pour être embarquée côté mobile. */
object SupabaseModule {
    const val URL = "https://lhqnjzuwndgtcjeyyjwj.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_uMFmqZ90XeOjcGaRFNl5Ig_aboyZD1q"

    val client = createSupabaseClient(
        supabaseUrl = URL,
        supabaseKey = PUBLISHABLE_KEY
    ) {
        install(Auth) {
            alwaysAutoRefresh = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
    }
}
