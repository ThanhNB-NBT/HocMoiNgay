package com.thanhnb.hocmoingay.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Clover
import com.thanhnb.hocmoingay.core.ui.Cookie
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.TickUpNumber
import com.thanhnb.hocmoingay.core.ui.rise
import com.thanhnb.hocmoingay.feature.ScreenHeader
import kotlin.math.roundToInt

/** Tab Hồ sơ (spec §7.2): chuỗi ngày, XP, lịch học 12 tuần, 4 mạch tiếng Anh, chương thành thạo, Cài đặt. */
@Composable
fun ProfileScreen(vm: ProfileViewModel, email: String?, onOpenSettings: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val fun_ = LocalFun.current
    val card = BorderStroke(2.dp, cs.outlineVariant)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenHeader("Hồ sơ", modifier = Modifier.rise(0), inset = 0.dp)
        Row(Modifier.rise(1), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp).background(cs.primaryContainer, Cookie), contentAlignment = Alignment.Center) {
                Text(email?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.headlineMedium, color = cs.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(email ?: "Chưa đăng nhập", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tiến độ đồng bộ trên mọi thiết bị", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        // coral: khoảnh khắc mạnh duy nhất của màn — chuỗi ngày
        Pushable(null, fun_.coralContainer, RoundedCornerShape(28.dp), Modifier.fillMaxWidth().rise(2), edge = fun_.coral) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Chuỗi ngày", style = MaterialTheme.typography.titleMedium, color = fun_.onCoralContainer)
                    Row(verticalAlignment = Alignment.Bottom) {
                        TickUpNumber(ui.streak, MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold), fun_.onCoralContainer)
                        Text(" ngày", style = MaterialTheme.typography.titleLarge, color = fun_.onCoralContainer, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${ui.totalXp} XP", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = fun_.onCoralContainer)
                    Text("${ui.activeDays} ngày có học", style = MaterialTheme.typography.bodyMedium, color = fun_.onCoralContainer)
                }
            }
        }
        Section("Lịch học", Modifier.rise(3)) {
            Heatmap(ui.heat, ui.months, cs.primary, cs.surfaceContainerHighest)
        }
        ProvideTrack(Track.ENGLISH) { Section("Tiếng Anh 7 ngày qua", Modifier.rise(4)) { StrandBars(ui.strands, ui.note) } }
        if (ui.courses.isNotEmpty()) Section("Chương đã thành thạo", Modifier.rise(5)) { ui.courses.forEach { CourseRow(it) } }
        Spacer(Modifier.height(14.dp))
        Pushable(
            onOpenSettings, cs.surfaceContainerLowest, RoundedCornerShape(24.dp), Modifier.fillMaxWidth().rise(6),
            edge = cs.outlineVariant, border = card,
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(cs.secondaryContainer, Clover), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, null, tint = cs.onSecondaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cài đặt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Giao diện, giờ nhắc, ngôn ngữ, mật khẩu", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Section(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Spacer(Modifier.height(14.dp))
    Pushable(null, cs.surfaceContainerLowest, RoundedCornerShape(24.dp), modifier.fillMaxWidth(), edge = cs.outlineVariant, border = BorderStroke(2.dp, cs.outlineVariant)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

/** Lịch học kiểu GitHub: nhãn tháng trên đầu, T2/T4/T6 bên trái, chú thích Ít → Nhiều; ô co theo bề ngang màn. */
@Composable
private fun Heatmap(cells: List<List<Int?>>, months: List<String?>, color: Color, empty: Color) {
    val days = cells.sumOf { c -> c.count { (it ?: 0) > 0 } }
    val cs = MaterialTheme.colorScheme
    val small = MaterialTheme.typography.labelSmall
    fun tone(lv: Int) = if (lv == 0) empty else color.copy(alpha = 0.25f + 0.1875f * lv) // mức 4 → đậm hẳn
    Text("$days ngày có học trong $HEAT_WEEKS tuần qua", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
    BoxWithConstraints(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "Lịch học $HEAT_WEEKS tuần, $days ngày có học" }) {
        val gap = 4.dp
        val labelW = 26.dp
        val n = cells.size.coerceAtLeast(1)
        val side = minOf((maxWidth - labelW - gap * (n - 1)) / n, 22.dp)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Spacer(Modifier.width(labelW - gap))
                months.forEach { m ->
                    Box(Modifier.width(side)) {
                        if (m != null) Text(m, style = small, color = cs.onSurfaceVariant, softWrap = false, modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                Column(Modifier.width(labelW - gap), verticalArrangement = Arrangement.spacedBy(gap)) {
                    listOf("T2", "", "T4", "", "T6", "", "").forEach {
                        Box(Modifier.height(side), contentAlignment = Alignment.CenterStart) { Text(it, style = small, color = cs.onSurfaceVariant) }
                    }
                }
                cells.forEach { col ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        col.forEach { lv ->
                            Box(Modifier.size(side).then(if (lv != null) Modifier.background(tone(lv), RoundedCornerShape(4.dp)) else Modifier))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text("Ít", style = small, color = cs.onSurfaceVariant)
                (0..4).forEach { Box(Modifier.size(12.dp).background(tone(it), RoundedCornerShape(3.dp))) }
                Text("Nhiều", style = small, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StrandBars(m: Map<String, Double>, note: String?) {
    val total = m.values.sum()
    val t = LocalTrack.current
    if (total <= 0) {
        Text("Tuần này chưa có phút học tiếng Anh.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    STRANDS.forEach { k ->
        val v = m[k] ?: 0.0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(STRAND_NAMES.getValue(k), Modifier.width(150.dp), style = MaterialTheme.typography.bodyMedium)
            LinearProgressIndicator(
                progress = { (v / total).toFloat() }, modifier = Modifier.weight(1f).height(10.dp),
                color = t.accent, trackColor = t.container, strokeCap = StrokeCap.Round,
            )
            Text(if (v < 10) "${(v * 10).roundToInt() / 10.0} ph".replace('.', ',') else "${v.roundToInt()} ph", Modifier.width(60.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelLarge)
        }
    }
    note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun CourseRow(c: ProfileViewModel.CourseLine) = ProvideTrack(if (c.track == "english") Track.ENGLISH else Track.CODE) {
    val t = LocalTrack.current
    Column {
        Row {
            Text(c.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("${c.stats.mastered}/${c.stats.chapters} chương", style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { c.stats.masteredPct / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp),
            color = t.accent, trackColor = t.container, strokeCap = StrokeCap.Round,
        )
    }
}
