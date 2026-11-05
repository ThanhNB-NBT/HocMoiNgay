package com.thanhnb.hocmoingay.core.speech

import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

fun ttsStatusOf(init: Int, lang: Int): Tts.Status = when {
    init != TextToSpeech.SUCCESS -> Tts.Status.UNAVAILABLE
    lang == TextToSpeech.LANG_MISSING_DATA -> Tts.Status.MISSING_DATA
    lang == TextToSpeech.LANG_NOT_SUPPORTED -> Tts.Status.UNAVAILABLE
    else -> Tts.Status.READY
}

/** Một TextToSpeech cho cả app (`Locale.US`). Tốc độ chọn một lần dùng cho mọi thẻ, nhớ qua lần mở app. Tạo trên main thread. */
class Tts(ctx: Context) {
    enum class Status { LOADING, READY, MISSING_DATA, UNAVAILABLE }

    private val _status = MutableStateFlow(Status.LOADING)
    val status = _status.asStateFlow()
    private val _speaking = MutableStateFlow(false)
    val speaking = _speaking.asStateFlow()
    /** Ai đang phát: thẻ bị huỷ (pager bỏ trang cũ) chỉ được dừng tiếng của chính nó. */
    private var owner: Any? = null
    private lateinit var engine: TextToSpeech
    private val prefs = ctx.applicationContext.getSharedPreferences("tts", Context.MODE_PRIVATE)
    private val _rate = MutableStateFlow(prefs.getFloat("rate", DEFAULT_RATE))
    val rate = _rate.asStateFlow()

    init {
        engine = TextToSpeech(ctx.applicationContext) { code ->
            _status.value = ttsStatusOf(code, if (code == TextToSpeech.SUCCESS) engine.setLanguage(Locale.US) else 0)
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _speaking.value = true }
            override fun onDone(utteranceId: String?) { _speaking.value = false }
            @Deprecated("bắt buộc override") override fun onError(utteranceId: String?) { _speaking.value = false }
            override fun onStop(utteranceId: String?, interrupted: Boolean) { _speaking.value = false }
        })
    }

    fun setRate(r: Float) {
        _rate.value = r
        prefs.edit().putFloat("rate", r).apply()
    }

    fun speak(text: String, owner: Any) {
        if (_status.value != Status.READY || text.isBlank()) return
        this.owner = owner
        engine.setSpeechRate(_rate.value)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u${System.nanoTime()}")
    }

    /**
     * Hội thoại nhiều người: mỗi người (theo thứ tự xuất hiện) một giọng tiếng Anh khác và một cao độ khác,
     * nên máy chỉ có một giọng vẫn phân biệt được. Giọng/cao độ được chép vào từng câu lúc xếp hàng, nên trả lại mặc định ngay sau đó.
     */
    fun speakTurns(turns: List<Pair<String, String>>, owner: Any) {
        if (_status.value != Status.READY || turns.isEmpty()) return
        this.owner = owner
        engine.setSpeechRate(_rate.value)
        val base = engine.voice
        val voices = runCatching {
            engine.voices.orEmpty().filter {
                it.locale.language == "en" && !it.isNetworkConnectionRequired && TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features
            }.sortedBy { it.name }.filter { it != base }
        }.getOrDefault(emptyList())
        speakerSlots(turns.map { it.first }).forEachIndexed { i, slot ->
            if (slot == 0 || voices.isEmpty()) base?.let { engine.voice = it } else engine.voice = voices[(slot - 1) % voices.size]
            engine.setPitch(PITCHES[slot % PITCHES.size])
            engine.speak(turns[i].second, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "u${System.nanoTime()}")
        }
        base?.let { engine.voice = it }
        engine.setPitch(1f)
    }

    fun stop(owner: Any) {
        if (this.owner !== owner) return
        engine.stop()
        _speaking.value = false
    }

    /** Máy thiếu giọng tiếng Anh: mở màn cài dữ liệu giọng của bộ đọc. */
    fun installVoice(ctx: Context): Boolean = runCatching {
        ctx.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

/** Mỗi câu thuộc người thứ mấy (0, 1, 2… theo thứ tự xuất hiện): người 0 dùng giọng mặc định. */
fun speakerSlots(who: List<String>): List<Int> {
    val order = who.distinct()
    return who.map { order.indexOf(it) }
}

private val PITCHES = floatArrayOf(1f, 0.82f, 1.22f, 0.92f)

/** 1,0 của bộ đọc Android nhanh với người mới học; mặc định chậm hơn. */
const val DEFAULT_RATE = 0.75f

val LocalTts = staticCompositionLocalOf<Tts?> { null }
