package com.thanhnb.hocmoingay.core.review

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRepoTest {
    private val cards = mutableMapOf<String, ReviewCardEntity>()
    private val logs = mutableMapOf<String, DailyLogEntity>()
    private val clock = Instant.parse("2026-10-05T03:00:00Z").toEpochMilli()
    private val log = DailyLogRepo(
        get = { logs[it] }, put = { logs[it.day] = it }, tx = { it() }, userId = { "u1" }, afterWrite = {},
        zone = { ZoneId.of("Asia/Ho_Chi_Minh") },
    )
    private val repo = ReviewRepo(
        due = { now, limit -> cards.values.filter { !it.deleted && it.kind == "recall" && it.due <= now }.sortedBy { it.due }.take(limit) },
        byIds = { ids -> ids.mapNotNull { cards[it] } },
        put = { cards[it.id] = it }, log = log, tx = { it() }, afterWrite = {}, now = { clock },
    )

    private fun card(id: String, due: Long) = ReviewCardEntity(
        id = id, userId = "u1", ref = "l#$id", kind = "recall", track = "english", courseId = "c", due = due, updatedAt = 5,
    )

    @Test fun nhoTheMoiThiSangReviewHaiNgay() = runTest {
        cards["a"] = card("a", clock)
        repo.rate("a", Rating.GOOD)
        val a = cards.getValue("a")
        assertEquals(CardState.REVIEW, a.state)
        assertEquals(1, a.reps)
        assertEquals(clock + 2 * DAY_MS, a.due)
        assertEquals(clock, a.lastReview)
        assertEquals(2, a.scheduledDays)
        assertTrue(a.dirty)
        assertTrue(a.updatedAt > 5)
        assertEquals(1, logs.getValue("2026-10-05").reviews)
    }

    @Test fun quenTheDangReviewThiTangLapses() = runTest {
        cards["a"] = card("a", clock).copy(
            state = CardState.REVIEW, reps = 3, stability = 10.0, difficulty = 5.0, lastReview = clock - 10 * DAY_MS,
        )
        repo.rate("a", Rating.AGAIN)
        val a = cards.getValue("a")
        assertEquals(1, a.lapses)
        assertEquals(CardState.REVIEW, a.state) // app không có bước học lại
        assertEquals(clock + DAY_MS, a.due)
        assertEquals(10, a.elapsedDays)
    }

    @Test fun theKhongCoHoacDaXoaThiKhongGhi() = runTest {
        cards["b"] = card("b", clock).copy(deleted = true)
        repo.rate("x", Rating.GOOD)
        repo.rate("b", Rating.GOOD)
        assertEquals(0, cards.getValue("b").reps)
        assertTrue(logs.isEmpty())
    }

    @Test fun chiLayTheDenHan() = runTest {
        cards["a"] = card("a", clock - 1)
        cards["b"] = card("b", clock + DAY_MS)
        assertEquals(listOf("a"), repo.dueCards().map { it.id })
    }

    @Test fun xemTruocBonNut() {
        val p = repo.preview(card("a", clock))
        assertEquals(mapOf(Rating.AGAIN to 1L, Rating.HARD to 1L, Rating.GOOD to 2L, Rating.EASY to 8L), p.mapValues { wholeDays(it.value) })
    }
}
