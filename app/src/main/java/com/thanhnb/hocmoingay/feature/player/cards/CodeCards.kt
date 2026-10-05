package com.thanhnb.hocmoingay.feature.player.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.CodeBlock
import com.thanhnb.hocmoingay.core.code.Markdown
import com.thanhnb.hocmoingay.core.lesson.Card
import com.thanhnb.hocmoingay.core.lesson.CodeReview
import com.thanhnb.hocmoingay.core.lesson.CodeTask
import com.thanhnb.hocmoingay.core.lesson.FillBlank
import com.thanhnb.hocmoingay.core.lesson.FindBug
import com.thanhnb.hocmoingay.core.lesson.OrderLines
import com.thanhnb.hocmoingay.core.lesson.PredictOutput
import com.thanhnb.hocmoingay.core.lesson.RunExample
import com.thanhnb.hocmoingay.core.net.ApiResult
import com.thanhnb.hocmoingay.core.net.FreeRun
import com.thanhnb.hocmoingay.core.net.compileEta
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.player.blankParts
import com.thanhnb.hocmoingay.feature.player.fillOk
import com.thanhnb.hocmoingay.feature.player.linesOk
import com.thanhnb.hocmoingay.feature.player.norm
import com.thanhnb.hocmoingay.feature.player.permutation
import com.thanhnb.hocmoingay.feature.player.predictOk
import com.thanhnb.hocmoingay.feature.player.reviewOk
import com.thanhnb.hocmoingay.feature.settings.LANGUAGES
import kotlin.math.roundToLong
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

fun langName(k: String): String =
    LANGUAGES[k] ?: mapOf("postgres" to "PostgreSQL", "sqlite3" to "SQLite", "bash" to "Bash")[k] ?: k

private fun mono(s: String) = buildAnnotatedString { withStyle(SpanStyle(fontFamily = JetBrainsMono)) { append(s) } }

@Composable
fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) =
    PushButton(text, onClick, modifier, color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, enabled = enabled)

@Composable
fun OfflineNote() = Text("Cần mạng để chạy code", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Prompt(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

@Composable
fun CodeCardView(card: Card, ctx: CardCtx) = when (card) {
    is PredictOutput -> PredictCard(card, ctx)
    is RunExample -> RunExampleCard(card, ctx)
    is FillBlank -> FillBlankCard(card, ctx)
    is OrderLines -> OrderLinesCard(card, ctx)
    is FindBug -> FindBugCard(card, ctx)
    is CodeReview -> CodeReviewCard(card, ctx)
    is CodeTask -> CodeTaskCard(card, ctx)
    else -> LaterCard(ctx) // loại card app chưa biết vẽ
}

@Composable
private fun PredictCard(c: PredictOutput, ctx: CardCtx) {
    var given by rememberSaveable { mutableStateOf("") }
    var checked by remember { mutableStateOf<Boolean?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Prompt("Đoán xem chương trình in ra gì")
        CodeBlock(c.code, c.lang)
        val choices = c.choices
        if (choices != null) {
            val perm = remember(ctx.seed) { permutation(choices.size, ctx.seed) }
            perm.forEach { i ->
                val ok = predictOk(choices[i], c.answer)
                val mark = when { checked == null -> Mark.IDLE; ok -> Mark.RIGHT; given == choices[i] -> Mark.WRONG; else -> Mark.IDLE }
                ChoiceRow(mono(choices[i]), mark, enabled = checked == null, why = null) {
                    if (checked != null) return@ChoiceRow
                    given = choices[i]; checked = ok; ctx.onAnswer(ok, true)
                }
            }
        } else {
            OutlinedTextField(
                given, { given = it }, Modifier.fillMaxWidth(), label = { Text("Kết quả in ra") }, minLines = 2,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono), enabled = checked == null,
            )
            if (checked == null) PushButton("Kiểm tra", {
                val ok = predictOk(given, c.answer); checked = ok; ctx.onAnswer(ok, true)
            }, Modifier.fillMaxWidth(), enabled = given.isNotBlank())
        }
        if (checked == false) {
            Text("Kết quả đúng", style = MaterialTheme.typography.labelLarge)
            CodeBlock(c.answer, "text")
        }
    }
}

@Composable
private fun RunExampleCard(c: RunExample, ctx: CardCtx) {
    Ungraded(ctx)
    val scope = rememberCoroutineScope()
    var guess by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var out by remember { mutableStateOf<FreeRun?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Prompt("Đoán trước, rồi chạy thật để đối chiếu")
        CodeBlock(c.code, c.lang)
        OutlinedTextField(
            guess, { guess = it }, Modifier.fillMaxWidth(), label = { Text("Bạn đoán in ra gì? (không bắt buộc)") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono),
        )
        ActionButton(
            when { !busy -> "Chạy thật"; compileEta(c.lang) != null -> "Đang biên dịch… thường khoảng ${compileEta(c.lang)} giây"; else -> "Đang chạy…" },
            {
                busy = true; err = null
                scope.launch {
                    when (val r = ctx.api.runFree(c.lang, c.code)) {
                        is ApiResult.Ok -> out = r.value
                        is ApiResult.Err -> err = r.message
                    }
                    busy = false
                }
            },
            Modifier.fillMaxWidth(), enabled = ctx.online && !busy,
        )
        if (!ctx.online) OfflineNote()
        err?.let { Why(it, ok = false) }
        out?.let { r ->
            Text("Kết quả thật", style = MaterialTheme.typography.labelLarge)
            CodeBlock((r.compile?.stderr.orEmpty() + r.run.stdout + r.run.stderr).ifEmpty { "(không in gì)" }, "text")
            r.timeMs?.let { Text("${it.roundToLong()} ms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun FillBlankCard(c: FillBlank, ctx: CardCtx) {
    val parts = remember(c.text) { blankParts(c.text) }
    val n = parts.size - 1
    val fills = remember { mutableStateListOf(*Array(n) { "" }) }
    var checked by remember { mutableStateOf<Boolean?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Prompt("Điền vào chỗ trống")
        CodeBlock(parts.mapIndexed { i, p -> if (i < n) "${p}__${i + 1}__" else p }.joinToString(""), c.lang)
        val f = LocalFun.current
        repeat(n) { i ->
            // sau khi chấm: chỉ đọc (không làm mờ), viền + icon mint/coral theo từng ô
            val right = checked != null && c.answers[i].any { norm(it) == norm(fills[i]) }
            val tone = if (right) f.mint else f.coral
            OutlinedTextField(
                fills[i], { fills[i] = it }, Modifier.fillMaxWidth(), label = { Text("Chỗ trống ${i + 1}") }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono), readOnly = checked != null,
                trailingIcon = if (checked == null) null else {
                    { Icon(if (right) Icons.Filled.Check else Icons.Filled.Close, if (right) "Đúng" else "Sai", tint = tone) }
                },
                colors = if (checked == null) OutlinedTextFieldDefaults.colors() else OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = tone, unfocusedBorderColor = tone, focusedLabelColor = tone, unfocusedLabelColor = tone,
                ),
            )
        }
        if (checked == null) PushButton("Kiểm tra", {
            val ok = fillOk(fills.toList(), c.answers); checked = ok; ctx.onAnswer(ok, true)
        }, Modifier.fillMaxWidth(), enabled = fills.all { it.isNotBlank() })
        if (checked == false) Why("Đáp án: " + c.answers.mapIndexed { i, a -> "${i + 1}) ${a.first()}" }.joinToString("   "), ok = false)
    }
}

@Composable
private fun FindBugCard(c: FindBug, ctx: CardCtx) {
    var picked by remember { mutableStateOf(setOf<Int>()) }
    var checked by remember { mutableStateOf<Boolean?>(null) }
    val f = LocalFun.current
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Prompt("Chạm vào dòng có lỗi")
        CodeBlock(
            c.code, c.lang, numbered = true,
            marks = if (checked == null) picked.associateWith { cs.secondaryContainer }
            else c.bugLines.associateWith { f.mintContainer } + (picked - c.bugLines.toSet()).associateWith { f.coralContainer },
            onLine = { n -> if (checked == null) picked = if (n in picked) picked - n else picked + n }, // giữ chiều cao dòng sau khi chấm
        )
        if (checked == null) PushButton("Kiểm tra", {
            val ok = linesOk(picked, c.bugLines); checked = ok; ctx.onAnswer(ok, true)
        }, Modifier.fillMaxWidth(), enabled = picked.isNotEmpty())
        else Why(c.why, ok = checked == true)
    }
}

@Composable
private fun CodeReviewCard(c: CodeReview, ctx: CardCtx) {
    var picked by remember { mutableStateOf(setOf<Int>()) }
    var checked by remember { mutableStateOf<Boolean?>(null) }
    val f = LocalFun.current
    val cs = MaterialTheme.colorScheme
    val issueLines = remember(c) { c.issues.flatMap { it.lines }.toSet() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Prompt("Đọc code như người review: chạm vào các dòng có vấn đề")
        CodeBlock(
            c.code, c.lang, numbered = true,
            marks = if (checked == null) picked.associateWith { cs.secondaryContainer }
            else issueLines.associateWith { f.mintContainer } + (picked - issueLines).associateWith { f.coralContainer },
            onLine = { n -> if (checked == null) picked = if (n in picked) picked - n else picked + n }, // giữ chiều cao dòng sau khi chấm
        )
        if (checked == null) PushButton("Kiểm tra", {
            val ok = reviewOk(picked, c.issues); checked = ok; ctx.onAnswer(ok, true)
        }, Modifier.fillMaxWidth(), enabled = picked.isNotEmpty())
        else c.issues.forEach { i -> Why("Dòng ${i.lines.joinToString(", ")}: ${i.why}", ok = i.lines.any { it in picked }) }
    }
}

@Composable
private fun CodeTaskCard(c: CodeTask, ctx: CardCtx) {
    val f = LocalFun.current
    val pass = (ctx.state["pass"] as? JsonPrimitive)?.booleanOrNull == true
    val lang = (ctx.state["lang"] as? JsonPrimitive)?.contentOrNull
    // active làm khoá: pager dựng sẵn trang kế khi chưa tới lượt, học lại bài đã đạt thì effect phải chạy lại lúc tới trang
    LaunchedEffect(pass, ctx.active) { if (pass && ctx.active && !ctx.answered) ctx.onAnswer(true, false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (c.starter != null) "Sửa code mẫu" else "Tự viết code", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
        Markdown(c.promptMd)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            c.langs.forEach { l ->
                Text(
                    langName(l), Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (pass) Pushable(null, f.mintContainer, RoundedCornerShape(24.dp), Modifier.fillMaxWidth(), edge = f.mint) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, null, tint = f.mint)
                Spacer(Modifier.width(8.dp))
                Text("Đã nộp đạt" + (lang?.let { " bằng ${langName(it)}" } ?: ""), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
        ActionButton(if (pass) "Mở lại editor" else "Mở editor", { ctx.openEditor(c.key) }, Modifier.fillMaxWidth())
        if (!pass && !ctx.answered) TextButton(onClick = { ctx.onAnswer(true, false) }) { Text("Để sau") }
    }
}
