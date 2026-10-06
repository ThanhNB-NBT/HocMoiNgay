package com.thanhnb.hocmoingay.core.lesson

import com.thanhnb.hocmoingay.core.db.ProgressEntity
import kotlin.random.Random
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Bài kiểm cuối chương lưu ở progress với `lesson_id = '<khoá>/<cấp>/<chương>#checkpoint'` (spec §4.1). */
const val CHECKPOINT = "#checkpoint"

/** Đạt từ 80% thì chương được tính là thành thạo (spec §7.4). */
const val MASTERED = 80

fun isCheckpoint(lessonId: String) = lessonId.endsWith(CHECKPOINT)
fun chapterOf(checkpointId: String) = checkpointId.removeSuffix(CHECKPOINT)

/** Card trong bài kiểm mang khoá "<slug bài>.<khoá card>"; editor gọi server (lesson_secrets) bằng bài và khoá gốc. */
fun checkpointOrigin(lessonId: String, key: String): Pair<String, String> =
    if (!isCheckpoint(lessonId) || '.' !in key) lessonId to key
    else "${chapterOf(lessonId)}/${key.substringBefore('.')}" to key.substringAfter('.')

/**
 * Bài kiểm cuối chương (spec §7.4, A5/D4): tối đa 10 card `review: true` rút ngẫu nhiên trong chương,
 * cộng card `code` cuối cùng của chương. [lessons] là (id bài, JSON body) theo thứ tự đề cương.
 * Rút theo [seed] để mở lại (trình phát, editor) trong ngày ra cùng một đề. Chương không có card nào để kiểm thì trả null.
 */
fun buildCheckpoint(title: String, lessons: List<Pair<String, String>>, seed: Int): LessonBody? {
    val picked = mutableListOf<JsonObject>()
    var make: JsonObject? = null
    for ((id, json) in lessons) {
        val cards = runCatching { LessonJson.parseToJsonElement(json).jsonObject["cards"] }.getOrNull() as? JsonArray ?: continue
        val slug = id.substringAfterLast('/')
        for (c in cards) {
            val o = c as? JsonObject ?: continue
            val key = (o["key"] as? JsonPrimitive)?.content ?: continue
            val tagged = JsonObject(o + ("key" to JsonPrimitive("$slug.$key")))
            when {
                (o["type"] as? JsonPrimitive)?.content == "code" -> make = tagged
                (o["review"] as? JsonPrimitive)?.booleanOrNull == true -> picked += tagged
            }
        }
    }
    val cards = picked.shuffled(Random(seed)).take(10) + listOfNotNull(make)
    if (cards.isEmpty()) return null
    val body = buildJsonObject {
        put("title", "Kiểm tra cuối chương: $title")
        put("kind", "checkpoint")
        put("can_do", "nhớ lại được phần chính của chương “$title” — đạt từ 80% là chương thành thạo")
        put("estimate_min", 3 + cards.size)
        put("cards", JsonArray(cards))
    }
    return parseLesson(body.toString())
}

/**
 * Bài kiểm đang chờ của một khoá: mọi bài của chương đã `ready` và đã xong; bài kiểm chưa làm,
 * hoặc chưa đạt [MASTERED] và lần làm gần nhất từ hôm qua trở về trước (chưa đạt thì hôm sau hiện lại).
 */
fun pendingCheckpoints(
    courseId: String, outline: List<OutlineLevel>, progress: Map<String, ProgressEntity>,
    today: String, dayOf: (Long) -> String,
): List<String> = outline.flatMap { lv ->
    lv.chapters.mapNotNull { ch ->
        val id = "$courseId/${lv.level}/${ch.id}$CHECKPOINT"
        val allDone = ch.lessons.isNotEmpty() &&
            ch.lessons.all { it.status == "ready" && progress[it.id]?.let { p -> !p.deleted && p.status == "done" } == true }
        val cp = progress[id]?.takeUnless { it.deleted }
        val waiting = cp == null || cp.status != "done" || ((cp.score ?: 0) < MASTERED && dayOf(cp.updatedAt) < today)
        id.takeIf { allDone && waiting }
    }
}
