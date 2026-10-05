package com.thanhnb.hocmoingay.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.lesson.cardStateOf
import com.thanhnb.hocmoingay.core.net.CodeApi
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
        }
    }

    fun start() { _started.value = true }
    fun answer(ok: Boolean, graded: Boolean) = _q.update { it.answer(ok, graded) }
    fun next() {
        _q.update { it.next() }
        if (_q.value.finished) writeScope.launch { repo.finish(lessonId, _q.value.score) }
    }
    fun saveCard(key: String, value: JsonObject) { writeScope.launch { repo.updateCard(lessonId, key) { value } } }
}
