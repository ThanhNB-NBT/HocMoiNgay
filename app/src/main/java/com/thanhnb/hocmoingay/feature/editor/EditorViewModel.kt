package com.thanhnb.hocmoingay.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.db.CodeDraftDao
import com.thanhnb.hocmoingay.core.db.CodeDraftEntity
import com.thanhnb.hocmoingay.core.lesson.CodeTask
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.net.ApiResult
import com.thanhnb.hocmoingay.core.net.CodeApi
import com.thanhnb.hocmoingay.core.net.Judged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class EditorUi(
    val body: LessonBody? = null,
    val card: CodeTask? = null,
    val lang: String? = null,
    val code: String? = null,          // null = đang nạp
    val notice: String? = null,        // ví dụ: offline, chưa tải được code khởi đầu
    val busy: Busy? = null,
    val result: Outcome? = null,
    val hints: Int = 0,
    val hintLines: List<String>? = null,
    val missing: Boolean = false,
) {
    enum class Busy { RUN, SUBMIT, HINT }
    val maxHints: Int get() = if (body?.kind == "problem") body.hints.size + 1 else 0
}

sealed interface Outcome {
    data class Ran(val j: Judged) : Outcome
    data class Submitted(val j: Judged) : Outcome
    data class Failed(val message: String) : Outcome
}

class EditorViewModel(
    private val lessonId: String,
    private val cardKey: String,
    private val repo: LessonRepo,
    private val drafts: CodeDraftDao,
    private val api: CodeApi,
    val online: StateFlow<Boolean>,
    private val preferred: Flow<String>,
    private val flushScope: CoroutineScope,              // graph.scope: nháp cuối vẫn được ghi sau khi màn đóng
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _ui = MutableStateFlow(EditorUi())
    val ui = _ui.asStateFlow()
    private val pending = MutableStateFlow<CodeDraftEntity?>(null)

    init {
        // StateFlow tự gộp: gõ nhanh chỉ ghi bản mới nhất, ghi tuần tự nên không bao giờ bản cũ đè bản mới
        viewModelScope.launch { pending.filterNotNull().collect { drafts.put(it) } }
        viewModelScope.launch {
            val l = repo.load(lessonId)
            val card = l?.body?.cards?.firstOrNull { it.key == cardKey } as? CodeTask
            if (card == null || card.langs.isEmpty()) { _ui.update { it.copy(missing = true) }; return@launch }
            val st = repo.cardState(lessonId, cardKey)
            _ui.update { it.copy(body = l.body, card = card, hints = st.int("hints")) }
            val saved = repo.language(lessonId)
            val pref = preferred.first()
            pick(listOf(saved, pref).firstOrNull { it != null && it in card.langs } ?: card.langs.first())
        }
    }

    private fun JsonObject.int(k: String) = (this[k] as? JsonPrimitive)?.intOrNull ?: 0

    fun pick(lang: String) {
        val card = _ui.value.card ?: return
        flush()
        _ui.update { it.copy(lang = lang, code = null, notice = null, hintLines = null) }
        viewModelScope.launch {
            repo.setLanguage(lessonId, lang)
            val draft = drafts.get(lessonId, cardKey, lang)
            val code = draft ?: card.starterFor(lang) ?: when (val r = api.starter(lessonId, cardKey, lang)) {
                is ApiResult.Ok -> r.value
                is ApiResult.Err -> { _ui.update { it.copy(notice = "Chưa tải được code khởi đầu: ${r.message}") }; "" }
            }
            if (draft == null && code.isNotEmpty()) drafts.put(CodeDraftEntity(lessonId, cardKey, lang, code, now()))
            if (_ui.value.lang == lang) _ui.update { it.copy(code = code) }
            if (_ui.value.hints >= _ui.value.maxHints && _ui.value.maxHints > 0) loadHintLines(cacheOnly = true)
        }
    }

    fun onEdit(code: String) {
        val lang = _ui.value.lang ?: return
        _ui.update { it.copy(code = code) }
        pending.value = CodeDraftEntity(lessonId, cardKey, lang, code, now())
    }

    /** Ghi nháp đang chờ. Gọi khi đổi ngôn ngữ và khi màn đóng (onCleared), trên scope sống lâu hơn ViewModel. */
    fun flush() {
        val d = pending.value ?: return
        flushScope.launch { drafts.put(d) }
    }

    override fun onCleared() = flush()

    fun run() = act(EditorUi.Busy.RUN) { lang, code ->
        when (val r = api.runTests(lessonId, cardKey, lang, code)) {
            is ApiResult.Ok -> Outcome.Ran(r.value)
            is ApiResult.Err -> Outcome.Failed(r.message)
        }
    }

    fun submit() = act(EditorUi.Busy.SUBMIT) { lang, code ->
        when (val r = api.submit(lessonId, cardKey, lang, code, _ui.value.hints)) {
            is ApiResult.Ok -> {
                val pass = r.value.allPass
                repo.updateCard(lessonId, cardKey) { s ->
                    JsonObject(
                        s + if (pass) mapOf("pass" to JsonPrimitive(true), "lang" to JsonPrimitive(lang), "hints" to JsonPrimitive(_ui.value.hints))
                        else mapOf("fails" to JsonPrimitive(s.int("fails") + 1)),
                    )
                }
                Outcome.Submitted(r.value)
            }
            is ApiResult.Err -> Outcome.Failed(r.message)
        }
    }

    private fun act(kind: EditorUi.Busy, block: suspend (String, String) -> Outcome) {
        val s = _ui.value
        val lang = s.lang ?: return
        val code = s.code ?: return
        if (s.busy != null) return
        flush()
        _ui.update { it.copy(busy = kind, result = null) }
        viewModelScope.launch {
            val out = block(lang, code)
            _ui.update { it.copy(busy = null, result = out) }
        }
    }

    fun dismissResult() = _ui.update { it.copy(result = null) }

    /** Mở thêm một nấc gợi ý: nấc 1..n là `hints` của bài, nấc cuối là Parsons từ lời giải mẫu (gọi server một lần, cache ở card_state). */
    fun openHint() {
        val s = _ui.value
        if (s.busy != null) return
        // đã mở nấc cuối nhưng ngôn ngữ hiện tại chưa có dòng Parsons (vừa đổi ngôn ngữ): tải cho ngôn ngữ này
        if (s.maxHints > 0 && s.hints >= s.maxHints && s.hintLines == null) { viewModelScope.launch { loadHintLines(cacheOnly = false) }; return }
        if (s.hints >= s.maxHints) return
        val level = s.hints + 1
        _ui.update { it.copy(hints = level) }
        viewModelScope.launch {
            repo.useHint(lessonId, level)
            repo.updateCard(lessonId, cardKey) { JsonObject(it + ("hints" to JsonPrimitive(level))) }
            if (level == s.maxHints) loadHintLines(cacheOnly = false)
        }
    }

    private suspend fun loadHintLines(cacheOnly: Boolean) {
        val lang = _ui.value.lang ?: return
        val cached = (repo.cardState(lessonId, cardKey)["hint_lines"] as? JsonObject)?.get(lang) as? JsonArray
        if (cached != null) { _ui.update { it.copy(hintLines = cached.map { e -> e.jsonPrimitive.content }) }; return }
        if (cacheOnly) return
        _ui.update { it.copy(busy = EditorUi.Busy.HINT) }
        when (val r = api.hint(lessonId, cardKey, lang)) {
            is ApiResult.Ok -> {
                repo.updateCard(lessonId, cardKey) { s ->
                    val old = s["hint_lines"] as? JsonObject ?: JsonObject(emptyMap())
                    JsonObject(s + ("hint_lines" to JsonObject(old + (lang to JsonArray(r.value.map(::JsonPrimitive))))))
                }
                _ui.update { it.copy(busy = null, hintLines = r.value) }
            }
            is ApiResult.Err -> _ui.update { it.copy(busy = null, notice = r.message) }
        }
    }
}
