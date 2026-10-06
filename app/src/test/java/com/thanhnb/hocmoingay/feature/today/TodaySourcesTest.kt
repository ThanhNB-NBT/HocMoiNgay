package com.thanhnb.hocmoingay.feature.today

import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.OutlineChapter
import com.thanhnb.hocmoingay.core.lesson.OutlineLesson
import com.thanhnb.hocmoingay.core.lesson.OutlineLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodaySourcesTest {
    private fun course(id: String, track: String = "code", outline: String = "[]") = CourseEntity(id, track, id, outline = outline)
    private fun prog(id: String, at: Long) = ProgressEntity(lessonId = id, userId = "u", status = "done", updatedAt = at)
    private val ol = listOf(
        OutlineLevel("A1", "A1", listOf(OutlineChapter("c1", "C1", listOf(OutlineLesson("e/A1/c1/x", "x", "ready"))))),
        OutlineLevel(
            "A2", "A2",
            listOf(OutlineChapter("c2", "C2", listOf(OutlineLesson("e/A2/c2/a", "a", "ready"), OutlineLesson("e/A2/c2/b", "b", "ready"), OutlineLesson("e/A2/c2/c", "c", "planned")))),
        ),
    )

    @Test fun khoaDangHocTheoCaiDatRoiTheoTienDoGanNhat() {
        val cs = listOf(course("python"), course("kotlin"), course("luyen-code"), course("english-work", "english"))
        assertEquals("kotlin", activeCodeCourse(cs, listOf("kotlin"), emptyList())?.id)
        val p = listOf(prog("python/a/b/c", 1), prog("kotlin/a/b/c", 5), prog("luyen-code/de/x/y", 9))
        assertEquals("kotlin", activeCodeCourse(cs, emptyList(), p)?.id) // luyen-code không phải khoá đang học
    }

    @Test fun chuaHocGiThiLayKhoaDauTienCoBaiReady() {
        val ready = """[{"level":"l","title":"L","chapters":[{"id":"c","title":"C","lessons":[{"id":"kotlin/l/c/a","title":"a","status":"ready"}]}]}]"""
        assertEquals("kotlin", activeCodeCourse(listOf(course("python"), course("kotlin", outline = ready)), emptyList(), emptyList())?.id)
    }

    @Test fun baiTiepTheoTheoThuTuDeCuongVaCap() {
        assertEquals("e/A1/c1/x", nextLesson(ol, emptySet())?.id)
        assertEquals("e/A2/c2/b", nextLesson(ol, setOf("e/A2/c2/a"), level = "A2")?.id)
        assertNull(nextLesson(ol, setOf("e/A2/c2/a", "e/A2/c2/b"), level = "A2")) // c còn planned
    }

    @Test fun luyenCodeUuTienMauDaHoc() {
        fun l(id: String) = OutlineLesson(id, id, "ready")
        val ls = listOf(l("a") to listOf("dp"), l("b") to listOf("hashing"), l("c") to listOf("hashing"))
        assertEquals("a", pickPractice(ls, emptySet())?.id)
        assertEquals("c", pickPractice(ls, setOf("b"))?.id)
    }

    @Test fun baiKiemDangChoMangTenChuong() {
        val outline = """[{"level":"A2","title":"A2","chapters":[{"id":"hop","title":"Họp nhóm","lessons":[{"id":"e/A2/hop/a","title":"a","status":"ready"}]}]}]"""
        val p = checkpointPick(course("e", "english", outline), mapOf("e/A2/hop/a" to prog("e/A2/hop/a", 1)), "2026-10-06") { "2026-10-06" }!!
        assertEquals("e/A2/hop#checkpoint", p.lessonId)
        assertEquals("Họp nhóm", p.title)
        assertEquals(PickKind.CHECKPOINT, p.kind)
    }
}
