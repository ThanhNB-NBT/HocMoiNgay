package com.thanhnb.hocmoingay

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.TodayPlaceholder
import com.thanhnb.hocmoingay.feature.learn.LearnScreen
import com.thanhnb.hocmoingay.feature.learn.LearnViewModel
import com.thanhnb.hocmoingay.feature.settings.SettingsScreen
import com.thanhnb.hocmoingay.feature.settings.SettingsViewModel

private fun tabLabel(k: NavKey) = when (k) {
    Today -> "Hôm nay"
    Learn -> "Học"
    Review -> "Ôn tập"
    else -> "Hồ sơ"
}

private fun tabIcon(k: NavKey): ImageVector = when (k) {
    Today -> Icons.Filled.Home
    Learn -> Icons.AutoMirrored.Filled.List
    Review -> Icons.Filled.Refresh
    else -> Icons.Filled.Person
}

@Composable
fun AppNav(graph: AppGraph) {
    val backStack = rememberNavBackStack(Today)
    val top = backStack.lastOrNull()
    // Inset do từng màn tự lo; Scaffold chỉ chừa chỗ cho thanh tab.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (top in TABS) NavigationBar {
                TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = top == tab,
                        onClick = { backStack.selectTab(tab) },
                        icon = { Icon(tabIcon(tab), contentDescription = null) },
                        label = { Text(tabLabel(tab)) },
                    )
                }
            }
        },
    ) { pad ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(pad).consumeWindowInsets(pad),
            entryProvider = entryProvider {
                entry<Today> { TodayPlaceholder() }
                entry<Learn> {
                    LearnScreen(viewModel { LearnViewModel(graph.db.curriculum()) }, onOpenCourse = { backStack.add(CourseDetail(it)) })
                }
                entry<Review> { Placeholder("Ôn tập", "Thẻ đến hạn — giai đoạn d") }
                entry<Profile> {
                    Placeholder("Hồ sơ", "Streak, XP, heatmap — giai đoạn e") {
                        FilledTonalButton(onClick = { backStack.add(Settings) }) { Text("Cài đặt") }
                    }
                }
                entry<Settings> {
                    SettingsScreen(viewModel { SettingsViewModel(graph.settings) }, onBack = { backStack.removeLastOrNull() })
                }
                entry<CourseDetail> { k ->
                    Placeholder("Khoá ${k.courseId}", "Đề cương — giai đoạn c") {
                        Button(onClick = { backStack.add(LessonPlayer("${k.courseId}/nhap-mon/lam-quen/xin-chao")) }) { Text("Mở thử trình phát bài") }
                    }
                }
                entry<LessonPlayer> { k -> Placeholder("Trình phát bài", k.lessonId) }
                entry<CodeEditor> { k -> Placeholder("Editor", "${k.lessonId}#${k.cardKey}") }
            },
        )
    }
}
