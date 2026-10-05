package com.thanhnb.hocmoingay.feature.player.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.code.inlineMd
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.ui.Pushable

enum class Mark { IDLE, PICKED, RIGHT, WRONG }

@Composable
fun ChoiceRow(text: AnnotatedString, mark: Mark, enabled: Boolean, why: String?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val f = LocalFun.current
    val (face, edge, fg) = when (mark) {
        Mark.IDLE -> Triple(cs.surfaceContainerLowest, cs.outlineVariant, cs.onSurface)
        Mark.PICKED -> Triple(cs.secondaryContainer, cs.secondary, cs.onSecondaryContainer)
        Mark.RIGHT -> Triple(f.mintContainer, f.mint, cs.onSurface)
        Mark.WRONG -> Triple(f.coralContainer, f.coral, f.onCoralContainer)
    }
    Column {
        // lựa chọn đã tô màu sau khi chấm vẫn rõ nét; chỉ ô còn IDLE mới mờ đi
        Pushable(
            onClick, face, RoundedCornerShape(20.dp), Modifier.fillMaxWidth(), edge = edge,
            border = if (mark == Mark.IDLE) BorderStroke(2.dp, cs.outlineVariant) else null, enabled = enabled || mark != Mark.IDLE,
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text, Modifier.weight(1f), color = fg, style = MaterialTheme.typography.bodyLarge)
                when (mark) {
                    Mark.RIGHT -> Icon(Icons.Filled.Check, "Đúng", tint = f.mint)
                    Mark.WRONG -> Icon(Icons.Filled.Close, "Sai", tint = f.coral)
                    else -> {}
                }
            }
        }
        if (why != null) Why(why, ok = mark == Mark.RIGHT)
    }
}

@Composable
fun Why(text: String, ok: Boolean) {
    val f = LocalFun.current
    Text(
        // chỉ đổi phông cho `code`, giữ màu chữ của dòng để đủ tương phản trên nền coral
        inlineMd(text, SpanStyle(fontFamily = JetBrainsMono), SpanStyle(fontWeight = FontWeight.Bold)),
        Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = if (ok) MaterialTheme.colorScheme.onSurface else f.onCoralContainer,
    )
}
