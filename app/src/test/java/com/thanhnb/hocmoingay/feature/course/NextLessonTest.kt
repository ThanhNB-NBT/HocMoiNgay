package com.thanhnb.hocmoingay.feature.course

import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.OutlineChapter
import com.thanhnb.hocmoingay.core.lesson.OutlineLesson
import com.thanhnb.hocmoingay.core.lesson.OutlineLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NextLessonTest {
    private val outline = listOf(
        OutlineLevel("1", "Nhập môn", listOf(OutlineChapter("a", "A", listOf(OutlineLesson("x/1", "Một", "ready"), OutlineLesson("x/2", "Hai", "ready"))))),
        OutlineLevel("2", "Cơ bản", listOf(OutlineChapter("b", "B", listOf(OutlineLesson("x/3", "Ba", "planned"), OutlineLesson("x/4", "Bốn", "ready"))))),
    )

    private fun p(id: String, status: String) = id to ProgressEntity(lessonId = id, userId = "u", status = status, updatedAt = 0L)

    @Test fun chuaHocGiThiLaBaiDau() = assertEquals("x/1", nextLesson(outline, emptyMap())?.id)

    @Test fun baiDangDoVanLaBaiToiLuot() = assertEquals("x/1", nextLesson(outline, mapOf(p("x/1", "started")))?.id)

    @Test fun boQuaBaiXongVaBaiChuaSoan() =
        assertEquals("x/4", nextLesson(outline, mapOf(p("x/1", "done"), p("x/2", "done")))?.id)

    @Test fun xongHetThiKhongCo() =
        assertNull(nextLesson(outline, mapOf(p("x/1", "done"), p("x/2", "done"), p("x/4", "done"))))
}
