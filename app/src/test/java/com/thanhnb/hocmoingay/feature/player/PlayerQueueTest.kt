package com.thanhnb.hocmoingay.feature.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerQueueTest {
    @Test fun saiThiDayXuongCuoi() {
        val q = PlayerQueue(3).answer(ok = false).next()
        assertEquals(listOf(0, 1, 2, 0), q.order)
        assertEquals(1, q.current)
        assertEquals(0f, q.progress)
    }

    @Test fun diemChiTinhLanDau() {
        var q = PlayerQueue(2).answer(false).next() // card 0 sai
        q = q.answer(true).next()                    // card 1 đúng
        q = q.answer(true).next()                    // card 0 làm lại đúng
        assertTrue(q.finished)
        assertEquals(50, q.score)
        assertEquals(1f, q.progress)
    }

    @Test fun cardKhongChamKhongVaoDiem() {
        val q = PlayerQueue(2).answer(true, graded = false).next().answer(true).next()
        assertEquals(100, q.score)
    }

    @Test fun traLoiHaiLanCungViTriChiTinhLanDau() {
        val q = PlayerQueue(1).answer(false).answer(true)
        assertEquals(listOf(0, 0), q.order)
        assertEquals(false, q.results[0])
    }

    @Test fun baiRongLaXong() {
        assertTrue(PlayerQueue(0).finished)
        assertEquals(100, PlayerQueue(0).score)
    }
}
