package com.thanhnb.hocmoingay.core.lesson

import com.thanhnb.hocmoingay.core.review.reviewed
import com.thanhnb.hocmoingay.core.review.resolveRating
import com.thanhnb.hocmoingay.core.log.XP_CODE_FIRST_TRY
import kotlinx.serialization.json.intOrNull
import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import com.thanhnb.hocmoingay.core.log.NEW_CARDS_PER_DAY
import com.thanhnb.hocmoingay.core.log.XP_LESSON
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.JsonPrimitive
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.sync.ReviewCardIds
import com.thanhnb.hocmoingay.core.sync.nextUpdatedAt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/** Bài luyện code mà card code bấm "Để sau": chưa nộp đạt thì bài chưa tính là xong. */
fun LessonBody.codePending(state: JsonObject): Boolean = kind == "problem" &&
    cards.any { it is CodeTask && ((state[it.key] as? JsonObject)?.get("pass") as? JsonPrimitive)?.booleanOrNull != true }

fun cardStateOf(row: ProgressEntity?): JsonObject =
    row?.cardState?.let { runCatching { LessonJson.parseToJsonElement(it) as? JsonObject }.getOrNull() } ?: JsonObject(emptyMap())

/**
 * Ghi tiến độ học theo luật đồng bộ của b: đọc bản cũ, mốc mới hơn, dirty, rồi afterWrite.
 * Mọi đọc–sửa–ghi nằm trong [tx] (cùng transaction với hàm gộp của SyncEngine).
 */
class LessonRepo(
    private val lesson: suspend (String) -> LessonEntity?,
    private val track: suspend (courseId: String) -> String?,
    private val getProgress: suspend (String) -> ProgressEntity?,
    private val putProgress: suspend (ProgressEntity) -> Unit,
    private val cardsByIds: suspend (List<String>) -> List<ReviewCardEntity>,
    private val putCards: suspend (List<ReviewCardEntity>) -> Unit,
    private val tx: suspend (suspend () -> Unit) -> Unit,
    private val userId: () -> String?,
    private val afterWrite: () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
    private val cpu: CoroutineDispatcher = Dispatchers.Default, // test truyền dispatcher của runTest
    private val log: DailyLogRepo? = null,                      // null ở test cũ: không ghi XP/thẻ mới
    private val course: suspend (String) -> CourseEntity? = { null }, // đề cương để dựng bài kiểm cuối chương
) {
    data class Loaded(val entity: LessonEntity, val body: LessonBody, val track: String = "code")

    suspend fun load(lessonId: String): Loaded? {
        val e = lesson(lessonId) ?: return null
        val b = withContext(cpu) { parseLesson(e.body) } ?: return null
        return Loaded(e, b, track(e.courseId) ?: "code")
    }

    private suspend fun edit(lessonId: String, change: (ProgressEntity) -> ProgressEntity) {
        val uid = userId() ?: return
        tx {
            val old = getProgress(lessonId)?.takeUnless { it.deleted }
            val base = old ?: ProgressEntity(lessonId = lessonId, userId = uid, status = "started", updatedAt = 0)
            val new = change(base)
            if (new != base || old == null) {
                putProgress(new.copy(updatedAt = nextUpdatedAt(getProgress(lessonId)?.updatedAt, now()), dirty = true, deleted = false))
            }
        }
        afterWrite()
    }

    /** Mở bài: tạo hàng `started` nếu chưa có; bài đã `done` thì giữ nguyên. */
    suspend fun cardState(lessonId: String, key: String): JsonObject =
        cardStateOf(getProgress(lessonId))[key] as? JsonObject ?: JsonObject(emptyMap())
    suspend fun language(lessonId: String): String? = getProgress(lessonId)?.language

    suspend fun start(lessonId: String) = edit(lessonId) { it }

    suspend fun updateCard(lessonId: String, key: String, change: (JsonObject) -> JsonObject) = edit(lessonId) { p ->
        val s = cardStateOf(p)
        val cur = s[key] as? JsonObject ?: JsonObject(emptyMap())
        p.copy(cardState = JsonObject(s + (key to change(cur))).toString())
    }

    suspend fun setLanguage(lessonId: String, lang: String) = edit(lessonId) { it.copy(language = lang) }

    /** Số nấc gợi ý đã mở (chỉ tăng); d dùng để chấm thẻ resolve. */
    suspend fun useHint(lessonId: String, level: Int) = edit(lessonId) { it.copy(hintsUsed = maxOf(it.hintsUsed, level)) }

    /**
     * Một lần nộp code (thay khối ghi card_state trước đây nằm trong EditorViewModel).
     * - Lượt thường: đạt → pass/lang/hints; trượt trước lần đạt đầu → fails + 1 (Quyết định 7 của d).
     * - Đạt lần đầu với 0 lần trượt: +10 XP.
     * - Bài `problem`: chưa có thẻ `resolve` thì tạo và chấm ngay theo (gợi ý, fails). Lượt giải lại ([review])
     *   chấm thẻ đó theo gợi ý và [sessionFails] của chính lượt này, không sửa card_state, không cộng XP.
     */
    suspend fun recordSubmit(
        lessonId: String, key: String, lang: String, pass: Boolean, hints: Int,
        review: Boolean = false, sessionFails: Int = 0,
    ) {
        val uid = userId() ?: return
        val before = cardState(lessonId, key)
        if (!review) updateCard(lessonId, key) { s ->
            JsonObject(
                s + when {
                    pass -> mapOf("pass" to JsonPrimitive(true), "lang" to JsonPrimitive(lang), "hints" to JsonPrimitive(hints))
                    s.flag("pass") -> emptyMap() // đã đạt rồi: fails chỉ đếm trượt trước lần đạt đầu
                    else -> mapOf("fails" to JsonPrimitive(s.count("fails") + 1))
                },
            )
        }
        if (!pass) return
        val t = now()
        val fails = before.count("fails")
        if (!review && !before.flag("pass") && fails == 0) log?.add(t) { it.copy(xp = it.xp + XP_CODE_FIRST_TRY) }
        val l = load(lessonId) ?: return
        if (l.body.kind != "problem") return
        val ref = "$lessonId#$key"
        val cardId = ReviewCardIds.of(uid, ref)
        var wrote = false
        tx {
            val raw = cardsByIds(listOf(cardId)).firstOrNull()
            val old = raw?.takeUnless { it.deleted }
            if (old != null && !review) return@tx // nộp lại ngoài lượt ôn: lịch giữ nguyên
            val base = old ?: ReviewCardEntity(
                id = cardId, userId = uid, ref = ref, kind = "resolve", track = l.track, courseId = l.entity.courseId,
                due = t, updatedAt = 0,
            )
            val r = if (review) resolveRating(hints, sessionFails) else resolveRating(hints, fails)
            // đọc updatedAt cả của hàng đã xoá để bản mới thắng LWW (minor 1 của c)
            putCards(listOf(base.reviewed(r, t).copy(updatedAt = nextUpdatedAt(raw?.updatedAt, t), dirty = true, deleted = false)))
            wrote = true
        }
        if (wrote) afterWrite()
    }

    /**
     * Xong bài: `done`, điểm cao nhất, và thẻ recall (New) cho card `review: true` và ghi chú `review` chưa có thẻ.
     * Quá [NEW_CARDS_PER_DAY] thẻ mới trong ngày thì thẻ dư hẹn từ 0 giờ hôm sau (spec §7.4).
     * Lần đầu thành `done`: `lessons` + 1, +20 XP.
     */
    suspend fun finish(lessonId: String, score: Int) {
        val uid = userId() ?: return
        val l = load(lessonId) ?: return
        val trackName = l.track
        val t = now()
        var firstDone = false
        edit(lessonId) { p ->
            val best = maxOf(p.score ?: 0, score)
            if (l.body.codePending(cardStateOf(p))) p.copy(score = best)
            else {
                firstDone = p.status != "done"
                p.copy(status = "done", score = best, completedAt = p.completedAt ?: t)
            }
        }
        val refs = l.body.cards.filter { it.review }.map { "$lessonId#${it.key}" } + l.body.review.map { "$lessonId#${it.key}" }
        val usedToday = log?.day(t)?.newCards ?: 0
        var fresh = 0
        tx {
            val ids = refs.associateBy { ReviewCardIds.of(uid, it) }
            val have = cardsByIds(ids.keys.toList()).filterNot { it.deleted }.map { it.id }.toSet()
            val add = ids.filterKeys { it !in have }.entries.mapIndexed { i, (id, ref) ->
                val due = if (log == null || usedToday + i < NEW_CARDS_PER_DAY) t else log.nextDayStart(t)
                ReviewCardEntity(
                    id = id, userId = uid, ref = ref, kind = "recall", track = trackName, courseId = l.entity.courseId,
                    due = due, updatedAt = nextUpdatedAt(null, t), dirty = true,
                )
            }
            fresh = add.size
            if (add.isNotEmpty()) putCards(add)
        }
        afterWrite()
        if (log != null && (firstDone || fresh > 0)) log.add(t) {
            it.copy(
                lessons = it.lessons + if (firstDone) 1 else 0,
                xp = it.xp + if (firstDone) XP_LESSON else 0,
                newCards = it.newCards + fresh,
            )
        }
    }
}

private fun JsonObject.flag(k: String) = (this[k] as? JsonPrimitive)?.booleanOrNull == true
private fun JsonObject.count(k: String) = (this[k] as? JsonPrimitive)?.intOrNull ?: 0
