package com.thanhnb.hocmoingay.core.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    @Test fun noiDungHetThi100BoHoaThuongVaDauCau() {
        val s = scoreSpeech("I'm blocked by the staging server.", "i'm blocked by the staging server")
        assertEquals(100, s.percent)
        assertTrue(s.marks.all { it.ok })
    }

    @Test fun thieuTuThiDanhDauTuThieuVaGiuChuGoc() {
        val s = scoreSpeech("I'm blocked by the staging server.", "I'm blocked the server")
        assertEquals(listOf(true, true, false, true, false, true), s.marks.map { it.ok })
        assertEquals("server.", s.marks.last().text) // hiện đúng chữ của câu mẫu, kể cả dấu câu
        assertEquals(66, s.percent)
    }

    @Test fun noiThemTuKhongTruDiem() {
        assertEquals(100, scoreSpeech("Wrap up the tests.", "okay so wrap up the tests today").percent)
    }

    @Test fun daoThuTuThiChiTinhChuoiConChungDaiNhat() {
        assertEquals(50, scoreSpeech("review the pull request", "pull request review the").percent)
    }

    @Test fun cauMauKhongCoTuThi0() {
        assertEquals(0, scoreSpeech("—", "anything").percent)
    }

    @Test fun cumMustUseKhopTienToVoiTuDai() {
        assertTrue(phraseUsed("fix a bug", "Yesterday I fixed a bug"))
        assertTrue(phraseUsed("on track", "We are on track now."))
        assertFalse(phraseUsed("wrap up", "I wrapped it up"))       // phải liền nhau
        assertFalse(phraseUsed("fix a bug", "I fixed about bugs"))  // từ ngắn "a" phải khớp đúng
        assertFalse(phraseUsed("fix a bug", "fix"))
    }

    @Test fun tuMoiPhut() {
        assertEquals(120, wordsPerMinute("one two three four five six", 3))
        assertEquals(0, wordsPerMinute("", 60))
    }
}
