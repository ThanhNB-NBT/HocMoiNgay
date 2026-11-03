package com.thanhnb.hocmoingay.feature.course

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import com.thanhnb.hocmoingay.core.ui.lift
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.remember
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Skeleton
import com.thanhnb.hocmoingay.core.ui.ThinProgress
import com.thanhnb.hocmoingay.feature.learn.GlyphTile
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.CurriculumDao
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.OutlineLesson
import com.thanhnb.hocmoingay.core.lesson.OutlineLevel
import com.thanhnb.hocmoingay.core.lesson.courseStats
import com.thanhnb.hocmoingay.core.lesson.parseOutline
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Tile
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.rise

import com.thanhnb.hocmoingay.core.ui.shared
import com.thanhnb.hocmoingay.feature.learn.courseGlyph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class CourseUi(val course: CourseEntity, val outline: List<OutlineLevel>, val progress: Map<String, ProgressEntity>) {
    val stats = courseStats(course.id, outline, progress)
}

class CourseViewModel(courseId: String, dao: CurriculumDao, progress: Flow<List<ProgressEntity>>) : ViewModel() {
    val ui = combine(dao.observeCourse(courseId), progress) { c, ps ->
        c?.let { CourseUi(it, parseOutline(it.outline), ps.associateBy { p -> p.lessonId }) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Bài tới lượt: bài sẵn sàng đầu tiên chưa xong, theo thứ tự đề cương. */
fun nextLesson(outline: List<OutlineLevel>, progress: Map<String, ProgressEntity>): OutlineLesson? =
    outline.asSequence().flatMap { lv -> lv.chapters.asSequence().flatMap { it.lessons } }
        .firstOrNull { it.status == "ready" && progress[it.id]?.status != "done" }

/** Một dòng của đề cương phẳng: dựng sẵn để biết vị trí bài tới lượt mà cuộn tới. */
private sealed interface PathRow {
    data class Level(val index: Int, val level: OutlineLevel) : PathRow
    data class Chapter(val key: String, val title: String) : PathRow
    data class Lesson(val n: Int, val lesson: OutlineLesson) : PathRow
}

/**
 * Đề cương khoá: đầu màn nền màu mảng nhạt (ô ký hiệu đặc, tên, tiến độ); dưới là lộ trình — các bài là nút tròn
 * nối bằng một đường dọc, bài tới lượt là nút lớn kèm thẻ có nút học ngay. Mở màn tự cuộn tới bài tới lượt.
 */
@Composable
fun CourseScreen(vm: CourseViewModel, onBack: () -> Unit, onOpenLesson: (String) -> Unit) {
    val ui = vm.ui.collectAsStateWithLifecycle().value ?: return Skeleton(Modifier.safeDrawingPadding())
    val c = ui.course
    val next = remember(ui) { nextLesson(ui.outline, ui.progress) }
    ProvideTrack(if (c.track == "code") Track.CODE else Track.ENGLISH) {
        // Mở sẵn cấp chứa bài tới lượt (không có thì cấp đầu có bài sẵn sàng), gập các cấp khác
        val firstOpen = remember(ui) {
            next?.let { n -> ui.outline.indexOfFirst { lv -> lv.chapters.any { ch -> ch.lessons.any { it.id == n.id } } } }
                ?: ui.outline.indexOfFirst { lv -> lv.chapters.any { ch -> ch.lessons.any { it.status == "ready" } } }
        }.coerceAtLeast(0)
        var open by rememberSaveable { mutableStateOf(setOf(firstOpen)) }
        val rows = remember(ui, open) {
            buildList {
                ui.outline.forEachIndexed { li, lv ->
                    add(PathRow.Level(li, lv))
                    if (li in open) lv.chapters.forEach { ch ->
                        add(PathRow.Chapter("${lv.level}/${ch.id}", ch.title))
                        ch.lessons.forEachIndexed { i, l -> add(PathRow.Lesson(i + 1, l)) }
                    }
                }
            }
        }
        val list = rememberLazyListState()
        LaunchedEffect(next?.id) {
            val i = rows.indexOfFirst { it is PathRow.Lesson && it.lesson.id == next?.id }
            if (i > 3) list.scrollToItem(i + 1 - 2) // +1 vì dòng đầu là header; chừa 2 dòng phía trên cho thấy bài vừa xong
        }
        LazyColumn(Modifier.fillMaxSize(), state = list) {
            item("head") { Header(ui, onBack) }
            rows.forEach { r ->
                when (r) {
                    is PathRow.Level -> item("lv-${r.level.level}") {
                        LevelHeader(r.level.title, expanded = r.index in open, count = r.level.chapters.sumOf { it.lessons.size }) {
                            open = if (r.index in open) open - r.index else open + r.index
                        }
                    }
                    is PathRow.Chapter -> item("ch-${r.key}") {
                        Text(
                            r.title.uppercase(), Modifier.fillMaxWidth().trackLine().padding(start = 78.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is PathRow.Lesson -> item(r.lesson.id) {
                        val l = r.lesson
                        LessonNode(r.n, l, ui.progress[l.id], l.id == next?.id, Modifier.rise(r.n - 1)) { onOpenLesson(l.id) }
                    }
                }
            }
            item("end") { Spacer(Modifier.navigationBarsPadding().height(32.dp)) }
        }
    }
}

/** Đầu màn: nền màu mảng nhạt tràn lên thanh trạng thái, bo đáy. */
@Composable
private fun Header(ui: CourseUi, onBack: () -> Unit) {
    val c = ui.course
    val s = ui.stats
    val t = LocalTrack.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)).background(t.container)
            .statusBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
    ) {
        IconButton(onClick = onBack, Modifier.offset(x = (-12).dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = t.onContainer) }
        Box(
            Modifier.shared("glyph-${c.id}").size(56.dp).background(t.accent, Tile),
            contentAlignment = Alignment.Center,
        ) {
            Text(courseGlyph(c.id, c.title), color = t.onAccent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        Text(c.title, Modifier.shared("title-${c.id}"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = t.onContainer)
        Row(Modifier.padding(top = 14.dp, bottom = 6.dp)) {
            Text("${s.done}/${s.ready} bài", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = t.onContainer)
            Text("${s.masteredPct}% chương thành thạo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = t.onContainer)
        }
        ThinProgress(s.done.toFloat() / s.ready.coerceAtLeast(1), t.accent, Modifier.fillMaxWidth(), track = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f))
    }
}

@Composable
private fun LevelHeader(title: String, expanded: Boolean, count: Int, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("$count bài", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, if (expanded) "Thu gọn" else "Mở rộng")
    }
}

private val NodeX = 42.dp // tâm nút trên trục lộ trình (lề 20 + nửa nút 22)

/** Đường dọc của lộ trình, vẽ suốt chiều cao dòng để các dòng nối liền nhau. */
@Composable
private fun Modifier.trackLine(): Modifier {
    val line = MaterialTheme.colorScheme.outlineVariant
    return drawBehind { drawLine(line, Offset(NodeX.toPx(), 0f), Offset(NodeX.toPx(), size.height), strokeWidth = 3.dp.toPx()) }
}

/** Một bài trên lộ trình: xong = nút xanh lá có dấu tích; tới lượt = nút lớn màu mảng + thẻ học ngay; chưa học = vòng rỗng; chưa soạn = mờ. */
@Composable
private fun LessonNode(n: Int, l: OutlineLesson, p: ProgressEntity?, current: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val f = LocalFun.current
    val t = LocalTrack.current
    val ready = l.status == "ready"
    val done = p?.status == "done"
    val started = p?.status == "started"
    Row(
        modifier.fillMaxWidth().trackLine().alpha(if (ready) 1f else 0.5f)
            .clickable(enabled = ready && !current, role = Role.Button, onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
            when {
                done -> Box(Modifier.size(44.dp).background(f.mint, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Check, "Đã xong", tint = cs.surfaceContainerLowest, modifier = Modifier.size(22.dp))
                }
                current -> Box(
                    Modifier.requiredSize(52.dp).drawBehind { drawCircle(t.accent.copy(alpha = 0.18f), radius = size.minDimension / 2 + 5.dp.toPx()) }
                        .background(t.accent, CircleShape).border(3.dp, cs.surfaceContainerLowest, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("$n", color = t.onAccent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
                else -> Box(
                    Modifier.size(44.dp).background(cs.surfaceContainerLowest, CircleShape).border(3.dp, cs.outlineVariant, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text("$n", color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) }
            }
        }
        Spacer(Modifier.width(14.dp))
        if (current) {
            val shape = RoundedCornerShape(16.dp)
            Column(Modifier.weight(1f).lift(shape).background(cs.surfaceContainerLowest, shape).padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(if (started) "ĐANG HỌC" else "TỚI LƯỢT", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = t.accent)
                Text(
                    l.title, Modifier.padding(top = 2.dp, bottom = 10.dp).shared("lesson-${l.id}"), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                PushButton(if (started) "Học tiếp" else "Bắt đầu học", onClick, Modifier.fillMaxWidth(), color = t.accent, contentColor = t.onAccent)
            }
        } else {
            Text(
                l.title, Modifier.weight(1f).shared("lesson-${l.id}"), style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (done) FontWeight.Medium else null, color = if (done) cs.onSurface else cs.onSurfaceVariant,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            when {
                !ready -> Text("Sắp có", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                started -> Text("Đang học", style = MaterialTheme.typography.labelMedium, color = t.accent)
            }
        }
    }
}
