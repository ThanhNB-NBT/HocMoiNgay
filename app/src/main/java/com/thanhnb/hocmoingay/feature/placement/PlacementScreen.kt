package com.thanhnb.hocmoingay.feature.placement

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.db.PlacementEntity
import com.thanhnb.hocmoingay.core.speech.Tts
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Critter
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.rise
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.english.SpeakerBar
import com.thanhnb.hocmoingay.feature.english.ttsStatus
import com.thanhnb.hocmoingay.feature.player.cards.ChoiceRow
import com.thanhnb.hocmoingay.feature.player.cards.Mark
import com.thanhnb.hocmoingay.feature.player.cards.Why
import com.thanhnb.hocmoingay.feature.player.permutation
import com.thanhnb.hocmoingay.feature.settings.SettingsRepo
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlacementViewModel(
    private val rows: suspend () -> List<PlacementEntity>,
    private val settings: SettingsRepo,
    private val writeScope: CoroutineScope, // graph.scope: lưu trình độ dù người học thoát ngay ở màn kết quả
    seed: Long = System.currentTimeMillis(),
) : ViewModel() {
    data class Ui(
        val loading: Boolean = true,
        val questions: List<PlacementQ> = emptyList(),
        val started: Boolean = false,
        val pos: Int = 0,
        val picked: Map<String, Int> = emptyMap(), // id câu → chỉ số lựa chọn gốc
        val saved: Boolean? = null,                // null = chưa xong; false = cài đặt chưa kéo về nên không lưu
    ) {
        val finished: Boolean get() = started && questions.isNotEmpty() && pos >= questions.size
        val scores: List<LevelScore> get() = levelScores(questions, questions.filter { picked[it.id] == it.body.answer }.map { it.id }.toSet())
        val level: String get() = placementLevel(scores)
    }

    private val rnd = Random(seed)
    private val _ui = MutableStateFlow(Ui())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch { _ui.value = Ui(loading = false, questions = pickPlacement(parsePlacement(rows()), rnd)) }
    }

    fun start() = _ui.update { it.copy(started = true) }

    /** Chọn lại được cho tới khi bấm Tiếp. */
    fun pick(choice: Int) = _ui.update { u ->
        val q = u.questions.getOrNull(u.pos) ?: return@update u
        u.copy(picked = u.picked + (q.id to choice))
    }

    fun next() {
        val u = _ui.value
        val q = u.questions.getOrNull(u.pos) ?: return
        if (q.id !in u.picked) return
        _ui.update { it.copy(pos = it.pos + 1) }
        if (_ui.value.finished) save(_ui.value.level)
    }

    private fun save(level: String) {
        writeScope.launch {
            val ok = settings.loaded.first()
            if (ok) settings.update { it.copy(englishLevel = level) }
            _ui.update { it.copy(saved = ok) }
        }
    }
}

@Composable
fun PlacementScreen(vm: PlacementViewModel, onBack: () -> Unit) {
    val ui = vm.ui.collectAsStateWithLifecycle().value
    ProvideTrack(Track.ENGLISH) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Đóng") }
                if (ui.started && !ui.finished) {
                    val p by animateFloatAsState(ui.pos.toFloat() / ui.questions.size, spring(dampingRatio = 0.7f, stiffness = 200f), label = "tiến độ")
                    Box(Modifier.weight(1f).height(14.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(LocalTrack.current.accent))
                    }
                }
            }
            when {
                ui.loading -> Box(Modifier.fillMaxSize())
                ui.questions.isEmpty() -> Placeholder("Chưa có bài xếp lớp", "Bộ câu hỏi được tải về khi có mạng. Mở lại app sau khi kết nối.")
                !ui.started -> Intro(ui.questions.size, vm::start)
                ui.finished -> Result(ui, onBack)
                else -> Question(ui, vm)
            }
        }
    }
}

@Composable
private fun Intro(n: Int, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        Text("Bài xếp lớp tiếng Anh", style = MaterialTheme.typography.displaySmall)
        Text(
            "$n câu từ A1 tới C1, gồm từ vựng, ngữ pháp và nghe. Mất khoảng 10 phút. Phần nghe cần bật loa hoặc đeo tai nghe.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Câu nào không chắc thì chọn đáp án bạn thấy hợp nhất.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        PushButton("Bắt đầu", onStart, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward)
    }
}

private fun skillLabel(s: String) = when (s) {
    "vocab" -> "Từ vựng"
    "grammar" -> "Ngữ pháp"
    else -> "Nghe"
}

@Composable
private fun ColumnScope.Question(ui: PlacementViewModel.Ui, vm: PlacementViewModel) {
    val q = ui.questions[ui.pos]
    key(q.id) {
        val perm = remember { permutation(q.body.choices.size, q.id) }
        val picked = ui.picked[q.id]
        val status = ttsStatus()
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("${skillLabel(q.skill)} · câu ${ui.pos + 1}/${ui.questions.size}", style = MaterialTheme.typography.labelLarge, color = LocalTrack.current.accent)
            if (q.skill == "listening") {
                val text = q.body.text.orEmpty()
                SpeakerBar(text, autoPlay = true)
                // đường dự phòng: máy không đọc được thì hiện chữ để vẫn làm tiếp được
                if (status == Tts.Status.UNAVAILABLE || status == Tts.Status.MISSING_DATA) Text("“$text”", style = MaterialTheme.typography.bodyLarge)
            }
            Text(q.body.q, style = MaterialTheme.typography.titleLarge)
            perm.forEach { i ->
                ChoiceRow(AnnotatedString(q.body.choices[i]), if (picked == i) Mark.PICKED else Mark.IDLE, enabled = true, why = null) { vm.pick(i) }
            }
        }
        PushButton(
            if (ui.pos + 1 == ui.questions.size) "Xem kết quả" else "Tiếp", vm::next,
            Modifier.fillMaxWidth().padding(20.dp), enabled = picked != null, icon = Icons.AutoMirrored.Filled.ArrowForward,
        )
    }
}

@Composable
private fun Result(ui: PlacementViewModel.Ui, onBack: () -> Unit) {
    val f = LocalFun.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Critter(Modifier.size(88.dp))
        Text("Trình độ của bạn", style = MaterialTheme.typography.headlineSmall)
        Pushable(null, f.coralContainer, RoundedCornerShape(32.dp), Modifier.fillMaxWidth(), edge = f.coral) {
            Text(
                ui.level, Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold), color = f.onCoralContainer,
            )
        }
        ui.scores.filter { it.total > 0 }.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().rise(i), verticalAlignment = Alignment.CenterVertically) {
                Text(s.level, Modifier.width(48.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${s.right}/${s.total} câu đúng", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                if (s.passed) Icon(Icons.Filled.Check, "Đạt", tint = f.mint)
            }
        }
        Text(
            "Trình độ là cấp cao nhất bạn đúng từ 70% trở lên, khi các cấp thấp hơn cũng đạt. Làm lại được trong Cài đặt.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (ui.saved == false) Why("Chưa lưu được trình độ vì cài đặt chưa tải về máy. Mở app khi có mạng rồi làm lại.", ok = false)
        PushButton("Xong", onBack, Modifier.fillMaxWidth())
    }
}
