package com.kost.project

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClientInstance {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    val client = createSupabaseClient(
        supabaseUrl = "https://sjkhwgmkkcnwoxbplbxz.supabase.co",
        supabaseKey = "sb_publishable_4g_CZ-aNbtSvWcY-Qa2fBg_jzw-Gkji"
    ) {
        install(Postgrest)
        install(Auth)
        defaultSerializer = KotlinXSerializer(json)
    }
}

