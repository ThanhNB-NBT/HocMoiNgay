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

/** 1,0 của bộ đọc Android nhanh với người mới học; mặc định chậm hơn. */
const val DEFAULT_RATE = 0.75f

val LocalTts = staticCompositionLocalOf<Tts?> { null }
