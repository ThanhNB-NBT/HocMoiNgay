package com.thanhnb.hocmoingay.feature.mock

import com.thanhnb.hocmoingay.core.lesson.parseLesson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MockScoreTest {
    @Test fun toeicMoiPhan5Den495() {
        assertEquals(495, toeicScaled(100, 100, reading = false))
        assertEquals(495, toeicScaled(100, 100, reading = true))
        assertEquals(5, toeicScaled(0, 100, reading = false))
        assertEquals(255, toeicScaled(50, 100, reading = false))
        assertEquals(220, toeicScaled(50, 100, reading = true)) // Reading khó lấy điểm hơn ở cùng số câu đúng
        assertEquals(475, toeicScaled(95, 100, reading = false))
        assertEquals(toeicScaled(50, 100, false), toeicScaled(10, 20, false)) // đề ngắn quy tỉ lệ về 100 câu
        for (raw in 0..100) {
            val l = toeicScaled(raw, 100, false)
            assert(l in 5..495 && l % 5 == 0) { "L $raw → $l" }
            if (raw > 0) assert(l >= toeicScaled(raw - 1, 100, false)) { "L không giảm dần" }
        }
        assertEquals(5, toeicScaled(3, 0, false))
    }

    @Test fun ieltsListeningVaHaiBangReading() {
        assertEquals(7.0, ieltsBand(30, 40, bandTable("ielts_academic", "listening")), 0.0)
        assertEquals(7.0, ieltsBand(30, 40, bandTable("ielts_general", "listening")), 0.0) // Listening dùng chung
        assertEquals(7.0, ieltsBand(30, 40, bandTable("ielts_academic", "reading")), 0.0)
        assertEquals(6.0, ieltsBand(30, 40, bandTable("ielts_general", "reading")), 0.0) // General khó lấy band hơn
        assertEquals(7.5, ieltsBand(33, 40, IELTS_ACADEMIC_READING), 0.0)
        assertEquals(7.5, ieltsBand(32, 40, IELTS_LISTENING), 0.0)
        assertEquals(8.5, ieltsBand(39, 40, IELTS_GENERAL_READING), 0.0)
        assertEquals(9.0, ieltsBand(40, 40, IELTS_GENERAL_READING), 0.0)
        assertEquals(0.0, ieltsBand(0, 40, IELTS_LISTENING), 0.0)
        assertEquals(7.0, ieltsBand(15, 20, IELTS_LISTENING), 0.0) // 15/20 ≈ 30/40
        assertEquals("Band 6.0", scaledLabel("ielts_general", "reading", 30, 40))
        assertEquals("255/495", scaledLabel("toeic_lr", "listening", 50, 100))
    }

    @Test fun chamDeMau() {
        val body = parseLesson(File("src/debug/assets/showcase/en-thi-thu.json").readText()) { error(it) }!!
        val parts = partCards(body)
        assertEquals(listOf(2, 3), parts.map { it.second.size })
        assertEquals(listOf(4, 4), parts.map { (_, cs) -> cs.sumOf { mockQuestions(it).size } })
        val answers = mapOf(
            "l_hoi_thoai#0" to "1", "l_dien#0" to "saturday", "l_dien#1" to "9", "l_dien#2" to "40",   // Listening đủ 4
            "r_doan#0" to "0", "r_doan#1" to "1", "r_tom_tat#0" to "Expensive.",                       // Reading sai câu 2, bỏ trống câu cuối
        )
        val r = mockResults(body, answers)
        assertEquals(listOf(4 to 4, 2 to 4), r.map { it.correct to it.total })
        assertEquals(listOf("Band 9.0", "Band 5.5"), r.map { it.label }) // 4/4 → 40/40; 2/4 → 20/40 Academic
        assertEquals(75, mockPercent(r))
        assertNull(toeicTotal("ielts_academic", r))
        assertEquals(255 + 220, toeicTotal("toeic_lr", r.map { it.copy(correct = 2) }))
    }

    @Test fun cauNhieuDapAnVaBoTrong() {
        val q = MockQ("q#0", "?", listOf("a", "b", "c"), listOf(0, 2))
        assertEquals(true, q.ok("2,0"))
        assertEquals(false, q.ok("0"))
        assertEquals(false, q.ok(null))
        assertEquals("a; c", q.right)
        assertEquals("(bỏ trống)", q.shown(""))
    }
}
