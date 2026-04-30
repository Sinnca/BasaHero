package com.basahero.elearning.data.remote

import com.basahero.elearning.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

// ─────────────────────────────────────────────────────────────────────────────
// SupabaseClient
// Singleton Supabase client — initialized once, reused everywhere.
// Credentials come from BuildConfig (set in local.properties — never hardcoded).
// ─────────────────────────────────────────────────────────────────────────────
object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)          // Database queries (student_progress)
        install(Auth)               // Teacher login / signup
        install(Realtime)           // Live updates
    }
}