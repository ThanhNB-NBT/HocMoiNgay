package com.thanhnb.hocmoingay.feature.profile

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileStatsTest {
    @Test fun luoiMuoiHaiTuanCotCuoiChuaHomNay() {
        val today = LocalDate.of(2026, 10, 7) // Thứ Tư
        val g = heatmap(mapOf(today to 45, today.minusDays(1) to 5), today)
        assertEquals(12, g.size)
        assertEquals(3, g.last()[2]) // Thứ Tư: 45 XP → mức 3
        assertEquals(1, g.last()[1])
        assertNull(g.last()[3])      // Thứ Năm chưa tới
        assertEquals(0, g.first()[0])
    }

    @Test fun nhanThangODauCotCoMungMot() {
        // 12 tuần tới 7/10/2026, cột 0 bắt đầu Thứ Hai 20/7
        val m = heatMonths(LocalDate.of(2026, 10, 7))
        assertEquals(12, m.size)
        assertNull(m[0])             // cột 1 đã có nhãn Th8 nên cột 0 để trống
        assertEquals("Th8", m[1])    // 27/7–2/8
        assertEquals("Th9", m[6])    // 31/8–6/9
        assertEquals("Th10", m[10])  // 28/9–4/10
        assertEquals(3, m.count { it != null })
    }

    @Test fun congMachBayNgayGanNhat() {
        fun log(day: String, s: String) = DailyLogEntity(day = day, userId = "u", strands = s, updatedAt = 1)
        val m = weekStrands(
            listOf(log("2026-09-30", """{"input":9}"""), log("2026-10-01", """{"input":1.5,"output":2}"""), log("2026-10-07", """{"input":0.5}""")),
            LocalDate.of(2026, 10, 7),
        )
        assertEquals(2.0, m.getValue("input"), 1e-9)
        assertEquals(2.0, m.getValue("output"), 1e-9)
        assertEquals(0.0, m.getValue("fluency"), 1e-9)
    }

    @Test fun ghiChuKhiMachDuoiMuoiLamPhanTram() {
        assertNull(strandNote(STRANDS.associateWith { 0.0 }))
        assertNull(strandNote(STRANDS.associateWith { 1.0 }))
        val n = strandNote(mapOf("input" to 5.0, "output" to 4.0, "language" to 10.0, "fluency" to 1.0))!!
        assertTrue(n, n.startsWith("Lưu loát mới chiếm 5%"))
        // 14,6% không được hiện thành "15%" (mâu thuẫn với ngưỡng dưới 15%)
        assertTrue(strandNote(mapOf("input" to 30.0, "output" to 14.6, "language" to 30.0, "fluency" to 25.4))!!.contains("14%"))
    }
}
