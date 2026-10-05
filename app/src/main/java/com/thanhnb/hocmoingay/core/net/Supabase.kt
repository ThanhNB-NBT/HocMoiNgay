package com.thanhnb.hocmoingay.core.net

import io.ktor.client.engine.okhttp.OkHttp
import com.thanhnb.hocmoingay.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

fun createSupabase(): SupabaseClient = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
    defaultSerializer = KotlinXSerializer(SyncJson)
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Functions)
    install(Storage)
    slowCalls()
}

/** submit = Piston + Gemini (deadline 25 s ở server) + khởi động lạnh; Cloudflare cắt ở 100 s. */
internal fun SupabaseClientBuilder.slowCalls() {
    requestTimeout = 90.seconds
    // supabase-kt chỉ đặt requestTimeout; read timeout mặc định 10 s của OkHttp cắt trước khi server trả lời
    httpEngine = OkHttp.create { config { readTimeout(90, TimeUnit.SECONDS) } }
}
