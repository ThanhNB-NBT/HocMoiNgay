package com.thanhnb.hocmoingay.feature.today

import com.thanhnb.hocmoingay.core.db.ReviewCardEntity

/** Thẻ ôn ước 15 giây/thẻ khi chia thời lượng (spec không nói; D×2 thẻ ≈ D/2 phút). */
const val REVIEW_SECONDS = 15

enum class PickKind { RESOLVE, CHECKPOINT, NEXT, PRACTICE, PLACEMENT }

sealed interface TodayItem { val minutes: Int }

data class ReviewBlock(val cards: List<ReviewCardEntity>) : TodayItem {
    override val minutes: Int get() = (cards.size * REVIEW_SECONDS + 59) / 60
}

/** Một bài (hoặc thẻ resolve, hoặc bài xếp lớp) trong hàng đợi. [track] "code" | "english". */
data class Pick(
    val kind: PickKind, val track: String, val lessonId: String, val title: String,
    override val minutes: Int, val cardKey: String? = null,
) : TodayItem

/** [extra]: vượt phần thời gian còn lại, vẫn hiện nhưng gắn nhãn "thêm" (spec §7.4). */
data class Planned(val item: TodayItem, val extra: Boolean)

/** Nguồn của hàng đợi; mục code xét theo thứ tự resolve → bài kiểm → bài tiếp → luyện code. */
data class TodayInput(
    val dailyMinutes: Int,
    val doneMinutes: Int = 0,
    val due: List<ReviewCardEntity> = emptyList(),
    val resolve: Pick? = null,
    val codeCheckpoint: Pick? = null,
    val codeNext: Pick? = null,
    val practice: Pick? = null,
    val englishCheckpoint: Pick? = null,
    val englishNext: Pick? = null,
)

/** Spec §7.4: ôn thẻ recall (≤ D×2, quá hạn lâu nhất, xen kẽ) → một mục code → một bài tiếng Anh. */
fun planToday(i: TodayInput): List<Planned> {
    val d = i.dailyMinutes
    val reviews = interleave(i.due.sortedBy { it.due }.take(d * 2))
    val code = listOfNotNull(i.resolve?.takeIf { d >= 20 }, i.codeCheckpoint, i.codeNext, i.practice).firstOrNull()
    val english = i.englishCheckpoint ?: i.englishNext
    var left = d - i.doneMinutes
    return listOfNotNull(reviews.takeIf { it.isNotEmpty() }?.let(::ReviewBlock), code, english).map { item ->
        val extra = item.minutes > left
        if (!extra) left -= item.minutes
        Planned(item, extra)
    }
}

/**
 * Xếp xen kẽ theo khoá (A2): mỗi bước lấy thẻ sớm nhất của khoá còn nhiều thẻ nhất mà khác khoá vừa xếp;
 * hết cách thì đành để hai thẻ cùng khoá đứng liền.
 */
fun interleave(cards: List<ReviewCardEntity>): List<ReviewCardEntity> {
    val groups = LinkedHashMap<String, ArrayDeque<ReviewCardEntity>>()
    cards.forEach { groups.getOrPut(it.courseId) { ArrayDeque() }.addLast(it) }
    val out = ArrayList<ReviewCardEntity>(cards.size)
    var last: String? = null
    while (out.size < cards.size) {
        val open = groups.entries.filter { it.value.isNotEmpty() }
        val pick = open.filter { it.key != last }
            .maxWithOrNull(compareBy<Map.Entry<String, ArrayDeque<ReviewCardEntity>>> { it.value.size }.thenByDescending { it.value.first().due })
            ?: open.first()
        out += pick.value.removeFirst()
        last = pick.key
    }
    return out
}
