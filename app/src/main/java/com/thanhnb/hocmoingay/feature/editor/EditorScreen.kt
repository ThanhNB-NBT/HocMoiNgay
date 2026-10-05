package com.thanhnb.hocmoingay.feature.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.code.CodeBlock
import com.thanhnb.hocmoingay.core.code.Markdown
import com.thanhnb.hocmoingay.core.net.compileEta
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.player.cards.ActionButton
import com.thanhnb.hocmoingay.feature.player.cards.OfflineNote
import com.thanhnb.hocmoingay.feature.player.cards.Why
import com.thanhnb.hocmoingay.feature.player.cards.langName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel, onBack: () -> Unit) {
    val s = vm.ui.collectAsStateWithLifecycle().value
    val online = vm.online.collectAsStateWithLifecycle().value
    if (s.missing) return Placeholder("Không mở được bài tập", "Bài chưa có trên máy hoặc không có ngôn ngữ nào chạy được.")
    val card = s.card ?: return Box(Modifier.fillMaxSize())
    val lang = s.lang ?: return Box(Modifier.fillMaxSize())
    var showPrompt by rememberSaveable { mutableStateOf(true) }
    var showHints by remember { mutableStateOf(false) }
    ProvideTrack(Track.CODE) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }
                TextButton(onClick = { showPrompt = !showPrompt }) {
                    Text("Đề bài"); Icon(if (showPrompt) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
                }
                Spacer(Modifier.weight(1f))
                LangMenu(lang, card.langs, enabled = s.busy == null, onPick = vm::pick)
            }
            if (showPrompt) Markdown(card.promptMd, Modifier.padding(horizontal = 20.dp).heightIn(max = 180.dp).verticalScroll(rememberScrollState()))
            s.notice?.let { Why(it, ok = false) }
            if (s.code == null) Box(Modifier.weight(1f).fillMaxWidth())
            else SoraEditor(s.code, lang, vm::onEdit, Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp))
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (s.busy) {
                    EditorUi.Busy.RUN -> Text(compileEta(lang)?.let { "Đang biên dịch… thường khoảng $it giây" } ?: "Đang chạy…", style = MaterialTheme.typography.labelLarge)
                    EditorUi.Busy.SUBMIT -> Text("Đang chấm… thường 10–20 giây", style = MaterialTheme.typography.labelLarge)
                    else -> if (!online) OfflineNote()
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (s.maxHints > 0) ActionButton("Gợi ý ${s.hints}/${s.maxHints}", { showHints = true })
                    ActionButton("Chạy", vm::run, Modifier.weight(1f), enabled = online && s.busy == null)
                    PushButton("Nộp", vm::submit, Modifier.weight(1f), enabled = online && s.busy == null)
                }
            }
        }
        s.result?.let { out ->
            ModalBottomSheet(onDismissRequest = vm::dismissResult) { ResultBody(out, lang, card.solutions[lang]) }
        }
        if (showHints) ModalBottomSheet(onDismissRequest = { showHints = false }) {
            HintSheet(s, online, onMore = vm::openHint)
        }
    }
}

@Composable
private fun LangMenu(lang: String, langs: List<String>, enabled: Boolean, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        ActionButton(langName(lang) + " ▾", { open = true }, enabled = enabled && langs.size > 1)
        DropdownMenu(open, { open = false }) {
            langs.forEach { l -> DropdownMenuItem(text = { Text(langName(l)) }, onClick = { open = false; if (l != lang) onPick(l) }) }
        }
    }
}

@Composable
private fun HintSheet(s: EditorUi, online: Boolean, onMore: () -> Unit) {
    val hints = s.body?.hints.orEmpty()
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Gợi ý", style = MaterialTheme.typography.headlineSmall)
        Text("Mỗi nấc mở thêm làm thẻ ôn của bài này đến sớm hơn.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        hints.take(s.hints).forEachIndexed { i, h -> Text("${i + 1}. $h", style = MaterialTheme.typography.bodyLarge) }
        s.hintLines?.let { lines ->
            Text("${hints.size + 1}. Các dòng của lời giải mẫu, đã xáo trộn:", style = MaterialTheme.typography.bodyLarge)
            CodeBlock(lines.joinToString("\n"), s.lang ?: "text")
        }
        val canMore = s.hints < s.maxHints || (s.hints == s.maxHints && s.hintLines == null)
        if (canMore) {
            val needNet = s.hints + 1 >= s.maxHints
            ActionButton(
                if (s.busy == EditorUi.Busy.HINT) "Đang tải…" else "Mở nấc ${minOf(s.hints + 1, s.maxHints)}",
                onMore, Modifier.fillMaxWidth(), enabled = s.busy == null && (online || !needNet),
            )
            if (needNet && !online) OfflineNote()
        }
    }
}
