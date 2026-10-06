package com.thanhnb.hocmoingay.feature.today

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Critter
import com.thanhnb.hocmoingay.core.theme.LocalFun
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

/** Tab Hôm nay (spec §7.4): thẻ ngày (phút/XP/chuỗi) + hàng đợi ôn thẻ → mục code → bài tiếng Anh. */
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
    fun open(item: TodayItem) = when (item) {
        is ReviewBlock -> onOpenReview()
        is Pick -> when (item.kind) {
            PickKind.PLACEMENT -> onPlacement()
            PickKind.RESOLVE -> onResolve(item.lessonId, item.cardKey.orEmpty())
            else -> onOpenLesson(item.lessonId)
        }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 20.dp, bottom = 22.dp).rise(0), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(date, style = type.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(hello, style = type.displaySmall)
            }
            Critter(Modifier.size(68.dp))
        }
        DayCard(ui, Modifier.rise(1))
        notify()
        Text("Việc hôm nay", style = type.headlineSmall, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp).rise(2))
        when {
            ui.loading -> Unit
            ui.plan.isEmpty() -> AllDone(onOpenLearn, Modifier.rise(3))
            else -> ui.plan.forEachIndexed { i, p ->
                PlanRow(p, Modifier.rise(3 + i)) { open(p.item) }
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

fun streakText(n: Int) = if (n == 0) "Chưa có chuỗi ngày" else "Chuỗi $n ngày"

@Composable
private fun DayCard(ui: TodayViewModel.Ui, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Pushable(null, cs.primaryContainer, RoundedCornerShape(32.dp), modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("Hôm nay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cs.onPrimaryContainer)
            Row(verticalAlignment = Alignment.Bottom) {
                TickUpNumber(ui.doneMinutes, MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.Bold), cs.onPrimaryContainer)
                Text(
                    "/${ui.dailyMinutes} phút", style = MaterialTheme.typography.headlineSmall, color = cs.onPrimaryContainer,
                    modifier = Modifier.padding(start = 6.dp, bottom = 14.dp),
                )
            }
            LinearProgressIndicator(
                progress = { (ui.doneMinutes.toFloat() / ui.dailyMinutes).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = cs.onPrimaryContainer, trackColor = cs.onPrimaryContainer.copy(alpha = 0.15f), strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(14.dp))
            Text("${streakText(ui.streak)} · +${ui.xpToday} XP hôm nay", style = MaterialTheme.typography.bodyLarge, color = cs.onPrimaryContainer)
        }
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

@Composable
private fun PlanRow(p: Planned, modifier: Modifier, onClick: () -> Unit) {
    val track = when ((p.item as? Pick)?.track) { "english" -> Track.ENGLISH; "code" -> Track.CODE; else -> null }
    val content = @Composable {
        val (label, title, note) = describe(p.item)
        val cs = MaterialTheme.colorScheme
        val face = if (track == null) cs.secondaryContainer else LocalTrack.current.container
        val ink = if (track == null) cs.onSecondaryContainer else LocalTrack.current.onContainer
        Pushable(onClick, face, RoundedCornerShape(24.dp), modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = ink.copy(alpha = 0.8f))
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(note, style = MaterialTheme.typography.bodyMedium, color = ink)
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${p.item.minutes} phút", style = MaterialTheme.typography.labelLarge, color = ink)
                    if (p.extra) Text(
                        "thêm", style = MaterialTheme.typography.labelMedium, color = ink,
                        modifier = Modifier.padding(top = 4.dp).background(ink.copy(alpha = 0.12f), CircleShape).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
    if (track != null) ProvideTrack(track) { content() } else content()
}

@Composable
private fun AllDone(onOpenLearn: () -> Unit, modifier: Modifier) {
    val f = LocalFun.current
    val ink = MaterialTheme.colorScheme.onSurface
    Pushable(null, f.mintContainer, RoundedCornerShape(28.dp), modifier.fillMaxWidth(), edge = f.mint) {
        Column(Modifier.padding(20.dp)) {
            Text("Xong việc hôm nay", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink)
            Text("Không còn thẻ đến hạn hay bài đang chờ. Muốn học thêm thì chọn khoá ở tab Học.", style = MaterialTheme.typography.bodyMedium, color = ink)
            Spacer(Modifier.height(14.dp))
            PushButton("Mở tab Học", onOpenLearn, Modifier.fillMaxWidth())
        }
    }
}
