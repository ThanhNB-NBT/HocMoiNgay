package com.thanhnb.hocmoingay.core.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HocDbTest {
    private lateinit var db: HocDb

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HocDb::class.java).build()
    }

    @After fun close() = db.close()

    private fun p(u: Long, dirty: Boolean) = ProgressEntity(lessonId = "a", userId = "u1", status = "started", updatedAt = u, dirty = dirty)

    @Test fun cleanChiKhiUpdatedAtKhop() = runTest {
        val l = db.learner()
        l.upsertProgress(listOf(p(200, true))) // người học sửa (200) sau khi lượt sync đã đẩy bản 100
        l.cleanProgress("a", 100)
        assertTrue("bản 200 chưa đẩy, phải còn dirty", l.progressByKeys(listOf("a")).single().dirty)
        l.cleanProgress("a", 200)
        assertFalse(l.progressByKeys(listOf("a")).single().dirty)
        assertEquals(emptyList<ProgressEntity>(), l.dirtyProgress())
    }

    @Test fun anKhoaGachDuoiOBanRelease() = runTest {
        db.curriculum().upsertCourses(listOf(CourseEntity("_sample-code", "code", "Mẫu"), CourseEntity("python", "code", "Python")))
        assertEquals(listOf("python"), db.curriculum().observeCourses(showSamples = false).first().map { it.id })
        assertEquals(setOf("_sample-code", "python"), db.curriculum().observeCourses(showSamples = true).first().map { it.id }.toSet())
    }

    @Test fun wipeChiXoaDuLieuNguoiHoc() = runTest {
        val l = db.learner()
        l.upsertProgress(listOf(p(1, true)))
        l.upsertReviewCards(listOf(ReviewCardEntity(id = "r1", userId = "u1", ref = "a#b", kind = "recall", track = "code", courseId = "python", due = 1, updatedAt = 1)))
        l.upsertDailyLog(listOf(DailyLogEntity(day = "2026-10-05", userId = "u1", updatedAt = 1)))
        l.upsertSettings(listOf(SettingsEntity(userId = "u1", updatedAt = 1)))
        db.curriculum().upsertCourses(listOf(CourseEntity("python", "code", "Python")))
        l.wipeAll()
        assertEquals(0, l.progressByKeys(listOf("a")).size)
        assertEquals(0, l.reviewCardsByKeys(listOf("r1")).size)
        assertEquals(0, l.dailyLogByKeys(listOf("2026-10-05")).size)
        assertEquals(null, l.settings())
        assertEquals(listOf("python"), db.curriculum().observeCourses(showSamples = true).first().map { it.id })
    }

    @Test fun nhapCodeTheoBaiCardVaNgonNgu() = runTest {
        val d = db.drafts()
        d.put(CodeDraftEntity("l1", "sua", "python", "print(1)", 1))
        d.put(CodeDraftEntity("l1", "sua", "kotlin", "println(1)", 1))
        assertEquals("print(1)", d.get("l1", "sua", "python"))
        assertEquals("println(1)", d.get("l1", "sua", "kotlin"))
        assertEquals(null, d.get("l1", "khac", "python"))
    }

    @Test fun cursorLuuVaXoa() = runTest {
        val s = db.syncState()
        s.put(SyncStateEntity("progress", "2026-10-05T00:00:00Z"))
        s.put(SyncStateEntity("courses", "2026-10-05T00:00:00Z"))
        s.clear(listOf("progress"))
        assertEquals(null, s.cursor("progress"))
        assertEquals("2026-10-05T00:00:00Z", s.cursor("courses"))
    }

    @Test fun dueRecallChiLayTheRecallDenHanChuaXoa() = runTest {
        val l = db.learner()
        fun c(id: String, due: Long, kind: String = "recall", deleted: Boolean = false) = ReviewCardEntity(
            id = id, userId = "u1", ref = "r$id", kind = kind, track = "english", courseId = "c", due = due, updatedAt = 1, deleted = deleted,
        )
        l.upsertReviewCards(listOf(c("a", 50), c("b", 10), c("c", 500), c("d", 5, kind = "resolve"), c("e", 1, deleted = true)))
        assertEquals(listOf("b", "a"), l.dueRecall(100, 10).map { it.id })
        assertEquals(2, l.observeDueCount(100).first())
        assertEquals(10L, l.observeNextDue().first())
    }
}
