package com.thanhnb.hocmoingay.feature.player.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.Markdown
import com.thanhnb.hocmoingay.core.lesson.FreeText
import com.thanhnb.hocmoingay.core.lesson.SpeakFree
import com.thanhnb.hocmoingay.core.lesson.TimedTalk
import com.thanhnb.hocmoingay.core.net.ApiResult
import com.thanhnb.hocmoingay.core.net.WriteFb
import com.thanhnb.hocmoingay.core.speech.SpeechScore
import com.thanhnb.hocmoingay.core.speech.Talk
import com.thanhnb.hocmoingay.core.speech.phraseUsed
import com.thanhnb.hocmoingay.core.speech.rememberTalk
import com.thanhnb.hocmoingay.core.speech.scoreSpeech
import com.thanhnb.hocmoingay.core.speech.wordsPerMinute
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.english.MicButton
import com.thanhnb.hocmoingay.feature.english.SpeakerBar
import com.thanhnb.hocmoingay.feature.english.rememberMic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Máy không có dịch vụ nhận dạng (spec §7.2): bỏ qua card nói, không chặn bài. */
@Composable
private fun NoMic(ctx: CardCtx) {
    Ungraded(ctx)
    Text("Máy này chưa có dịch vụ nhận dạng giọng nói, nên bỏ qua card nói. Bấm Tiếp để học tiếp.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun rememberHasMic(talk: Talk?): Boolean = remember(talk) { talk?.available() == true }

private const val MIC_DENIED = "Cần quyền micro để luyện nói. Bạn có thể bỏ qua card này."

/** speak (đọc to) và shadow (nghe rồi nhắc lại ngay, C7): chấm LCS theo từ, tô từ sai/thiếu. Không tính vào điểm bài (Quyết định 4). */
@Composable
fun RepeatCard(text: String, shadow: Boolean, ctx: CardCtx) {
    val talk = rememberTalk()
    if (talk == null || !rememberHasMic(talk)) return NoMic(ctx)
    val f = LocalFun.current
    var denied by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf<SpeechScore?>(null) }
    val mic = rememberMic(onDenied = { denied = true }) { denied = false; tried = true; score = null; talk.start() }
    LaunchedEffect(talk.on) {
        if (talk.on || !tried || talk.text.isBlank()) return@LaunchedEffect
        val s = scoreSpeech(text, talk.text)
        score = s
        ctx.save(buildJsonObject { put("score", s.percent); put("heard", talk.text) })
        if (!ctx.answered) ctx.onAnswer(true, false)
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(if (shadow) "Nghe rồi nhắc lại ngay" else "Đọc to câu này", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (shadow) SpeakerBar(text, autoPlay = ctx.active)
        val s = score
        Text(
            if (s == null) AnnotatedString(text) else buildAnnotatedString {
                s.marks.forEach { m ->
                    withStyle(if (m.ok) SpanStyle() else SpanStyle(color = f.coral, textDecoration = TextDecoration.Underline)) { append(m.text) }
                    append(" ")
                }
            },
            style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
        )
        if (s != null) Text("Khớp ${s.percent}% số từ của câu mẫu", style = MaterialTheme.typography.titleMedium)
        if (talk.on) Text(talk.shown.ifEmpty { "Đang nghe…" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (tried && !talk.on && talk.text.isBlank() && talk.error == null) Text("Chưa nghe thấy gì. Bấm micro rồi nói to hơn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        talk.error?.let { Why(it, ok = false) }
        if (denied) Why(MIC_DENIED, ok = false)
        MicButton(talk.on) { if (talk.on) talk.stop() else mic() }
        if (!ctx.answered) TextButton(onClick = { ctx.onAnswer(true, false) }) { Text("Bỏ qua") }
    }
}

/** timed_talk (C8): 3 vòng 60/45/30 giây cùng một nội dung; đếm từ/phút và các cụm must_use đã dùng; không gọi Gemini. */
@Composable
fun TimedTalkCard(c: TimedTalk, ctx: CardCtx) {
    val talk = rememberTalk()
    if (talk == null || !rememberHasMic(talk) || c.rounds.isEmpty()) return NoMic(ctx)
    val scope = rememberCoroutineScope()
    var round by rememberSaveable { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var left by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf("") }
    val wpm = remember { mutableStateListOf<Int>() }
    var denied by remember { mutableStateOf(false) }
    val secs = c.rounds.getOrElse(round) { 0 }
    val mic = rememberMic(onDenied = { denied = true }) {
        denied = false
        running = true
        talk.start(secs * 1000L)
        scope.launch {
            left = secs
            while (left > 0 && talk.on) { delay(1000); left-- }
            talk.stop()
            withTimeoutOrNull(3000) { snapshotFlow { talk.on }.first { !it } } // chờ kết quả lượt cuối
            if (talk.error != null && talk.text.isBlank()) { running = false; return@launch } // lỗi nhận dạng: giữ vòng này để thử lại
            last = talk.text
            wpm += wordsPerMinute(talk.text, secs - left)
            running = false
            round++
            if (round >= c.rounds.size) {
                ctx.save(buildJsonObject { put("wpm", buildJsonArray { wpm.forEach { add(it) } }) })
                if (!ctx.answered) ctx.onAnswer(true, false)
            }
        }
    }
    val src = if (running) talk.shown else last
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Nói theo giờ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(c.prompt, style = MaterialTheme.typography.titleLarge)
        Text("Nói cùng một nội dung ${c.rounds.size} lần, mỗi lần ngắn hơn: ${c.rounds.joinToString(" → ") { "$it giây" }}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (c.mustUse.isNotEmpty()) {
            Text("Cố dùng các cụm:", style = MaterialTheme.typography.labelLarge)
            c.mustUse.forEach { p ->
                val used = phraseUsed(p, src)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(if (used) Icons.Filled.Check else Icons.Filled.Add, null, tint = if (used) LocalFun.current.mint else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    Text(p, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (running) {
            Text("$left", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = LocalTrack.current.accent)
            Text(talk.shown.ifEmpty { "Đang nghe…" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        wpm.forEachIndexed { i, n -> Text("Vòng ${i + 1}: $n từ/phút", style = MaterialTheme.typography.bodyLarge) }
        talk.error?.let { Why(it, ok = false) }
        if (denied) Why(MIC_DENIED, ok = false)
        if (round < c.rounds.size) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MicButton(running) { if (running) talk.stop() else mic() }
                Text(if (running) "Bấm để dừng sớm" else "Bắt đầu vòng ${round + 1} ($secs giây)", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (!ctx.answered) TextButton(onClick = { talk.cancel(); ctx.onAnswer(true, false) }) { Text("Bỏ qua") }
    }
}

/** speak_free: nói tự do tối đa 90 giây, sửa transcript nếu máy nghe nhầm, rồi gửi `feedback` mode speaking. */
@Composable
fun SpeakFreeCard(c: SpeakFree, ctx: CardCtx) {
    val talk = rememberTalk()
    if (talk == null || !rememberHasMic(talk)) return NoMic(ctx)
    var transcript by rememberSaveable { mutableStateOf("") }
    var denied by remember { mutableStateOf(false) }
    val mic = rememberMic(onDenied = { denied = true }) { denied = false; talk.start(90_000) }
    LaunchedEffect(talk.on) { if (!talk.on && talk.text.isNotBlank()) transcript = talk.text }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Markdown(c.prompt)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MicButton(talk.on) { if (talk.on) talk.stop() else mic() }
            Text(if (talk.on) "Đang nghe… bấm để dừng (tối đa 90 giây)" else "Bấm micro rồi nói", style = MaterialTheme.typography.bodyLarge)
        }
        if (talk.on) Text(talk.shown, color = MaterialTheme.colorScheme.onSurfaceVariant)
        talk.error?.let { Why(it, ok = false) }
        if (denied) Why(MIC_DENIED, ok = false)
        if (transcript.isNotBlank() && !talk.on) {
            OutlinedTextField(transcript, { transcript = it }, Modifier.fillMaxWidth(), label = { Text("App nghe được (sửa nếu nhận sai)") })
        }
        FeedbackArea(transcript, minChars = 10, speaking = true, sample = c.sample, ctx = ctx)
    }
}

/** free_text mode writing: viết rồi gửi `feedback` mode writing. */
@Composable
fun WritingCard(c: FreeText, ctx: CardCtx) {
    var text by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Markdown(c.prompt)
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 140.dp), label = { Text("Bài viết của bạn (tiếng Anh)") })
        FeedbackArea(text, minChars = 20, speaking = false, sample = c.sample, ctx = ctx)
    }
}

/** Nút Nhận xét + Bỏ qua dùng chung; thiếu mạng thì tắt nút và cho bỏ qua (spec §7.2). */
@Composable
private fun FeedbackArea(text: String, minChars: Int, speaking: Boolean, sample: String, ctx: CardCtx) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var fb by remember { mutableStateOf<WriteFb?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    fb?.let { return WriteFbView(it, sample) }
    if (!ctx.online) Text("Cần mạng để nhận xét. Bạn có thể bỏ qua card này.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    PushButton(if (busy) "Đang nhận xét…" else "Nhận xét", {
        busy = true; err = null
        scope.launch {
            when (val r = ctx.api.writing(ctx.lessonId, ctx.cardKey, text, speaking)) {
                is ApiResult.Ok -> {
                    fb = r.value
                    ctx.save(buildJsonObject { put("text", text); put("score", r.value.score) })
                    if (!ctx.answered) ctx.onAnswer(true, false)
                }
                is ApiResult.Err -> err = r.message
            }
            busy = false
        }
    }, Modifier.fillMaxWidth(), enabled = ctx.online && !busy && text.trim().length >= minChars)
    if (!ctx.answered) TextButton(onClick = { ctx.onAnswer(true, false) }) { Text("Bỏ qua") }
    err?.let { Why(it, ok = false) }
}

@Composable
private fun WriteFbView(r: WriteFb, sample: String) {
    Pushable(null, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(24.dp), Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${r.score}/10", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(if (r.taskDone) "Đã làm đúng yêu cầu của đề." else "Chưa làm đủ yêu cầu của đề.", style = MaterialTheme.typography.bodyMedium)
            r.fixes.forEach { x ->
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { append(x.wrong) }
                        append("  →  ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(x.right) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (x.why.isNotBlank()) Text(x.why, style = MaterialTheme.typography.bodySmall)
            }
            if (r.tone.isNotBlank()) Text("Giọng văn: ${r.tone}", style = MaterialTheme.typography.bodyMedium)
            if (r.usedChunks.isNotEmpty()) Text("Cụm dùng tốt: " + r.usedChunks.joinToString("; "), style = MaterialTheme.typography.bodyMedium)
            if (r.notes.isNotBlank()) Text(r.notes, style = MaterialTheme.typography.bodyMedium)
            if (r.betterVersion.isNotBlank()) Text("Cách nói tự nhiên hơn: ${r.betterVersion}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            if (sample.isNotBlank()) Text("Bài mẫu: $sample", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
