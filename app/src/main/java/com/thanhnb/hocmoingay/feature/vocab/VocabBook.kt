package com.thanhnb.hocmoingay.feature.vocab

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.Vocab
import com.thanhnb.hocmoingay.core.lesson.parseLesson
import com.thanhnb.hocmoingay.core.review.Rating
import com.thanhnb.hocmoingay.core.review.ReviewRepo
import com.thanhnb.hocmoingay.core.speech.LocalTts
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Critter
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.english.SpeakerBar
import com.thanhnb.hocmoingay.feature.review.RatingRow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Một từ trong sổ: [ref] = `<bài>#<key card>`, trùng ref của thẻ ôn mà bài học sinh ra. */
data class VocabEntry(val ref: String, val level: String, val vocab: Vocab)

val LEVELS = listOf("A1", "A2", "B1", "B2", "C1")

/** Gom card `vocab` theo thứ tự bài; id bài dạng `<khoá>/<cấp>/<chương>/<bài>`. Từ lặp ở bài sau thì giữ lần đầu. */
fun vocabEntries(lessons: List<Pair<String, LessonBody>>): List<VocabEntry> {
    val seen = HashSet<String>()
    return lessons.flatMap { (id, body) ->
        body.cards.filterIsInstance<Vocab>().map { VocabEntry("$id#${it.key}", id.split('/').getOrElse(1) { "" }, it) }
    }.filter { seen.add(it.vocab.word.trim().lowercase()) }
}

/** Phiên thẻ lật: từ đến hạn trước, rồi từ chưa học; tối đa [n]. */
fun flashcardQueue(entries: List<VocabEntry>, cards: Map<String, ReviewCardEntity>, now: Long, n: Int = 10): List<String> {
    val due = entries.filter { cards[it.ref]?.let { c -> c.due <= now } == true }
    val fresh = entries.filter { it.ref !in cards }
    return (due + fresh).take(n).map { it.ref }
}

class VocabViewModel(
    private val courseId: String,
    lessonsOf: suspend (String) -> List<LessonEntity>,
    recall: Flow<List<ReviewCardEntity>>,
    private val repo: ReviewRepo,
    private val uid: () -> String?,
    /** Ghi bằng scope của app: rời màn ngay sau khi chấm vẫn không mất lượt chấm. */
    private val writeScope: CoroutineScope,
) : ViewModel() {
    data class Ui(val entries: List<VocabEntry>, val cards: Map<String, ReviewCardEntity>)

    private val entries = flow {
        emit(vocabEntries(lessonsOf(courseId).mapNotNull { l -> parseLesson(l.body)?.let { l.id to it } }))
    }

    val ui = combine(entries, recall) { es, cs -> Ui(es, cs.associateBy { it.ref }) }
        .flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val blank = ReviewCardEntity(id = "", userId = "", ref = "", kind = "recall", track = "english", courseId = courseId, due = 0, updatedAt = 0)

    fun preview(ref: String): Map<Rating, Long> = repo.preview(ui.value?.cards?.get(ref) ?: blank)

    fun learn(ref: String, r: Rating) {
        val u = uid() ?: return
        writeScope.launch { repo.learn(u, ref, courseId, r) }
    }
}

/** Sổ từ vựng: mọi từ của khoá tiếng Anh theo cấp, nghe phát âm, và học thẻ lật (chấm 4 mức, vào lịch ôn FSRS). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabScreen(vm: VocabViewModel, onBack: () -> Unit) = ProvideTrack(Track.ENGLISH) {
    val ui = vm.ui.collectAsStateWithLifecycle().value
    var level by rememberSaveable { mutableStateOf<String?>(null) }
    var session by rememberSaveable { mutableStateOf<List<String>?>(null) }
    BackHandler(session != null) { session = null }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (session == null) "Sổ từ vựng" else "Học thẻ lật", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { if (session != null) session = null else onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }
                },
            )
        },
    ) { pad ->
        val mod = Modifier.padding(pad)
        when {
            ui == null -> Box(mod.fillMaxSize())
            ui.entries.isEmpty() -> Placeholder("Chưa có từ vựng", "Giáo trình tiếng Anh được tải về khi có mạng.", mod)
            session != null -> Flashcards(vm, ui, session!!, mod) { session = null }
            else -> WordList(ui, level, { level = it }, mod) { session = it }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordList(ui: VocabViewModel.Ui, level: String?, onLevel: (String?) -> Unit, modifier: Modifier, onStart: (List<String>) -> Unit) {
    val t = LocalTrack.current
    val cs = MaterialTheme.colorScheme
    val now = remember { System.currentTimeMillis() }
    val shown = ui.entries.filter { level == null || it.level == level }
    val learned = shown.count { it.ref in ui.cards }
    val queue = flashcardQueue(shown, ui.cards, now)
    val dueN = shown.count { ui.cards[it.ref]?.let { c -> c.due <= now } == true }
    val tts = LocalTts.current
    val owner = remember { Any() }
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "filter") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(level == null, { onLevel(null) }, label = { Text("Tất cả") })
                LEVELS.filter { l -> ui.entries.any { it.level == l } }.forEach { l -> FilterChip(level == l, { onLevel(l) }, label = { Text(l) }) }
            }
        }
        item(key = "summary") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                Text("Đã học $learned/${shown.size} từ" + if (dueN > 0) " · $dueN từ đến hạn ôn" else "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LinearProgressIndicator(
                    progress = { if (shown.isEmpty()) 0f else learned.toFloat() / shown.size }, Modifier.fillMaxWidth().height(8.dp),
                    color = t.accent, trackColor = t.container, strokeCap = StrokeCap.Round,
                )
                PushButton(
                    if (queue.isEmpty()) "Đã học hết, chưa tới hạn ôn" else "Học thẻ lật · ${queue.size} từ", { onStart(queue) },
                    Modifier.fillMaxWidth(), color = t.accent, contentColor = t.onAccent, enabled = queue.isNotEmpty(),
                )
                Text("Bấm vào từ để nghe và xem ví dụ.", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
        items(shown, key = { it.ref }) { e ->
            val v = e.vocab
            val c = ui.cards[e.ref]
            val expanded = open == e.ref
            Column(
                Modifier.fillMaxWidth().background(cs.surfaceContainerLowest, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, cs.outlineVariant), RoundedCornerShape(16.dp))
                    .clickable { open = if (expanded) null else e.ref; if (!expanded) tts?.speak(v.word, owner) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(v.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = t.accent)
                            if (v.ipa.isNotBlank()) Text("  ${v.ipa}", style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMono, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(v.meaningVi, style = MaterialTheme.typography.bodyMedium, maxLines = if (expanded) Int.MAX_VALUE else 1, overflow = TextOverflow.Ellipsis)
                    }
                    val (label, bg, fg) = when {
                        c == null -> Triple("Mới", cs.surfaceContainerHigh, cs.onSurfaceVariant)
                        c.due <= now -> Triple("Ôn", t.accent, t.onAccent)
                        else -> Triple("Đã học", t.container, t.onContainer)
                    }
                    Text(label, Modifier.background(bg, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp), color = fg, style = MaterialTheme.typography.labelMedium)
                }
                if (expanded) {
                    v.examples.forEach { ex ->
                        Text("• $ex", Modifier.fillMaxWidth().clickable { tts?.speak(ex, owner) }.padding(vertical = 2.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                    if (v.collocations.isNotEmpty()) Text("Hay đi cùng: " + v.collocations.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    Text("Trình độ ${e.level}", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Flashcards(vm: VocabViewModel, ui: VocabViewModel.Ui, refs: List<String>, modifier: Modifier, onExit: () -> Unit) {
    val byRef = remember(ui.entries) { ui.entries.associateBy { it.ref } }
    val queue = remember(refs) { refs.mapNotNull { byRef[it] } }
    var i by rememberSaveable { mutableIntStateOf(0) }
    var flipped by rememberSaveable { mutableStateOf(false) }
    val t = LocalTrack.current
    val cs = MaterialTheme.colorScheme
    val tts = LocalTts.current
    val owner = remember { Any() }
    if (i >= queue.size) {
        Column(
            modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Critter(Modifier.size(96.dp))
            Text("Xong ${queue.size} từ", style = MaterialTheme.typography.displaySmall)
            Text("Từ đã học sẽ quay lại ở tab Ôn tập đúng lúc bạn sắp quên, dạng điền vào câu.", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
            PushButton("Về sổ từ", onExit, Modifier.fillMaxWidth())
        }
        return
    }
    val e = queue[i]
    val v = e.vocab
    Column(modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { i.toFloat() / queue.size }, Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(10.dp),
            color = t.accent, trackColor = t.container, strokeCap = StrokeCap.Round,
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${i + 1}/${queue.size} · ${e.level}", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Text(v.word, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = t.accent)
            if (v.ipa.isNotBlank()) Text(v.ipa, style = MaterialTheme.typography.titleMedium, fontFamily = JetBrainsMono, color = cs.onSurfaceVariant)
            key(e.ref) { SpeakerBar(v.word, autoPlay = true) }
            if (flipped) {
                Text(v.meaningVi, style = MaterialTheme.typography.headlineSmall)
                v.examples.forEach { ex ->
                    Pushable({ tts?.speak(ex, owner) }, cs.surfaceContainerHigh, RoundedCornerShape(16.dp), Modifier.fillMaxWidth()) {
                        Text(ex, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (v.collocations.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    v.collocations.forEach {
                        Text(it, Modifier.background(t.container, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp), color = t.onContainer)
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                Text("Nhớ nghĩa của từ này không? Nghĩ trong đầu rồi lật thẻ.", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
            }
        }
        if (flipped) RatingRow(vm.preview(e.ref)) { r -> vm.learn(e.ref, r); flipped = false; i++ }
        else PushButton("Lật thẻ", { flipped = true }, Modifier.fillMaxWidth().padding(16.dp))
    }
}
