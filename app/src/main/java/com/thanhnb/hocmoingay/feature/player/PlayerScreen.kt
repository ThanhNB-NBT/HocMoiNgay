package com.thanhnb.hocmoingay.feature.player

import com.thanhnb.hocmoingay.core.lesson.codePending
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.OrderLines
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Critter
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.TickUpNumber
import com.thanhnb.hocmoingay.core.ui.shared
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.player.cards.CardCtx
import com.thanhnb.hocmoingay.feature.player.cards.CardView
import kotlinx.serialization.json.JsonObject

@Composable
fun PlayerScreen(vm: PlayerViewModel, lessonId: String, onBack: () -> Unit, onOpenEditor: (String) -> Unit) {
    val body = vm.body.collectAsStateWithLifecycle().value
    val missing = vm.missing.collectAsStateWithLifecycle().value
    val q = vm.queue.collectAsStateWithLifecycle().value
    val started = vm.started.collectAsStateWithLifecycle().value
    val online = vm.online.collectAsStateWithLifecycle().value
    val states = vm.cardState.collectAsStateWithLifecycle().value
    val track = vm.track.collectAsStateWithLifecycle().value
    val haptic = LocalHapticFeedback.current
    if (missing) return Placeholder("Không mở được bài", "Bài chưa có trên máy hoặc dữ liệu hỏng. Mở lại app khi có mạng để kéo giáo trình mới.")
    if (body == null) return Box(Modifier.fillMaxSize())
    ProvideTrack(if (track == "english") Track.ENGLISH else Track.CODE) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(q.progress, onBack)
            when {
                !started -> Intro(body, lessonId, vm::start)
                q.finished -> Done(q.score, body.codePending(states), body.hasReview, onBack)
                else -> {
                    // số trang tăng khi card sai được thêm vào cuối: lambda phải đọc giá trị mới, không phải q lúc dựng đầu
                    val pages by rememberUpdatedState(q.order.size)
                    val pager = rememberPagerState { pages }
                    LaunchedEffect(q.pos) {
                        pager.animateScrollToPage(q.pos, animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f))
                        pager.scrollToPage(q.pos) // chốt đúng trang nếu animation bị cắt ngang
                    }
                    val result = q.results[q.pos]
                    LaunchedEffect(q.pos, result) {
                        if (result != null) haptic.performHapticFeedback(if (result) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
                    }
                    VerticalPager(pager, Modifier.weight(1f), userScrollEnabled = false, key = { it }) { page ->
                        val idx = q.order.getOrNull(page) ?: return@VerticalPager
                        val card = body.cards[idx]
                        val ctx = CardCtx(
                            lessonId = lessonId, cardKey = card.key, seed = "$lessonId#${card.key}#$page", online = online, api = vm.api,
                            state = states[card.key] as? JsonObject ?: JsonObject(emptyMap()),
                            save = { vm.saveCard(card.key, it) }, openEditor = onOpenEditor,
                            answered = page in q.results, onAnswer = { ok, graded -> if (page == q.pos) vm.answer(ok, graded) }, active = page == q.pos,
                        )
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)
                                .answerFeedback(q.results[page]),
                        ) {
                            if (card is OrderLines) Text("Xếp các dòng thành chương trình đúng", Modifier.padding(bottom = 12.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            key(page) { CardView(card, ctx) }
                        }
                    }
                    BottomBar(q, onNext = vm::next)
                }
            }
        }
    }
}

/** Đúng: cả khối card nảy nhẹ 1 → 1.03 → 1. Sai: lắc ngang kiểu lò xo. */
private fun Modifier.answerFeedback(result: Boolean?): Modifier = composed {
    val x = remember { Animatable(0f) }
    val s = remember { Animatable(1f) }
    LaunchedEffect(result) {
        when (result) {
            true -> { s.snapTo(1.03f); s.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f)) }
            false -> { x.snapTo(18f); x.animateTo(0f, spring(dampingRatio = 0.25f, stiffness = 600f)) }
            null -> {}
        }
    }
    graphicsLayer { translationX = x.value * density; scaleX = s.value; scaleY = s.value }
}

@Composable
private fun TopBar(progress: Float, onBack: () -> Unit) {
    val p by animateFloatAsState(progress, spring(dampingRatio = 0.7f, stiffness = 200f), label = "tiến độ")
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Đóng bài") }
        Box(Modifier.weight(1f).height(14.dp).clip(CircleShape).background(cs.surfaceContainerHigh)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(cs.primaryContainer))
        }
    }
}

@Composable
private fun Intro(b: LessonBody, lessonId: String, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        Text(b.title, Modifier.shared("lesson-$lessonId"), style = MaterialTheme.typography.displaySmall)
        Pushable(null, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(28.dp), Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Học xong bài này, bạn sẽ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(b.canDo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Text("${b.cards.size} thẻ · khoảng ${b.estimateMin} phút", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        PushButton("Bắt đầu", onStart, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward)
    }
}

@Composable
private fun BottomBar(q: PlayerQueue, onNext: () -> Unit) {
    val r = q.results[q.pos]
    val f = LocalFun.current
    // luôn giữ chỗ: pager đổi chiều cao giữa lúc cuộn thì dừng lệch giữa hai trang
    Box(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (r == true) PushButton("Tiếp", onNext, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward)
        else if (r == false) PushButton("Làm lại sau", onNext, Modifier.fillMaxWidth(), color = f.coral, contentColor = f.onCoral)
    }
}

/** Khoảnh khắc mạnh của màn: khối coral có điểm đếm tăng dần. */
@Composable
private fun Done(score: Int, codePending: Boolean, hasReview: Boolean, onBack: () -> Unit) {
    val f = LocalFun.current
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.weight(1f))
        Critter(Modifier.size(96.dp))
        Text("Xong bài!", style = MaterialTheme.typography.displaySmall)
        Pushable(null, f.coralContainer, RoundedCornerShape(32.dp), Modifier.fillMaxWidth(), edge = f.coral) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    TickUpNumber(score, MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold), f.onCoralContainer)
                    Text("%", style = MaterialTheme.typography.headlineMedium, color = f.onCoralContainer)
                }
                Text("trả lời đúng ngay lần đầu", style = MaterialTheme.typography.bodyLarge, color = f.onCoralContainer)
            }
        }
        val note = when {
            codePending -> "Bài code còn để sau — nộp đạt thì bài mới tính là xong."
            hasReview -> "Các thẻ đánh dấu ôn tập đã vào lịch ôn."
            else -> null
        }
        note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.weight(1f))
        PushButton("Về đề cương", onBack, Modifier.fillMaxWidth())
    }
}
