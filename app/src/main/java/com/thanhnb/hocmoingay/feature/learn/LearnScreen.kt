package com.thanhnb.hocmoingay.feature.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.BuildConfig
import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.CurriculumDao
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.feature.Placeholder
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

/** Khung tab Học: chỉ danh sách khoá. Plan c thêm % chương thành thạo và shared-element. */
@Composable
fun LearnScreen(vm: LearnViewModel, onOpenCourse: (String) -> Unit) {
    val list = vm.courses.collectAsStateWithLifecycle().value
    when {
        list == null -> Box(Modifier.fillMaxSize())
        list.isEmpty() -> Placeholder("Chưa có khoá học", "Giáo trình được tải về khi có mạng. Mở lại app sau khi kết nối.")
        else -> LazyColumn(Modifier.fillMaxSize().safeDrawingPadding()) {
            groupCourses(list).forEach { (label, items) ->
                item(key = "h-$label") {
                    Text(label, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp))
                }
                items(items, key = { it.id }) { c ->
                    ProvideTrack(if (c.track == "code") Track.CODE else Track.ENGLISH) {
                        ListItem(
                            headlineContent = { Text(c.title) },
                            supportingContent = { Text(c.description, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { Box(Modifier.size(12.dp).background(LocalTrack.current.accent, CircleShape)) },
                            modifier = Modifier.clickable { onOpenCourse(c.id) },
                        )
                    }
                }
            }
        }
    }
}
