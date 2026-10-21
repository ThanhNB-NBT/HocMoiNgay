package com.thanhnb.hocmoingay.feature.player.cards

import androidx.compose.animation.AnimatedContent
import com.thanhnb.hocmoingay.core.theme.mono
import com.thanhnb.hocmoingay.core.theme.MONO_FEATURES
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.CodeBlock
import com.thanhnb.hocmoingay.core.code.inlineMd
import com.thanhnb.hocmoingay.core.lesson.Trace
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalTrack

import kotlinx.serialization.json.JsonPrimitive

/**
 * Chạy từng bước: người học tự bấm qua từng dòng, xem biến đổi và màn hình in ra.
 * Không chấm điểm; chỉ khi tới bước cuối mới báo xong để thanh dưới hiện nút Tiếp.
 */
@Composable
fun TraceCard(c: Trace, ctx: CardCtx) {
    val last = c.steps.lastIndex
    var i by rememberSaveable { mutableIntStateOf(if (ctx.answered) last else 0) }
    LaunchedEffect(i, ctx.active) { if (ctx.active && !ctx.answered && i == last) ctx.onAnswer(true, false) }
    val step = c.steps[i]
    val prev = c.steps.getOrNull(i - 1)
    val track = LocalTrack.current
    val cs = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Xem từng dòng chạy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        CodeBlock(c.code, c.lang, numbered = true, marks = mapOf(step.line to track.container))
        // nút ngay dưới code: phần biến/màn hình dài ra thì nút vẫn đứng yên, bấm liên tục không trượt
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton({ i-- }, enabled = i > 0, shape = RoundedCornerShape(14.dp)) { Text("Lùi") }
            Button(
                { i++ }, Modifier.weight(1f), enabled = i < last, shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = track.accent, contentColor = track.onAccent),
            ) { Text(if (i < last) "Chạy dòng tiếp" else "Đã chạy hết") }
        }

        Text("Bước ${i + 1} / ${c.steps.size}  ·  dòng ${step.line}", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        AnimatedContent(i, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }, label = "ghi chú") { k ->
            Text(
                inlineMd(c.steps[k].note, SpanStyle(fontFamily = JetBrainsMono, fontFeatureSettings = MONO_FEATURES), SpanStyle(fontWeight = FontWeight.Bold)),
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        if (step.vars.isNotEmpty()) {
            Section("Biến") {
                step.vars.forEach { (name, v) ->
                    val value = (v as? JsonPrimitive)?.content ?: v.toString()
                    val changed = prev?.vars?.get(name)?.let { (it as? JsonPrimitive)?.content != value } ?: true
                    VarRow(name, value, changed, track.container, track.onContainer)
                }
            }
        }

        val printed = c.steps.take(i + 1).mapNotNull { it.out }
        if (printed.isNotEmpty()) {
            Section("Màn hình") {
                Text(
                    printed.joinToString("\n"), Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.mono(),
                )
            }
        }

    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.surfaceContainerLow).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        content()
    }
}

/** Một biến: tên bên trái, giá trị bên phải. Giá trị vừa đổi ở bước này được tô nền và trượt lên. */
@Composable
private fun VarRow(name: String, value: String, changed: Boolean, hi: Color, onHi: Color) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (changed) hi else cs.surfaceContainerLowest, tween(250), label = "nền biến")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.width(120.dp), style = MaterialTheme.typography.bodyMedium.mono(), color = cs.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        AnimatedContent(
            value,
            transitionSpec = { (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith fadeOut(tween(100)) },
            label = "giá trị",
        ) { v ->
            Text(
                v, Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodyMedium.mono(), color = if (changed) onHi else cs.onSurface,
            )
        }
    }
}
