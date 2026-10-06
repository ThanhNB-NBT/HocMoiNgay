package com.thanhnb.hocmoingay.core.log

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.lesson.LessonJson
import com.thanhnb.hocmoingay.core.sync.nextUpdatedAt
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

/** Ghi `daily_log` của ngày theo giờ máy (Kết quả b: đọc bản cũ, nextUpdatedAt, dirty, afterWrite). */
class DailyLogRepo(
    private val get: suspend (day: String) -> DailyLogEntity?,
    private val put: suspend (DailyLogEntity) -> Unit,
    private val tx: suspend (suspend () -> Unit) -> Unit,
    private val userId: () -> String?,
    private val afterWrite: () -> Unit,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    fun dayOf(at: Long): String = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate().toString()

    suspend fun day(at: Long): DailyLogEntity? = get(dayOf(at))?.takeUnless { it.deleted }

    /** 0 giờ ngày hôm sau theo giờ máy: hạn của thẻ mới vượt giới hạn trong ngày. */
    fun nextDayStart(at: Long): Long =
        Instant.ofEpochMilli(at).atZone(zone()).toLocalDate().plusDays(1).atStartOfDay(zone()).toInstant().toEpochMilli()

    suspend fun add(at: Long, change: (DailyLogEntity) -> DailyLogEntity) {
        val uid = userId() ?: return
        val day = dayOf(at)
        tx {
            val old = get(day)
            val base = old?.takeUnless { it.deleted } ?: DailyLogEntity(day = day, userId = uid, updatedAt = 0)
            put(change(base).copy(updatedAt = nextUpdatedAt(old?.updatedAt, at), dirty = true, deleted = false))
        }
        afterWrite()
    }

    /** Cộng số phút theo mạch (input/output/language/fluency) của bài tiếng Anh. */
    suspend fun addStrands(at: Long, minutes: Map<String, Double>) {
        if (minutes.isNotEmpty()) add(at) { it.copy(strands = mergeStrands(it.strands, minutes)) }
    }
}

/** Cộng dồn vào JSON `strands`, làm tròn 0,01 phút; khoá lạ (bản app mới hơn) giữ nguyên. */
fun mergeStrands(json: String, add: Map<String, Double>): String {
    val old = runCatching { LessonJson.parseToJsonElement(json).jsonObject }.getOrNull() ?: JsonObject(emptyMap())
    val m = old.toMutableMap()
    add.forEach { (k, v) ->
        val cur = (m[k] as? JsonPrimitive)?.doubleOrNull ?: 0.0
        m[k] = JsonPrimitive(Math.round((cur + v) * 100) / 100.0)
    }
    return JsonObject(m).toString()
}
