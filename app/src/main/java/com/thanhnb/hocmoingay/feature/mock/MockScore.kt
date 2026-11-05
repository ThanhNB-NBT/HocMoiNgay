package com.thanhnb.hocmoingay.feature.mock

import com.thanhnb.hocmoingay.core.lesson.Card
import com.thanhnb.hocmoingay.core.lesson.Gap
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.Listen
import com.thanhnb.hocmoingay.core.lesson.MockPart
import com.thanhnb.hocmoingay.core.lesson.Quiz
import com.thanhnb.hocmoingay.core.lesson.Read
import com.thanhnb.hocmoingay.feature.player.gapNorm
import com.thanhnb.hocmoingay.feature.player.parseQuestions
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Một câu chấm được trong đề thi thử, [id] = "<key card>#<thứ tự>". Câu chọn: [choices] + [answer] (chỉ số từ 0);
 * câu gõ (gap): [accepted]. Câu trả lời lưu dạng chuỗi: chỉ số đã chọn nối dấu phẩy, hoặc chữ đã gõ.
 */
data class MockQ(
    val id: String, val q: String, val choices: List<String> = emptyList(), val answer: List<Int> = emptyList(),
    val accepted: List<String> = emptyList(), val why: String = "",
) {
    val typed: Boolean get() = choices.isEmpty()
    fun ok(given: String?): Boolean = when {
        given.isNullOrBlank() -> false
        typed -> accepted.any { gapNorm(it) == gapNorm(given) }
        else -> given.split(',').mapNotNull { it.toIntOrNull() }.toSet() == answer.toSet()
    }
    fun shown(given: String?): String = when {
        given.isNullOrBlank() -> "(bỏ trống)"
        typed -> given
        else -> given.split(',').mapNotNull { it.toIntOrNull() }.joinToString("; ") { choices.getOrElse(it) { "?" } }
    }
    val right: String get() = if (typed) accepted.firstOrNull().orEmpty() else answer.joinToString("; ") { choices.getOrElse(it) { "?" } }
}

/** Các câu chấm được trong một card; explain (hướng dẫn) không có câu nào. */
fun mockQuestions(card: Card): List<MockQ> = when (card) {
    is Quiz -> listOf(MockQ("${card.key}#0", card.q, card.choices, card.answer, why = card.why.getOrNull(card.answer.firstOrNull() ?: 0).orEmpty()))
    is Listen -> parseQuestions(card.questions).mapIndexed { i, q -> MockQ("${card.key}#$i", q.q, q.choices, q.answer, why = q.why.getOrNull(q.answer.first()).orEmpty()) }
    is Read -> parseQuestions(card.questions).mapIndexed { i, q -> MockQ("${card.key}#$i", q.q, q.choices, q.answer, why = q.why.getOrNull(q.answer.first()).orEmpty()) }
    is Gap -> card.answers.mapIndexed { i, a -> MockQ("${card.key}#$i", "", accepted = a, why = card.why) }
    else -> emptyList()
}

/** Cắt [LessonBody.cards] theo [LessonBody.parts]: phần nào lấy đúng số card của phần đó, theo thứ tự. */
fun partCards(body: LessonBody): List<Pair<MockPart, List<Card>>> {
    var from = 0
    return body.parts.map { p -> (p to body.cards.drop(from).take(p.cards)).also { from += p.cards } }
}

data class PartResult(val part: MockPart, val correct: Int, val total: Int, val label: String)

fun mockResults(body: LessonBody, answers: Map<String, String>): List<PartResult> = partCards(body).map { (p, cards) ->
    val qs = cards.flatMap(::mockQuestions)
    val ok = qs.count { it.ok(answers[it.id]) }
    PartResult(p, ok, qs.size, scaledLabel(body.exam.orEmpty(), p.skill, ok, qs.size))
}

/** Tổng TOEIC L&R (10–990) khi đề có cả hai phần; IELTS không cộng vì band tổng cần đủ 4 kỹ năng. */
fun toeicTotal(exam: String, results: List<PartResult>): Int? =
    if (exam != "toeic_lr" || results.isEmpty()) null
    else results.sumOf { toeicScaled(it.correct, it.total, it.part.skill == "reading") }

/** Phần trăm câu đúng cả đề: điểm bài lưu vào tiến độ. */
fun mockPercent(results: List<PartResult>): Int {
    val total = results.sumOf { it.total }
    return if (total == 0) 0 else results.sumOf { it.correct } * 100 / total
}

fun scaledLabel(exam: String, skill: String, correct: Int, total: Int): String = when (exam) {
    "toeic_lr" -> "${toeicScaled(correct, total, skill == "reading")}/495"
    else -> "Band " + String.format(Locale.US, "%.1f", ieltsBand(correct, total, bandTable(exam, skill)))
}

// ponytail: bảng ước tính. ETS không công bố bảng cố định (mỗi đề một bảng), đây là mốc trung bình hay được trích;
// IELTS lấy theo bảng Cambridge công bố cho đề 40 câu. Đề ngắn hơn được quy tỉ lệ về 100 / 40 câu.
private val TOEIC_L = intArrayOf(5, 40, 95, 150, 205, 255, 305, 355, 405, 455, 495) // điểm ở 0, 10, 20 … 100 câu đúng
private val TOEIC_R = intArrayOf(5, 20, 70, 120, 170, 220, 275, 330, 385, 435, 495)

/** TOEIC Listening/Reading: 5–495 mỗi phần, bước 5; nội suy giữa các mốc 10 câu. */
fun toeicScaled(correct: Int, total: Int, reading: Boolean): Int {
    if (total <= 0) return 5
    val raw = correct.coerceIn(0, total) * 100.0 / total
    val t = if (reading) TOEIC_R else TOEIC_L
    val i = (raw / 10).toInt().coerceAtMost(9)
    val v = t[i] + (t[i + 1] - t[i]) * (raw - i * 10) / 10
    return ((v / 5).roundToInt() * 5).coerceIn(5, 495)
}

/** (số câu đúng tối thiểu trên 40, band), xếp giảm dần. */
val IELTS_LISTENING = listOf(39 to 9.0, 37 to 8.5, 35 to 8.0, 32 to 7.5, 30 to 7.0, 26 to 6.5, 23 to 6.0, 18 to 5.5, 16 to 5.0,
    13 to 4.5, 10 to 4.0, 8 to 3.5, 6 to 3.0, 4 to 2.5, 2 to 2.0, 1 to 1.0)
val IELTS_ACADEMIC_READING = listOf(39 to 9.0, 37 to 8.5, 35 to 8.0, 33 to 7.5, 30 to 7.0, 27 to 6.5, 23 to 6.0, 19 to 5.5, 15 to 5.0,
    13 to 4.5, 10 to 4.0, 8 to 3.5, 6 to 3.0, 4 to 2.5, 2 to 2.0, 1 to 1.0)
val IELTS_GENERAL_READING = listOf(40 to 9.0, 39 to 8.5, 37 to 8.0, 36 to 7.5, 34 to 7.0, 32 to 6.5, 30 to 6.0, 27 to 5.5, 23 to 5.0,
    19 to 4.5, 15 to 4.0, 12 to 3.5, 9 to 3.0, 6 to 2.5, 3 to 2.0, 1 to 1.0)

fun bandTable(exam: String, skill: String): List<Pair<Int, Double>> = when {
    skill == "listening" -> IELTS_LISTENING
    exam == "ielts_general" -> IELTS_GENERAL_READING
    else -> IELTS_ACADEMIC_READING
}

fun ieltsBand(correct: Int, total: Int, table: List<Pair<Int, Double>>): Double {
    if (total <= 0) return 0.0
    val raw = (correct.coerceIn(0, total) * 40.0 / total).roundToInt()
    return table.firstOrNull { raw >= it.first }?.second ?: 0.0
}
