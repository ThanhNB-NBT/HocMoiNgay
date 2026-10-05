package com.thanhnb.hocmoingay.core.net

import com.thanhnb.hocmoingay.core.db.ProgressEntity
import java.time.Instant
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationTest {
    private val t = Instant.parse("2026-10-05T02:37:12.123Z").toEpochMilli()

    @Test fun maHoaGuiLenServer() {
        val row = ProgressEntity(lessonId = "python/nhap-mon/lam-quen/nhap-du-lieu", userId = "u1", status = "done",
            cardState = """{"sua":{"ok":true}}""", updatedAt = t, dirty = true)
        val o = SyncJson.encodeToJsonElement(ProgressEntity.serializer(), row).jsonObject
        assertEquals("2026-10-05T02:37:12.123Z", o["updated_at"]!!.jsonPrimitive.content)
        assertEquals(JsonPrimitive(true), o["card_state"]!!.jsonObject["sua"]!!.jsonObject["ok"])
        assertFalse("dirty không được gửi lên", "dirty" in o)
        assertTrue("null phải gửi rõ để ghi đè trên server", o["score"] is JsonNull)
        assertEquals(JsonPrimitive(false), o["deleted"])
    }

    @Test fun giaiMaHangServerCoMicroGiayVaOffset() {
        val json = """{"lesson_id":"a","user_id":"u1","status":"started","score":null,"hints_used":2,"language":"python",
            "completed_at":"2026-10-05T02:37:12.123456+00:00","card_state":{"q":1},
            "updated_at":"2026-10-05T09:37:12.5+07:00","synced_at":"2026-10-05T02:37:13.000001+00:00","deleted":false}"""
        val r = SyncJson.decodeFromString(ProgressEntity.serializer(), json)
        assertEquals(t, r.completedAt)
        assertEquals(Instant.parse("2026-10-05T02:37:12.5Z").toEpochMilli(), r.updatedAt)
        assertEquals("""{"q":1}""", r.cardState)
        assertFalse(r.dirty)
    }
}
