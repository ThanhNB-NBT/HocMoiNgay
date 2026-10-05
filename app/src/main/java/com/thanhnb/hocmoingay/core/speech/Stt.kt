package com.thanhnb.hocmoingay.core.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
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
    else -> "Không nhận dạng được giọng nói (mã $code)."
}

/** SpeechRecognizer `en-US`, ưu tiên offline (spec §7.2). Mọi hàm gọi trên main thread. */
class Stt(private val ctx: Context) {
    class Listening(val stop: () -> Unit, val cancel: () -> Unit)

    private val main = Handler(Looper.getMainLooper())

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(ctx)

    /**
     * Nghe một lượt tới khi người học ngừng nói. [Listening.stop] giao phần đã nghe; [Listening.cancel] bỏ, không gọi [onDone].
     * Ưu tiên offline; máy chưa tải gói tiếng Anh offline thì báo ERROR_LANGUAGE_UNAVAILABLE, khi đó nghe lại một lần không ép offline.
     */
    fun listen(onPartial: (String) -> Unit, onDone: (Result<String>) -> Unit): Listening {
        var r: SpeechRecognizer? = null
        var finished = false
        fun close(x: SpeechRecognizer) = main.post { x.destroy() } // không destroy ngay trong callback của chính nó
        fun finish(res: Result<String>) {
            if (finished) return
            finished = true
            r?.let(::close)
            onDone(res)
        }
        fun start(offline: Boolean) {
            val x = SpeechRecognizer.createSpeechRecognizer(ctx)
            r = x
            x.setRecognitionListener(object : RecognitionListener {
                override fun onResults(b: Bundle?) = finish(Result.success(b.best()))
                override fun onPartialResults(b: Bundle?) = onPartial(b.best())
                override fun onError(error: Int) {
                    if (offline && error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE && !finished) {
                        close(x)
                        main.post { if (!finished) start(offline = false) }
                        return
                    }
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
        start(offline = true)
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
