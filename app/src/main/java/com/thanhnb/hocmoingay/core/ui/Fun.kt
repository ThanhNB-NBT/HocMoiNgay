package com.thanhnb.hocmoingay.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.theme.Ink
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hình "bánh quy" [lobes] múi: r(θ) = R·(1 − d + d·cos(n·θ)).
 * Tự vẽ vì material3 1.4.0 chưa có MaterialShapes; [turn] xoay hình theo vòng (0..1).
 */
class CookieShape(private val lobes: Int, private val depth: Float = 0.08f, private val turn: Float = 0f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = min(size.width, size.height) / 2
        val p = Path()
        for (i in 0..STEPS) {
            val t = (2 * PI * i / STEPS).toFloat()
            val rr = r * (1 - depth + depth * cos(lobes * (t - 2 * PI.toFloat() * turn)))
            val x = size.width / 2 + rr * cos(t)
            val y = size.height / 2 + rr * sin(t)
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return Outline.Generic(p)
    }

    private companion object { const val STEPS = 144 }
}

val Cookie = CookieShape(9, 0.06f)
val Flower = CookieShape(6, 0.10f)
val Clover = CookieShape(4, 0.14f)

/** Mỗi khoá một hình khác nhau (4–10 múi) để danh sách không lặp một khuôn. */
fun shapeFor(key: String) = CookieShape(4 + (key.hashCode() and 0x7fffffff) % 7, 0.09f)

private val EDGE = 4.dp
private val Snappy = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)

/** Cạnh dưới của khối: mặt [face] tối đi một nấc. */
fun edgeOf(face: Color) = lerp(face, Color.Black, 0.22f)

/**
 * Khối "ấn được": mặt nằm trên một cạnh dày [EDGE] cùng hình; bấm thì mặt lún xuống sát cạnh và rung nhẹ.
 * Cái lún chính là phản hồi, không scale, không nảy. [onClick] null = khối tĩnh (vẫn có cạnh).
 */
@Composable
fun Pushable(
    onClick: (() -> Unit)?,
    face: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    edge: Color = edgeOf(face),
    border: BorderStroke? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val focused by src.collectIsFocusedAsState()
    val drop by animateDpAsState(if (pressed && enabled) EDGE - 1.dp else 0.dp, tween(if (pressed) 70 else 140, easing = Snappy), label = "push")
    val haptic = LocalHapticFeedback.current
    val click = if (onClick == null) Modifier else Modifier.clickable(src, indication = null, enabled = enabled, role = Role.Button) {
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        onClick()
    }
    // propagateMinConstraints: khi caller cho fillMaxWidth thì mặt khối cũng giãn theo, không chỉ cạnh
    Box(modifier.alpha(if (enabled) 1f else 0.5f).then(click), propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().padding(top = EDGE).clip(shape).background(edge))
        Box(
            Modifier.padding(bottom = EDGE).offset { IntOffset(0, drop.roundToPx()) }.clip(shape).background(face)
                .then(if (border != null) Modifier.border(border, shape) else Modifier)
                .then(if (focused) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
            content = content,
        )
    }
}

/** Nút khối hình viên thuốc. Mặc định vàng lê + chữ mực — mỗi màn chỉ một nút kiểu này cho hành động chính. */
@Composable
fun PushButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) = Pushable(onClick, color, CircleShape, modifier, enabled = enabled) {
    Row(
        Modifier.align(Alignment.Center).heightIn(min = 52.dp).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
        }
    }
}

/** Số đếm từ 0 lên [target] khi hiện ra (1,2 s, tới nhanh rồi chậm dần), xong thì phồng 1 → 1.06 → 1 một lần. */
@Composable
fun TickUpNumber(target: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val v = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(target) {
        v.animateTo(target.toFloat(), tween(1200, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
        pop.animateTo(1.06f, tween(120)); pop.animateTo(1f, spring(dampingRatio = 0.5f))
    }
    Text(
        v.value.roundToInt().toString(), color = color, style = style.copy(fontFeatureSettings = "tnum"),
        modifier = modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
    )
}

/**
 * Linh vật của app: bánh quy vàng lê có hai mắt. Xoay rất chậm, thở nhẹ, chớp mắt vài giây một lần;
 * chạm vào thì nảy lên. Khoảnh khắc "có người ở đây" duy nhất của màn.
 */
@Composable
fun Critter(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primaryContainer) {
    val inf = rememberInfiniteTransition(label = "critter")
    val turn by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "turn")
    val breathe by inf.animateFloat(1f, 1.04f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "breathe")
    val blink by inf.animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes { durationMillis = 4200; 1f at 3900; 0.1f at 4000; 1f at 4120 }),
        label = "blink",
    )
    val bump = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val shape = remember(turn) { CookieShape(9, 0.06f, turn / 9) }
    Box(
        modifier
            .graphicsLayer { val s = breathe * bump.value; scaleX = s; scaleY = s }
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                scope.launch { bump.snapTo(0.82f); bump.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 400f)) }
            }
            .background(color, shape)
            .drawBehind {
                // hai mắt mực, khép theo [blink]
                val w = size.width * 0.085f
                val h = size.height * 0.16f * blink
                for (dx in listOf(-0.15f, 0.15f)) {
                    val c = Offset(size.width * (0.5f + dx), size.height * 0.46f)
                    drawRoundRect(Ink, topLeft = Offset(c.x - w / 2, c.y - h / 2), size = Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w / 2))
                }
            },
    )
}

/** Hiện ra: trồi 12dp + rõ dần trong 500 ms, phần tử thứ [index] trễ 70 ms mỗi bậc. */
fun Modifier.rise(index: Int = 0): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(index * 70L); a.animateTo(1f, tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))) }
    graphicsLayer { alpha = a.value; translationY = (1 - a.value) * 12.dp.toPx() }
}

/** Icon một path cho icon mà material-icons-core không có; path lấy từ Material Icons (Apache 2.0). */
fun pathIcon(name: String, path: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(addPathNodes(path), fill = SolidColor(Color.Black)).build()
