package com.thanhnb.hocmoingay.feature.today

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.thanhnb.hocmoingay.core.ui.BarSpace
import com.thanhnb.hocmoingay.core.ui.tabInsets
import com.thanhnb.hocmoingay.core.ui.lift
import com.thanhnb.hocmoingay.core.ui.StatusIcons
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.thanhnb.hocmoingay.core.ui.BoltGold
import com.thanhnb.hocmoingay.core.ui.BoltIcon
import com.thanhnb.hocmoingay.core.ui.CardsIcon
import com.thanhnb.hocmoingay.core.ui.FlameIcon
import com.thanhnb.hocmoingay.core.ui.FlameOrange
import com.thanhnb.hocmoingay.core.ui.ProgressRing
import com.thanhnb.hocmoingay.core.ui.Skeleton
import com.thanhnb.hocmoingay.core.ui.StatChip
import com.thanhnb.hocmoingay.core.ui.Tile
import com.thanhnb.hocmoingay.feature.learn.GlyphTile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Sunrise
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.TickUpNumber
import com.thanhnb.hocmoingay.core.ui.rise
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

fun greeting(hour: Int) = when (hour) {
    in 5..10 -> "Chào buổi sáng"
    in 11..12 -> "Chào buổi trưa"
    in 13..17 -> "Chào buổi chiều"
    in 18..22 -> "Chào buổi tối"
    else -> "Khuya rồi đó"
}

fun vnDate(d: LocalDate): String {
    val thu = if (d.dayOfWeek == DayOfWeek.SUNDAY) "Chủ Nhật"
    else "Thứ " + listOf("Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy")[d.dayOfWeek.value - 1]
    return "$thu, ${d.dayOfMonth} tháng ${d.monthValue}"
}

/**
 * Tab Hôm nay (spec §7.4): dải màu chính tràn lên thanh trạng thái (ngày, chip chuỗi/XP, lời chào, vòng mục tiêu, MỘT nút),
 * rồi "Việc hôm nay" là lộ trình nối nét đứt — chỉ việc đầu nổi lên thành thẻ.
 */
@Composable
fun TodayScreen(
    vm: TodayViewModel,
    onOpenLearn: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenLesson: (String) -> Unit,
    onResolve: (lessonId: String, cardKey: String) -> Unit,
    onPlacement: () -> Unit,
    notify: @Composable () -> Unit = {},
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val (hello, date) = remember { greeting(LocalTime.now().hour) to vnDate(LocalDate.now()) }
    val type = MaterialTheme.typography
    val cs = MaterialTheme.colorScheme
    fun open(item: TodayItem) = when (item) {
        is ReviewBlock -> onOpenReview()
        is Pick -> when (item.kind) {
            PickKind.PLACEMENT -> onPlacement()
            PickKind.RESOLVE -> onResolve(item.lessonId, item.cardKey.orEmpty())
            else -> onOpenLesson(item.lessonId)
        }
    }
    if (ui.loading) return Skeleton(Modifier.tabInsets())

    val scroll = rememberScrollState()
    var bandPx by remember { mutableIntStateOf(0) }
    val statusPx = WindowInsets.statusBars.getTop(LocalDensity.current)
    // còn thấy dải màu dưới thanh trạng thái → icon trạng thái màu sáng; cuộn qua rồi thì phủ nền giấy + icon tối
    val onBand by remember { derivedStateOf { scroll.value < bandPx - statusPx } }
    StatusIcons(light = onBand && cs.onPrimary.luminance() > 0.5f)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            Band(ui, date, hello, ui.plan.firstOrNull()?.item, Modifier.onSizeChanged { bandPx = it.height }, ::open)
            Box(Modifier.padding(horizontal = 20.dp)) { notify() }
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp).rise(2), verticalAlignment = Alignment.Bottom) {
                Text("Việc hôm nay", Modifier.weight(1f), style = type.titleLarge, fontWeight = FontWeight.Bold)
                if (ui.plan.isNotEmpty()) Text(
                    "${ui.plan.size} việc · ${ui.plan.sumOf { it.item.minutes }} phút",
                    style = type.labelLarge, color = cs.onSurfaceVariant,
                )
            }
            if (ui.plan.isEmpty()) AllDone(onOpenLearn, Modifier.padding(horizontal = 20.dp).rise(3))
            else Route(ui.plan, ::open)
            BarSpace()
        }
        if (!onBand) Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(cs.background))
    }
}

fun streakText(n: Int) = if (n == 0) "Chưa có chuỗi ngày" else "Chuỗi $n ngày"

/** Nhãn nút chính của khối mục tiêu: ngắn để luôn vừa một dòng. */
fun startLabel(item: TodayItem) = when (item) {
    is ReviewBlock -> "Ôn ${item.cards.size} thẻ"
    is Pick -> when (item.kind) {
        PickKind.PLACEMENT -> "Làm bài xếp lớp"
        PickKind.RESOLVE -> "Giải lại bài"
        PickKind.CHECKPOINT -> "Làm bài kiểm tra"
        else -> "Học tiếp"
    }
}

/** Dải màu chính đầu màn: khoảnh khắc duy nhất tô đặc của tab, có vòng tròn trang trí mờ ở góc phải trên. */
@Composable
private fun Band(ui: TodayViewModel.Ui, date: String, hello: String, next: TodayItem?, modifier: Modifier, onStart: (TodayItem) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val ink = cs.onPrimary
    val soft = ink.copy(alpha = 0.82f)
    val lightInk = ink.luminance() > 0.5f
    val left = (ui.dailyMinutes - ui.doneMinutes).coerceAtLeast(0)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(cs.primary)
            .drawBehind { drawCircle(ink.copy(alpha = 0.07f), radius = 110.dp.toPx(), center = Offset(size.width - 50.dp.toPx(), 70.dp.toPx())) }
            .statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 22.dp),
    ) {
        Row(Modifier.rise(0), verticalAlignment = Alignment.CenterVertically) {
            Text(date, Modifier.weight(1f), style = type.labelLarge, color = soft)
            BandChip(FlameIcon, ui.streak, if (lightInk) FlameOnBand else FlameOrange, "Chuỗi ngày")
            Spacer(Modifier.width(6.dp))
            BandChip(BoltIcon, ui.xpToday, if (lightInk) BoltOnBand else BoltGold, "XP hôm nay")
        }
        Text(hello, Modifier.padding(top = 6.dp, bottom = 18.dp).rise(0), style = type.headlineLarge, fontWeight = FontWeight.Bold, color = ink)
        Row(Modifier.rise(1), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                ui.doneMinutes.toFloat() / ui.dailyMinutes.coerceAtLeast(1), ink, ink.copy(alpha = 0.22f),
                Modifier.size(84.dp).semantics(mergeDescendants = true) { contentDescription = "Đã học ${ui.doneMinutes} trên ${ui.dailyMinutes} phút" },
                stroke = 8f,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TickUpNumber(ui.doneMinutes, type.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), ink)
                    Text("/${ui.dailyMinutes} phút", style = type.labelSmall, color = soft)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Mục tiêu hôm nay", style = type.labelMedium, color = soft)
                Text(
                    when {
                        left == 0 -> "Đạt mục tiêu rồi!"
                        ui.doneMinutes == 0 -> "Học ${ui.dailyMinutes} phút nhé"
                        else -> "Còn $left phút nữa"
                    },
                    Modifier.padding(vertical = 2.dp), style = type.titleLarge, fontWeight = FontWeight.Bold, color = ink,
                )
                Text(if (ui.streak > 0) "Giữ ${streakText(ui.streak).lowercase()} của bạn" else "Học đều mỗi ngày để giữ chuỗi", style = type.bodySmall, color = soft)
            }
        }
        if (next != null) {
            Spacer(Modifier.height(18.dp))
            PushButton(
                startLabel(next), { onStart(next) }, Modifier.fillMaxWidth().rise(1),
                color = ink, contentColor = cs.primary, icon = Icons.AutoMirrored.Filled.ArrowForward,
            )
        }
    }
}

// màu icon chip trên dải màu chính: sáng hơn để nổi trên nền xanh đậm
private val FlameOnBand = Color(0xFFFFB27A)
private val BoltOnBand = Color(0xFFFFD35C)

@Composable
private fun BandChip(icon: ImageVector, value: Int, tint: Color, label: String) {
    val ink = MaterialTheme.colorScheme.onPrimary
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$label: $value" }
            .background(ink.copy(alpha = 0.16f), CircleShape).padding(start = 7.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (value == 0) ink.copy(alpha = 0.55f) else tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("$value", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = ink)
    }
}

/** (nhãn, tiêu đề, ghi chú) của một mục. */
fun describe(item: TodayItem): Triple<String, String, String> = when (item) {
    is ReviewBlock -> Triple("Ôn tập", "Ôn ${item.cards.size} thẻ đến hạn", "Xen kẽ các khoá, thẻ quá hạn lâu nhất trước")
    is Pick -> when (item.kind) {
        PickKind.RESOLVE -> Triple("Giải lại", item.title, "Bài luyện code đến hạn ôn, làm bằng ngôn ngữ ưa thích")
        PickKind.CHECKPOINT -> Triple("Kiểm tra cuối chương", item.title, "Đạt từ 80% là chương thành thạo")
        PickKind.NEXT -> Triple(if (item.track == "english") "Tiếng Anh" else "Bài tiếp theo", item.title, "Học tiếp từ chỗ đang dừng")
        PickKind.PRACTICE -> Triple("Luyện code", item.title, "Bài luyện theo mẫu giải đã gặp")
        PickKind.PLACEMENT -> Triple("Tiếng Anh", item.title, "Khoảng 30 câu để biết nên bắt đầu từ cấp nào")
    }
}

/** Lộ trình việc: một nét đứt dọc nối các ô; việc đầu là thẻ nổi, các việc sau là dòng trơn. */
@Composable
private fun Route(plan: List<Planned>, onOpen: (TodayItem) -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp).drawBehind {
            val x = 30.dp.toPx() // giữa ô 40dp, lề 10 + 10
            drawLine(
                line, Offset(x, 30.dp.toPx()), Offset(x, size.height - 30.dp.toPx()), strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
            )
        },
    ) {
        plan.forEachIndexed { i, p -> PlanRow(p, first = i == 0, Modifier.rise(3 + i)) { onOpen(p.item) } }
    }
}

@Composable
private fun PlanRow(p: Planned, first: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val track = when ((p.item as? Pick)?.track) { "english" -> Track.ENGLISH; "code" -> Track.CODE; else -> null }
    val content = @Composable {
        val (label, title, _) = describe(p.item)
        val cs = MaterialTheme.colorScheme
        val shape = RoundedCornerShape(18.dp)
        Pushable(
            onClick, if (first) cs.surfaceContainerLowest else Color.Transparent, shape,
            modifier.fillMaxWidth().padding(vertical = if (first) 4.dp else 0.dp).then(if (first) Modifier.lift(shape) else Modifier),
            edge = Color.Transparent,
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = if (first) 12.dp else 9.dp), verticalAlignment = Alignment.CenterVertically) {
                when (val item = p.item) {
                    is ReviewBlock -> Box(Modifier.size(40.dp).background(cs.secondaryContainer, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(CardsIcon, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
                    }
                    is Pick -> GlyphTile(if (item.kind == PickKind.PLACEMENT) "english-work" else item.lessonId.substringBefore('/'), item.title, 40.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = if (first) FontWeight.Bold else FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (p.extra) "$label · học thêm" else label,
                        style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("${p.item.minutes} phút", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
            }
        }
    }
    if (track != null) ProvideTrack(track) { content() } else content()
}

@Composable
private fun AllDone(onOpenLearn: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Pushable(null, cs.surfaceContainerLowest, shape, modifier.fillMaxWidth().lift(shape), edge = Color.Transparent) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Sunrise(Modifier.size(72.dp))
            Spacer(Modifier.height(8.dp))
            Text("Xong việc hôm nay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Không còn thẻ đến hạn hay bài đang chờ. Muốn học thêm thì chọn một khoá.",
                style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            PushButton("Chọn khoá học", onOpenLearn, Modifier.fillMaxWidth())
        }
    }
}
