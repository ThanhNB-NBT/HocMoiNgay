package com.thanhnb.hocmoingay.core.sync

import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.net.parseTs
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEngineTest {
    private class FakeRemote : Remote {
        val upserts = mutableListOf<Pair<String, List<JsonObject>>>()
        val server = mutableMapOf<String, MutableList<JsonObject>>()
        val pullSinces = mutableListOf<String>()
        var onUpsert: () -> Unit = {}
        override suspend fun upsert(table: String, onConflict: String, rows: List<JsonObject>) {
            upserts += table to rows
            onUpsert()
        }
        override suspend fun pullSince(table: String, since: String, max: Int): List<JsonObject> {
            if (table == "daily_log") pullSinces += since
            return server[table].orEmpty()
                .filter { parseTs(it["synced_at"]!!.jsonPrimitive.content) >= parseTs(since) }
                .sortedBy { parseTs(it["synced_at"]!!.jsonPrimitive.content) }
                .take(max)
        }
    }

    private class MemCursors : CursorStore {
        val m = mutableMapOf<String, String>()
        override suspend fun get(table: String) = m[table]
        override suspend fun put(table: String, cursor: String) { m[table] = cursor }
    }

    private val local = linkedMapOf<String, DailyLogEntity>()
    private val courses = linkedMapOf<String, CourseEntity>()
    private val remote = FakeRemote()
    private val cursors = MemCursors()

    private val dailyTable = LearnerTable(
        name = "daily_log", onConflict = "user_id,day", serializer = DailyLogEntity.serializer(), key = { it.day },
        dirty = { local.values.filter { it.dirty } },
        byKeys = { ks -> ks.mapNotNull { local[it] } },
        save = { rs -> rs.forEach { local[it.day] = it } },
        clean = { k, u -> local[k]?.takeIf { it.updatedAt == u }?.let { local[k] = it.copy(dirty = false) } },
    )
    private val courseTable = CurriculumTable(
        name = "courses", serializer = CourseEntity.serializer(), key = { it.id }, isDeleted = { it.deleted },
        save = { rs -> rs.forEach { courses[it.id] = it } },
        delete = { ids -> ids.forEach { courses.remove(it) } },
    )

    private fun engine(pageSize: Int = 500) = SyncEngine(remote, cursors, listOf(courseTable, dailyTable), pageSize)

    private fun day(d: String, synced: String, updated: String = "2026-10-05T00:00:00Z", xp: Int = 0) = buildJsonObject {
        put("day", d); put("user_id", "u1"); put("xp", xp); put("minutes", 0); put("reviews", 0); put("lessons", 0)
        put("new_cards", 0); put("strands", buildJsonObject {}); put("updated_at", updated); put("synced_at", synced); put("deleted", false)
    }

    @Test fun dayChiHangDirtyVaBoCoDirty() = runTest {
        local["d1"] = DailyLogEntity(day = "d1", userId = "u1", xp = 5, updatedAt = 100, dirty = true)
        local["d2"] = DailyLogEntity(day = "d2", userId = "u1", updatedAt = 100)
        engine().syncAll()
        val sent = remote.upserts.single { it.first == "daily_log" }.second
        assertEquals(listOf("d1"), sent.map { it["day"]!!.jsonPrimitive.content })
        assertFalse("dirty không gửi lên", "dirty" in sent.single())
        assertFalse(local["d1"]!!.dirty)
    }

    @Test fun suaTrongLucDayVanGiuDirty() = runTest {
        local["d1"] = DailyLogEntity(day = "d1", userId = "u1", updatedAt = 100, dirty = true)
        remote.onUpsert = { local["d1"] = local["d1"]!!.copy(xp = 9, updatedAt = 200, dirty = true) } // người học sửa giữa lượt
        engine().syncAll()
        assertTrue("bản 200 chưa đẩy, phải còn dirty", local["d1"]!!.dirty)
        assertEquals(200L, local["d1"]!!.updatedAt)
    }

    @Test fun keoLui5GiayVaKhongLuiCursor() = runTest {
        cursors.m["daily_log"] = "2026-10-05T00:00:10Z"
        remote.server["daily_log"] = mutableListOf(day("d1", synced = "2026-10-05T00:00:07Z"))
        engine().syncAll()
        assertEquals(listOf("2026-10-05T00:00:05Z"), remote.pullSinces)
        assertEquals("d1", local["d1"]!!.day) // hàng commit muộn vẫn được kéo về
        assertEquals("2026-10-05T00:00:10Z", cursors.m["daily_log"])
    }

    @Test fun phanTrangTheoSyncedAt() = runTest {
        remote.server["daily_log"] = mutableListOf(
            day("d1", synced = "2026-10-05T00:00:01Z"),
            day("d2", synced = "2026-10-05T00:00:02Z"),
            day("d3", synced = "2026-10-05T00:00:03+00:00"),
        )
        engine(pageSize = 2).syncAll()
        assertEquals(setOf("d1", "d2", "d3"), local.keys)
        assertEquals(listOf("1970-01-01T00:00:00Z", "2026-10-05T00:00:02Z", "2026-10-05T00:00:03Z"), remote.pullSinces)
        assertEquals("2026-10-05T00:00:03Z", cursors.m["daily_log"])
    }

    @Test fun caTrangCungMotMocThiDung() = runTest {
        remote.server["daily_log"] = mutableListOf(
            day("d1", synced = "2026-10-05T00:00:01Z"),
            day("d2", synced = "2026-10-05T00:00:01Z"),
        )
        engine(pageSize = 2).syncAll() // không được lặp vô tận
        assertEquals(setOf("d1", "d2"), local.keys)
    }

    @Test fun realtimeKhongDeBanLocalDirtyMoiHon() = runTest {
        local["d1"] = DailyLogEntity(day = "d1", userId = "u1", xp = 7, updatedAt = 300_000, dirty = true)
        engine().mergeRemote("daily_log", day("d1", synced = "2026-10-05T00:00:01Z", updated = "1970-01-01T00:03:20Z", xp = 1)) // 200_000 ms
        assertEquals(7, local["d1"]!!.xp)
        assertTrue(local["d1"]!!.dirty)
    }

    @Test fun giaoTrinhXoaMemThiXoaLocal() = runTest {
        courses["old"] = CourseEntity("old", "code", "Cũ")
        remote.server["courses"] = mutableListOf(
            buildJsonObject { put("id", "old"); put("track", "code"); put("title", "Cũ"); put("outline", kotlinx.serialization.json.JsonArray(emptyList())); put("deleted", true); put("synced_at", "2026-10-05T00:00:01Z") },
            buildJsonObject { put("id", "python"); put("track", "code"); put("title", "Python"); put("outline", kotlinx.serialization.json.JsonArray(emptyList())); put("deleted", false); put("synced_at", "2026-10-05T00:00:02Z") },
        )
        engine().syncAll()
        assertEquals(setOf("python"), courses.keys)
        assertEquals("[]", courses["python"]!!.outline)
    }

    // SettingsRepo dựa vào cursor để biết đã kéo bảng ít nhất một lần, kể cả khi server chưa có hàng nào
    @Test fun keoRongVanDanhDauDaKeo() = runTest {
        engine().syncAll()
        assertEquals("1970-01-01T00:00:00Z", cursors.m["daily_log"])
    }

    @Test fun dayLoiMotBangVanKeoCacBangKhac() = runTest {
        local["d1"] = DailyLogEntity(day = "d1", userId = "u1", updatedAt = 100, dirty = true)
        remote.onUpsert = { throw IllegalStateException("23514 check_violation") }
        remote.server["courses"] = mutableListOf(
            buildJsonObject { put("id", "python"); put("track", "code"); put("title", "Python"); put("outline", kotlinx.serialization.json.JsonArray(emptyList())); put("deleted", false); put("synced_at", "2026-10-05T00:00:02Z") },
        )
        val e = runCatching { engine().syncAll() }.exceptionOrNull()
        assertTrue("lỗi vẫn phải ném ra để WorkManager thử lại", e is IllegalStateException)
        assertEquals(setOf("python"), courses.keys)
        assertTrue(local["d1"]!!.dirty)
    }
}
