package com.thanhnb.hocmoingay.feature.player

import com.thanhnb.hocmoingay.core.lesson.Card
import com.thanhnb.hocmoingay.core.lesson.Dialogue
import com.thanhnb.hocmoingay.core.lesson.Explain
import com.thanhnb.hocmoingay.core.lesson.FreeText
import com.thanhnb.hocmoingay.core.lesson.LessonJson
import com.thanhnb.hocmoingay.core.lesson.Listen
import com.thanhnb.hocmoingay.core.lesson.Match
import com.thanhnb.hocmoingay.core.lesson.MinimalPair
import com.thanhnb.hocmoingay.core.lesson.Quiz
import com.thanhnb.hocmoingay.core.lesson.Read
import com.thanhnb.hocmoingay.core.lesson.Shadow
import com.thanhnb.hocmoingay.core.lesson.Speak
import com.thanhnb.hocmoingay.core.lesson.SpeakFree
import com.thanhnb.hocmoingay.core.lesson.TimedTalk
import com.thanhnb.hocmoingay.core.lesson.Vocab
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Mạch của card trong bài tiếng Anh (spec §6.3, 4 mạch của Nation). Card lập trình trả null. */
fun strandOf(card: Card): String? = when (card) {
    is Vocab, is MinimalPair, is Speak, is Quiz, is Match -> "language"
    is Listen, is Read, is Explain -> "input"
    is Shadow, is TimedTalk -> "fluency"
    is Dialogue, is SpeakFree, is FreeText -> "output"
    else -> null
}

/** Câu hỏi của listen/read: `answer` là mảng chỉ số từ 0 (Kết quả f1). */
@Serializable
data class ListenQuestion(val q: String, val choices: List<String>, val answer: List<Int> = listOf(0), val why: List<String> = emptyList())

fun parseQuestions(raw: List<JsonObject>): List<ListenQuestion> = raw.mapNotNull { o ->
    runCatching { LessonJson.decodeFromJsonElement(ListenQuestion.serializer(), o) }.getOrNull()
        ?.takeIf { q -> q.answer.isNotEmpty() && q.answer.all { it in q.choices.indices } }
}

@Serializable data class DialogueOption(val text: String, val ok: Boolean = false, val why: String = "")

sealed interface Turn {
    data class Line(val who: String, val text: String, val vi: String) : Turn
    data class Pick(val options: List<DialogueOption>) : Turn
}

/** `{who, text, vi?}` hoặc `{who: me, options: [...]}`; lượt chọn không có đáp án đúng bị bỏ để không kẹt bài. */
fun parseTurns(raw: List<JsonObject>): List<Turn> = raw.mapNotNull { o ->
    val who = (o["who"] as? JsonPrimitive)?.content ?: return@mapNotNull null
    if (who == "me") {
        val opts = o["options"]?.let { runCatching { LessonJson.decodeFromJsonElement(ListSerializer(DialogueOption.serializer()), it) }.getOrNull() }
        opts?.takeIf { l -> l.any { it.ok } }?.let { Turn.Pick(it) }
    } else {
        (o["text"] as? JsonPrimitive)?.content?.let { Turn.Line(who, it, (o["vi"] as? JsonPrimitive)?.content.orEmpty()) }
    }
}

/** minimal_pair (C6): mỗi cặp một lượt; app đọc một từ chọn theo seed nên xoay màn không đổi. */
fun minimalRounds(pairs: List<List<String>>, seed: String): List<Pair<List<String>, Int>> {
    val rnd = Random(seed.hashCode())
    return pairs.filter { it.size >= 2 }.map { it to rnd.nextInt(it.size) }
}
