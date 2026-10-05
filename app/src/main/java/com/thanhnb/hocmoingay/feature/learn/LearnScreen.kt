package com.thanhnb.hocmoingay.feature.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.BuildConfig
import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.CurriculumDao
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.ScreenHeader
import com.thanhnb.hocmoingay.feature.TrackLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

fun groupCourses(list: List<CourseEntity>) =
    listOf("code" to "Lập trình", "english" to "Tiếng Anh")
        .map { (track, label) -> label to list.filter { it.track == track } }
        .filter { it.second.isNotEmpty() }

class LearnViewModel(dao: CurriculumDao) : ViewModel() {
    val courses = dao.observeCourses(showSamples = BuildConfig.DEBUG)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Ký hiệu ngắn trên ô màu của thẻ khoá; khoá lạ thì lấy 2 chữ đầu tên. */
fun courseGlyph(id: String, title: String) = when (id) {
    "python" -> "py"
    "javascript" -> "js"
    "kotlin" -> "kt"
    "java" -> "jv"
    "sql" -> "sql"
    "dsa" -> "O(n)"
    "luyen-code" -> "</>"
    "git" -> "git"
    "mang-may-tinh" -> "tcp"
    "he-dieu-hanh" -> "os"
    "linux-docker" -> "\$_"
    "thiet-ke-he-thong" -> "sys"
    "bao-mat" -> "#!"
    "english-work" -> "Aa"
    else -> title.filter { it.isLetterOrDigit() }.take(2).lowercase()
}

/** Tab Học: danh sách khoá theo mảng. Plan c thêm % chương thành thạo và shared-element. */
@Composable
fun LearnScreen(vm: LearnViewModel, onOpenCourse: (String) -> Unit) {
    val list = vm.courses.collectAsStateWithLifecycle().value
    when {
        list == null -> Box(Modifier.fillMaxSize())
        list.isEmpty() -> Placeholder("Chưa có khoá học", "Giáo trình được tải về khi có mạng. Mở lại app sau khi kết nối.")
        else -> LazyColumn(
            Modifier.fillMaxSize().safeDrawingPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") { ScreenHeader("Học", subtitle = "${list.size} khoá · chọn một khoá để xem lộ trình") }
            groupCourses(list).forEach { (label, items) ->
                val track = if (items.first().track == "code") Track.CODE else Track.ENGLISH
                item(key = "h-$label") {
                    ProvideTrack(track) {
                        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            TrackLabel(label, LocalTrack.current)
                            Spacer(Modifier.width(8.dp))
                            Text("${items.size} khoá", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(items, key = { it.id }) { c ->
                    ProvideTrack(track) { CourseCard(c) { onOpenCourse(c.id) } }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(c: CourseEntity, onClick: () -> Unit) {
    val t = LocalTrack.current
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick, color = cs.surfaceContainerLow, shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = t.container, contentColor = t.onContainer, shape = RoundedCornerShape(16.dp)) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    Text(courseGlyph(c.id, c.title), fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(c.description, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = cs.onSurfaceVariant)
        }
    }
}
