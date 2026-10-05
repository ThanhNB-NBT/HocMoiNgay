package com.thanhnb.hocmoingay.core.code

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.net.Assets
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface Md {
    data class Heading(val level: Int, val text: String) : Md
    data class Para(val text: String) : Md
    data class Bullet(val text: String, val number: Int?) : Md
    data class Code(val lang: String, val code: String) : Md
    data class Image(val alt: String, val path: String) : Md
}

private val IMG = Regex("!\\[([^\\]]*)]\\(([^)]+)\\)")
private val NUM_ITEM = Regex("(\\d+)\\. (.*)")
private val INLINE = Regex("`([^`]+)`|\\*\\*([^*]+)\\*\\*")

/** Markdown tối giản, đủ cho giáo trình. Khối ```mermaid hiện như code, vì sơ đồ để sang f3. */
fun parseMd(md: String): List<Md> {
    val out = mutableListOf<Md>()
    val para = mutableListOf<String>()
    fun flush() { if (para.isNotEmpty()) { out += Md.Para(para.joinToString(" ")); para.clear() } }
    val lines = md.lines()
    var i = 0
    while (i < lines.size) {
        val t = lines[i].trim()
        val img = IMG.matchEntire(t)
        val num = NUM_ITEM.matchEntire(t)
        when {
            t.startsWith("```") -> {
                flush()
                val buf = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) buf += lines[i++]
                out += Md.Code(t.removePrefix("```").trim(), buf.joinToString("\n"))
            }
            t.isEmpty() -> flush()
            img != null -> { flush(); out += Md.Image(img.groupValues[1], img.groupValues[2]) }
            t.startsWith("#") -> { flush(); val lv = t.takeWhile { it == '#' }.length; out += Md.Heading(minOf(lv, 3), t.drop(lv).trim()) }
            t.startsWith("- ") || t.startsWith("* ") -> { flush(); out += Md.Bullet(t.drop(2), null) }
            num != null -> { flush(); out += Md.Bullet(num.groupValues[2], num.groupValues[1].toInt()) }
            else -> para += t
        }
        i++
    }
    flush()
    return out
}

fun inlineMd(text: String, code: SpanStyle, bold: SpanStyle): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in INLINE.findAll(text)) {
        append(text.substring(last, m.range.first))
        if (m.groups[1] != null) withStyle(code) { append(m.groupValues[1]) } else withStyle(bold) { append(m.groupValues[2]) }
        last = m.range.last + 1
    }
    append(text.substring(last))
}

val LocalAssets = staticCompositionLocalOf<Assets?> { null }

@Composable
fun Markdown(md: String, modifier: Modifier = Modifier) {
    val blocks = remember(md) { parseMd(md) }
    val cs = MaterialTheme.colorScheme
    val ty = MaterialTheme.typography
    val code = SpanStyle(fontFamily = JetBrainsMono, color = cs.secondary, background = cs.surfaceContainerHigh)
    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (b in blocks) when (b) {
            is Md.Heading -> Text(inlineMd(b.text, code, bold), style = if (b.level == 1) ty.titleLarge else ty.titleMedium, fontWeight = FontWeight.Bold)
            is Md.Para -> Text(inlineMd(b.text, code, bold), style = ty.bodyLarge)
            is Md.Bullet -> Row {
                Text(b.number?.let { "$it." } ?: "•", Modifier.width(24.dp), style = ty.bodyLarge, color = cs.secondary, fontWeight = FontWeight.Bold)
                Text(inlineMd(b.text, code, bold), style = ty.bodyLarge)
            }
            is Md.Code -> CodeBlock(b.code, b.lang)
            is Md.Image -> AssetImage(b.path, b.alt)
        }
    }
}

@Composable
private fun AssetImage(path: String, alt: String) {
    val assets = LocalAssets.current
    val img by produceState<ImageBitmap?>(null, path) {
        val bytes = assets?.bytes(path) ?: return@produceState
        value = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(20.dp)
    val bmp = img
    if (bmp != null) {
        Image(bmp, alt, Modifier.fillMaxWidth().clip(shape), contentScale = ContentScale.FillWidth)
    } else {
        Box(Modifier.fillMaxWidth().height(120.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
            Text(alt.ifEmpty { "Ảnh" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Khối code: cuộn ngang trong khung riêng, thân trang không cuộn ngang. [marks] tô nền theo số dòng (từ 1); [onLine] cho chạm từng dòng. */
@Composable
fun CodeBlock(
    code: String, lang: String, modifier: Modifier = Modifier,
    numbered: Boolean = false, marks: Map<Int, Color> = emptyMap(), onLine: ((Int) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    val text = rememberHighlighted(code.trimEnd(), lang)
    val lines = remember(text) {
        var s = 0
        text.text.split('\n').map { l -> text.subSequence(s, s + l.length).also { s += l.length + 1 } }
    }
    val shape = RoundedCornerShape(20.dp)
    BoxWithConstraints(modifier.fillMaxWidth().clip(shape).background(cs.surfaceContainerLowest).border(BorderStroke(2.dp, cs.outlineVariant), shape)) {
        Column(Modifier.horizontalScroll(rememberScrollState()).widthIn(min = maxWidth).width(IntrinsicSize.Max).padding(vertical = 10.dp)) {
            lines.forEachIndexed { i, l ->
                val n = i + 1
                Row(
                    Modifier.fillMaxWidth()
                        .then(marks[n]?.let { Modifier.background(it) } ?: Modifier)
                        .then(if (onLine != null) Modifier.heightIn(min = 40.dp).clickable { onLine(n) } else Modifier)
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (numbered) Text("$n", Modifier.width(28.dp), fontFamily = JetBrainsMono, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    Text(l, fontFamily = JetBrainsMono, style = MaterialTheme.typography.bodyMedium, softWrap = false)
                }
            }
        }
    }
}
