package com.thanhnb.hocmoingay.core.speech

import android.speech.SpeechRecognizer
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SttTest {
    @Test fun khongNgheThayGiKhongPhaiLoi() {
        assertNull(sttError(SpeechRecognizer.ERROR_NO_MATCH))
        assertNull(sttError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT))
    }

    @Test fun loiThanhCauTiengViet() {
        assertTrue("quyền" in sttError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)!!)
        assertTrue("mạng" in sttError(SpeechRecognizer.ERROR_NETWORK)!!)
        assertTrue("mã 99" in sttError(99)!!)
    }

    @Test fun thieuGoiTiengAnhNoiRoNguyenNhan() {
        assertTrue("tiếng Anh" in sttError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)!!)
        assertTrue("tiếng Anh" in sttError(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED)!!)
    }
}
