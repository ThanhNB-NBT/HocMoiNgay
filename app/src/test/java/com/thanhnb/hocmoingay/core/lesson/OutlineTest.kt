package com.thanhnb.hocmoingay.core.lesson

import com.thanhnb.hocmoingay.core.db.ProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class OutlineTest {
    private val outline = parseOutline(
        """[{"level":"nhap-mon","title":"Nhập môn","chapters":[
            {"id":"a","title":"A","lessons":[{"id":"py/nhap-mon/a/x","title":"X","status":"ready"},{"id":"py/nhap-mon/a/y","title":"Y","status":"planned"}]},
            {"id":"b","title":"B","lessons":[{"id":"py/nhap-mon/b/z","title":"Z","status":"ready"}]}]}]""",
    )
    private fun p(id: String, status: String = "done", score: Int? = null, deleted: Boolean = false) =
        id to ProgressEntity(lessonId = id, userId = "u", status = status, score = score, updatedAt = 1, deleted = deleted)

    @Test fun demBaiXongVaChuongThanhThao() {
        val prog = mapOf(
            p("py/nhap-mon/a/x"), p("py/nhap-mon/b/z", status = "started"),
            p("py/nhap-mon/a#checkpoint", score = 85), p("py/nhap-mon/b#checkpoint", score = 60),
        )
        assertEquals(CourseStats(done = 1, ready = 2, mastered = 1, chapters = 2), courseStats("py", outline, prog))
        assertEquals(50, courseStats("py", outline, prog).masteredPct)
    }

    @Test fun hangXoaKhongTinh() = assertEquals(0, courseStats("py", outline, mapOf(p("py/nhap-mon/a/x", deleted = true))).done)

    @Test fun outlineHongThiRong() = assertEquals(emptyList<OutlineLevel>(), parseOutline("{}"))
}
