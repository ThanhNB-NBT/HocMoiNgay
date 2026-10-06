package com.thanhnb.hocmoingay.feature.today

import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayPlanTest {
    private fun card(id: String, course: String, due: Long) =
        ReviewCardEntity(id = id, userId = "u", ref = "l#$id", kind = "recall", track = "code", courseId = course, due = due, updatedAt = 1)
    private fun pick(kind: PickKind, id: String, min: Int = 5, track: String = "code") = Pick(kind, track, id, id, min)
    private fun first(i: TodayInput) = (planToday(i).single().item as Pick).lessonId

    @Test fun xenKeKhongDeHaiTheCungKhoaLienNhau() {
        val out = interleave(listOf(card("a1", "A", 1), card("a2", "A", 2), card("a3", "A", 3), card("b1", "B", 4), card("b2", "B", 5)))
        assertEquals(listOf("a1", "b1", "a2", "b2", "a3"), out.map { it.id })
    }

    @Test fun hetCachThiDanhDungLien() {
        val out = interleave(listOf(card("a1", "A", 1), card("a2", "A", 2), card("a3", "A", 3), card("b1", "B", 9)))
        assertEquals(listOf("a1", "b1", "a2", "a3"), out.map { it.id })
    }

    @Test fun layToiDaHaiLanDTheQuaHanLauNhat() {
        val due = (1..30).map { card("c$it", "K${it % 3}", due = 100L - it) } // c30 quá hạn lâu nhất
        val r = planToday(TodayInput(dailyMinutes = 10, due = due)).single().item as ReviewBlock
        assertEquals(20, r.cards.size)
        assertEquals((11..30).map { "c$it" }.toSet(), r.cards.map { it.id }.toSet())
        assertEquals(5, r.minutes) // 20 thẻ × 15 giây
    }

    @Test fun thuTuUuTienMucCode() {
        val all = TodayInput(
            20, resolve = pick(PickKind.RESOLVE, "r"), codeCheckpoint = pick(PickKind.CHECKPOINT, "k"),
            codeNext = pick(PickKind.NEXT, "n"), practice = pick(PickKind.PRACTICE, "p"),
        )
        assertEquals("r", first(all))
        assertEquals("k", first(all.copy(dailyMinutes = 10))) // D < 20: bỏ qua resolve
        assertEquals("n", first(all.copy(resolve = null, codeCheckpoint = null)))
        assertEquals("p", first(TodayInput(20, practice = pick(PickKind.PRACTICE, "p"))))
    }

    @Test fun baiTiengAnhUuTienBaiKiem() {
        val i = TodayInput(
            30, englishCheckpoint = pick(PickKind.CHECKPOINT, "ek", track = "english"),
            englishNext = pick(PickKind.NEXT, "en", track = "english"),
        )
        assertEquals("ek", first(i))
    }

    @Test fun mucVuotThoiGianConLaiGanNhanThem() {
        val i = TodayInput(10, doneMinutes = 2, codeNext = pick(PickKind.NEXT, "n", min = 6), englishNext = pick(PickKind.NEXT, "e", min = 5, track = "english"))
        assertEquals(listOf(false, true), planToday(i).map { it.extra }) // còn 8: bài code 6 vừa, bài Anh 5 > 2
    }

    @Test fun khongCoGiThiHangDoiRong() = assertTrue(planToday(TodayInput(20)).isEmpty())
}
