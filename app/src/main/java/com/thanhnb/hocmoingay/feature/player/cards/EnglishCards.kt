package com.thanhnb.hocmoingay.feature.player.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.Markdown
import com.thanhnb.hocmoingay.core.lesson.Dialogue
import com.thanhnb.hocmoingay.core.lesson.Listen
import com.thanhnb.hocmoingay.core.lesson.MinimalPair
import com.thanhnb.hocmoingay.core.lesson.Read
import com.thanhnb.hocmoingay.core.lesson.Say
import com.thanhnb.hocmoingay.core.lesson.Vocab
import com.thanhnb.hocmoingay.core.speech.LocalTts
import com.thanhnb.hocmoingay.core.speech.Tts
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.english.SpeakerBar
import com.thanhnb.hocmoingay.feature.english.ttsStatus
import com.thanhnb.hocmoingay.feature.player.ListenQuestion
import com.thanhnb.hocmoingay.feature.player.Turn
import com.thanhnb.hocmoingay.feature.player.minimalRounds
import com.thanhnb.hocmoingay.feature.player.parseQuestions
import com.thanhnb.hocmoingay.feature.player.parseTurns
import com.thanhnb.hocmoingay.feature.player.permutation
import kotlinx.coroutines.delay

/** Từ vựng: chỉ trình bày, không chấm. Thẻ ôn dạng cloze nằm ở tab Ôn tập. */
@Composable
fun VocabCard(c: Vocab, ctx: CardCtx) {
    Ungraded(ctx)
    val t = LocalTrack.current
    val tts = LocalTts.current
    val owner = remember { Any() }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(c.word, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = t.accent)
        if (c.ipa.isNotBlank()) Text(c.ipa, style = MaterialTheme.typography.titleMedium, fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SpeakerBar(c.word, autoPlay = ctx.active)
        Text(c.meaningVi, style = MaterialTheme.typography.titleLarge)
        c.examples.forEach { e ->
            Pushable({ tts?.speak(e, owner) }, MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp), Modifier.fillMaxWidth()) {
                Text(e, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (c.collocations.isNotEmpty()) {
            Text("Hay đi cùng", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                c.collocations.forEach {
                    Text(it, Modifier.background(t.container, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp), color = t.onContainer)
                }
            }
        }
    }
}

/** Nhiều câu hỏi trong một card: card đúng khi mọi câu đúng ngay lần chọn đầu. */
@Composable
private fun QuestionsBlock(qs: List<ListenQuestion>, ctx: CardCtx) {
    if (qs.isEmpty()) { Ungraded(ctx); return }
    var results by remember { mutableStateOf(mapOf<Int, Boolean>()) }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        qs.forEachIndexed { i, q ->
            QuizBlock(q.q, q.choices, q.answer, q.why, "${ctx.seed}#q$i") { ok ->
                results = results + (i to ok)
                if (results.size == qs.size) ctx.onAnswer(results.values.all { it }, true)
            }
        }
    }
}

@Composable
fun ListenCard(c: Listen, ctx: CardCtx) {
    val text = c.text
    if (text == null) { // Quyết định 8: chưa hỗ trợ file âm thanh
        Ungraded(ctx)
        Text("Bài nghe dạng file âm thanh chưa có trong bản này. Bấm Tiếp để học tiếp.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val status = ttsStatus()
    var show by rememberSaveable { mutableStateOf(false) }
    val qs = remember(c) { parseQuestions(c.questions) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Nghe rồi trả lời", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        SpeakerBar(text, autoPlay = ctx.active)
        // máy không đọc được thì hiện chữ luôn (đường dự phòng), còn lại chỉ hiện khi người học muốn hoặc đã trả lời
        if (show || ctx.answered || status == Tts.Status.UNAVAILABLE || status == Tts.Status.MISSING_DATA) {
            Text(text, Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp)).padding(14.dp), style = MaterialTheme.typography.bodyLarge)
        } else {
            TextButton(onClick = { show = true }) { Text("Hiện lời") }
        }
        QuestionsBlock(qs, ctx)
    }
}

@Composable
fun ReadCard(c: Read, ctx: CardCtx) {
    val qs = remember(c) { parseQuestions(c.questions) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Markdown(c.md)
        QuestionsBlock(qs, ctx)
    }
}

/** C6: app đọc một từ trong cặp, người học chọn từ vừa nghe. Đúng cả các lượt ngay lần đầu thì card đúng. */
@Composable
fun MinimalPairCard(c: MinimalPair, ctx: CardCtx) {
    val rounds = remember(ctx.seed) { minimalRounds(c.pairs, ctx.seed) }
    val status = ttsStatus()
    if (rounds.isEmpty() || status == Tts.Status.UNAVAILABLE || status == Tts.Status.MISSING_DATA) {
        Ungraded(ctx)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Máy chưa đọc được tiếng Anh nên tạm bỏ qua phần luyện âm này.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SpeakerBar("") // thiếu giọng: hiện nút cài giọng
        }
        return
    }
    var round by rememberSaveable { mutableIntStateOf(0) }
    var mistakes by rememberSaveable { mutableIntStateOf(0) }
    val (pair, target) = rounds[round]
    var right by remember(round) { mutableStateOf(false) }
    var wrong by remember(round) { mutableStateOf(setOf<Int>()) }
    LaunchedEffect(round, right) {
        if (!right) return@LaunchedEffect
        delay(700)
        if (round + 1 < rounds.size) round++ else if (!ctx.answered) ctx.onAnswer(mistakes == 0, true)
    }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Nghe rồi chọn từ bạn nghe được", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (c.focus.isNotBlank()) Text("Luyện: ${c.focus}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        key(round) { SpeakerBar(pair[target], autoPlay = ctx.active) }
        Text("Lượt ${round + 1}/${rounds.size}", style = MaterialTheme.typography.labelLarge)
        pair.forEachIndexed { i, w ->
            ChoiceRow(
                AnnotatedString(w),
                when { right && i == target -> Mark.RIGHT; i in wrong -> Mark.WRONG; else -> Mark.IDLE },
                enabled = !right && !ctx.answered, why = null,
            ) {
                if (right || ctx.answered || i in wrong) return@ChoiceRow
                if (i == target) right = true else { wrong = wrong + i; mistakes++ }
            }
        }
    }
}

/** Hội thoại: câu phía bên kia đọc bằng TTS (chạm để xem nghĩa); lượt của mình chọn câu đáp, sai thì xem vì sao rồi chọn lại. */
@Composable
fun DialogueCard(c: Dialogue, ctx: CardCtx) {
    val turns = remember(c) { parseTurns(c.turns) }
    val picks = remember(turns) { turns.indices.filter { turns[it] is Turn.Pick } }
    if (picks.isEmpty()) Ungraded(ctx)
    var solved by rememberSaveable { mutableIntStateOf(0) }
    var mistakes by rememberSaveable { mutableIntStateOf(0) }
    val until = picks.getOrNull(solved) ?: turns.lastIndex // hiện tới lượt chọn đang chờ
    val tts = LocalTts.current
    val owner = remember { Any() }
    LaunchedEffect(until, ctx.active) {
        if (!ctx.active) return@LaunchedEffect
        val line = (until downTo 0).firstNotNullOfOrNull { turns.getOrNull(it) as? Turn.Line }
        if (line != null) tts?.speak(line.text, owner)
    }
    LaunchedEffect(solved) { if (picks.isNotEmpty() && solved == picks.size && !ctx.answered) ctx.onAnswer(mistakes == 0, true) }
    DisposableEffect(Unit) { onDispose { tts?.stop(owner) } }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Hội thoại", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (c.setting.isNotBlank()) Text(c.setting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        turns.forEachIndexed { i, turn ->
            if (i > until) return@forEachIndexed
            when (turn) {
                is Turn.Line -> LineBubble(turn) { tts?.speak(turn.text, owner) }
                is Turn.Pick ->
                    if (picks.indexOf(i) < solved) MyBubble(turn.options.first { it.ok }.text)
                    else PickBlock(turn, "${ctx.seed}#$i", onWrong = { mistakes++ }, onRight = { solved++ })
            }
        }
    }
}

/** Câu ví dụ: bấm để nghe, bấm lần nữa ẩn/hiện nghĩa. */
@Composable
fun SayRow(s: Say) {
    val tts = LocalTts.current
    val owner = remember { Any() }
    var vi by rememberSaveable(s.en) { mutableStateOf(false) }
    Pushable({ tts?.speak(s.en, owner); vi = !vi }, MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp), Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(s.en, style = MaterialTheme.typography.bodyLarge)
            if (vi && s.vi.isNotBlank()) Text(s.vi, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LineBubble(l: Turn.Line, onSpeak: () -> Unit) {
    var vi by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(0.85f)) {
        Text(l.who, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Pushable({ onSpeak(); vi = !vi }, MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text(l.text, style = MaterialTheme.typography.bodyLarge)
                if (vi && l.vi.isNotBlank()) Text(l.vi, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MyBubble(text: String) {
    val t = LocalTrack.current
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Text(text, Modifier.fillMaxWidth(0.85f).background(t.container, RoundedCornerShape(20.dp)).padding(14.dp), color = t.onContainer, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PickBlock(p: Turn.Pick, seed: String, onWrong: () -> Unit, onRight: () -> Unit) {
    val perm = remember(seed) { permutation(p.options.size, seed) }
    var wrong by remember(seed) { mutableStateOf(setOf<Int>()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bạn trả lời", style = MaterialTheme.typography.labelLarge, color = LocalTrack.current.accent)
        perm.forEach { i ->
            val o = p.options[i]
            ChoiceRow(AnnotatedString(o.text), if (i in wrong) Mark.WRONG else Mark.IDLE, enabled = i !in wrong, why = if (i in wrong) o.why.ifBlank { null } else null) {
                when {
                    o.ok -> onRight()
                    i !in wrong -> { wrong = wrong + i; onWrong() }
                }
            }
        }
    }
}
