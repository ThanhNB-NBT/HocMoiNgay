package com.thanhnb.hocmoingay

import com.thanhnb.hocmoingay.feature.reminder.NotifyCard
import com.thanhnb.hocmoingay.feature.profile.ProfileViewModel
import com.thanhnb.hocmoingay.feature.today.TodayViewModel
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.thanhnb.hocmoingay.core.code.LocalAssets
import com.thanhnb.hocmoingay.core.speech.LocalStt
import com.thanhnb.hocmoingay.core.speech.LocalTts
import com.thanhnb.hocmoingay.core.ui.LocalShared
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.pathIcon
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.course.CourseScreen
import com.thanhnb.hocmoingay.feature.course.CourseViewModel
import com.thanhnb.hocmoingay.feature.editor.EditorScreen
import com.thanhnb.hocmoingay.feature.editor.EditorViewModel
import com.thanhnb.hocmoingay.feature.learn.LearnScreen
import com.thanhnb.hocmoingay.feature.learn.LearnViewModel
import com.thanhnb.hocmoingay.feature.placement.PlacementScreen
import com.thanhnb.hocmoingay.feature.placement.PlacementViewModel
import com.thanhnb.hocmoingay.feature.player.PlayerScreen
import com.thanhnb.hocmoingay.feature.player.PlayerViewModel
import com.thanhnb.hocmoingay.feature.profile.ProfileScreen
import com.thanhnb.hocmoingay.feature.review.ReviewScreen
import com.thanhnb.hocmoingay.feature.review.ReviewViewModel
import com.thanhnb.hocmoingay.feature.settings.AppSettings
import com.thanhnb.hocmoingay.feature.settings.SettingsScreen
import com.thanhnb.hocmoingay.feature.settings.SettingsViewModel
import com.thanhnb.hocmoingay.feature.vocab.VocabScreen
import com.thanhnb.hocmoingay.feature.vocab.VocabViewModel
import com.thanhnb.hocmoingay.feature.today.TodayScreen
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.map

private fun tabLabel(k: NavKey) = when (k) {
    Today -> "Hôm nay"
    Learn -> "Học"
    Review -> "Ôn tập"
    else -> "Hồ sơ"
}

private fun tabIcon(k: NavKey): ImageVector = when (k) {
    Today -> Icons.Filled.Home
    Learn -> BookIcon
    Review -> CardsIcon
    else -> Icons.Filled.Person
}

// material-icons-core không có sách/thẻ; path lấy từ Material Icons "menu_book" và "style" (Apache 2.0)
private val BookIcon = pathIcon("Book", "M21,5c-1.11,-0.35 -2.33,-0.5 -3.5,-0.5c-1.95,0 -4.05,0.4 -5.5,1.5c-1.45,-1.1 -3.55,-1.5 -5.5,-1.5S2.45,4.9 1,6v14.65c0,0.25 0.25,0.5 0.5,0.5c0.1,0 0.15,-0.05 0.25,-0.05C3.1,20.45 5.05,20 6.5,20c1.95,0 4.05,0.4 5.5,1.5c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.65,0 3.35,0.3 4.75,1.05c0.1,0.05 0.15,0.05 0.25,0.05c0.25,0 0.5,-0.25 0.5,-0.5V6C22.4,5.55 21.75,5.25 21,5zM21,18.5c-1.1,-0.35 -2.3,-0.5 -3.5,-0.5c-1.7,0 -4.15,0.65 -5.5,1.5V8c1.35,-0.85 3.8,-1.5 5.5,-1.5c1.2,0 2.4,0.15 3.5,0.5V18.5z")
private val CardsIcon = pathIcon("Cards", "M2.53,19.65l1.34,0.56v-9.03l-2.43,5.86c-0.41,1.02 0.08,2.19 1.09,2.61zM22.03,15.95L17.07,3.98c-0.31,-0.75 -1.04,-1.21 -1.81,-1.23 -0.26,0 -0.53,0.04 -0.79,0.15L7.1,5.95c-0.75,0.31 -1.21,1.03 -1.23,1.8 -0.01,0.27 0.04,0.54 0.15,0.8l4.96,11.97c0.31,0.76 1.05,1.22 1.83,1.22 0.26,0 0.52,-0.05 0.77,-0.15l7.36,-3.05c1.02,-0.42 1.51,-1.59 1.09,-2.59zM7.88,8.75c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1 1,0.45 1,1 -0.45,1 -1,1zM5.88,19.75c0,1.1 0.9,2 2,2h1.45l-3.45,-8.34v6.34z")
/** Thanh tab: viền trên dày; tab đang chọn là khối vàng lê nổi (cùng ngôn ngữ với nút khối), icon nảy lò xo khi được chọn. */
@Composable
private fun TabBar(top: NavKey?, onSelect: (NavKey) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.background(cs.surface)) {
        HorizontalDivider(thickness = 2.dp, color = cs.outlineVariant)
        Row(Modifier.navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp).selectableGroup()) {
            TABS.forEach { tab ->
                val sel = top == tab
                val bounce = remember { Animatable(1f) }
                LaunchedEffect(sel) { if (sel) { bounce.snapTo(0.75f); bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f)) } }
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                        .selectable(sel, remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(width = 60.dp, height = 38.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value }) {
                        if (sel) {
                            Pushable(null, cs.primaryContainer, CircleShape, Modifier.matchParentSize()) {
                                Icon(tabIcon(tab), null, tint = cs.onPrimaryContainer, modifier = Modifier.align(Alignment.Center).size(22.dp))
                            }
                        } else {
                            Icon(tabIcon(tab), null, tint = cs.onSurfaceVariant, modifier = Modifier.align(Alignment.Center).size(24.dp))
                        }
                    }
                    Text(
                        tabLabel(tab), style = MaterialTheme.typography.labelMedium, maxLines = 1,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                        color = if (sel) cs.onSurface else cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun AppNav(graph: AppGraph) {
    val backStack = rememberNavBackStack(Today)
    val top = backStack.lastOrNull()
    // Inset do từng màn tự lo; Scaffold chỉ chừa chỗ cho thanh tab.
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (top in TABS) TabBar(top) { backStack.selectTab(it) }
        },
    ) { pad ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalShared provides this, LocalAssets provides graph.assets, LocalTts provides graph.tts, LocalStt provides graph.stt) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.padding(pad).consumeWindowInsets(pad),
                    // mỗi entry một ViewModelStore: mở bài B không dùng lại ViewModel của bài A
                    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()),
                    entryProvider = entryProvider {
                        entry<Today> {
                            val s by graph.settings.settings.collectAsStateWithLifecycle(AppSettings())
                            TodayScreen(
                                viewModel {
                                    TodayViewModel(
                                        graph.db.curriculum(), graph.db.learner(), graph.settings.settings,
                                        { graph.lessons.load(it)?.body }, graph.log::dayOf, BuildConfig.SHOW_SAMPLES,
                                    )
                                },
                                onOpenLearn = { backStack.selectTab(Learn) },
                                onOpenReview = { backStack.selectTab(Review) },
                                onOpenLesson = { backStack.add(LessonPlayer(it)) },
                                onResolve = { l, k -> backStack.add(CodeEditor(l, k, review = true)) },
                                onPlacement = { backStack.add(Placement) },
                                notify = { NotifyCard(s.reminders) },
                            )
                        }
                        entry<Learn> {
                            val s by graph.settings.settings.collectAsStateWithLifecycle(AppSettings())
                            LearnScreen(
                                viewModel { LearnViewModel(graph.db.curriculum(), graph.db.learner().observeAllProgress()) },
                                englishLevel = s.englishLevel,
                                onOpenCourse = { backStack.add(CourseDetail(it)) },
                                onPlacement = { backStack.add(Placement) },
                                onOpenVocab = { backStack.add(VocabBook(it)) },
                            )
                        }
                        entry<VocabBook> { k ->
                            VocabScreen(
                                viewModel {
                                    VocabViewModel(
                                        k.courseId, graph.db.curriculum()::lessonsOf, graph.db.learner().observeRecallOf(k.courseId),
                                        graph.reviews, graph.auth::currentUserId, graph.scope,
                                    )
                                },
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<Review> {
                            ReviewScreen(
                                viewModel { ReviewViewModel(graph.reviews, { graph.lessons.load(it)?.body }, graph.scope, graph.db.learner().observeNextDue(), graph.log) },
                                online = graph.net.online.collectAsStateWithLifecycle().value, api = graph.code, icon = CardsIcon,
                            )
                        }
                        entry<Profile> {
                            ProfileScreen(
                                viewModel { ProfileViewModel(graph.db.curriculum(), graph.db.learner(), graph.log::dayOf, BuildConfig.SHOW_SAMPLES) },
                                graph.supabase.auth.currentUserOrNull()?.email, onOpenSettings = { backStack.add(Settings) },
                            )
                        }
                        entry<Settings> {
                            SettingsScreen(
                                viewModel { SettingsViewModel(graph.settings) }, onBack = { backStack.removeLastOrNull() }, onPlacement = { backStack.add(Placement) },
                                email = graph.auth.email(), changePassword = graph.auth::changePassword,
                            )
                        }
                        entry<Placement> {
                            PlacementScreen(
                                viewModel { PlacementViewModel(graph.db.curriculum()::placement, graph.settings, graph.scope) },
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                        entry<CourseDetail> { k ->
                            CourseScreen(
                                viewModel { CourseViewModel(k.courseId, graph.db.curriculum(), graph.db.learner().observeAllProgress()) },
                                onBack = { backStack.removeLastOrNull() },
                                onOpenLesson = { backStack.add(LessonPlayer(it)) },
                            )
                        }
                        entry<LessonPlayer> { k ->
                            PlayerScreen(
                                viewModel { PlayerViewModel(k.lessonId, graph.lessons, graph.code, graph.net.online, graph.db.learner().observeProgress(k.lessonId), graph.scope, graph.log) },
                                k.lessonId, onBack = { backStack.removeLastOrNull() },
                                onOpenEditor = { key -> backStack.add(CodeEditor(k.lessonId, key)) },
                                onOpenLesson = { backStack.add(LessonPlayer(it)) },
                            )
                        }
                        entry<CodeEditor> { k ->
                            EditorScreen(
                                viewModel {
                                    EditorViewModel(
                                        k.lessonId, k.cardKey, graph.lessons, graph.db.drafts(), graph.code, graph.net.online,
                                        graph.settings.settings.map { it.preferredLanguage }, graph.scope, review = k.review,
                                    )
                                },
                                onBack = { backStack.removeLastOrNull() },
                            )
                        }
                    },
                )
            }
        }
    }
}
