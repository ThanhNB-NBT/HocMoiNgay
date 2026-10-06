package com.thanhnb.hocmoingay.feature.reminder

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class RemindersTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private fun at(h: Int, m: Int) = ZonedDateTime.of(2026, 10, 6, h, m, 0, 0, zone)

    @Test fun choToiGioKeTiepTrongNgay() = assertEquals(Duration.ofMinutes(30), delayTo("20:30", at(20, 0)))

    @Test fun dungGioHoacQuaGioThiSangMai() {
        assertEquals(Duration.ofHours(24), delayTo("20:30", at(20, 30)))
        assertEquals(Duration.ofMinutes(23 * 60 + 30), delayTo("20:30", at(21, 0)))
    }

    @Test fun gioBoKhoiCaiDatThiHuy() =
        assertEquals(setOf("07:00"), staleTimes(listOf("07:00", "20:30"), listOf("20:30", "21:00")))
}
