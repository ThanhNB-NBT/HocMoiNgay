package com.thanhnb.hocmoingay.core.net

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeApiTest {
    // đúng hình dạng run-code trả (server/functions/run-code/index.ts, _shared/judge.ts, run-code/grade.ts)
    private val judged = """
      {"compiled":true,"tests":[
        {"name":"vd1","pass":true,"expected":[0,1],"actual":[0,1],"time_ms":1,"hidden":false},
        {"name":"test 3","pass":false,"error":"sai","time_ms":0,"hidden":true}],
       "stdout":"","stderr":"","time_ms":420,"memory_kb":2048,
       "grade":{"correctness":8,"readability":7,"complexity":"O(n)","complexity_ok":true,"best_practices":7,"idiomatic":6,
                "summary":"Ổn","suggestions":["Đặt tên rõ hơn"]}}
    """.trimIndent()

    @Test fun nopGuiDuTruongVaDocKetQua() = runTest {
        var sent: JsonObject? = null
        val api = CodeApi { fn, body -> assertEquals("run-code", fn); sent = body; judged }
        val r = api.submit("l", "k", "rust", "fn main(){}", hints = 1) as ApiResult.Ok
        assertEquals("submit", sent!!["mode"]!!.jsonPrimitive.content)
        assertEquals("rust", sent!!["language"]!!.jsonPrimitive.content)
        assertEquals(1, sent!!["hints_used"]!!.jsonPrimitive.int)
        assertFalse(r.value.allPass)
        assertTrue(r.value.tests[1].hidden)
        assertNull(r.value.tests[1].expected)
        assertEquals(8, r.value.grade!!.correctness)
    }

    @Test fun chayTuDoKhongGuiLessonId() = runTest {
        var sent: JsonObject? = null
        val api = CodeApi { _, body ->
            sent = body
            """{"compile":null,"run":{"stdout":"42\n","stderr":"","code":0,"signal":null,"status":null},"time_ms":30,"memory_kb":null}"""
        }
        val r = api.runFree("python", "print(42)") as ApiResult.Ok
        assertEquals("42\n", r.value.run.stdout)
        assertFalse("lesson_id" in sent!!)
    }

    @Test fun loiMangThanhCauTiengViet() = runTest {
        val api = CodeApi { _, _ -> throw java.io.IOException("connection reset") }
        assertEquals(ApiResult.Err("Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại."), api.runFree("python", "x"))
    }

    @Test fun sandboxTatVaHetPhien() {
        assertTrue(errorText(ApiHttp(502, """{"error":"sandbox_unavailable"}""")).startsWith("Máy chạy code"))
        assertTrue(errorText(ApiHttp(401, "")).startsWith("Phiên đăng nhập"))
    }

    @Test fun duLieuLaKhongVo() = runTest {
        assertEquals(ApiResult.Err("Máy chủ trả dữ liệu không đọc được."), CodeApi { _, _ -> "<html>" }.starter("l", "k", "go"))
    }

    @Test fun thoiGianBienDich() {
        assertNull(compileEta("python"))
        assertEquals(4, compileEta("kotlin"))
        assertEquals(2, compileEta("rust"))
    }
}
