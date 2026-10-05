package com.thanhnb.hocmoingay.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Cookie
import com.thanhnb.hocmoingay.core.ui.Critter
import com.thanhnb.hocmoingay.core.ui.Flower
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.TickUpNumber
import com.thanhnb.hocmoingay.core.ui.rise
import com.thanhnb.hocmoingay.feature.settings.AppSettings
import kotlinx.coroutines.flow.Flow
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

/** Khung tab Hôm nay. Plan e thay thẻ mục tiêu bằng hàng đợi bài + thẻ ôn thật, thêm chuỗi ngày. */
@Composable
fun TodayScreen(settings: Flow<AppSettings>, onOpenLearn: () -> Unit) {
    val s by settings.collectAsStateWithLifecycle(AppSettings())
    val (hello, date) = remember { greeting(LocalTime.now().hour) to vnDate(LocalDate.now()) }
    val type = MaterialTheme.typography

    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Row(Modifier.padding(top = 20.dp, bottom = 22.dp).rise(0), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(date, style = type.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(hello, style = type.displaySmall)
            }
            Critter(Modifier.size(68.dp))
        }
        GoalCard(s.dailyMinutes, onOpenLearn, Modifier.rise(1))
        Text("Hai mảng để học", style = type.headlineSmall, modifier = Modifier.padding(top = 32.dp, bottom = 14.dp).rise(2))
        TrackCard(
            Track.CODE, "Lập trình", "Đọc, viết và chạy code thật ngay trên máy", "print(\"xin chào\")",
            mono = true, badge = Cookie, glyph = "</>", onOpenLearn, Modifier.rise(3),
        )
        Spacer(Modifier.height(14.dp))
        TrackCard(
            Track.ENGLISH, "Tiếng Anh công việc", "Họp, email, phỏng vấn: nghe rồi nói to", "Quick sync at 3pm?",
            mono = false, badge = Flower, glyph = "Aa", onOpenLearn, Modifier.rise(4),
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun GoalCard(minutes: Int, onStart: () -> Unit, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Pushable(null, cs.primaryContainer, RoundedCornerShape(32.dp), modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Text("Mục tiêu mỗi ngày", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cs.onPrimaryContainer)
            Row(verticalAlignment = Alignment.Bottom) {
                TickUpNumber(minutes, MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.Bold), cs.onPrimaryContainer)
                Spacer(Modifier.width(8.dp))
                Text("phút", style = MaterialTheme.typography.headlineSmall, color = cs.onPrimaryContainer, modifier = Modifier.padding(bottom = 14.dp))
            }
            Text(
                "Mỗi sáng app xếp sẵn bài mới và thẻ cần ôn vừa đủ chừng này.",
                style = MaterialTheme.typography.bodyMedium, color = cs.onPrimaryContainer,
            )
            Spacer(Modifier.height(18.dp))
            PushButton(
                "Bắt đầu học", onStart, Modifier.fillMaxWidth(),
                color = cs.onPrimaryContainer, contentColor = cs.primaryContainer, icon = Icons.AutoMirrored.Filled.ArrowForward,
            )
        }
    }
}

@Composable
private fun TrackCard(
    track: Track, title: String, note: String, sample: String, mono: Boolean,
    badge: Shape, glyph: String, onClick: () -> Unit, modifier: Modifier,
) = ProvideTrack(track) {
    val t = LocalTrack.current
    Pushable(onClick, t.container, RoundedCornerShape(28.dp), modifier.fillMaxWidth(), edge = t.accent) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).background(t.accent, badge), contentAlignment = Alignment.Center) {
                Text(glyph, color = t.onAccent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = t.onContainer)
                Text(note, style = MaterialTheme.typography.bodyMedium, color = t.onContainer)
                Spacer(Modifier.height(10.dp))
                Text(
                    sample, color = t.onContainer, maxLines = 1,
                    style = MaterialTheme.typography.labelLarge, fontFamily = if (mono) JetBrainsMono else null,
                    modifier = Modifier.background(t.onContainer.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}
