package com.thanhnb.hocmoingay.core.net

import android.util.Log
import com.thanhnb.hocmoingay.core.lesson.LessonJson
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(f: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Ok -> ApiResult.Ok(f(value))
    is ApiResult.Err -> this
}

/** Lỗi HTTP từ edge function (AppGraph đổi RestException của supabase-kt sang lớp này). */
class ApiHttp(val status: Int, val body: String) : Exception("HTTP $status")

fun errorText(e: Throwable): String = when {
    e is ApiHttp && e.status == 502 && "sandbox_unavailable" in e.body -> "Máy chạy code đang tắt hoặc quá tải. Thử lại sau ít phút."
    e is ApiHttp && e.status == 502 -> "Máy chấm đang bận. Thử lại sau ít phút."
    e is ApiHttp && (e.status == 401 || e.status == 403) -> "Phiên đăng nhập hết hạn. Đăng nhập lại rồi thử."
    e is ApiHttp && e.status == 404 -> "Máy chủ chưa có bài này. Mở lại app để kéo giáo trình mới."
    e is ApiHttp && e.status == 400 -> "Máy chủ từ chối yêu cầu: ${e.body.take(120)}"
    e is ApiHttp -> "Máy chủ lỗi (HTTP ${e.status}). Thử lại sau."
    e is SerializationException -> "Máy chủ trả dữ liệu không đọc được."
    e::class.simpleName.orEmpty().contains("Timeout") -> "Máy chủ trả lời quá lâu. Thử lại sau."
    else -> "Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại."
}

/** Thời gian biên dịch thường gặp (giây, làm tròn lên từ số đo ở server/piston/VERSIONS.md); null = không phải biên dịch. */
fun compileEta(lang: String): Int? = when (lang) {
    "kotlin" -> 4
    "java", "rust", "cpp", "csharp", "go" -> 2
    else -> null
}

@Serializable data class RunOut(val stdout: String = "", val stderr: String = "", val code: Int? = null, val signal: String? = null, val status: String? = null)
@Serializable data class FreeRun(
    val compile: RunOut? = null, val run: RunOut,
    @SerialName("time_ms") val timeMs: Double? = null, @SerialName("memory_kb") val memoryKb: Long? = null,
)
@Serializable data class TestReport(
    val name: String, val pass: Boolean, val expected: JsonElement? = null, val actual: JsonElement? = null,
    val error: String? = null, @SerialName("time_ms") val timeMs: Double = 0.0, val hidden: Boolean = false, // harness đo ms thực
)
@Serializable data class Grade(
    val correctness: Int, val readability: Int, val complexity: String,
    @SerialName("complexity_ok") val complexityOk: Boolean,
    @SerialName("best_practices") val bestPractices: Int, val idiomatic: Int,
    val summary: String, val suggestions: List<String> = emptyList(),
)
@Serializable data class Judged(
    val compiled: Boolean,
    @SerialName("compile_output") val compileOutput: String? = null,
    val tests: List<TestReport> = emptyList(),
    val stdout: String = "", val stderr: String = "",
    @SerialName("time_ms") val timeMs: Double = 0.0, @SerialName("memory_kb") val memoryKb: Long? = null,
    val grade: Grade? = null, @SerialName("grade_error") val gradeError: String? = null,
) {
    val allPass: Boolean get() = compiled && tests.isNotEmpty() && tests.all { it.pass }
}
@Serializable data class ExplainFb(
    val correct: Boolean, val score: Int, val missing: List<String> = emptyList(),
    val misconceptions: List<String> = emptyList(), val notes: String = "",
)
@Serializable data class Fix(val wrong: String, val right: String, val why: String = "")
/** Kết quả `feedback` mode writing/speaking (server/functions/feedback/logic.ts, schema WRITE). */
@Serializable data class WriteFb(
    @SerialName("task_done") val taskDone: Boolean, val score: Int, val fixes: List<Fix> = emptyList(), val tone: String = "",
    @SerialName("better_version") val betterVersion: String = "", @SerialName("used_chunks") val usedChunks: List<String> = emptyList(),
    val notes: String = "",
)
@Serializable private data class StarterOut(val code: String)
@Serializable private data class HintOut(val lines: List<String>)

/** Gọi edge function `run-code` / `feedback` (spec §5.3–5.4). Lỗi nào cũng thành câu tiếng Việt, không ném ra UI. */
class CodeApi(private val call: suspend (fn: String, body: JsonObject) -> String) {
    private suspend inline fun <reified T> go(fn: String, body: JsonObject): ApiResult<T> = try {
        ApiResult.Ok(LessonJson.decodeFromString<T>(call(fn, body)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("CodeApi", "$fn ${body["mode"]} lỗi", e)
        ApiResult.Err(errorText(e))
    }

    private fun card(mode: String, lessonId: String, cardKey: String, lang: String, extra: JsonObject = JsonObject(emptyMap())) =
        JsonObject(buildJsonObject { put("mode", mode); put("lesson_id", lessonId); put("card_key", cardKey); put("language", lang) } + extra)

    suspend fun runFree(lang: String, code: String, stdin: String = ""): ApiResult<FreeRun> =
        go("run-code", buildJsonObject { put("mode", "run"); put("language", lang); put("code", code); put("stdin", stdin) })

    suspend fun runTests(lessonId: String, cardKey: String, lang: String, code: String): ApiResult<Judged> =
        go("run-code", card("run", lessonId, cardKey, lang, buildJsonObject { put("code", code) }))

    suspend fun submit(lessonId: String, cardKey: String, lang: String, code: String, hints: Int): ApiResult<Judged> =
        go("run-code", card("submit", lessonId, cardKey, lang, buildJsonObject { put("code", code); put("hints_used", hints) }))

    suspend fun starter(lessonId: String, cardKey: String, lang: String): ApiResult<String> =
        go<StarterOut>("run-code", card("starter", lessonId, cardKey, lang)).map { it.code }

    suspend fun hint(lessonId: String, cardKey: String, lang: String): ApiResult<List<String>> =
        go<HintOut>("run-code", card("hint", lessonId, cardKey, lang)).map { it.lines }

    suspend fun explain(lessonId: String, cardKey: String, text: String): ApiResult<ExplainFb> =
        go("feedback", buildJsonObject { put("mode", "explain"); put("lesson_id", lessonId); put("card_key", cardKey); put("text", text) })

    suspend fun writing(lessonId: String, cardKey: String, text: String, speaking: Boolean): ApiResult<WriteFb> =
        go("feedback", buildJsonObject {
            put("mode", if (speaking) "speaking" else "writing"); put("lesson_id", lessonId); put("card_key", cardKey); put("text", text)
        })
}
