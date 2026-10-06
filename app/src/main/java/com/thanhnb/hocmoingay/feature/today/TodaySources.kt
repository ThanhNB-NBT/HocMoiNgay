package com.thanhnb.hocmoingay.feature.today

import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.CHECKPOINT
import com.thanhnb.hocmoingay.core.lesson.OutlineLesson
import com.thanhnb.hocmoingay.core.lesson.OutlineLevel
import com.thanhnb.hocmoingay.core.lesson.parseOutline
import com.thanhnb.hocmoingay.core.lesson.pendingCheckpoints

const val PRACTICE_COURSE = "luyen-code"
const val ENGLISH_COURSE = "english-work"
const val CHECKPOINT_MINUTES = 10
const val PLACEMENT_MINUTES = 10

private fun List<OutlineLevel>.lessons() = flatMap { lv -> lv.chapters.flatMap { it.lessons } }

/** Khoá lập trình đang học: active_courses → khoá có tiến độ mới nhất → khoá đầu tiên có bài ready. `luyen-code` có mục riêng. */
fun activeCodeCourse(courses: List<CourseEntity>, active: List<String>, progress: List<ProgressEntity>): CourseEntity? {
    val code = courses.filter { it.track == "code" && it.id != PRACTICE_COURSE }
    code.firstOrNull { it.id in active }?.let { return it }
    return progress.filter { !it.deleted }.sortedByDescending { it.updatedAt }
        .firstNotNullOfOrNull { p -> code.firstOrNull { it.id == p.lessonId.substringBefore('/') } }
        ?: code.firstOrNull { c -> parseOutline(c.outline).lessons().any { it.status == "ready" } }
}

/** Bài `ready` đầu tiên chưa xong theo thứ tự đề cương; [level] giới hạn một cấp (tiếng Anh theo trình độ). */
fun nextLesson(outline: List<OutlineLevel>, done: Set<String>, level: String? = null): OutlineLesson? =
    outline.filter { level == null || it.level == level }.lessons().firstOrNull { it.status == "ready" && it.id !in done }

/** Luyện code: ưu tiên bài chưa làm có mẫu giải đã gặp ở bài luyện đã xong; chưa có thì bài đầu tiên chưa làm. */
fun pickPractice(lessons: List<Pair<OutlineLesson, List<String>>>, done: Set<String>): OutlineLesson? {
    val learned = lessons.filter { it.first.id in done }.flatMap { it.second }.toSet()
    val open = lessons.filter { it.first.status == "ready" && it.first.id !in done }
    return (open.firstOrNull { (_, p) -> p.any { it in learned } } ?: open.firstOrNull())?.first
}

/** Bài kiểm đang chờ đầu tiên của khoá, kèm tên chương. */
fun checkpointPick(c: CourseEntity, progress: Map<String, ProgressEntity>, today: String, dayOf: (Long) -> String): Pick? {
    val outline = parseOutline(c.outline)
    val id = pendingCheckpoints(c.id, outline, progress, today, dayOf).firstOrNull() ?: return null
    val title = outline.firstNotNullOfOrNull { lv -> lv.chapters.firstOrNull { "${c.id}/${lv.level}/${it.id}$CHECKPOINT" == id }?.title } ?: ""
    return Pick(PickKind.CHECKPOINT, c.track, id, title, CHECKPOINT_MINUTES)
}
