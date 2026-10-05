package com.thanhnb.hocmoingay.feature.today

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TodayTest {
    @Test fun loiChaoTheoGio() {
        assertEquals("Chào buổi sáng", greeting(5))
        assertEquals("Chào buổi sáng", greeting(10))
        assertEquals("Chào buổi trưa", greeting(11))
        assertEquals("Chào buổi chiều", greeting(13))
        assertEquals("Chào buổi tối", greeting(18))
        assertEquals("Khuya rồi đó", greeting(23))
        assertEquals("Khuya rồi đó", greeting(2))
    }

    @Test fun ngayTiengViet() {
        assertEquals("Thứ Hai, 5 tháng 10", vnDate(LocalDate.of(2026, 10, 5)))
        assertEquals("Chủ Nhật, 11 tháng 10", vnDate(LocalDate.of(2026, 10, 11)))
    }
}
