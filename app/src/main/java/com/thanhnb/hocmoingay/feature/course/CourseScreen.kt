package com.thanhnb.hocmoingay.feature.course

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
import com.thanhnb.hocmoingay.core.ui.Clover
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.rise
import com.thanhnb.hocmoingay.core.ui.shapeFor
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

@Composable
fun CourseScreen(vm: CourseViewModel, onBack: () -> Unit, onOpenLesson: (String) -> Unit) {
    val ui = vm.ui.collectAsStateWithLifecycle().value ?: return Box(Modifier.fillMaxSize())
    val c = ui.course
    ProvideTrack(if (c.track == "code") Track.CODE else Track.ENGLISH) {
        val t = LocalTrack.current
        // Mở sẵn cấp đầu tiên có bài sẵn sàng, gập các cấp khác (khoá python có hàng trăm bài 'Sắp có')
        val firstReady = ui.outline.indexOfFirst { lv -> lv.chapters.any { ch -> ch.lessons.any { it.status == "ready" } } }.coerceAtLeast(0)
        var open by rememberSaveable { mutableStateOf(setOf(firstReady)) }
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), contentPadding = PaddingValues(bottom = 32.dp)) {
            item("top") {
                IconButton(onClick = onBack, Modifier.padding(start = 4.dp, top = 4.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") }
            }
            item("head") {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Box(Modifier.size(72.dp).shared("glyph-${c.id}").background(t.accent, shapeFor(c.id)), contentAlignment = Alignment.Center) {
                        Text(courseGlyph(c.id, c.title), color = t.onAccent, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(c.title, Modifier.shared("title-${c.id}"), style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(6.dp))
                    Text(c.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Stat("${ui.stats.done}/${ui.stats.ready}", "bài đã xong")
                        Stat("${ui.stats.masteredPct}%", "chương thành thạo")
                    }
                }
            }
            ui.outline.forEachIndexed { li, lv ->
                item("lv-${lv.level}") {
                    LevelHeader(lv.title, expanded = li in open, count = lv.chapters.sumOf { it.lessons.size }) {
                        open = if (li in open) open - li else open + li
                    }
                }
                if (li in open) lv.chapters.forEach { ch ->
                    item("ch-${lv.level}/${ch.id}") {
                        Text(ch.title, Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    itemsIndexed(ch.lessons, key = { _, l -> l.id }) { i, l ->
                        LessonRow(i + 1, l, ui.progress[l.id], Modifier.padding(horizontal = 16.dp, vertical = 4.dp).rise(i)) { onOpenLesson(l.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    val t = LocalTrack.current // màu của mảng, cùng tông ô ký hiệu
    Pushable(null, t.container, RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = t.onContainer)
            Text(label, style = MaterialTheme.typography.labelMedium, color = t.onContainer)
        }
    }
}

@Composable
private fun LevelHeader(title: String, expanded: Boolean, count: Int, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 20.dp, end = 12.dp, top = 24.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
        Text("$count bài", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, if (expanded) "Thu gọn" else "Mở rộng")
    }
}

@Composable
private fun LessonRow(n: Int, l: OutlineLesson, p: ProgressEntity?, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val f = LocalFun.current
    val t = LocalTrack.current
    val ready = l.status == "ready"
    val done = p?.status == "done"
    Pushable(
        if (ready) onClick else null, cs.surfaceContainerLowest, RoundedCornerShape(20.dp), modifier.fillMaxWidth(),
        edge = cs.outlineVariant, border = BorderStroke(2.dp, cs.outlineVariant), enabled = ready,
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val (bg, fg) = when {
                done -> f.mintContainer to f.mint
                ready -> t.container to t.onContainer
                else -> cs.surfaceContainerHigh to cs.onSurfaceVariant
            }
            Box(Modifier.size(40.dp).background(bg, Clover), contentAlignment = Alignment.Center) {
                if (done) Icon(Icons.Filled.Check, "Đã xong", tint = fg, modifier = Modifier.size(20.dp))
                else Text("$n", color = fg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(12.dp))
            Text(l.title, Modifier.weight(1f).shared("lesson-${l.id}"), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!ready) Text("Sắp có", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            else if (p?.status == "started") Text("Đang học", style = MaterialTheme.typography.labelMedium, color = t.accent)
        }
    }
}
