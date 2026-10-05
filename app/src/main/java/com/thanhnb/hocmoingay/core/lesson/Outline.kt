package com.thanhnb.hocmoingay.core.lesson

import android.util.Log
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable data class OutlineLesson(val id: String, val title: String, val status: String = "planned")
@Serializable data class OutlineChapter(val id: String, val title: String, val lessons: List<OutlineLesson> = emptyList())
@Serializable data class OutlineLevel(val level: String, val title: String, val chapters: List<OutlineChapter> = emptyList())

fun parseOutline(json: String): List<OutlineLevel> = try {
    LessonJson.decodeFromString(ListSerializer(OutlineLevel.serializer()), json)
} catch (e: IllegalArgumentException) {
    Log.w("Outline", "đề cương hỏng", e); emptyList()
}

data class CourseStats(val done: Int, val ready: Int, val mastered: Int, val chapters: Int) {
    val masteredPct: Int get() = if (chapters == 0) 0 else mastered * 100 / chapters
}

/** Chương thành thạo = bài kiểm cuối chương `<khoá>/<cấp>/<chương>#checkpoint` đã xong với điểm ≥ 80 (giai đoạn e mới sinh bài kiểm). */
fun courseStats(courseId: String, outline: List<OutlineLevel>, progress: Map<String, ProgressEntity>): CourseStats {
    fun doneRow(id: String) = progress[id]?.takeIf { !it.deleted && it.status == "done" }
    val lessons = outline.flatMap { lv -> lv.chapters.flatMap { it.lessons } }
    val chapters = outline.flatMap { lv -> lv.chapters.map { "$courseId/${lv.level}/${it.id}" } }
    return CourseStats(
        done = lessons.count { doneRow(it.id) != null },
        ready = lessons.count { it.status == "ready" },
        mastered = chapters.count { (doneRow("$it#checkpoint")?.score ?: 0) >= 80 },
        chapters = chapters.size,
    )
}
