package com.thanhnb.hocmoingay.core.lesson

import com.thanhnb.hocmoingay.core.db.ProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointTest {
    private fun lesson(slug: String, vararg cards: String) =
        "py/co-ban/vong-lap/$slug" to """{"title":"$slug","cards":[${cards.joinToString(",")}]}"""
    private fun quiz(k: String, review: Boolean = true) =
        """{"key":"$k","type":"quiz","review":$review,"q":"q","choices":["a","b"],"answer":[0]}"""
    private fun code(k: String) = """{"key":"$k","type":"code","langs":["python"],"tests":[]}"""

    @Test fun rutToiDaMuoiCardOnCongCardCodeCuoiCung() {
        val ls = listOf(
            lesson("a", *Array(8) { quiz("q$it") }, code("c1")),
            lesson("b", *Array(6) { quiz("q$it") }, quiz("x", review = false), code("c2")),
        )
        val b = buildCheckpoint("Vòng lặp", ls, seed = 1)!!
        assertEquals("checkpoint", b.kind)
        assertTrue(b.canDo.contains("Vòng lặp")) // đầu bài "Học xong bài này, bạn sẽ …" không bỏ trống
        assertEquals(11, b.cards.size)
        assertEquals("b.c2", b.cards.last().key)
        assertTrue(b.cards.dropLast(1).all { it is Quiz && it.review })
        assertTrue("b.x" !in b.cards.map { it.key })
        assertEquals(b.cards.map { it.key }, buildCheckpoint("Vòng lặp", ls, seed = 1)!!.cards.map { it.key })
    }

    @Test fun chuongKhongCoCardOnVaCodeThiKhongCoBaiKiem() =
        assertNull(buildCheckpoint("t", listOf(lesson("a", quiz("q", review = false))), 1))

    @Test fun khoaGocCuaCardTrongBaiKiem() {
        assertEquals("py/co-ban/vong-lap/b" to "c2", checkpointOrigin("py/co-ban/vong-lap#checkpoint", "b.c2"))
        assertEquals("py/co-ban/vong-lap/b" to "two_sum", checkpointOrigin("py/co-ban/vong-lap/b", "two_sum"))
    }

    private val outline = listOf(
        OutlineLevel(
            "co-ban", "Cơ bản",
            listOf(
                OutlineChapter("vong-lap", "Vòng lặp", listOf(OutlineLesson("py/co-ban/vong-lap/a", "a", "ready"), OutlineLesson("py/co-ban/vong-lap/b", "b", "ready"))),
                OutlineChapter("ham", "Hàm", listOf(OutlineLesson("py/co-ban/ham/a", "a", "ready"), OutlineLesson("py/co-ban/ham/b", "b", "planned"))),
            ),
        ),
    )
    private fun done(id: String, score: Int? = null, at: Long = 0) =
        id to ProgressEntity(lessonId = id, userId = "u", status = "done", score = score, updatedAt = at)
    private val dayOf: (Long) -> String = { if (it < 100) "2026-10-05" else "2026-10-06" }
    private val cp = "py/co-ban/vong-lap#checkpoint"

    @Test fun baiKiemChoKhiXongMoiBaiCuaChuong() {
        val p = mapOf(done("py/co-ban/vong-lap/a"), done("py/co-ban/vong-lap/b"), done("py/co-ban/ham/a"))
        assertEquals(listOf(cp), pendingCheckpoints("py", outline, p, "2026-10-06", dayOf)) // chương Hàm còn bài planned
    }

    @Test fun chuaDatThiHomSauMoiHienLai() {
        val base = mapOf(done("py/co-ban/vong-lap/a"), done("py/co-ban/vong-lap/b"))
        assertEquals(emptyList<String>(), pendingCheckpoints("py", outline, base + done(cp, 60, at = 200), "2026-10-06", dayOf))
        assertEquals(listOf(cp), pendingCheckpoints("py", outline, base + done(cp, 60, at = 50), "2026-10-06", dayOf))
        assertEquals(emptyList<String>(), pendingCheckpoints("py", outline, base + done(cp, 85, at = 50), "2026-10-06", dayOf))
    }
}
