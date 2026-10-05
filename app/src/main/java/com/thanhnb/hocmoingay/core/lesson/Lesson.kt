package com.thanhnb.hocmoingay.core.lesson

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** JSON giáo trình: trường lạ bỏ qua, loại card nằm ở khoá "type". */
val LessonJson = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

@Serializable
data class LessonBody(
    val title: String,
    val kind: String = "concept",
    @SerialName("estimate_min") val estimateMin: Int = 5,
    @SerialName("can_do") val canDo: String = "",
    val hints: List<String> = emptyList(),
    val pattern: List<String> = emptyList(),
    val difficulty: String? = null,
    val review: List<ReviewNote> = emptyList(),
    @Transient val cards: List<Card> = emptyList(), // parse riêng từng card để một card hỏng không kéo cả bài
) {
    /** Bài có thẻ ôn (card `review: true` hoặc ghi chú `review`): màn Xong mới nói "đã vào lịch ôn". */
    val hasReview: Boolean get() = cards.any { it.review } || review.isNotEmpty()
}

@Serializable data class ReviewNote(val key: String, val front: String, val back: String)

@Serializable
sealed interface Card {
    val key: String
    val review: Boolean
}

// ===== Dùng chung
@Serializable @SerialName("explain") data class Explain(override val key: String, override val review: Boolean = false, val md: String) : Card
@Serializable @SerialName("quiz") data class Quiz(
    override val key: String, override val review: Boolean = false,
    val q: String, val choices: List<String>, val answer: List<Int>, val why: List<String> = emptyList(),
) : Card
@Serializable @SerialName("match") data class Match(override val key: String, override val review: Boolean = false, val pairs: List<List<String>>) : Card
@Serializable @SerialName("free_text") data class FreeText(
    override val key: String, override val review: Boolean = false,
    val mode: String, val prompt: String, val rubric: String = "", val sample: String = "",
) : Card

// ===== Lập trình
@Serializable @SerialName("predict_output") data class PredictOutput(
    override val key: String, override val review: Boolean = false,
    val lang: String, val code: String, val answer: String, val choices: List<String>? = null,
) : Card
@Serializable @SerialName("run_example") data class RunExample(override val key: String, override val review: Boolean = false, val lang: String, val code: String) : Card
@Serializable @SerialName("fill_blank") data class FillBlank(
    override val key: String, override val review: Boolean = false,
    val lang: String, val text: String, val answers: List<List<String>>,
) : Card
@Serializable @SerialName("order_lines") data class OrderLines(
    override val key: String, override val review: Boolean = false,
    val lang: String, val lines: List<String>, val distractors: List<String> = emptyList(),
    val fade: List<JsonArray> = emptyList(), // [[chỉ số dòng từ 0, token bị che]]
) : Card {
    val fades: Map<Int, String> get() = fade.associate { it[0].jsonPrimitive.int to it[1].jsonPrimitive.content }
}
@Serializable @SerialName("find_bug") data class FindBug(
    override val key: String, override val review: Boolean = false,
    val lang: String, val code: String, @SerialName("bug_lines") val bugLines: List<Int>, val why: String,
) : Card
@Serializable data class Issue(val lines: List<Int>, val why: String)
@Serializable @SerialName("code_review") data class CodeReview(
    override val key: String, override val review: Boolean = false,
    val lang: String, val code: String, val issues: List<Issue>,
) : Card
@Serializable @SerialName("code") data class CodeTask(
    override val key: String, override val review: Boolean = false,
    val kind: String = "function",
    @SerialName("prompt_md") val promptMd: String = "",
    val langs: List<String> = emptyList(),
    val starter: JsonElement? = null,          // chuỗi chung, hoặc {ngôn ngữ: code}
    val tests: List<JsonObject> = emptyList(), // chỉ test công khai
    val solutions: Map<String, String> = emptyMap(), // chỉ bài concept mới có
    val signature: JsonObject? = null,
) : Card {
    fun starterFor(lang: String): String? = when (val s = starter) {
        is JsonPrimitive -> s.content
        is JsonObject -> (s[lang] as? JsonPrimitive)?.content
        else -> null
    }
}

// ===== Tiếng Anh (giai đoạn d vẽ; c chỉ parse để không bị coi là card lạ)
@Serializable @SerialName("vocab") data class Vocab(
    override val key: String, override val review: Boolean = false, val word: String, val ipa: String = "",
    @SerialName("meaning_vi") val meaningVi: String = "", val examples: List<String> = emptyList(),
    val collocations: List<String> = emptyList(), val cloze: String = "",
) : Card
@Serializable @SerialName("listen") data class Listen(
    override val key: String, override val review: Boolean = false, val text: String? = null, val audio: String? = null,
    val questions: List<JsonObject> = emptyList(),
) : Card
@Serializable @SerialName("read") data class Read(override val key: String, override val review: Boolean = false, val md: String, val questions: List<JsonObject> = emptyList()) : Card
@Serializable @SerialName("minimal_pair") data class MinimalPair(override val key: String, override val review: Boolean = false, val pairs: List<List<String>>, val focus: String = "") : Card
@Serializable @SerialName("speak") data class Speak(override val key: String, override val review: Boolean = false, val text: String) : Card
@Serializable @SerialName("shadow") data class Shadow(override val key: String, override val review: Boolean = false, val text: String) : Card
@Serializable @SerialName("timed_talk") data class TimedTalk(
    override val key: String, override val review: Boolean = false, val prompt: String,
    val rounds: List<Int> = listOf(60, 45, 30), @SerialName("must_use") val mustUse: List<String> = emptyList(),
) : Card
@Serializable @SerialName("dialogue") data class Dialogue(
    override val key: String, override val review: Boolean = false, val setting: String = "", val turns: List<JsonObject>,
) : Card
@Serializable @SerialName("speak_free") data class SpeakFree(
    override val key: String, override val review: Boolean = false, val prompt: String, val rubric: String = "", val sample: String = "",
) : Card

/** Trả null khi body không phải object JSON. Card lạ hoặc thiếu trường thì bỏ qua và ghi log, các card khác vẫn dùng được. */
fun parseLesson(json: String, log: (String) -> Unit = { Log.w("Lesson", it) }): LessonBody? {
    val o = try {
        LessonJson.parseToJsonElement(json) as? JsonObject
    } catch (e: IllegalArgumentException) { // SerializationException là lớp con
        null
    } ?: return null.also { log("body bài không phải object JSON") }
    val meta = try {
        LessonJson.decodeFromJsonElement(LessonBody.serializer(), o)
    } catch (e: IllegalArgumentException) {
        log("meta bài hỏng: ${e.message}"); return null
    }
    val cards = (o["cards"] as? JsonArray).orEmpty().mapNotNull { el ->
        try {
            LessonJson.decodeFromJsonElement(Card.serializer(), el)
        } catch (e: IllegalArgumentException) {
            log("bỏ card ${(el as? JsonObject)?.get("key")}: ${e.message?.lineSequence()?.firstOrNull()}"); null
        }
    }
    return meta.copy(cards = cards)
}
