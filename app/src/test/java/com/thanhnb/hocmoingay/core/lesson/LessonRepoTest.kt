package com.thanhnb.hocmoingay.core.lesson

import kotlinx.serialization.json.jsonObject
import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import com.thanhnb.hocmoingay.core.review.DAY_MS
import java.time.ZoneOffset
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.sync.ReviewCardIds
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonRepoTest {
    private val id = "_sample-code/nhap-mon/mau/moi-loai-card"
    private val body = """{"title":"t","cards":[
        {"key":"doan","type":"predict_output","review":true,"lang":"python","code":"x","answer":"1"},
        {"key":"e","type":"explain","md":"m"}],
        "review":[{"key":"r1","front":"F","back":"B"}]}"""
    private val progress = mutableMapOf<String, ProgressEntity>()
    private val cards = mutableMapOf<String, ReviewCardEntity>()
    private var writes = 0
    private var clock = 1_000L
    private val repo = LessonRepo(
        lesson = { if (it == id) LessonEntity(id, "_sample-code", body = body) else null },
        track = { "code" },
        getProgress = { progress[it] },
        putProgress = { progress[it.lessonId] = it },
        cardsByIds = { ids -> ids.mapNotNull { cards[it] } },
        putCards = { l -> l.forEach { cards[it.id] = it } },
        tx = { it() },
        userId = { "u1" },
        afterWrite = { writes++ },
        now = { clock },
    )

    @Test fun batDauTaoHangStartedDirty() = runTest {
        repo.start(id)
        val p = progress.getValue(id)
        assertEquals("started", p.status)
        assertTrue(p.dirty)
        assertEquals(1, writes)
    }

    @Test fun xongBaiTaoTheRecallChoCardReviewVaGhiChu() = runTest {
        repo.start(id); clock = 2_000
        repo.finish(id, score = 80)
        val p = progress.getValue(id)
        assertEquals("done", p.status)
        assertEquals(80, p.score)
        assertEquals(2_000L, p.completedAt)
        val refs = cards.values.map { it.ref }.toSet()
        assertEquals(setOf("$id#doan", "$id#r1"), refs)
        val c = cards.getValue(ReviewCardIds.of("u1", "$id#doan"))
        assertEquals("recall", c.kind)
        assertEquals("code", c.track)
        assertEquals("_sample-code", c.courseId)
        assertEquals(2_000L, c.due)
        assertTrue(c.dirty)
    }

    @Test fun baiProblemDeSauThiChuaXong() = runTest {
        val pid = "_sample-code/nhap-mon/mau/bai-problem"
        val pbody = """{"title":"t","kind":"problem","cards":[{"key":"e","type":"explain","md":"m"},
            {"key":"two_sum","type":"code","prompt_md":"p","langs":["python"],"tests":[]}]}"""
        val r = LessonRepo(
            lesson = { LessonEntity(pid, "_sample-code", body = pbody) }, track = { "code" },
            getProgress = { progress[it] }, putProgress = { progress[it.lessonId] = it },
            cardsByIds = { emptyList() }, putCards = {}, tx = { it() }, userId = { "u1" }, afterWrite = {}, now = { clock },
        )
        r.start(pid); r.finish(pid, 100) // bấm "Để sau" ở card code rồi đi hết bài
        assertEquals("started", progress.getValue(pid).status)
        r.updateCard(pid, "two_sum") { JsonObject(it + ("pass" to JsonPrimitive(true))) }
        r.finish(pid, 100)
        assertEquals("done", progress.getValue(pid).status)
    }

    @Test fun hocLaiGiuDoneVaKhongDeThe() = runTest {
        repo.start(id); repo.finish(id, 80)
        val idDoan = ReviewCardIds.of("u1", "$id#doan")
        cards[idDoan] = cards.getValue(idDoan).copy(reps = 3, due = 99_000, dirty = false) // đã ôn vài lần
        clock = 5_000
        repo.start(id)
        assertEquals("done", progress.getValue(id).status)
        repo.finish(id, 50)
        assertEquals(3, cards.getValue(idDoan).reps)
        assertEquals(99_000L, cards.getValue(idDoan).due)
        assertEquals(80, progress.getValue(id).score) // giữ điểm cao nhất
    }

    @Test fun cardStateGopTheoKey() = runTest {
        repo.updateCard(id, "sua") { buildJsonObject { put("pass", true) } }
        repo.updateCard(id, "dien") { buildJsonObject { put("x", 1) } }
        val s = cardStateOf(progress[id])
        assertEquals(JsonPrimitive(true), (s["sua"] as kotlinx.serialization.json.JsonObject)["pass"])
        assertTrue("dien" in s)
    }

    @Test fun goiYChiTang() = runTest {
        repo.useHint(id, 2); repo.useHint(id, 1)
        assertEquals(2, progress.getValue(id).hintsUsed)
    }

    @Test fun updatedAtLuonTang() = runTest {
        repo.start(id); val a = progress.getValue(id).updatedAt
        repo.setLanguage(id, "rust")
        assertTrue(progress.getValue(id).updatedAt > a)
        assertEquals("rust", progress.getValue(id).language)
    }

    private val pid = "luyen-code/de/hashing/tong-hai-so"
    private val pbody = """{"title":"p","kind":"problem","cards":[{"key":"two_sum","type":"code","langs":["python"],"tests":[]}]}"""
    private val logs = mutableMapOf<String, DailyLogEntity>()
    private val log = DailyLogRepo({ logs[it] }, { logs[it.day] = it }, { it() }, { "u1" }, {}, zone = { ZoneOffset.UTC })
    private val logged = LessonRepo(
        lesson = {
            when (it) {
                id -> LessonEntity(id, "_sample-code", body = body)
                pid -> LessonEntity(pid, "luyen-code", body = pbody)
                else -> null
            }
        },
        track = { "code" },
        getProgress = { progress[it] }, putProgress = { progress[it.lessonId] = it },
        cardsByIds = { ids -> ids.mapNotNull { cards[it] } }, putCards = { l -> l.forEach { cards[it.id] = it } },
        tx = { it() }, userId = { "u1" }, afterWrite = {}, now = { clock }, log = log,
    )

    @Test fun xongLanDauCongHaiMuoiXpVaMotBaiHocLaiKhongCong() = runTest {
        logged.finish(id, 80); logged.finish(id, 100)
        val d = logs.getValue("1970-01-01")
        assertEquals(20, d.xp)
        assertEquals(1, d.lessons)
        assertEquals(2, d.newCards)
    }

    @Test fun quaHaiMuoiTheMoiThiTheDuDenHanNgayMai() = runTest {
        logs["1970-01-01"] = DailyLogEntity(day = "1970-01-01", userId = "u1", newCards = 19, updatedAt = 1)
        logged.finish(id, 80)
        assertEquals(listOf(clock, DAY_MS), cards.values.map { it.due }.sorted()) // thẻ thứ 2 sang 0 giờ hôm sau (UTC)
        assertEquals(21, logs.getValue("1970-01-01").newCards)
    }

    @Test fun datLanDauKhongTruotTaoTheResolveDeVaCongMuoiXp() = runTest {
        logged.recordSubmit(pid, "two_sum", "python", pass = true, hints = 0)
        val c = cards.getValue(ReviewCardIds.of("u1", "$pid#two_sum"))
        assertEquals("resolve", c.kind)
        assertEquals("luyen-code", c.courseId)
        assertEquals(clock + 8 * DAY_MS, c.due) // Dễ: 8 ngày (AppFsrs)
        assertEquals(10, logs.getValue("1970-01-01").xp)
        assertEquals(JsonPrimitive(true), cardStateOf(progress[pid])["two_sum"]?.jsonObject?.get("pass"))
    }

    @Test fun truotRoiDatThiNhoVaKhongCongXp() = runTest {
        logged.recordSubmit(pid, "two_sum", "python", pass = false, hints = 0)
        logged.recordSubmit(pid, "two_sum", "python", pass = true, hints = 0)
        assertEquals(clock + 2 * DAY_MS, cards.values.single().due) // Nhớ: 2 ngày
        assertEquals(0, logs["1970-01-01"]?.xp ?: 0)
    }

    @Test fun giaiLaiChamTheoLuotNayVaKhongSuaCardState() = runTest {
        logged.recordSubmit(pid, "two_sum", "python", pass = true, hints = 0)
        val st = progress.getValue(pid).cardState
        logged.recordSubmit(pid, "two_sum", "python", pass = true, hints = 0) // nộp lại lượt thường: lịch giữ nguyên
        assertEquals(1, cards.values.single().reps)
        clock += 8 * DAY_MS
        logged.recordSubmit(pid, "two_sum", "rust", pass = true, hints = 2, review = true, sessionFails = 0)
        val c = cards.values.single()
        assertEquals(2, c.reps)
        assertEquals(clock, c.lastReview)
        assertEquals(1, c.lapses) // 2 gợi ý → Quên khi thẻ đang Review
        assertEquals(st, progress.getValue(pid).cardState)
        assertEquals(10, logs.values.sumOf { it.xp })
    }

    @Test fun baiConceptKhongTaoTheResolve() = runTest {
        logged.recordSubmit(id, "code1", "python", pass = true, hints = 0)
        assertTrue(cards.isEmpty())
    }
}
