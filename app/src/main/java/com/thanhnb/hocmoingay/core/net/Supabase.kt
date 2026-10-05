package com.thanhnb.hocmoingay.core.net

import com.thanhnb.hocmoingay.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer

fun createSupabase(): SupabaseClient = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
    defaultSerializer = KotlinXSerializer(SyncJson)
    install(Auth)
    install(Postgrest)
    install(Realtime)
}
