package com.thanhnb.hocmoingay.feature.placement

import com.thanhnb.hocmoingay.core.db.PlacementEntity
import com.thanhnb.hocmoingay.core.lesson.LessonJson
import kotlin.random.Random
import kotlinx.serialization.Serializable

/** `placement_questions.body` (push.py: mọi trường trừ id/level/skill). `answer` là MỘT chỉ số, nội dung đặt đúng ở choices[0]. */
@Serializable
data class PlacementBody(val q: String, val choices: List<String>, val answer: Int = 0, val text: String? = null)

data class PlacementQ(val id: String, val level: String, val skill: String, val body: PlacementBody)

val LEVELS = listOf("A1", "A2", "B1", "B2", "C1")
val SKILLS = listOf("vocab", "grammar", "listening")

/** Bỏ hàng đã xoá, JSON hỏng, hoặc `answer` ngoài phạm vi: một câu lỗi không được làm hỏng cả bài. */
fun parsePlacement(rows: List<PlacementEntity>): List<PlacementQ> = rows.filterNot { it.deleted }.mapNotNull { r ->
    runCatching { PlacementQ(r.id, r.level, r.skill, LessonJson.decodeFromString(PlacementBody.serializer(), r.body)) }
        .getOrNull()?.takeIf { it.body.answer in it.body.choices.indices }
}

/** Khoảng 30 câu (spec §7.2): mỗi ô cấp × kỹ năng [perCell] câu; cấp dễ trước, trong một cấp xáo thứ tự. Ô thiếu thì lấy phần có. */
fun pickPlacement(all: List<PlacementQ>, rnd: Random, perCell: Int = 2): List<PlacementQ> = LEVELS.flatMap { l ->
    SKILLS.flatMap { s -> all.filter { it.level == l && it.skill == s }.shuffled(rnd).take(perCell) }.shuffled(rnd)
}

data class LevelScore(val level: String, val right: Int, val total: Int) {
    val passed: Boolean get() = total > 0 && right * 10 >= total * 7
}

fun levelScores(asked: List<PlacementQ>, correct: Set<String>): List<LevelScore> = LEVELS.map { l ->
    val qs = asked.filter { it.level == l }
    LevelScore(l, qs.count { it.id in correct }, qs.size)
}

/** Cấp cao nhất đúng ≥ 70%, chỉ tính khi mọi cấp thấp hơn cũng đạt; chưa đạt cấp nào thì A1. Cấp không có câu thì bỏ qua. */
fun placementLevel(scores: List<LevelScore>): String {
    var level = LEVELS.first()
    for (s in scores) {
        if (s.total == 0) continue
        if (s.passed) level = s.level else break
    }
    return level
}
