package com.thanhnb.hocmoingay.core.log

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyLogRepoTest {
    private val rows = mutableMapOf<String, DailyLogEntity>()
    private var uid: String? = "u1"
    private var writes = 0
    private val repo = DailyLogRepo(
        get = { rows[it] }, put = { rows[it.day] = it }, tx = { it() }, userId = { uid }, afterWrite = { writes++ },
        zone = { ZoneId.of("Asia/Ho_Chi_Minh") },
    )
    private val at = Instant.parse("2026-10-05T03:00:00Z").toEpochMilli()

    @Test fun ngayTheoMuiGioMay() {
        assertEquals("2026-10-06", repo.dayOf(Instant.parse("2026-10-05T18:00:00Z").toEpochMilli())) // 01:00 sáng giờ VN
    }

    @Test fun congDonMachVaDanhDauDirty() = runTest {
        repo.addStrands(at, mapOf("input" to 1.5))
        val first = rows.getValue("2026-10-05").updatedAt
        repo.addStrands(at, mapOf("input" to 0.25, "output" to 2.0))
        val r = rows.getValue("2026-10-05")
        val s = Json.parseToJsonElement(r.strands).jsonObject
        assertEquals(1.75, s.getValue("input").jsonPrimitive.double, 1e-9)
        assertEquals(2.0, s.getValue("output").jsonPrimitive.double, 1e-9)
        assertTrue(r.dirty)
        assertTrue(r.updatedAt > first)
        assertEquals(2, writes)
    }

    @Test fun giuKhoaLa() {
        val s = Json.parseToJsonElement(mergeStrands("""{"x":1,"input":0.1}""", mapOf("input" to 0.2))).jsonObject
        assertEquals(1.0, s.getValue("x").jsonPrimitive.double, 0.0)
        assertEquals(0.3, s.getValue("input").jsonPrimitive.double, 1e-9)
    }

    @Test fun chuaDangNhapThiKhongGhi() = runTest {
        uid = null
        repo.add(at) { it.copy(reviews = it.reviews + 1) }
        assertTrue(rows.isEmpty())
        assertEquals(0, writes)
    }

    @Test fun dauNgayMaiTheoGioMay() =
        assertEquals(Instant.parse("2026-10-05T17:00:00Z").toEpochMilli(), repo.nextDayStart(at)) // 0 giờ 6/10 giờ VN

    @Test fun docHangCuaNgay() = runTest {
        repo.add(at) { it.copy(xp = 3) }
        assertEquals(3, repo.day(at)?.xp)
    }
}
