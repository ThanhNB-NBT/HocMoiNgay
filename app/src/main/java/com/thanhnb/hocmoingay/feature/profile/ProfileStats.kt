package com.thanhnb.hocmoingay.feature.profile

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.lesson.LessonJson
import java.time.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

/** 4 mạch tiếng Anh theo khoá của `daily_log.strands` (Kết quả d). */
val STRANDS = listOf("input", "output", "language", "fluency")
val STRAND_NAMES = mapOf("input" to "Nghe – đọc", "output" to "Nói – viết", "language" to "Từ vựng – ngữ pháp", "fluency" to "Lưu loát")
private val STRAND_TIPS = mapOf(
    "input" to "thêm bài có card nghe hoặc đọc",
    "output" to "làm thêm card nói và viết",
    "language" to "ôn thẻ từ vựng nhiều hơn",
    "fluency" to "thử card nói theo giờ hoặc shadowing",
)

/** Mức màu ô heatmap theo XP trong ngày: 0 trống, 1..4 đậm dần. */
fun heatLevel(xp: Int): Int = when {
    xp <= 0 -> 0
    xp < 20 -> 1
    xp < 40 -> 2
    xp < 80 -> 3
    else -> 4
}

/** Số tuần trên lịch học: vừa một màn điện thoại, không phải vuốt. */
const val HEAT_WEEKS = 12

private fun heatStart(today: LocalDate, weeks: Int) = today.minusDays((today.dayOfWeek.value - 1).toLong()).minusWeeks((weeks - 1).toLong())

/** [weeks] cột × 7 hàng (Thứ Hai trên cùng), cột cuối là tuần chứa [today]; ô sau hôm nay là null (không vẽ). */
fun heatmap(xpByDay: Map<LocalDate, Int>, today: LocalDate, weeks: Int = HEAT_WEEKS): List<List<Int?>> {
    val start = heatStart(today, weeks)
    return List(weeks) { w ->
        List(7) { d ->
            val day = start.plusDays((w * 7 + d).toLong())
            if (day.isAfter(today)) null else heatLevel(xpByDay[day] ?: 0)
        }
    }
}

/**
 * Nhãn tháng trên đầu cột như GitHub: cột có ngày mùng 1 ghi "Th<tháng>"; cột đầu ghi tháng của nó nếu 2 cột đầu chưa có nhãn.
 * Còn lại null.
 */
fun heatMonths(today: LocalDate, weeks: Int = HEAT_WEEKS): List<String?> {
    val start = heatStart(today, weeks)
    val labels = MutableList<String?>(weeks) { w ->
        val mon = start.plusWeeks(w.toLong())
        (0L..6L).map { mon.plusDays(it) }.firstOrNull { it.dayOfMonth == 1 }?.let { "Th${it.monthValue}" }
    }
    if (labels.take(2).all { it == null }) labels[0] = "Th${start.monthValue}"
    return labels
}

/** Phút theo 4 mạch trong 7 ngày gần nhất (tính cả hôm nay). */
fun weekStrands(logs: List<DailyLogEntity>, today: LocalDate): Map<String, Double> {
    val from = today.minusDays(6).toString()
    val to = today.toString()
    val sum = STRANDS.associateWith { 0.0 }.toMutableMap()
    logs.filter { !it.deleted && it.day >= from && it.day <= to }.forEach { l ->
        val o = runCatching { LessonJson.parseToJsonElement(l.strands).jsonObject }.getOrNull() ?: return@forEach
        STRANDS.forEach { k -> sum[k] = sum.getValue(k) + ((o[k] as? JsonPrimitive)?.doubleOrNull ?: 0.0) }
    }
    return sum
}

/** Ghi chú cho mạch dưới 15% tổng (spec §7.2, C1); tuần chưa học tiếng Anh thì không ghi. */
fun strandNote(m: Map<String, Double>): String? {
    val total = m.values.sum()
    if (total <= 0) return null
    val low = STRANDS.filter { (m[it] ?: 0.0) / total < 0.15 }
    if (low.isEmpty()) return null
    return low.joinToString(" ") {
        "${STRAND_NAMES.getValue(it)} mới chiếm ${((m[it] ?: 0.0) * 100 / total).toInt()}% tuần này: ${STRAND_TIPS.getValue(it)}."
    }
}
