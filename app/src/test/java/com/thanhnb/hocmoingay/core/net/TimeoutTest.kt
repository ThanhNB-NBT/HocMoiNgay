package com.thanhnb.hocmoingay.core.net

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ServerSocket
import kotlin.concurrent.thread

class TimeoutTest {
    /** submit chờ Piston + Gemini 10–20 s mà không có byte nào về; read timeout mặc định 10 s của OkHttp cắt trước. */
    @Test fun functionTraLoiSau11GiayVanNhanDuoc() = runBlocking {
        val server = ServerSocket(0)
        thread {
            server.accept().use { s ->
                val r = s.getInputStream().bufferedReader()
                while (!r.readLine().isNullOrEmpty()) Unit
                Thread.sleep(11_000)
                s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}".toByteArray())
                s.getOutputStream().flush()
            }
        }
        val c = createSupabaseClient("http://127.0.0.1:${server.localPort}", "k") { install(Functions); slowCalls() }
        assertEquals(200, c.functions.invoke("run-code").status.value)
        server.close()
    }
}
