package com.thanhnb.hocmoingay.core.log

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTest {
    private val today = LocalDate.of(2026, 10, 6)
    private fun days(vararg back: Long) = back.map { today.minusDays(it) }.toSet()

    @Test fun chuoiTinhLuiTuHomNay() = assertEquals(3, streak(days(0, 1, 2, 4), today))
    @Test fun homNayChuaHocVanGiuChuoiTuHomQua() = assertEquals(2, streak(days(1, 2), today))
    @Test fun dutMotNgayLaMatChuoi() = assertEquals(0, streak(days(2, 3), today))

    @Test fun ngayTinhChuoiPhaiCoHoatDong() {
        val e = DailyLogEntity(day = "2026-10-06", userId = "u", updatedAt = 1)
        assertFalse(e.active())
        assertTrue(e.copy(reviews = 1).active())
        assertFalse(e.copy(reviews = 1, deleted = true).active())
    }

    @Test fun dongHoPhutGhiPhutNguyenVaLamTronPhanLe() {
        val wrote = mutableListOf<Int>()
        val m = MinuteMeter { _, n -> wrote += n }
        m.add(0, 50_000); m.add(0, 20_000)   // 70 giây: ghi 1, còn 10 giây
        assertEquals(listOf(1), wrote)
        m.flush(0)                            // 10 giây < 30: bỏ
        assertEquals(listOf(1), wrote)
        m.add(0, 40_000); m.flush(0)          // 40 giây ≥ 30: làm tròn lên 1
        assertEquals(listOf(1, 1), wrote)
        m.add(0, 130_000)
        assertEquals(listOf(1, 1, 2), wrote)
    }
}
