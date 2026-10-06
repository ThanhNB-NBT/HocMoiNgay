package com.thanhnb.hocmoingay.feature.review

import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import com.thanhnb.hocmoingay.core.log.MinuteMeter
import com.thanhnb.hocmoingay.core.review.MINUTE_MS
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.net.CodeApi
import com.thanhnb.hocmoingay.core.review.Rating
import com.thanhnb.hocmoingay.core.review.ReviewRepo
import com.thanhnb.hocmoingay.core.review.spanText
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Critter
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.player.cards.CardCtx
import com.thanhnb.hocmoingay.feature.player.cards.CardView
import com.thanhnb.hocmoingay.feature.player.cards.Why
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class ReviewViewModel(
    private val repo: ReviewRepo,
    private val load: suspend (lessonId: String) -> LessonBody?,
    private val writeScope: CoroutineScope, // graph.scope: chấm xong rồi rời tab vẫn ghi
    nextDue: Flow<Long?>,
    private val log: DailyLogRepo? = null,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    data class Ui(val loading: Boolean = true, val items: List<ReviewItem> = emptyList(), val pos: Int = 0)

    private val _ui = MutableStateFlow(Ui())
    val ui = _ui.asStateFlow()
    val nextDue = nextDue.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init { reload() }

    /** Mỗi bài chỉ parse một lần dù có nhiều thẻ. */
    fun reload() {
        viewModelScope.launch {
            val cache = mutableMapOf<String, LessonBody?>()
            val items = repo.dueCards().mapNotNull { c ->
                val id = c.ref.substringBeforeLast('#')
                reviewItem(c, cache.getOrPut(id) { load(id) })
            }
            _ui.value = Ui(loading = false, items = items)
            shownAt = now()
        }
    }

    fun preview(item: ReviewItem): Map<Rating, Long> = repo.preview(item.card)

    private var shownAt = 0L
    private val meter = MinuteMeter { at, m -> log?.let { l -> writeScope.launch { l.add(at) { it.copy(minutes = it.minutes + m) } } } }

    fun rate(r: Rating) {
        val item = _ui.value.items.getOrNull(_ui.value.pos) ?: return
        val t = now()
        meter.add(t, minOf(t - shownAt, 5 * MINUTE_MS))
        shownAt = t
        writeScope.launch { repo.rate(item.card.id, r) }
        _ui.update { it.copy(pos = it.pos + 1) }
        if (_ui.value.pos >= _ui.value.items.size) meter.flush(t)
    }

    override fun onCleared() = meter.flush(now())
}

@Composable
fun ReviewScreen(vm: ReviewViewModel, online: Boolean, api: CodeApi, icon: ImageVector) {
    val ui = vm.ui.collectAsStateWithLifecycle().value
    val next = vm.nextDue.collectAsStateWithLifecycle().value
    when {
        ui.loading -> Box(Modifier.fillMaxSize())
        ui.items.isEmpty() -> Placeholder(
            "Chưa có thẻ cần ôn",
            next?.let { "Thẻ tiếp theo đến hạn ${dueText(it)}." } ?: "Học xong bài nào, thẻ ôn của bài đó sẽ được xếp lịch và hiện ở đây.",
            icon = icon,
        )
        ui.pos >= ui.items.size -> Finished(ui.items.size, vm::reload)
        else -> Session(ui, vm, online, api)
    }
}

private fun dueText(ms: Long): String =
    DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM").format(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()))

@Composable
private fun Session(ui: ReviewViewModel.Ui, vm: ReviewViewModel, online: Boolean, api: CodeApi) {
    val item = ui.items[ui.pos]
    ProvideTrack(if (item.card.track == "english") Track.ENGLISH else Track.CODE) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Progress(ui.pos, ui.items.size)
            key(item.card.id) {
                var verdict by remember { mutableStateOf<Boolean?>(null) } // card tự chấm: đúng/sai
                var revealed by remember { mutableStateOf(false) }          // ghi chú đã lật, hoặc card không chấm đã xem
                val preview = remember { vm.preview(item) }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
                    when (item) {
                        is ReviewItem.Note -> NoteFace(item, revealed)
                        is ReviewItem.Cloze -> ClozeFace(item) { verdict = it }
                        is ReviewItem.Lesson -> CardView(
                            item.item,
                            CardCtx(
                                lessonId = item.lessonId, cardKey = item.item.key, seed = "${item.card.id}#${item.card.reps}",
                                online = online, api = api, state = JsonObject(emptyMap()), save = {}, openEditor = {},
                                answered = verdict != null || revealed,
                                onAnswer = { ok, graded -> if (graded) verdict = ok else revealed = true },
                            ),
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().heightIn(min = 96.dp)) {
                    val v = verdict
                    when {
                        v != null -> {
                            val r = if (v) Rating.GOOD else Rating.AGAIN
                            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Ôn lại sau ${spanText(preview.getValue(r))}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                PushButton("Tiếp", { vm.rate(r) }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward)
                            }
                        }
                        item is ReviewItem.Note && !revealed ->
                            PushButton("Hiện đáp án", { revealed = true }, Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp))
                        revealed -> RatingRow(preview, vm::rate)
                    }
                }
            }
        }
    }
}

@Composable
private fun Progress(pos: Int, total: Int) {
    val cs = MaterialTheme.colorScheme
    val p by animateFloatAsState(pos.toFloat() / total, spring(dampingRatio = 0.7f, stiffness = 200f), label = "tiến độ ôn")
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(14.dp).clip(CircleShape).background(cs.surfaceContainerHigh)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(LocalTrack.current.accent))
        }
        Spacer(Modifier.width(12.dp))
        Text("${pos + 1}/$total", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun NoteFace(n: ReviewItem.Note, revealed: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(n.front, style = MaterialTheme.typography.headlineSmall)
        if (revealed) {
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Text(n.back, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LocalTrack.current.accent)
        }
    }
}

@Composable
private fun ClozeFace(c: ReviewItem.Cloze, onVerdict: (Boolean) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<Boolean?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Điền cụm còn thiếu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(c.vocab.cloze, style = MaterialTheme.typography.headlineSmall)
        if (c.vocab.meaningVi.isNotBlank()) Text("Gợi ý: ${c.vocab.meaningVi}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            text, { text = it }, Modifier.fillMaxWidth(), enabled = result == null, singleLine = true, label = { Text("Cụm từ") },
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
        )
        val r = result
        if (r == null) PushButton("Kiểm tra", { result = clozeOk(text, c.answer).also(onVerdict) }, Modifier.fillMaxWidth(), enabled = text.isNotBlank())
        else Why(if (r) "Đúng: ${c.answer}" else "Đáp án: ${c.answer}", ok = r)
    }
}

internal data class Opt(val r: Rating, val label: String, val face: Color, val fg: Color)

/** 4 nút Again/Hard/Good/Easy, mỗi nút hiện trước khoảng cách tới lần ôn tiếp (spec §7.4). */
@Composable
internal fun RatingRow(preview: Map<Rating, Long>, onRate: (Rating) -> Unit) {
    val f = LocalFun.current
    val cs = MaterialTheme.colorScheme
    val opts = listOf(
        Opt(Rating.AGAIN, "Quên", f.coralContainer, f.onCoralContainer),
        Opt(Rating.HARD, "Khó", cs.surfaceContainerHigh, cs.onSurface),
        Opt(Rating.GOOD, "Nhớ", cs.primaryContainer, cs.onPrimaryContainer),
        Opt(Rating.EASY, "Dễ", f.mintContainer, cs.onSurface),
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opts.forEach { o ->
            Pushable({ onRate(o.r) }, o.face, RoundedCornerShape(20.dp), Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(o.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = o.fg)
                    Text(spanText(preview.getValue(o.r)), style = MaterialTheme.typography.labelMedium, color = o.fg)
                }
            }
        }
    }
}

@Composable
private fun Finished(count: Int, onAgain: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Critter(Modifier.size(96.dp))
        Text("Ôn xong $count thẻ", style = MaterialTheme.typography.displaySmall)
        Text("Mỗi thẻ đã được xếp lịch lại theo mức bạn nhớ.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        PushButton("Xem còn thẻ nào", onAgain, Modifier.fillMaxWidth())
    }
}
