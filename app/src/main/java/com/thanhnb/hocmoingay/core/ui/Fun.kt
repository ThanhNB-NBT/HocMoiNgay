package com.thanhnb.hocmoingay.core.ui

import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.DisposableEffect
import androidx.core.view.WindowCompat
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.fillMaxSize
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

// material-icons-core thiếu các icon này; path lấy từ Material Icons (Apache 2.0)
val BookIcon = pathIcon("Book", "M21,5c-1.11,-0.35 -2.33,-0.5 -3.5,-0.5c-1.95,0 -4.05,0.4 -5.5,1.5c-1.45,-1.1 -3.55,-1.5 -5.5,-1.5S2.45,4.9 1,6v14.65c0,0.25 0.25,0.5 0.5,0.5c0.1,0 0.15,-0.05 0.25,-0.05C3.1,20.45 5.05,20 6.5,20c1.95,0 4.05,0.4 5.5,1.5c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.65,0 3.35,0.3 4.75,1.05c0.1,0.05 0.15,0.05 0.25,0.05c0.25,0 0.5,-0.25 0.5,-0.5V6C22.4,5.55 21.75,5.25 21,5zM21,18.5c-1.1,-0.35 -2.3,-0.5 -3.5,-0.5c-1.7,0 -4.15,0.65 -5.5,1.5V8c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.2,0 2.4,0.15 3.5,0.5V18.5z")
val CardsIcon = pathIcon("Cards", "M2.53,19.65l1.34,0.56v-9.03l-2.43,5.86c-0.41,1.02 0.08,2.19 1.09,2.61zM22.03,15.95L17.07,3.98c-0.31,-0.75 -1.04,-1.21 -1.81,-1.23 -0.26,0 -0.53,0.04 -0.79,0.15L7.1,5.95c-0.75,0.31 -1.21,1.03 -1.23,1.8 -0.01,0.27 0.04,0.54 0.15,0.8l4.96,11.97c0.31,0.76 1.05,1.22 1.83,1.22 0.26,0 0.52,-0.05 0.77,-0.15l7.36,-3.05c1.02,-0.42 1.51,-1.59 1.09,-2.59zM7.88,8.75c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1 1,0.45 1,1 -0.45,1 -1,1zM5.88,19.75c0,1.1 0.9,2 2,2h1.45l-3.45,-8.34v6.34z")
val FlameIcon = pathIcon("Flame", "M19.48,12.35c-1.57,-4.08 -7.16,-4.3 -5.81,-10.23c0.1,-0.44 -0.37,-0.78 -0.75,-0.55C9.29,3.71 6.68,8 8.87,13.62c0.18,0.46 -0.36,0.89 -0.75,0.59c-1.81,-1.37 -2,-3.34 -1.84,-4.75c0.06,-0.52 -0.62,-0.77 -0.91,-0.34C4.69,10.16 4,11.84 4,14.37c0.38,5.6 5.11,7.32 6.81,7.54c2.43,0.31 5.06,-0.14 6.95,-1.87C19.84,18.11 20.6,15.03 19.48,12.35zM10.2,17.38c1.44,-0.35 2.18,-1.39 2.38,-2.31c0.33,-1.43 -0.96,-2.83 -0.09,-5.09c0.33,1.87 3.27,3.04 3.27,5.08C15.84,17.59 13.1,19.76 10.2,17.38z")
val BoltIcon = pathIcon("Bolt", "M11,21h-1l1,-7H7.5c-0.88,0 -0.33,-0.75 -0.31,-0.78C8.48,10.94 10.42,7.54 13.01,3h1l-1,7h3.51c0.4,0 0.62,0.19 0.4,0.66C12.97,17.55 11,21 11,21z")
val CalendarIcon = pathIcon("Calendar", "M19,4h-1V2h-2v2H8V2H6v2H5C3.89,4 3.01,4.9 3.01,6L3,20c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2V6C21,4.9 20.1,4 19,4zM19,20H5V10h14V20zM9,14H7v-2h2V14zM13,14h-2v-2h2V14zM17,14h-2v-2h2V14zM9,18H7v-2h2V18zM13,18h-2v-2h2V18zM17,18h-2v-2h2V18z")

/** Màu thương hiệu của chuỗi ngày (lửa) và XP (tia sét): giữ cố định ở mọi theme để người dùng nhận ra ngay. */
val FlameOrange = Color(0xFFF26B1D)
val BoltGold = Color(0xFFE7A10E)

/** Viên số liệu nhỏ ở đầu màn: icon màu + số đậm. [dim] = số 0, icon chuyển xám để không "báo động". */
@Composable
fun StatChip(icon: ImageVector, value: String, tint: Color, label: String, modifier: Modifier = Modifier, dim: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.background(cs.surfaceContainerLowest, CircleShape).border(1.dp, cs.outlineVariant, CircleShape)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (dim) cs.outline else tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.Bold, color = cs.onSurface)
    }
}

/** Vòng tiến độ: rãnh mờ + cung đầy dần theo [progress] (0..1), nội dung đặt giữa vòng. */
@Composable
fun ProgressRing(progress: Float, color: Color, track: Color, modifier: Modifier = Modifier, stroke: Float = 10f, content: @Composable BoxScope.() -> Unit = {}) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = Settle), label = "vòng")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = stroke.dp.toPx()
            val inset = w / 2
            val arc = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
            if (p > 0f) drawArc(color, -90f, 360f * p, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Thanh tiến độ mảnh bo tròn, dùng chung cho thẻ khoá và đề cương. */
@Composable
fun ThinProgress(progress: Float, color: Color, modifier: Modifier = Modifier, track: Color = MaterialTheme.colorScheme.surfaceContainerHigh) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700, easing = Settle), label = "thanh")
    Box(modifier.height(6.dp).clip(CircleShape).background(track)) {
        if (p > 0f) Box(Modifier.matchParentSize().graphicsLayer { scaleX = p; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }.background(color, CircleShape))
    }
}

/** Khung đang tải: [rows] khối xám nhạt nhịp sáng-tối, đúng hình bố cục sắp hiện thay vì màn trắng trơn. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, rows: Int = 4, header: Boolean = true) {
    val a by rememberInfiniteTransition(label = "tải").animateFloat(0.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "nhịp")
    val block = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier.fillMaxSize().padding(20.dp).alpha(a).semantics { contentDescription = "Đang tải" }) {
        if (header) {
            Box(Modifier.padding(top = 24.dp).size(160.dp, 36.dp).background(block, RoundedCornerShape(10.dp)))
            Spacer(Modifier.height(24.dp))
        }
        repeat(rows) {
            Box(Modifier.fillMaxWidth().height(76.dp).background(block, RoundedCornerShape(16.dp)))
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Bộ chọn dạng viên: viên đang chọn tô đặc màu chính, viên khác nền thẻ viền mảnh. Thay nút phân đoạn mặc định của M3. */
@Composable
fun PillTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, enabled: (Int) -> Boolean = { true }) {
    val cs = MaterialTheme.colorScheme
    // rãnh trắng nổi nhẹ chứa các viên; viên đang chọn tô màu chính
    Row(modifier.fillMaxWidth().lift(CircleShape).background(cs.surfaceContainerLowest, CircleShape).padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { i, l ->
            val sel = i == selected
            val on = enabled(i)
            val bg by animateColorAsState(if (sel) cs.primary else cs.primary.copy(alpha = 0f), tween(180), label = "viên")
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).alpha(if (on) 1f else 0.4f).clip(CircleShape).background(bg)
                    .selectable(sel, enabled = on, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    l, Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                    color = if (sel) cs.onPrimary else cs.onSurfaceVariant, maxLines = 1,
                )
            }
        }
    }
}

/** Phần đáy màn bị thanh tab kính nổi che (chưa tính thanh điều hướng hệ thống); 0 ở màn không có thanh tab. */
val LocalBarInset = compositionLocalOf { 0.dp }

/** Đặt cuối nội dung màn tab để mục cuối cùng cuộn lên được khỏi thanh tab kính. */
@Composable
fun BarSpace() = Spacer(Modifier.navigationBarsPadding().height(LocalBarInset.current))

/** Inset cho màn tab: chừa thanh trạng thái và hai mép; đáy để trống cho nội dung chạy xuống dưới kính. */
@Composable
fun Modifier.tabInsets(): Modifier = windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))

/** Bóng nổi mềm cho MỘT khối cần nhô lên trên nền giấy (thẻ việc đầu, khối gom nhóm); khối thường thì phẳng. */
@Composable
fun Modifier.lift(shape: Shape): Modifier {
    val cs = MaterialTheme.colorScheme
    return shadow(14.dp, shape, clip = false, ambientColor = cs.onSurface.copy(alpha = 0.10f), spotColor = cs.primary.copy(alpha = 0.30f))
}

/** Đặt màu icon thanh trạng thái khi màn này hiện (true = icon sáng, cho màn có dải màu đậm tràn lên trên); rời màn thì trả lại. */
@Composable
fun StatusIcons(light: Boolean) {
    val view = LocalView.current
    val window = LocalActivity.current?.window ?: return
    DisposableEffect(light) {
        val ctl = WindowCompat.getInsetsController(window, view)
        val old = ctl.isAppearanceLightStatusBars
        ctl.isAppearanceLightStatusBars = !light
        onDispose { ctl.isAppearanceLightStatusBars = old }
    }
}

/** Đáy cho bố cục cố định (không cuộn) trong màn tab: chừa thanh điều hướng + thanh tab kính. */
@Composable
fun Modifier.barPadding(): Modifier = navigationBarsPadding().padding(bottom = LocalBarInset.current)
