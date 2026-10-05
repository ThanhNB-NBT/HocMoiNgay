package com.thanhnb.hocmoingay

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thanhnb.hocmoingay.core.net.ApiHttp
import com.thanhnb.hocmoingay.core.net.ApiResult
import com.thanhnb.hocmoingay.core.net.CodeApi
import com.thanhnb.hocmoingay.core.net.createSupabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Xong khi của c: nộp bài problem bằng Python, Kotlin, Rust, C# đều pass. Chỉ chạy với server dev (bản debug có DEV_EMAIL). */
@RunWith(AndroidJUnit4::class)
class SubmitE2ETest {
    @Test fun nopTwoSumBonNgonNguDeuPass() = runBlocking {
        assumeTrue("chỉ chạy trên bản trỏ server dev", BuildConfig.DEV_EMAIL.isNotEmpty())
        val sb = createSupabase()
        sb.auth.signInWith(Email) { email = BuildConfig.DEV_EMAIL; password = BuildConfig.DEV_PASSWORD }
        val api = CodeApi { fn, body ->
            try { sb.functions.invoke(fn, body).bodyAsText() } catch (e: RestException) { throw ApiHttp(e.statusCode, e.error) }
        }
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val files = mapOf("python" to "python.py", "kotlin" to "kotlin.kt", "rust" to "rust.rs", "csharp" to "csharp.cs")
        val fails = mutableListOf<String>()
        for ((lang, f) in files) {
            val code = assets.open("two_sum/$f").bufferedReader().readText()
            when (val r = api.submit("_sample-code/nhap-mon/mau/bai-problem", "two_sum", lang, code, hints = 0)) {
                is ApiResult.Ok -> if (!r.value.allPass) fails += "$lang: ${r.value.tests.filterNot { it.pass }.map { it.name to it.error }}"
                is ApiResult.Err -> fails += "$lang: ${r.message}"
            }
        }
        assertTrue(fails.joinToString("\n"), fails.isEmpty())
    }
}
