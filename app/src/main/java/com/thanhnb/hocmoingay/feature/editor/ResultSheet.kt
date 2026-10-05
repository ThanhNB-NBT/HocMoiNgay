package com.thanhnb.hocmoingay.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.CodeBlock
import com.thanhnb.hocmoingay.core.net.Grade
import com.thanhnb.hocmoingay.core.net.TestReport
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.player.cards.Why

@Composable
fun ResultBody(out: Outcome, lang: String, solution: String?) {
    val f = LocalFun.current
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val j = when (out) {
            is Outcome.Failed -> { Why(out.message, ok = false); return@Column }
            is Outcome.Ran -> out.j
            is Outcome.Submitted -> out.j
        }
        val passed = j.tests.count { it.pass }
        val good = j.allPass
        Pushable(null, if (good) f.mintContainer else f.coralContainer, RoundedCornerShape(24.dp), Modifier.fillMaxWidth(), edge = if (good) f.mint else f.coral) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    when { !j.compiled -> "Chưa biên dịch được"; good -> "Qua hết ${j.tests.size} test"; else -> "Qua $passed/${j.tests.size} test" },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (good) cs.onSurface else f.onCoralContainer,
                )
                Text(
                    listOfNotNull("${j.timeMs} ms", j.memoryKb?.let { "$it KB" }, if (out is Outcome.Ran) "chỉ test công khai" else "gồm test ẩn").joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium, color = if (good) cs.onSurfaceVariant else f.onCoralContainer,
                )
            }
        }
        if (!j.compiled) CodeBlock(j.compileOutput.orEmpty().ifEmpty { j.stderr }, "text")
        j.tests.forEach { t -> TestRow(t) }
        if (j.stdout.isNotBlank() || j.stderr.isNotBlank()) {
            Text("Output", style = MaterialTheme.typography.labelLarge)
            CodeBlock((j.stdout + j.stderr).trimEnd(), "text")
        }
        if (out is Outcome.Submitted) {
            j.grade?.let { GradeCard(it) } ?: j.gradeError?.let { Text("Chưa có nhận xét: $it", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant) }
            if (good && solution != null) {
                var show by remember { mutableStateOf(false) }
                TextButton(onClick = { show = !show }) { Text(if (show) "Ẩn lời giải mẫu" else "Xem lời giải mẫu") }
                if (show) CodeBlock(solution, lang)
            }
        }
    }
}

@Composable
private fun TestRow(t: TestReport) {
    val f = LocalFun.current
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (t.pass) Icons.Filled.Check else Icons.Filled.Close, if (t.pass) "Đạt" else "Trượt", tint = if (t.pass) f.mint else f.coral, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (t.hidden) "Test ẩn" else t.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text("${t.timeMs} ms", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!t.pass && !t.hidden) {
            t.expected?.let { Text("Mong đợi: $it", fontFamily = JetBrainsMono, style = MaterialTheme.typography.bodySmall) }
            t.actual?.let { Text("Nhận được: $it", fontFamily = JetBrainsMono, style = MaterialTheme.typography.bodySmall) }
        }
        t.error?.let { if (!t.pass) Text(it.take(400), fontFamily = JetBrainsMono, style = MaterialTheme.typography.bodySmall, color = f.onCoralContainer) }
    }
}

@Composable
private fun GradeCard(g: Grade) {
    val cs = MaterialTheme.colorScheme
    Pushable(null, cs.secondaryContainer, RoundedCornerShape(24.dp), Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nhận xét code", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cs.onSecondaryContainer)
            listOf("Đúng" to g.correctness, "Dễ đọc" to g.readability, "Thực hành tốt" to g.bestPractices, "Đúng chất ngôn ngữ" to g.idiomatic).forEach { (k, v) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(k, Modifier.width(140.dp), style = MaterialTheme.typography.bodyMedium, color = cs.onSecondaryContainer)
                    Box(Modifier.weight(1f).height(10.dp).clip(CircleShape).background(cs.surfaceContainerLowest)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(v.coerceIn(0, 10) / 10f).clip(CircleShape).background(cs.secondary))
                    }
                    Text(" $v/10", style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer)
                }
            }
            Text("Độ phức tạp ${g.complexity}" + if (g.complexityOk) " · ổn" else " · chưa tối ưu", style = MaterialTheme.typography.bodyMedium, color = cs.onSecondaryContainer)
            Text(g.summary, style = MaterialTheme.typography.bodyMedium, color = cs.onSecondaryContainer)
            g.suggestions.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = cs.onSecondaryContainer) }
        }
    }
}
