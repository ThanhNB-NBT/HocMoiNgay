package com.thanhnb.hocmoingay.feature.player.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.CodeBlock
import com.thanhnb.hocmoingay.core.code.rememberHighlighted
import com.thanhnb.hocmoingay.core.lesson.OrderLines
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.feature.player.fadeOk
import com.thanhnb.hocmoingay.feature.player.orderOk
import com.thanhnb.hocmoingay.feature.player.permutation

/** Một mẩu dòng: id < lines.size là dòng thật (chỉ số trong `lines`, để khớp `fade`), id lớn hơn là dòng nhiễu. */
private data class Piece(val id: Int, val text: String)

@Composable
fun OrderLinesCard(c: OrderLines, ctx: CardCtx) {
    val cs = MaterialTheme.colorScheme
    val fades = remember(c) { c.fades }
    val pool = remember(ctx.seed) {
        val all = c.lines.mapIndexed { i, l -> Piece(i, l) } + c.distractors.mapIndexed { i, l -> Piece(c.lines.size + i, l) }
        permutation(all.size, ctx.seed).map { all[it] }
    }
    val placed = remember { mutableStateListOf<Piece>() }
    val fills = remember { mutableStateMapOf<Int, String>() }
    var checked by remember { mutableStateOf<Boolean?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Chương trình của bạn", style = MaterialTheme.typography.labelLarge)
        Column(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainerLow).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (placed.isEmpty()) Text("Chạm các dòng bên dưới theo đúng thứ tự", Modifier.padding(8.dp), color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            placed.forEachIndexed { pos, p ->
                val mark = when (checked) { null -> Mark.IDLE; else -> if (c.lines.getOrNull(pos) == p.text) Mark.RIGHT else Mark.WRONG }
                LineChip(p.text, c.lang, fades[p.id], fills[p.id].orEmpty(), onFill = { fills[p.id] = it }, mark = mark, enabled = checked == null) {
                    if (checked == null) placed.remove(p)
                }
            }
        }
        Text("Các dòng", style = MaterialTheme.typography.labelLarge)
        pool.filter { it !in placed }.forEach { p ->
            LineChip(p.text, c.lang, fades[p.id], "", onFill = null, mark = Mark.IDLE, enabled = checked == null) {
                if (checked == null) placed.add(p)
            }
        }
        if (checked == null) PushButton("Kiểm tra", {
            val ok = orderOk(placed.map { it.text }, c.lines) && fadeOk(fills, fades)
            checked = ok; ctx.onAnswer(ok, true)
        }, Modifier.fillMaxWidth(), enabled = placed.isNotEmpty())
        if (checked == false) {
            Text("Thứ tự đúng", style = MaterialTheme.typography.labelLarge)
            CodeBlock(c.lines.joinToString("\n"), c.lang)
        }
    }
}

/** Dòng code dạng khối. Dòng bị che token (faded Parsons) có ô nhập ngay trong dòng; khi còn ở kho thì hiện "____". */
@Composable
private fun LineChip(text: String, lang: String, fade: String?, fill: String, onFill: ((String) -> Unit)?, mark: Mark, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val f = LocalFun.current
    val face = when (mark) { Mark.RIGHT -> f.mintContainer; Mark.WRONG -> f.coralContainer; else -> cs.surfaceContainerLowest }
    val style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono, color = cs.onSurface)
    Pushable(
        onClick, face, RoundedCornerShape(14.dp), Modifier.fillMaxWidth(),
        edge = cs.outlineVariant, border = BorderStroke(2.dp, cs.outlineVariant), enabled = enabled || mark != Mark.IDLE,
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (fade == null) {
                Text(rememberHighlighted(text, lang), style = style, softWrap = false)
            } else {
                Text(text.substringBefore(fade), style = style, softWrap = false)
                if (onFill != null) {
                    BasicTextField(
                        fill, onFill, Modifier.widthIn(min = 56.dp).width(IntrinsicSize.Min)
                            .background(cs.secondaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                        textStyle = style, singleLine = true, enabled = enabled,
                    )
                } else {
                    Text("____", style = style.copy(color = cs.secondary), softWrap = false)
                }
                Text(text.substringAfter(fade), style = style, softWrap = false)
            }
        }
    }
}
