package com.thanhnb.hocmoingay.core.lesson

import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.sync.ReviewCardIds
import com.thanhnb.hocmoingay.core.sync.nextUpdatedAt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

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
) {
    data class Loaded(val entity: LessonEntity, val body: LessonBody)

    suspend fun load(lessonId: String): Loaded? {
        val e = lesson(lessonId) ?: return null
        val b = withContext(cpu) { parseLesson(e.body) } ?: return null
        return Loaded(e, b)
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

    /** Xong bài: `done`, điểm cao nhất, và thẻ recall (New, đến hạn ngay) cho card `review: true` và ghi chú `review` chưa có thẻ. */
    suspend fun finish(lessonId: String, score: Int) {
        val uid = userId() ?: return
        val l = load(lessonId) ?: return
        val trackName = track(l.entity.courseId) ?: "code"
        val t = now()
        edit(lessonId) { p -> p.copy(status = "done", score = maxOf(p.score ?: 0, score), completedAt = p.completedAt ?: t) }
        val refs = l.body.cards.filter { it.review }.map { "$lessonId#${it.key}" } + l.body.review.map { "$lessonId#${it.key}" }
        tx {
            val ids = refs.associateBy { ReviewCardIds.of(uid, it) }
            val have = cardsByIds(ids.keys.toList()).filterNot { it.deleted }.map { it.id }.toSet()
            val fresh = ids.filterKeys { it !in have }.map { (id, ref) ->
                ReviewCardEntity(
                    id = id, userId = uid, ref = ref, kind = "recall", track = trackName, courseId = l.entity.courseId,
                    due = t, updatedAt = nextUpdatedAt(null, t), dirty = true,
                )
            }
            if (fresh.isNotEmpty()) putCards(fresh)
        }
        afterWrite()
    }
}
