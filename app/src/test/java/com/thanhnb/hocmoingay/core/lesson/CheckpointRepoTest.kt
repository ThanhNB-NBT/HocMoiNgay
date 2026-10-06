package com.thanhnb.hocmoingay.core.lesson

import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckpointRepoTest {
    private val cp = "py/co-ban/vong-lap#checkpoint"
    private val bodies = mapOf(
        "py/co-ban/vong-lap/a" to """{"title":"a","cards":[{"key":"q","type":"quiz","review":true,"q":"q","choices":["a","b"],"answer":[0]}]}""",
        "py/co-ban/vong-lap/b" to """{"title":"b","cards":[{"key":"c","type":"code","langs":["python"],"tests":[]}]}""",
    )
    private val outline = """[{"level":"co-ban","title":"Cơ bản","chapters":[{"id":"vong-lap","title":"Vòng lặp","lessons":[
        {"id":"py/co-ban/vong-lap/a","title":"a","status":"ready"},{"id":"py/co-ban/vong-lap/b","title":"b","status":"ready"}]}]}]"""
    private val progress = mutableMapOf<String, ProgressEntity>()
    private val cards = mutableMapOf<String, ReviewCardEntity>()
    private val logs = mutableMapOf<String, DailyLogEntity>()
    private val log = DailyLogRepo({ logs[it] }, { logs[it.day] = it }, { it() }, { "u1" }, {}, zone = { ZoneOffset.UTC })
    private val repo = LessonRepo(
        lesson = { id -> bodies[id]?.let { LessonEntity(id, "py", body = it) } }, track = { "code" },
        getProgress = { progress[it] }, putProgress = { progress[it.lessonId] = it },
        cardsByIds = { ids -> ids.mapNotNull { cards[it] } }, putCards = { l -> l.forEach { cards[it.id] = it } },
        tx = { it() }, userId = { "u1" }, afterWrite = {}, now = { 1_000L },
        log = log, course = { if (it == "py") CourseEntity("py", "code", "Python", outline = outline) else null },
    )

    @Test fun napBaiKiemTuCacBaiCuaChuong() = runTest {
        val l = repo.load(cp)!!
        assertEquals("checkpoint", l.body.kind)
        assertEquals(listOf("a.q", "b.c"), l.body.cards.map { it.key })
        assertEquals("code", l.track)
        assertEquals(false, l.body.hasReview)
    }

    @Test fun datTamMuoiLanDauCongBaMuoiXpKhongTaoTheOn() = runTest {
        repo.finish(cp, 85); repo.finish(cp, 100)
        val d = logs.getValue("1970-01-01")
        assertEquals(30, d.xp)
        assertEquals(1, d.lessons)
        assertTrue(cards.isEmpty())
        assertEquals(100, progress.getValue(cp).score)
    }
}
