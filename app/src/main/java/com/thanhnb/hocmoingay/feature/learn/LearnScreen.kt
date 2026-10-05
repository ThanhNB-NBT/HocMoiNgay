package com.thanhnb.hocmoingay.feature.learn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.CourseStats
import com.thanhnb.hocmoingay.core.lesson.courseStats
import com.thanhnb.hocmoingay.core.lesson.parseOutline
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Cookie
import com.thanhnb.hocmoingay.core.ui.Flower
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.rise
import com.thanhnb.hocmoingay.core.ui.shapeFor
import com.thanhnb.hocmoingay.core.ui.shared
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.ScreenHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

fun <T> groupCourses(list: List<T>, track: (T) -> String) =
    listOf("code" to "Lập trình", "english" to "Tiếng Anh")
        .map { (t, label) -> label to list.filter { track(it) == t } }
        .filter { it.second.isNotEmpty() }

data class CourseItem(val course: CourseEntity, val stats: CourseStats)

class LearnViewModel(dao: CurriculumDao, progress: Flow<List<ProgressEntity>>) : ViewModel() {
    val courses = combine(dao.observeCourses(showSamples = BuildConfig.SHOW_SAMPLES), progress) { cs, ps ->
        val byId = ps.associateBy { it.lessonId }
        cs.map { CourseItem(it, courseStats(it.id, parseOutline(it.outline), byId)) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
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

/** Tab Học: danh sách khoá theo mảng, kèm tiến độ; ô ký hiệu + tiêu đề bay sang màn đề cương. */
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
            groupCourses(list) { it.course.track }.forEach { (label, items) ->
                val track = if (items.first().course.track == "code") Track.CODE else Track.ENGLISH
                item(key = "h-$label") {
                    ProvideTrack(track) {
                        val t = LocalTrack.current
                        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(26.dp).background(t.accent, if (track == Track.CODE) Cookie else Flower))
                            Spacer(Modifier.width(10.dp))
                            Text(label, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                            Text(
                                "${items.size} khoá", style = MaterialTheme.typography.labelLarge, color = t.onContainer,
                                modifier = Modifier.background(t.container, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                itemsIndexed(items, key = { _, it -> it.course.id }) { i, item ->
                    ProvideTrack(track) { CourseCard(item, Modifier.rise(i)) { onOpenCourse(item.course.id) } }
                }
            }
        }
    }
}

@Composable
private fun CourseCard(item: CourseItem, modifier: Modifier, onClick: () -> Unit) {
    val c = item.course
    val t = LocalTrack.current
    val cs = MaterialTheme.colorScheme
    Pushable(
        onClick, cs.surfaceContainerLowest, RoundedCornerShape(24.dp), modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        edge = cs.outlineVariant, border = BorderStroke(2.dp, cs.outlineVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.shared("glyph-${c.id}").size(58.dp).background(t.accent, shapeFor(c.id)), contentAlignment = Alignment.Center) {
                Text(courseGlyph(c.id, c.title), color = t.onAccent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.title, Modifier.shared("title-${c.id}"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(c.description, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val s = item.stats
            Spacer(Modifier.height(8.dp))
            Text(
                if (s.ready == 0) "Bài đang được soạn"
                else if (s.done == 0) "Chưa học bài nào · ${s.ready} bài sẵn sàng"
                else "${s.done}/${s.ready} bài · ${s.masteredPct}% chương thành thạo",
                style = MaterialTheme.typography.labelMedium, color = t.accent, fontWeight = FontWeight.SemiBold,
            )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.accent)
        }
    }
}
