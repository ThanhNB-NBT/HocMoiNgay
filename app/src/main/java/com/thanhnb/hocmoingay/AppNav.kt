package com.thanhnb.hocmoingay

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import com.thanhnb.hocmoingay.core.ui.LocalBarInset
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import com.thanhnb.hocmoingay.feature.reminder.NotifyCard
import com.thanhnb.hocmoingay.feature.profile.ProfileViewModel
import com.thanhnb.hocmoingay.feature.today.TodayViewModel
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.thanhnb.hocmoingay.core.ui.BookIcon
import com.thanhnb.hocmoingay.core.ui.CardsIcon
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

private val BarHeight = 64.dp
private val BarGap = 14.dp

/**
 * Thanh tab liquid glass: một viên kính nổi cách đáy và hai mép, khúc xạ + làm nhoè nội dung cuộn phía sau
 * (haze-glass; Android 13+ chạy shader, máy cũ tự lùi về kính mờ). Tab đang chọn là một "thấu kính" sáng hơn.
 */
@Composable
private fun TabBar(top: NavKey?, haze: HazeState, modifier: Modifier, onSelect: (NavKey) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val tint = cs.surfaceContainerLowest.copy(alpha = 0.6f)
    val glass = remember(tint) { GlassStyle { shape(RoundedCornerShape(BarHeight / 2)); tint(tint) } }
    Row(
        modifier.navigationBarsPadding().padding(start = BarGap, end = BarGap, bottom = BarGap).fillMaxWidth().height(BarHeight)
            .hazeGlass(HazeInput.Sources(haze), glass)
            .border(1.dp, cs.surfaceContainerLowest.copy(alpha = 0.55f), CircleShape)
            .padding(5.dp).selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TABS.forEach { tab ->
            val sel = top == tab
            val bounce = remember { Animatable(1f) }
            LaunchedEffect(sel) { if (sel) { bounce.snapTo(0.82f); bounce.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f)) } }
            val lens by animateColorAsState(if (sel) cs.secondaryContainer.copy(alpha = 0.8f) else cs.secondaryContainer.copy(alpha = 0f), tween(200), label = "thấu kính tab")
            val ink by animateColorAsState(if (sel) cs.primary else cs.onSurfaceVariant, tween(200), label = "màu tab")
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape).background(lens)
                    .selectable(sel, remember { MutableInteractionSource() }, indication = null, role = Role.Tab, onClick = { onSelect(tab) }),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(tabIcon(tab), null, tint = ink, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value })
                Text(
                    tabLabel(tab), style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, color = ink,
                )
            }
        }
    }
}

@Composable
fun AppNav(graph: AppGraph) {
    val backStack = rememberNavBackStack(Today)
    val top = backStack.lastOrNull()
    val showBar = top in TABS
    val haze = rememberHazeState()
    // Inset do từng màn tự lo; màn tab chạy nội dung xuống dưới thanh kính và chừa đáy bằng BarSpace().
    Box(Modifier.fillMaxSize()) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalBarInset provides if (showBar) BarHeight + BarGap + 16.dp else 0.dp, LocalShared provides this, LocalAssets provides graph.assets, LocalTts provides graph.tts, LocalStt provides graph.stt) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    modifier = Modifier.fillMaxSize().hazeSource(haze).background(MaterialTheme.colorScheme.background),
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
        if (showBar) TabBar(top, haze, Modifier.align(Alignment.BottomCenter)) { backStack.selectTab(it) }
    }
}
