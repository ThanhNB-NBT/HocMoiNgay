package com.thanhnb.hocmoingay.core.speech

import android.speech.tts.TextToSpeech
import org.junit.Assert.assertEquals
import org.junit.Test

class TtsTest {
    @Test fun mucDoTtsTheoKetQuaSetLanguage() {
        assertEquals(Tts.Status.READY, ttsStatusOf(TextToSpeech.SUCCESS, TextToSpeech.LANG_COUNTRY_AVAILABLE))
        assertEquals(Tts.Status.MISSING_DATA, ttsStatusOf(TextToSpeech.SUCCESS, TextToSpeech.LANG_MISSING_DATA))
        assertEquals(Tts.Status.UNAVAILABLE, ttsStatusOf(TextToSpeech.SUCCESS, TextToSpeech.LANG_NOT_SUPPORTED))
        assertEquals(Tts.Status.UNAVAILABLE, ttsStatusOf(TextToSpeech.ERROR, 0)) // máy không có bộ đọc
    }
}
