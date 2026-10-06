package com.thanhnb.hocmoingay.core.speech

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** Lỗi SpeechRecognizer → câu tiếng Việt. null = không nghe thấy gì (coi như nói rỗng, không phải lỗi). */
fun sttError(code: Int): String? = when (code) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> null
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Chưa cấp quyền micro cho app."
    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Nhận dạng giọng nói trên máy này cần mạng. Kiểm tra mạng rồi thử lại."
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Bộ nhận dạng đang bận. Thử lại sau giây lát."
    SpeechRecognizer.ERROR_AUDIO -> "Không thu được tiếng từ micro."
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
        "Máy chưa có gói nhận dạng tiếng Anh. Kết nối mạng rồi thử lại, hoặc tải gói tiếng Anh trong cài đặt giọng nói của máy."
    else -> "Không nhận dạng được giọng nói (mã $code). Cài app Google hoặc \"Dịch vụ lời nói của Google\" từ CH Play rồi thử lại."
}

/** Lỗi nên thử bộ nhận dạng khác thay vì báo ngay: không nghe thấy và thiếu quyền thì đổi bộ cũng vô ích. */
fun sttRetryable(code: Int) = code != SpeechRecognizer.ERROR_NO_MATCH && code != SpeechRecognizer.ERROR_SPEECH_TIMEOUT &&
    code != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS

/**
 * SpeechRecognizer `en-US`. Mọi hàm gọi trên main thread.
 * Máy Xiaomi/Oppo… hay đặt bộ nhận dạng mặc định của hãng: không có tiếng Anh, hoặc ép offline thì báo lỗi lạ
 * (mã 11 SERVER_DISCONNECTED, 13 LANGUAGE_UNAVAILABLE). Nên thử lần lượt: mặc định offline → mặc định online → từng bộ khác cài trên máy (Google).
 */
class Stt(private val ctx: Context) {
    class Listening(val stop: () -> Unit, val cancel: () -> Unit)

    private val main = Handler(Looper.getMainLooper())

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(ctx)

    /** Bộ nhận dạng cài trên máy, bộ Google lên trước. Cần `<queries>` RecognitionService trong manifest. */
    private fun services(): List<ComponentName> =
        ctx.packageManager.queryIntentServices(Intent(RecognitionService.SERVICE_INTERFACE), 0)
            .map { ComponentName(it.serviceInfo.packageName, it.serviceInfo.name) }
            .sortedByDescending { it.packageName.startsWith("com.google.") }

    /** Bộ nhận dạng lỗi thì lần sau bắt đầu luôn từ bộ chạy được, khỏi chờ thử lại mỗi lượt nói. */
    private var good: Pair<ComponentName?, Boolean>? = null

    /** Nghe một lượt tới khi người học ngừng nói. [Listening.stop] giao phần đã nghe; [Listening.cancel] bỏ, không gọi [onDone]. */
    fun listen(onPartial: (String) -> Unit, onDone: (Result<String>) -> Unit): Listening {
        // (component, offline); null = bộ mặc định của máy
        val plan = (listOfNotNull(good) + listOf<Pair<ComponentName?, Boolean>>(null to true, null to false) + services().map { it to false }).distinct()
        var r: SpeechRecognizer? = null
        var finished = false
        fun close(x: SpeechRecognizer) = main.post { x.destroy() } // không destroy ngay trong callback của chính nó
        fun finish(res: Result<String>) {
            if (finished) return
            finished = true
            r?.let(::close)
            onDone(res)
        }
        fun start(i: Int) {
            val (comp, offline) = plan[i]
            val x = runCatching { if (comp == null) SpeechRecognizer.createSpeechRecognizer(ctx) else SpeechRecognizer.createSpeechRecognizer(ctx, comp) }
                .getOrElse { if (i + 1 < plan.size) return start(i + 1) else return finish(Result.failure(Exception(sttError(SpeechRecognizer.ERROR_CLIENT)))) }
            r = x
            x.setRecognitionListener(object : RecognitionListener {
                override fun onResults(b: Bundle?) { good = plan[i]; finish(Result.success(b.best())) }
                override fun onPartialResults(b: Bundle?) = onPartial(b.best())
                override fun onError(error: Int) {
                    if (finished) return
                    if (sttRetryable(error) && i + 1 < plan.size) {
                        if (good == plan[i]) good = null
                        close(x)
                        main.postDelayed({ if (!finished) start(i + 1) }, 200) // thả micro cho bộ trước rồi mới mở bộ sau
                        return
                    }
                    if (!sttRetryable(error)) good = plan[i]
                    finish(sttError(error)?.let { Result.failure(Exception(it)) } ?: Result.success(""))
                }
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            x.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, offline)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true),
            )
        }
        start(0)
        return Listening(
            stop = { if (!finished) r?.stopListening() },
            cancel = { if (!finished) { finished = true; r?.let { it.cancel(); close(it) } } },
        )
    }

    private fun Bundle?.best(): String = this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
}

/** Một lần nói có thể gồm nhiều lượt nhận dạng (timed_talk, speak_free): hết một lượt mà còn giờ thì nghe tiếp. */
class Talk(private val stt: Stt) {
    var text by mutableStateOf("")
        private set
    var partial by mutableStateOf("")
        private set
    var on by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val shown: String get() = "$text $partial".trim()
    private var cur: Stt.Listening? = null
    private var until = 0L

    fun available() = stt.available()

    /** [maxMs] = 0: chỉ một lượt (speak, shadow). */
    fun start(maxMs: Long = 0) {
        text = ""; partial = ""; error = null
        until = SystemClock.elapsedRealtime() + maxMs
        on = true
        round()
    }

    private fun round() {
        cur = stt.listen(onPartial = { partial = it }) { r ->
            partial = ""
            r.onSuccess { if (it.isNotBlank()) text = "$text $it".trim() }.onFailure { error = it.message; on = false }
            if (on && SystemClock.elapsedRealtime() < until) round() else on = false
        }
    }

    /** Dừng và giữ phần đã nghe. */
    fun stop() { on = false; cur?.stop?.invoke() }

    fun cancel() { on = false; cur?.cancel?.invoke() }
}

val LocalStt = staticCompositionLocalOf<Stt?> { null }

/** null khi không có LocalStt (preview). Rời card thì huỷ lượt nghe đang chạy. */
@Composable
fun rememberTalk(): Talk? {
    val stt = LocalStt.current ?: return null
    val t = remember { Talk(stt) }
    DisposableEffect(Unit) { onDispose { t.cancel() } }
    return t
}
