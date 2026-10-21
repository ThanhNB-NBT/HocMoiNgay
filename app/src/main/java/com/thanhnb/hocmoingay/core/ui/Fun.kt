package com.thanhnb.hocmoingay.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Ô vuông bo góc vừa phải: nền cho icon của khoá, ảnh đại diện, ô số liệu. */
val Tile: Shape = RoundedCornerShape(percent = 26)

/** Đường cong nhanh rồi êm: cho mọi chuyển động ngắn trong app. */
val Settle = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Viền mảnh mặc định của khối: tối hơn mặt [face] một chút, đủ thấy trên nền giấy. */
fun edgeOf(face: Color) = lerp(face, Color.Black, 0.12f)

/**
 * Khối bấm được, mặt phẳng: không gờ, không nảy. Bấm thì mặt sẫm đi một nấc và co 1%.
 * [edge] là màu viền mảnh 1dp, mặc định outlineVariant (Transparent = không viền); [border] cho viền tự chọn.
 * [onClick] null = khối tĩnh.
 */
@Composable
fun Pushable(
    onClick: (() -> Unit)?,
    face: Color,
    shape: Shape,
    modifier: Modifier = Modifier,
    edge: Color = MaterialTheme.colorScheme.outlineVariant,
    border: BorderStroke? = null,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val focused by src.collectIsFocusedAsState()
    val down = pressed && enabled && onClick != null
    val scale by animateFloatAsState(if (down) 0.99f else 1f, tween(if (down) 80 else 160, easing = Settle), label = "bấm")
    val bg by animateColorAsState(if (down) lerp(face, Color.Black, 0.06f) else face, tween(120), label = "nền bấm")
    val haptic = LocalHapticFeedback.current
    val click = if (onClick == null) Modifier else Modifier.clickable(src, indication = null, enabled = enabled, role = Role.Button) {
        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        onClick()
    }
    val line = border ?: if (edge != Color.Transparent) BorderStroke(1.dp, edge) else null
    Box(
        modifier.alpha(if (enabled) 1f else 0.45f).graphicsLayer { scaleX = scale; scaleY = scale }.then(click)
            .clip(shape).background(bg)
            .then(if (line != null) Modifier.border(line, shape) else Modifier)
            .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
        propagateMinConstraints = true,
        content = content,
    )
}

/** Nút hành động chính: khối đặc màu mực (sáng) / giấy (tối), mỗi màn chỉ một nút kiểu này. */
@Composable
fun PushButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) = Pushable(onClick, color, RoundedCornerShape(14.dp), modifier, edge = Color.Transparent, enabled = enabled) {
    Row(
        Modifier.align(Alignment.Center).heightIn(min = 52.dp).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        Text(text, color = contentColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (icon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(18.dp))
        }
    }
}

/** Số đếm từ 0 lên [target] khi hiện ra (0,9 s, tới nhanh rồi chậm dần). */
@Composable
fun TickUpNumber(target: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val v = remember { Animatable(0f) }
    LaunchedEffect(target) { v.animateTo(target.toFloat(), tween(900, easing = Settle)) }
    Text(v.value.roundToInt().toString(), modifier, color = color, style = style.copy(fontFeatureSettings = "tnum"))
}

/**
 * Dấu hiệu của app, lấy từ icon "Bình minh": mặt trời nửa vòng nhô lên khỏi đường chân trời.
 * Hiện ra thì mặt trời nhô lên một lần rồi đứng yên.
 */
@Composable
fun Sunrise(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    val up = remember { Animatable(0f) }
    LaunchedEffect(Unit) { up.animateTo(1f, tween(700, easing = Settle)) }
    Canvas(modifier) {
        val w = size.width
        val stroke = w * 0.06f
        val horizon = size.height * 0.68f
        val r = w * 0.26f
        // mặt trời vàng như icon app, nhô quá nửa khỏi chân trời
        val cy = horizon - r * 0.35f * up.value + r * (1 - up.value) * 0.6f
        clipRect(bottom = horizon) {
            drawCircle(SunGold, r, Offset(w / 2, cy))
        }
        drawLine(color, Offset(w * 0.12f, horizon + stroke), Offset(w * 0.88f, horizon + stroke), stroke, StrokeCap.Round)
        drawLine(color.copy(alpha = 0.45f), Offset(w * 0.28f, horizon + stroke * 3), Offset(w * 0.72f, horizon + stroke * 3), stroke, StrokeCap.Round)
    }
}

private val SunGold = Color(0xFFF2B33D)

/** Hiện ra: trồi 6dp + rõ dần trong 280 ms, phần tử thứ [index] trễ 35 ms mỗi bậc. */
fun Modifier.rise(index: Int = 0): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(index * 35L); a.animateTo(1f, tween(280, easing = Settle)) }
    graphicsLayer { alpha = a.value; translationY = (1 - a.value) * 6.dp.toPx() }
}

/** Icon một path cho icon mà material-icons-core không có; path lấy từ Material Icons (Apache 2.0). */
fun pathIcon(name: String, path: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(addPathNodes(path), fill = SolidColor(Color.Black)).build()
