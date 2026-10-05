package com.thanhnb.hocmoingay.feature.review

import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.lesson.Card
import com.thanhnb.hocmoingay.core.lesson.CodeTask
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.Vocab
import com.thanhnb.hocmoingay.feature.player.norm

/** Một thẻ đến hạn đã ghép với nội dung bài. */
sealed interface ReviewItem {
    val card: ReviewCardEntity
    /** Lật mặt trước/sau rồi tự chấm bằng 4 nút. */
    data class Note(override val card: ReviewCardEntity, val front: String, val back: String) : ReviewItem
    /** Gõ cụm vào chỗ trống của `cloze`, tự chấm. */
    data class Cloze(override val card: ReviewCardEntity, val vocab: Vocab, val answer: String) : ReviewItem
    /** Card của bài, vẽ bằng CardView; card tự chấm thì Good/Again, không chấm thì 4 nút. */
    data class Lesson(override val card: ReviewCardEntity, val lessonId: String, val item: Card) : ReviewItem
}

/** null = bài hoặc card không còn (đổi key, xoá) hoặc là card code: bỏ qua, không làm kẹt phiên ôn. */
fun reviewItem(card: ReviewCardEntity, body: LessonBody?): ReviewItem? {
    body ?: return null
    val key = card.ref.substringAfterLast('#')
    body.review.firstOrNull { it.key == key }?.let { return ReviewItem.Note(card, it.front, it.back) }
    return when (val c = body.cards.firstOrNull { it.key == key }) {
        null, is CodeTask -> null
        is Vocab -> clozeAnswer(c.cloze, c.examples)?.let { ReviewItem.Cloze(card, c, it) } ?: ReviewItem.Note(card, c.meaningVi, c.word)
        else -> ReviewItem.Lesson(card, card.ref.substringBeforeLast('#'), c)
    }
}

/** Phần chữ điền vào `___` để `cloze` thành đúng một câu trong `examples` (Kết quả f1). */
fun clozeAnswer(cloze: String, examples: List<String>): String? {
    val i = cloze.indexOf("___")
    if (i < 0) return null
    val pre = cloze.substring(0, i)
    val suf = cloze.substring(i + 3)
    return examples.firstNotNullOfOrNull { e ->
        if (e.length > pre.length + suf.length && e.startsWith(pre) && e.endsWith(suf)) e.substring(pre.length, e.length - suf.length).trim().ifEmpty { null } else null
    }
}

fun clozeOk(given: String, answer: String): Boolean = norm(given).trimEnd('.', '!', '?').lowercase() == norm(answer).lowercase()
