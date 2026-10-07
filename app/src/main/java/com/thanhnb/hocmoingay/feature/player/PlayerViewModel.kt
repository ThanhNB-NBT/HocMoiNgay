package com.thanhnb.hocmoingay.feature.player

import com.thanhnb.hocmoingay.core.lesson.CodeTask
import com.thanhnb.hocmoingay.core.log.MinuteMeter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.lesson.cardStateOf
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import com.thanhnb.hocmoingay.core.net.CodeApi
import com.thanhnb.hocmoingay.core.review.MINUTE_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class PlayerViewModel(
    private val lessonId: String,
    private val repo: LessonRepo,
    val api: CodeApi,
    val online: StateFlow<Boolean>,
    progress: Flow<ProgressEntity?>,
    private val writeScope: CoroutineScope,              // graph.scope: ghi xong bài/card không bị huỷ khi đóng màn
    private val log: DailyLogRepo? = null,               // null ở test cũ: không ghi mạch
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _body = MutableStateFlow<LessonBody?>(null)
    val body = _body.asStateFlow()
    private val _track = MutableStateFlow("code")
    /** "code" | "english": màu mảng của trình phát, và bài tiếng Anh mới ghi phút theo mạch. */
    val track = _track.asStateFlow()
    private val _missing = MutableStateFlow(false)
    /** Bài không có trong Room hoặc body hỏng: màn báo lỗi, không quay vòng chờ mãi. */
    val missing = _missing.asStateFlow()
    private val _q = MutableStateFlow(PlayerQueue(0))
    val queue = _q.asStateFlow()
    private val _grammar = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    /** (id, tên) chủ điểm Ngữ pháp liên quan có trên máy: nút "Xem ngữ pháp" ở màn mở đầu. */
    val grammar = _grammar.asStateFlow()
    private val _started = MutableStateFlow(false)
    val started = _started.asStateFlow()
    val cardState: StateFlow<JsonObject> = progress.map { cardStateOf(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, JsonObject(emptyMap()))

    init {
        viewModelScope.launch {
            val l = repo.load(lessonId)
            if (l == null) { _missing.value = true; return@launch }
            _body.value = l.body
            _track.value = l.track
            _q.value = PlayerQueue(l.body.cards.size)
            repo.start(lessonId)
            _grammar.value = l.body.grammar.mapNotNull { g -> repo.load(g)?.let { g to it.body.title } }
        }
    }

    private var shownAt = 0L

    fun start() { _started.value = true; shownAt = now() }
    fun answer(ok: Boolean, graded: Boolean) = _q.update { it.answer(ok, graded) }
    private val meter = MinuteMeter { at, m -> log?.let { l -> writeScope.launch { l.add(at) { it.copy(minutes = it.minutes + m) } } } }

    fun next() {
        val t = now()
        val card = _q.value.current?.let { _body.value?.cards?.getOrNull(it) }
        // Quyết định 5 của d: mỗi card tối đa 5 phút để máy treo không cộng vô hạn; card code gồm cả thời gian ở editor
        val spent = minOf(t - shownAt, if (card is CodeTask) 30 * MINUTE_MS else 5 * MINUTE_MS)
        if (card != null) meter.add(t, spent)
        val strand = card?.takeIf { _track.value == "english" }?.let(::strandOf)
        if (strand != null && log != null) writeScope.launch { log.addStrands(t, mapOf(strand to spent / 60_000.0)) }
        shownAt = t
        _q.update { it.next() }
        if (_q.value.finished) {
            meter.flush(t)
            writeScope.launch { repo.finish(lessonId, _q.value.score) }
        }
    }

    override fun onCleared() = meter.flush(now())
    fun saveCard(key: String, value: JsonObject) { writeScope.launch { repo.updateCard(lessonId, key) { value } } }
}
