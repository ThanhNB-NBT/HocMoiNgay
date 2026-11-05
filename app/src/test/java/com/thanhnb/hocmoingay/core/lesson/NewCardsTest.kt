package com.thanhnb.hocmoingay.core.lesson

import com.thanhnb.hocmoingay.core.net.Criterion
import com.thanhnb.hocmoingay.core.net.WriteFb
import com.thanhnb.hocmoingay.core.speech.speakerSlots
import com.thanhnb.hocmoingay.feature.player.cards.numberedBlanks
import com.thanhnb.hocmoingay.feature.player.cards.scoreLabel
import com.thanhnb.hocmoingay.feature.player.cards.wordCount
import com.thanhnb.hocmoingay.feature.player.gapOk
import com.thanhnb.hocmoingay.feature.player.listenTurns
import com.thanhnb.hocmoingay.feature.player.strandOf
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Đợt 1 (IELTS/TOEIC): bài mẫu content/_sample/_sample-en sau push.py, chép vào assets showcase của bản debug. */
class NewCardsTest {
    private fun sample(name: String) = parseLesson(File("src/debug/assets/showcase/$name.json").readText()) { error(it) }!!

    @Test fun docTheMoi() {
        val l = sample("en-the-moi")
        assertEquals(listOf("Listen", "Gap", "Quiz", "FreeText", "SpeakFree", "Shadow"), l.cards.map { it::class.simpleName })
        val listen = l.cards[0] as Listen
        assertNull(listen.text)
        assertEquals(listOf("Receptionist", "Tom", "Receptionist"), listenTurns(listen.turns).map { it.first })
        val gap = l.cards[1] as Gap
        assertEquals(listOf(listOf("Baker"), listOf("Monday", "Mon")), gap.answers)
        assertEquals("language", strandOf(gap))
        val img = (l.cards[2] as Quiz).image!!
        assertEquals("assets/_sample-en/bieu-do-mau.png", img.asset)
        assertTrue(img.credit.isNotBlank())
        assertEquals("ielts_writing", (l.cards[3] as FreeText).rubric)
        val sp = l.cards[4] as SpeakFree
        assertEquals(listOf(60, 120, 0), listOf(sp.prepSec, sp.speakSec, if (sp.image == null) 0 else 1))
    }

    @Test fun docDeThiThu() {
        val l = sample("en-thi-thu")
        assertEquals("mock" to "ielts_academic", l.kind to l.exam)
        assertEquals(listOf(MockPart("Listening", "listening", 4, 2), MockPart("Reading", "reading", 5, 3)), l.parts)
        assertEquals(5, l.cards.size)
    }

    @Test fun bangCuDocDuocBaiCu() {
        // bài không có trường mới vẫn đọc như trước: không ảnh, không turns, không đồng hồ
        val l = parseLesson("""{"title":"t","cards":[{"key":"a","type":"speak_free","prompt":"p"},{"key":"b","type":"listen","text":"Hi"}]}""") { }!!
        val sp = l.cards[0] as SpeakFree
        assertEquals(0 to 0, sp.prepSec to sp.speakSec)
        assertTrue((l.cards[1] as Listen).turns.isEmpty())
        assertTrue(l.parts.isEmpty())
    }

    @Test fun gapKhongPhanBietHoaThuongVaDauCau() {
        val a = listOf(listOf("Monday", "Mon"), listOf("part-time"))
        assertTrue(gapOk(listOf(" monday. ", "Part time"), a))
        assertTrue(gapOk(listOf("MON", "part-time!"), a))
        assertFalse(gapOk(listOf("Tuesday", "part-time"), a))
        assertFalse(gapOk(listOf("Monday"), a))
        assertEquals("Name: (3) ______ Day: (4) ______", numberedBlanks("Name: ___ Day: ___", 3))
    }

    @Test fun moiNguoiMotGiong() {
        assertEquals(listOf(0, 1, 0, 2, 1), speakerSlots(listOf("A", "B", "A", "C", "B")))
    }

    @Test fun nhanDiemTheoRubric() {
        val fb = WriteFb(taskDone = true, score = 7)
        assertEquals("7/10", scoreLabel(fb, "viết lịch sự"))
        assertEquals("Band 6.5", scoreLabel(fb.copy(overall = 6.5, criteria = listOf(Criterion("TA", 6.5))), "ielts_writing"))
        assertEquals("Band 7.0", scoreLabel(fb.copy(overall = 7.0), "ielts_speaking"))
        assertEquals("160/200", scoreLabel(fb.copy(overall = 160.0), "toeic_writing"))
        assertEquals(4, wordCount("  The chart  shows\nvisitors. "))
        assertEquals(0, wordCount("   "))
    }
}
