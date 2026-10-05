package com.thanhnb.hocmoingay.core.review

import com.thanhnb.hocmoingay.core.review.Rating.AGAIN
import com.thanhnb.hocmoingay.core.review.Rating.EASY
import com.thanhnb.hocmoingay.core.review.Rating.GOOD
import com.thanhnb.hocmoingay.core.review.Rating.HARD
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FsrsTest {
    /** Scheduler() mặc định của py-fsrs: bước học 1 phút, 10 phút; học lại 10 phút. */
    private val py = Fsrs(learningSteps = listOf(1 * MINUTE_MS, 10 * MINUTE_MS), relearningSteps = listOf(10 * MINUTE_MS))
    private val t0 = Instant.parse("2022-11-29T12:30:00Z").toEpochMilli()

    @Test fun lichSuKhoangCachKhopPyFsrs() {
        val ratings = listOf(GOOD, GOOD, GOOD, GOOD, GOOD, GOOD, AGAIN, AGAIN, GOOD, GOOD, GOOD, GOOD, GOOD)
        var m = Memo()
        var t = t0
        val ivl = mutableListOf<Long>()
        for (r in ratings) {
            m = py.review(m, r, t)
            ivl += wholeDays(m.due - m.lastReview!!)
            t = m.due
        }
        assertEquals(listOf<Long>(0, 2, 11, 46, 163, 498, 0, 0, 2, 4, 7, 12, 21), ivl)
    }

    @Test fun stabilityDifficultyKhopPyFsrs() {
        var m = Memo()
        var t = t0
        for ((r, days) in listOf(AGAIN to 0, GOOD to 0, GOOD to 1, GOOD to 3, GOOD to 8, GOOD to 21)) {
            t += days * DAY_MS
            m = py.review(m, r, t)
        }
        assertEquals(53.62691, m.stability!!, 1e-4)
        assertEquals(6.3574867, m.difficulty!!, 1e-4)
    }

    @Test fun deNhieuLanThiDifficultyVeMot() {
        var m = Memo()
        repeat(10) { i -> m = py.review(m, EASY, t0 + i) }
        assertEquals(1.0, m.difficulty!!, 0.0)
    }

    @Test fun khongCoBuocHocThiVaoReviewItNhatMotNgay() {
        val m = Fsrs().review(Memo(), AGAIN, t0)
        assertEquals(CardState.REVIEW, m.state)
        assertTrue(m.due - t0 >= DAY_MS)
    }

    @Test fun quenOBuocHocThiQuayLaiSauMotPhut() {
        val m = py.review(Memo(), AGAIN, t0)
        assertEquals(CardState.LEARNING, m.state)
        assertEquals(0, m.step)
        assertEquals(t0 + MINUTE_MS, m.due)
    }

    @Test fun khoKhiChiCoMotBuocHocThiGapRuoi() {
        val m = Fsrs(learningSteps = listOf(10 * MINUTE_MS)).review(Memo(), HARD, t0)
        assertEquals(t0 + 15 * MINUTE_MS, m.due)
    }

    @Test fun appXemTruocBonNutChoTheMoi() {
        val p = AppFsrs.preview(Memo(due = t0), t0)
        assertEquals(mapOf(AGAIN to 1L, HARD to 1L, GOOD to 2L, EASY to 8L), p.mapValues { wholeDays(it.value.due - t0) })
        assertTrue(p.values.all { it.state == CardState.REVIEW && it.step == null })
    }

    @Test fun chuKhoangCach() {
        assertEquals("1 phút", spanText(30_000))
        assertEquals("10 phút", spanText(10 * MINUTE_MS))
        assertEquals("3 giờ", spanText(3 * 60 * MINUTE_MS))
        assertEquals("2 ngày", spanText(2 * DAY_MS))
        assertEquals("2 tháng", spanText(45 * DAY_MS))
        assertEquals("1,5 năm", spanText(548 * DAY_MS))
    }
}
