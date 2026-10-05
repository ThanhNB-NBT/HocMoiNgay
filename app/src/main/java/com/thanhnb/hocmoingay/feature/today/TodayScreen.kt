package com.thanhnb.hocmoingay.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.feature.ScreenHeader
import com.thanhnb.hocmoingay.feature.TrackLabel
import com.thanhnb.hocmoingay.feature.settings.AppSettings
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow

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

/** Khung tab Hôm nay. Plan e thay thẻ mục tiêu bằng hàng đợi bài + thẻ ôn thật. */
@Composable
fun TodayScreen(settings: Flow<AppSettings>, onOpenLearn: () -> Unit) {
    val s by settings.collectAsStateWithLifecycle(AppSettings())
    val (hello, date) = remember { greeting(LocalTime.now().hour) to vnDate(LocalDate.now()) }

    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        ScreenHeader("Hôm nay", subtitle = date, eyebrow = hello)
        GoalCard(s.dailyMinutes, Modifier.padding(horizontal = 20.dp))
        Text(
            "Học gì bây giờ?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 28.dp, bottom = 12.dp),
        )
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrackCard(Track.CODE, "Lập trình", "print(\"xin chào\")", mono = true, onOpenLearn, Modifier.weight(1f))
            TrackCard(Track.ENGLISH, "Tiếng Anh", "Quick sync\nat 3pm?", mono = false, onOpenLearn, Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun GoalCard(minutes: Int, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(color = cs.primary, contentColor = cs.onPrimary, shape = RoundedCornerShape(28.dp), modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp)) {
            Text("Mục tiêu mỗi ngày", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$minutes", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("phút", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 10.dp))
            }
            Text(
                "Mỗi sáng app sẽ xếp sẵn bài mới và thẻ cần ôn vừa đủ thời gian này.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TrackCard(track: Track, label: String, sample: String, mono: Boolean, onClick: () -> Unit, modifier: Modifier) =
    ProvideTrack(track) {
        val t = LocalTrack.current
        Surface(onClick = onClick, color = t.container, contentColor = t.onContainer, shape = RoundedCornerShape(24.dp), modifier = modifier) {
            Column(Modifier.padding(16.dp).height(132.dp), verticalArrangement = Arrangement.SpaceBetween) {
                TrackLabel(label, t)
                Text(
                    sample, style = if (mono) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    fontFamily = if (mono) JetBrainsMono else null,
                )
                Text("Xem khoá học →", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
