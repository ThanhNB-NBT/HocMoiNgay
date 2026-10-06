package com.thanhnb.hocmoingay.feature.player.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.Markdown
import com.thanhnb.hocmoingay.core.code.inlineMd
import com.thanhnb.hocmoingay.core.lesson.Card
import com.thanhnb.hocmoingay.core.lesson.Dialogue
import com.thanhnb.hocmoingay.core.lesson.Explain
import com.thanhnb.hocmoingay.core.lesson.FreeText
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
import com.thanhnb.hocmoingay.core.net.ApiResult
import com.thanhnb.hocmoingay.core.net.CodeApi
import com.thanhnb.hocmoingay.core.net.ExplainFb
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.player.permutation
import com.thanhnb.hocmoingay.feature.player.quizOk
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class CardCtx(
    val lessonId: String, val cardKey: String, val seed: String, val online: Boolean, val api: CodeApi,
    val state: JsonObject, val save: (JsonObject) -> Unit, val openEditor: (String) -> Unit,
    val answered: Boolean, val onAnswer: (ok: Boolean, graded: Boolean) -> Unit,
    val active: Boolean = true, // trang đang hiện; pager dựng trước trang kế khi rảnh nên card chưa hiện cũng chạy effect
)

@Composable
fun CardView(card: Card, ctx: CardCtx) = when (card) {
    is Explain -> ExplainCard(card, ctx)
    is Quiz -> QuizCard(card, ctx)
    is Match -> MatchCard(card, ctx)
    is FreeText -> if (card.mode == "explain") FreeTextCard(card, ctx) else WritingCard(card, ctx)
    is Speak -> RepeatCard(card.text, shadow = false, ctx)
    is Shadow -> RepeatCard(card.text, shadow = true, ctx)
    is TimedTalk -> TimedTalkCard(card, ctx)
    is SpeakFree -> SpeakFreeCard(card, ctx)
    is Vocab -> VocabCard(card, ctx)
    is Listen -> ListenCard(card, ctx)
    is Read -> ReadCard(card, ctx)
    is MinimalPair -> MinimalPairCard(card, ctx)
    is Dialogue -> DialogueCard(card, ctx)
    else -> CodeCardView(card, ctx)
}

/** Card không chấm: báo "đúng, không tính điểm" ngay, để thanh dưới hiện nút Tiếp. */
@Composable
fun Ungraded(ctx: CardCtx) = LaunchedEffect(ctx.active, ctx.answered) { if (ctx.active && !ctx.answered) ctx.onAnswer(true, false) }

@Composable
private fun ExplainCard(c: Explain, ctx: CardCtx) {
    Ungraded(ctx)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Markdown(c.md)
        c.say.forEach { SayRow(it) }
    }
}

/** Card tiếng Anh (giai đoạn d) và free_text writing: nói thật là chưa có, cho đi tiếp. */
@Composable
fun LaterCard(ctx: CardCtx) {
    Ungraded(ctx)
    Text("Loại card này sẽ có ở bản cập nhật sau. Bấm Tiếp để học tiếp.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun QuizCard(c: Quiz, ctx: CardCtx) = QuizBlock(c.q, c.choices, c.answer, c.why, ctx.seed) { ctx.onAnswer(it, true) }

/** Một câu trắc nghiệm (quiz, câu hỏi của listen/read): xáo theo [seed], `why` đi theo lựa chọn. */
@Composable
fun QuizBlock(q: String, choices: List<String>, answer: List<Int>, why: List<String>, seed: String, onDone: (Boolean) -> Unit) {
    val perm = remember(seed) { permutation(choices.size, seed) }
    val multi = answer.size > 1
    var picked by remember { mutableStateOf(setOf<Int>()) }
    var checked by remember { mutableStateOf(false) }
    val code = SpanStyle(fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.secondary)
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    fun check() { checked = true; onDone(quizOk(picked, answer)) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Markdown(q)
        if (multi) Text("Chọn tất cả đáp án đúng", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        perm.forEach { i ->
            val mark = when {
                !checked -> if (i in picked) Mark.PICKED else Mark.IDLE
                i in answer -> Mark.RIGHT
                i in picked -> Mark.WRONG
                else -> Mark.IDLE
            }
            ChoiceRow(
                inlineMd(choices[i], code, bold), mark, enabled = !checked,
                why = if (checked && mark != Mark.IDLE) why.getOrNull(i) else null,
                onClick = {
                    if (checked) return@ChoiceRow
                    if (multi) picked = if (i in picked) picked - i else picked + i
                    else { picked = setOf(i); check() }
                },
            )
        }
        if (multi && !checked) PushButton("Kiểm tra", ::check, Modifier.fillMaxWidth(), enabled = picked.isNotEmpty())
    }
}

@Composable
private fun MatchCard(c: Match, ctx: CardCtx) {
    val left = remember(ctx.seed) { permutation(c.pairs.size, ctx.seed + "L") }
    val right = remember(ctx.seed) { permutation(c.pairs.size, ctx.seed + "R") }
    var sel by remember { mutableStateOf<Int?>(null) }
    var matched by remember { mutableStateOf(setOf<Int>()) }
    var mistakes by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf<Int?>(null) } // vế phải vừa ghép sai, tô coral một nhịp
    val code = SpanStyle(fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.secondary)
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    LaunchedEffect(wrong) { if (wrong != null) { delay(500); wrong = null } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Ghép mỗi ô bên trái với ô đúng bên phải", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                left.forEach { i ->
                    ChoiceRow(
                        inlineMd(c.pairs[i][0], code, bold),
                        when { i in matched -> Mark.RIGHT; sel == i -> Mark.PICKED; else -> Mark.IDLE },
                        enabled = i !in matched, why = null,
                    ) { if (i !in matched) sel = i }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                right.forEach { j ->
                    ChoiceRow(
                        inlineMd(c.pairs[j][1], code, bold),
                        when { j in matched -> Mark.RIGHT; wrong == j -> Mark.WRONG; else -> Mark.IDLE },
                        enabled = j !in matched && sel != null, why = null,
                    ) {
                        val s = sel ?: return@ChoiceRow
                        if (j in matched) return@ChoiceRow
                        if (s == j) {
                            matched = matched + j; sel = null
                            if (matched.size == c.pairs.size) ctx.onAnswer(mistakes == 0, true)
                        } else { mistakes++; wrong = j }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreeTextCard(c: FreeText, ctx: CardCtx) {
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var fb by remember { mutableStateOf<ExplainFb?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Markdown(c.prompt)
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 140.dp), enabled = fb == null, label = { Text("Giải thích bằng lời của bạn") })
        if (fb == null) {
            if (!ctx.online) Text("Cần mạng để nhận xét. Bạn có thể bỏ qua card này.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PushButton(if (busy) "Đang nhận xét…" else "Nhận xét", {
                busy = true; err = null
                scope.launch {
                    when (val r = ctx.api.explain(ctx.lessonId, ctx.cardKey, text)) {
                        is ApiResult.Ok -> {
                            fb = r.value
                            ctx.save(buildJsonObject { put("text", text); put("score", r.value.score) })
                            ctx.onAnswer(true, false)
                        }
                        is ApiResult.Err -> err = r.message
                    }
                    busy = false
                }
            }, Modifier.fillMaxWidth(), enabled = ctx.online && !busy && text.trim().length >= 10)
            TextButton(onClick = { ctx.onAnswer(true, false) }, enabled = !ctx.answered) { Text("Bỏ qua") }
            err?.let { Why(it, ok = false) }
        }
        fb?.let { r ->
            Pushable(null, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(24.dp), Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${r.score}/10", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (r.missing.isNotEmpty()) Text("Còn thiếu: " + r.missing.joinToString("; "), style = MaterialTheme.typography.bodyMedium)
                    if (r.misconceptions.isNotEmpty()) Text("Hiểu chưa đúng: " + r.misconceptions.joinToString("; "), style = MaterialTheme.typography.bodyMedium)
                    if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodyMedium)
                    Text("Bài mẫu: ${c.sample}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
