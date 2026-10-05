package com.thanhnb.hocmoingay.core.review

import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import com.thanhnb.hocmoingay.core.sync.nextUpdatedAt

/** Bảng không có cột step: app không dùng bước học (Quyết định 2), nên thẻ chưa vào Review luôn ở bước 0. */
fun ReviewCardEntity.memo() = Memo(
    state = state,
    step = if (state == CardState.REVIEW) null else 0,
    stability = stability.takeIf { reps > 0 },
    difficulty = difficulty.takeIf { reps > 0 },
    due = due,
    lastReview = lastReview,
)

fun ReviewCardEntity.reviewed(r: Rating, now: Long, f: Fsrs = AppFsrs): ReviewCardEntity {
    val n = f.review(memo(), r, now)
    return copy(
        state = n.state, stability = n.stability ?: 0.0, difficulty = n.difficulty ?: 0.0, due = n.due, lastReview = now,
        elapsedDays = lastReview?.let { wholeDays(now - it).toInt().coerceAtLeast(0) } ?: 0,
        scheduledDays = wholeDays(n.due - now).toInt(),
        reps = reps + 1,
        lapses = lapses + if (r == Rating.AGAIN && state == CardState.REVIEW) 1 else 0,
    )
}

/** Thẻ `recall` đến hạn và chấm thẻ (spec §7.4). Thẻ `resolve` thuộc giai đoạn e. */
class ReviewRepo(
    private val due: suspend (now: Long, limit: Int) -> List<ReviewCardEntity>,
    private val byIds: suspend (List<String>) -> List<ReviewCardEntity>,
    private val put: suspend (ReviewCardEntity) -> Unit,
    private val log: DailyLogRepo,
    private val tx: suspend (suspend () -> Unit) -> Unit,
    private val afterWrite: () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
    private val fsrs: Fsrs = AppFsrs,
) {
    suspend fun dueCards(limit: Int = 100): List<ReviewCardEntity> = due(now(), limit)

    /** Khoảng cách (ms) tới lần ôn tiếp nếu bấm từng nút, để hiện trước dưới nút. */
    fun preview(c: ReviewCardEntity): Map<Rating, Long> {
        val t = now()
        return fsrs.preview(c.memo(), t).mapValues { it.value.due - t }
    }

    suspend fun rate(id: String, r: Rating) {
        val t = now()
        var wrote = false
        tx {
            val old = byIds(listOf(id)).firstOrNull()?.takeUnless { it.deleted }
            if (old != null) {
                put(old.reviewed(r, t, fsrs).copy(updatedAt = nextUpdatedAt(old.updatedAt, t), dirty = true))
                wrote = true
            }
        }
        if (!wrote) return
        afterWrite()
        log.add(t) { it.copy(reviews = it.reviews + 1) }
    }
}
