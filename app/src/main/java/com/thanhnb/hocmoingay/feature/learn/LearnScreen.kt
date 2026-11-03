package com.thanhnb.hocmoingay.feature.learn

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import com.thanhnb.hocmoingay.core.ui.BarSpace
import com.thanhnb.hocmoingay.core.ui.tabInsets
import com.thanhnb.hocmoingay.core.ui.lift
import com.thanhnb.hocmoingay.core.ui.BookIcon
import com.thanhnb.hocmoingay.core.ui.PillTabs
import com.thanhnb.hocmoingay.core.ui.Skeleton
import com.thanhnb.hocmoingay.core.ui.ThinProgress
import androidx.compose.ui.unit.Dp
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.thanhnb.hocmoingay.core.ui.Tile
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.rise

import com.thanhnb.hocmoingay.core.ui.shared
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.ScreenHeader
import com.thanhnb.hocmoingay.feature.today.ENGLISH_COURSE
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
    "english-grammar" -> "Gr"
    else -> title.filter { it.isLetterOrDigit() }.take(2).lowercase()
}

/** Ô ký hiệu của khoá (py, js, Aa...) tô màu của mảng hiện tại; dùng ở thẻ khoá, đề cương và Hôm nay. */
@Composable
fun GlyphTile(courseId: String, title: String, size: Dp, modifier: Modifier = Modifier) {
    val t = LocalTrack.current
    Box(modifier.size(size).background(t.container, Tile), contentAlignment = Alignment.Center) {
        Text(
            courseGlyph(courseId, title), color = t.onContainer, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, maxLines = 1,
            style = if (size >= 64.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
        )
    }
}

/**
 * Tab Học: chọn mảng ở đầu màn; khoá đang học (hoặc khoá gợi ý bắt đầu) là MỘT khối màu lớn,
 * các khoá còn lại gom trong một mặt phẳng ngăn bằng vạch. Ô ký hiệu + tiêu đề bay sang màn đề cương.
 */
@Composable
fun LearnScreen(
    vm: LearnViewModel, englishLevel: String?, onOpenCourse: (String) -> Unit, onPlacement: () -> Unit, onOpenVocab: (String) -> Unit,
) {
    val list = vm.courses.collectAsStateWithLifecycle().value
    var tab by rememberSaveable { mutableIntStateOf(0) }
    when {
        list == null -> Skeleton(Modifier.tabInsets())
        list.isEmpty() -> Placeholder("Chưa có khoá học", "Giáo trình được tải về khi có mạng. Mở lại app sau khi kết nối.")
        else -> {
            val groups = groupCourses(list) { it.course.track }
            val (label, items) = groups[tab.coerceIn(0, groups.lastIndex)]
            val track = if (items.first().course.track == "code") Track.CODE else Track.ENGLISH
            val featured = featuredCourse(items)
            val rest = items.filter { it !== featured }
            ProvideTrack(track) {
                LazyColumn(Modifier.fillMaxSize().tabInsets()) {
                    item(key = "header") { ScreenHeader("Học") }
                    if (groups.size > 1) item(key = "tabs") {
                        PillTabs(groups.map { (l, cs) -> "$l · ${cs.size}" }, groups.indexOfFirst { it.first == label }, { tab = it }, Modifier.padding(horizontal = 20.dp))
                    }
                    if (track == Track.ENGLISH) item(key = "placement") { PlacementCard(englishLevel, onPlacement) }
                    if (featured != null) item(key = "feat-${featured.course.id}") {
                        FeaturedCourse(featured, Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).rise(0)) { onOpenCourse(featured.course.id) }
                    }
                    if (track == Track.ENGLISH) item(key = "vocab") { VocabRow(Modifier.rise(1)) { onOpenVocab(ENGLISH_COURSE) } }
                    if (rest.isNotEmpty()) {
                        item(key = "sec") {
                            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
                                Text(if (featured == null) "Tất cả khoá" else "Các khoá khác", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${rest.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        item(key = "group") { CourseGroup(rest, Modifier.padding(horizontal = 20.dp).rise(2), onOpenCourse) }
                    }
                    item(key = "bar") { BarSpace() }
                }
            }
        }
    }
}

/** Khoá nên hiện nổi bật: đang học dở nhiều nhất; chưa học khoá nào thì khoá đầu tiên đã có bài. */
fun featuredCourse(items: List<CourseItem>): CourseItem? =
    items.filter { it.stats.done in 1 until it.stats.ready }.maxByOrNull { it.stats.done }
        ?: items.firstOrNull { it.stats.done == 0 && it.stats.ready > 0 }

@Composable
private fun FeaturedCourse(item: CourseItem, modifier: Modifier, onClick: () -> Unit) {
    val c = item.course
    val s = item.stats
    val t = LocalTrack.current
    val ink = t.onAccent
    val shape = RoundedCornerShape(22.dp)
    Pushable(onClick, t.accent, shape, modifier.fillMaxWidth().lift(shape), edge = Color.Transparent) {
        Box {
            // ký hiệu khoá khổng lồ, mờ, tràn ra góc phải dưới
            Text(
                courseGlyph(c.id, c.title), Modifier.align(Alignment.BottomEnd).offset(x = 6.dp, y = 22.dp),
                color = ink.copy(alpha = 0.14f), fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, fontSize = 92.sp, maxLines = 1,
            )
            Column(Modifier.padding(18.dp)) {
                Text(if (s.done > 0) "ĐANG HỌC" else "GỢI Ý BẮT ĐẦU", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = ink.copy(alpha = 0.82f))
                Text(c.title, Modifier.padding(top = 4.dp, bottom = 2.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (s.done > 0) "${s.done}/${s.ready} bài · ${s.masteredPct}% chương thành thạo" else "${s.ready} bài sẵn sàng", style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.85f))
                ThinProgress(s.done.toFloat() / s.ready.coerceAtLeast(1), ink, Modifier.padding(vertical = 14.dp).fillMaxWidth(0.82f), track = ink.copy(alpha = 0.25f))
                Row(
                    Modifier.background(ink, CircleShape).padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (s.done > 0) "Học tiếp" else "Bắt đầu", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = t.accent)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = t.accent, modifier = Modifier.padding(start = 6.dp).size(16.dp))
                }
            }
        }
    }
}

/** Các khoá còn lại: một mặt phẳng, mỗi khoá một dòng, ngăn bằng vạch mảnh. */
@Composable
private fun CourseGroup(items: List<CourseItem>, modifier: Modifier, onOpen: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.fillMaxWidth().lift(shape).clip(shape).background(cs.surfaceContainerLowest)) {
        items.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(Modifier.padding(start = 64.dp), thickness = 1.dp, color = cs.outlineVariant)
            CourseRow(item) { onOpen(item.course.id) }
        }
    }
}

@Composable
private fun CourseRow(item: CourseItem, onClick: () -> Unit) {
    val c = item.course
    val s = item.stats
    val cs = MaterialTheme.colorScheme
    Pushable(onClick, cs.surfaceContainerLowest, RectangleShape, Modifier.fillMaxWidth(), edge = Color.Transparent) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            GlyphTile(c.id, c.title, 38.dp, Modifier.shared("glyph-${c.id}"))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.title, Modifier.shared("title-${c.id}"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        s.ready == 0 -> "Bài đang được soạn"
                        s.done == 0 -> "${s.ready} bài"
                        else -> "${s.done}/${s.ready} bài"
                    },
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                )
            }
            if (s.ready > 0) Text(
                "${s.done * 100 / s.ready}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                color = if (s.done > 0) LocalTrack.current.accent else cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VocabRow(modifier: Modifier, onClick: () -> Unit) {
    val t = LocalTrack.current
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Pushable(onClick, cs.surfaceContainerLowest, shape, modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth(), edge = cs.outlineVariant) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(t.container, Tile), contentAlignment = Alignment.Center) {
                Icon(BookIcon, null, tint = t.onContainer, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sổ từ vựng", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Từ theo cấp A1–C1 · nghe phát âm · thẻ lật", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.accent)
        }
    }
}

@Composable
private fun PlacementCard(level: String?, onClick: () -> Unit) {
    val t = LocalTrack.current
    if (level != null) {
        Text(
            "Trình độ của bạn: $level · làm lại trong Cài đặt", Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Pushable(onClick, t.container, RoundedCornerShape(18.dp), Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).fillMaxWidth(), edge = Color.Transparent) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Chưa biết bắt đầu từ cấp nào?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = t.onContainer)
            Text("Làm bài xếp lớp khoảng 10 phút để app chọn trình độ cho bạn.", style = MaterialTheme.typography.bodyMedium, color = t.onContainer)
        }
    }
}
