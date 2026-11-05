package com.thanhnb.hocmoingay.feature.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.Markdown
import com.thanhnb.hocmoingay.core.lesson.Card
import com.thanhnb.hocmoingay.core.lesson.Explain
import com.thanhnb.hocmoingay.core.lesson.Gap
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.Listen
import com.thanhnb.hocmoingay.core.lesson.Quiz
import com.thanhnb.hocmoingay.core.lesson.Read
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.english.SpeakerBar
import com.thanhnb.hocmoingay.feature.player.cards.ChoiceRow
import com.thanhnb.hocmoingay.feature.player.cards.ImageBlock
import com.thanhnb.hocmoingay.feature.player.cards.Mark
import com.thanhnb.hocmoingay.feature.player.cards.Why
import com.thanhnb.hocmoingay.feature.player.cards.clock
import com.thanhnb.hocmoingay.feature.player.cards.numberedBlanks
import com.thanhnb.hocmoingay.feature.player.listenTurns
import kotlinx.coroutines.delay

/**
 * Thi thử (`kind: mock`): từng phần một, mỗi phần một trang cuộn có đồng hồ riêng; hết giờ hoặc bấm Nộp thì sang phần sau
 * và không quay lại được. Không hiện đúng/sai tới khi nộp phần cuối; khi đó quy đổi điểm theo [scaledLabel].
 * ponytail: bài nghe được bấm nghe lại (đề thật chỉ phát một lần); thêm giới hạn khi có bài nghe dạng file.
 */
@Composable
fun MockScreen(body: LessonBody, onFinish: (score: Int) -> Unit, onBack: () -> Unit) {
    val parts = remember(body) { partCards(body) }
    var part by rememberSaveable { mutableIntStateOf(0) }
    var answers by rememberSaveable { mutableStateOf(mapOf<String, String>()) }
    if (part >= parts.size) {
        val results = remember(answers) { mockResults(body, answers) }
        LaunchedEffect(Unit) { onFinish(mockPercent(results)) }
        return MockResult(body, results, answers, onBack)
    }
    val (p, cards) = parts[part]
    var left by rememberSaveable(part) { mutableIntStateOf(p.timeMin * 60) }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(part) {
        while (left > 0) { delay(1000); left-- }
        part++
    }
    // số câu đánh liền cả đề, như phiếu trả lời thật
    val start = parts.take(part).sumOf { (_, cs) -> cs.sumOf { mockQuestions(it).size } } + 1
    val f = LocalFun.current
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Phần ${part + 1}/${parts.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(p.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                clock(left), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = if (left <= 60) f.coral else LocalTrack.current.accent,
            )
        }
        // mỗi phần một ScrollState mới: sang phần sau thì về đầu trang
        val scroll = rememberSaveable(part, saver = ScrollState.Saver) { ScrollState(0) }
        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            var n = start
            cards.forEach { c ->
                MockCard(c, n, answers) { id, v -> answers = answers + (id to v) }
                n += mockQuestions(c).size
            }
            PushButton(if (part + 1 < parts.size) "Nộp phần này" else "Nộp bài", { confirm = true }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirm) {
        val empty = cards.flatMap(::mockQuestions).count { answers[it.id].isNullOrBlank() }
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(if (part + 1 < parts.size) "Nộp phần ${p.title}?" else "Nộp bài?") },
            text = { Text((if (empty > 0) "Còn $empty câu bỏ trống. " else "") + "Nộp rồi thì không quay lại sửa phần này được.") },
            confirmButton = { TextButton({ confirm = false; part++ }) { Text("Nộp") } },
            dismissButton = { TextButton({ confirm = false }) { Text("Làm tiếp") } },
        )
    }
}

/** Một card trong đề: chỉ đề và ô trả lời, không chấm tại chỗ. [n] là số của câu đầu tiên trong card. */
@Composable
private fun MockCard(c: Card, n: Int, answers: Map<String, String>, set: (String, String) -> Unit) {
    val qs = remember(c) { mockQuestions(c) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (c) {
            is Explain -> Markdown(c.md)
            is Listen -> {
                ImageBlock(c.image)
                val turns = remember(c) { listenTurns(c.turns) }
                SpeakerBar(c.text ?: turns.joinToString("\n") { it.second }, turns = turns)
            }
            is Read -> { ImageBlock(c.image); Markdown(c.md) }
            is Quiz -> ImageBlock(c.image)
            is Gap -> {
                ImageBlock(c.image)
                Text(
                    numberedBlanks(c.text, n), Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(16.dp)).padding(14.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> {}
        }
        qs.forEachIndexed { i, q ->
            if (q.typed) {
                OutlinedTextField(answers[q.id].orEmpty(), { set(q.id, it) }, Modifier.fillMaxWidth(), label = { Text("Câu ${n + i}") }, singleLine = true)
            } else {
                Text("${n + i}. ${q.q}", style = MaterialTheme.typography.titleMedium)
                val picked = answers[q.id].orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet()
                val multi = q.answer.size > 1
                // giữ đúng thứ tự lựa chọn của đề (TRUE/FALSE/NOT GIVEN, A/B/C/D)
                q.choices.forEachIndexed { k, ch ->
                    ChoiceRow(AnnotatedString(ch), if (k in picked) Mark.PICKED else Mark.IDLE, enabled = true, why = null) {
                        val next = if (!multi) setOf(k) else if (k in picked) picked - k else picked + k
                        set(q.id, next.sorted().joinToString(","))
                    }
                }
            }
        }
    }
}

@Composable
private fun MockResult(body: LessonBody, results: List<PartResult>, answers: Map<String, String>, onBack: () -> Unit) {
    val f = LocalFun.current
    val total = toeicTotal(body.exam.orEmpty(), results)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Kết quả thi thử", style = MaterialTheme.typography.displaySmall)
        results.forEach { r ->
            Pushable(null, f.coralContainer, RoundedCornerShape(18.dp), Modifier.fillMaxWidth(), edge = f.coral) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text(r.part.title, style = MaterialTheme.typography.titleMedium, color = f.onCoralContainer)
                    Text(r.label, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = f.onCoralContainer)
                    Text("${r.correct}/${r.total} câu đúng", style = MaterialTheme.typography.bodyLarge, color = f.onCoralContainer)
                }
            }
        }
        if (total != null) Text("Tổng Listening + Reading: $total/990", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Điểm quy đổi là ước tính theo bảng công khai; đề thật mỗi kỳ dùng một bảng riêng.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Xem lại", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        var n = 1
        partCards(body).forEach { (_, cards) ->
            cards.flatMap(::mockQuestions).forEach { q ->
                val given = answers[q.id]
                val ok = q.ok(given)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$n. " + q.q.ifBlank { "Chỗ trống" }, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Bạn: ${q.shown(given)}" + if (ok) "" else "   ·   Đáp án: ${q.right}",
                        style = MaterialTheme.typography.bodyMedium, color = if (ok) f.mint else f.coral, fontWeight = FontWeight.SemiBold,
                    )
                    if (!ok && q.why.isNotBlank()) Why(q.why, ok = false)
                }
                n++
            }
        }
        PushButton("Về đề cương", onBack, Modifier.fillMaxWidth())
    }
}
