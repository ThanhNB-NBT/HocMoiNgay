package com.thanhnb.hocmoingay.feature.player

import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.ui.Settle
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Check
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import com.thanhnb.hocmoingay.core.lesson.MASTERED
import com.thanhnb.hocmoingay.core.lesson.codePending
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.OrderLines
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.Sunrise
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.TickUpNumber
import com.thanhnb.hocmoingay.core.ui.shared
import com.thanhnb.hocmoingay.feature.Placeholder
import com.thanhnb.hocmoingay.feature.player.cards.CardCtx
import com.thanhnb.hocmoingay.feature.player.cards.CardView
import kotlinx.serialization.json.JsonObject

@Composable
fun PlayerScreen(vm: PlayerViewModel, lessonId: String, onBack: () -> Unit, onOpenEditor: (String) -> Unit, onOpenLesson: (String) -> Unit = {}) {
    val body = vm.body.collectAsStateWithLifecycle().value
    val missing = vm.missing.collectAsStateWithLifecycle().value
    val q = vm.queue.collectAsStateWithLifecycle().value
    val started = vm.started.collectAsStateWithLifecycle().value
    val online = vm.online.collectAsStateWithLifecycle().value
    val states = vm.cardState.collectAsStateWithLifecycle().value
    val track = vm.track.collectAsStateWithLifecycle().value
    val grammar = vm.grammar.collectAsStateWithLifecycle().value
    val haptic = LocalHapticFeedback.current
    if (missing) return Placeholder("Không mở được bài", "Bài chưa có trên máy hoặc dữ liệu hỏng. Mở lại app khi có mạng để kéo giáo trình mới.")
    if (body == null) return Box(Modifier.fillMaxSize())
    ProvideTrack(if (track == "english") Track.ENGLISH else Track.CODE) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(q.progress, onBack)
            when {
                !started -> Intro(body, lessonId, grammar, onOpenLesson, vm::start)
                q.finished -> Done(q.score, body.codePending(states), body.hasReview, body.kind == "checkpoint", onBack)
                else -> {
                    // số trang tăng khi card sai được thêm vào cuối: lambda phải đọc giá trị mới, không phải q lúc dựng đầu
                    val pages by rememberUpdatedState(q.order.size)
                    val pager = rememberPagerState { pages }
                    LaunchedEffect(q.pos) {
                        pager.animateScrollToPage(q.pos, animationSpec = tween(340, easing = Settle))
                        pager.scrollToPage(q.pos) // chốt đúng trang nếu animation bị cắt ngang
                    }
                    val result = q.results[q.pos]
                    LaunchedEffect(q.pos, result) {
                        if (result != null) haptic.performHapticFeedback(if (result) HapticFeedbackType.Confirm else HapticFeedbackType.Reject)
                    }
                    HorizontalPager(pager, Modifier.weight(1f), userScrollEnabled = false, key = { it }, pageSpacing = 24.dp, verticalAlignment = Alignment.Top) { page ->
                        val idx = q.order.getOrNull(page) ?: return@HorizontalPager
                        val card = body.cards[idx]
                        val ctx = CardCtx(
                            lessonId = lessonId, cardKey = card.key, seed = "$lessonId#${card.key}#$page", online = online, api = vm.api,
                            state = states[card.key] as? JsonObject ?: JsonObject(emptyMap()),
                            save = { vm.saveCard(card.key, it) }, openEditor = onOpenEditor,
                            answered = page in q.results, onAnswer = { ok, graded -> if (page == q.pos) vm.answer(ok, graded) }, active = page == q.pos,
                        )
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
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

@Composable
private fun TopBar(progress: Float, onBack: () -> Unit) {
    val p by animateFloatAsState(progress, tween(400, easing = Settle), label = "tiến độ")
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Đóng bài") }
        Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(cs.surfaceContainerHigh)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(CircleShape).background(LocalTrack.current.accent))
        }
    }
}

@Composable
private fun Intro(b: LessonBody, lessonId: String, grammar: List<Pair<String, String>>, onOpenLesson: (String) -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.weight(1f))
        Text(b.title, Modifier.shared("lesson-$lessonId"), style = MaterialTheme.typography.displaySmall)
        Pushable(null, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp), Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Học xong bài này, bạn sẽ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.height(6.dp))
                Text(b.canDo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Text("${b.cards.size} thẻ · khoảng ${b.estimateMin} phút", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        grammar.forEach { (id, title) ->
            OutlinedButton({ onOpenLesson(id) }, Modifier.fillMaxWidth()) {
                Text("Xem ngữ pháp: $title", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.weight(1f))
        PushButton("Bắt đầu", onStart, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward)
    }
}

/** Thanh dưới: trả lời xong thì cả thanh đổi nền xanh/đất nung, kèm một dòng nói đúng hay sai (chỉ câu có chấm). */
@Composable
private fun BottomBar(q: PlayerQueue, onNext: () -> Unit) {
    val r = q.results[q.pos]
    val graded = q.pos in q.graded
    val f = LocalFun.current
    val cs = MaterialTheme.colorScheme
    var last by remember { mutableStateOf(true) } // giữ chữ khi thanh đang thu lại
    if (r != null) last = r
    val tone = when { r == null || !graded -> cs.background; r -> f.mintContainer; else -> f.coralContainer }
    val bg by animateColorAsState(tone, tween(220), label = "nền phản hồi")
    // luôn giữ chỗ: pager đổi chiều cao giữa lúc cuộn thì dừng lệch giữa hai trang
    Column(
        Modifier.fillMaxWidth().background(bg).heightIn(min = 76.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            r != null && graded,
            enter = fadeIn(tween(160)) + expandVertically(tween(220, easing = Settle)),
            exit = fadeOut(tween(100)) + shrinkVertically(tween(160)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (last) Icons.Filled.Check else Icons.Filled.Close, null, Modifier.size(22.dp), tint = if (last) f.mint else f.coral)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (last) "Chính xác" else "Chưa đúng. Thẻ này sẽ quay lại ở cuối bài.",
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    color = if (last) cs.onSurface else f.onCoralContainer,
                )
            }
        }
        if (r == true) PushButton("Tiếp", onNext, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward)
        else if (r == false) PushButton("Tiếp", onNext, Modifier.fillMaxWidth(), color = f.coral, contentColor = f.onCoral)
    }
}

/** Khoảnh khắc mạnh của màn: khối coral có điểm đếm tăng dần. */
@Composable
private fun Done(score: Int, codePending: Boolean, hasReview: Boolean, checkpoint: Boolean, onBack: () -> Unit) {
    val f = LocalFun.current
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.weight(1f))
        Sunrise(Modifier.size(96.dp))
        Text(if (checkpoint) "Xong bài kiểm!" else "Xong bài!", style = MaterialTheme.typography.displaySmall)
        Pushable(null, f.coralContainer, RoundedCornerShape(18.dp), Modifier.fillMaxWidth(), edge = f.coral) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    TickUpNumber(score, MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold), f.onCoralContainer)
                    Text("%", style = MaterialTheme.typography.headlineMedium, color = f.onCoralContainer)
                }
                Text("trả lời đúng ngay lần đầu", style = MaterialTheme.typography.bodyLarge, color = f.onCoralContainer)
            }
        }
        val note = when {
            checkpoint && score >= MASTERED -> "Từ 80% trở lên: chương này đã thành thạo."
            checkpoint -> "Chưa tới 80%. Ôn lại các thẻ của chương, mai làm lại nhé — chương sau vẫn mở."
            codePending -> "Bài code còn để sau — nộp đạt thì bài mới tính là xong."
            hasReview -> "Các thẻ đánh dấu ôn tập đã vào lịch ôn."
            else -> null
        }
        note?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.weight(1f))
        PushButton("Về đề cương", onBack, Modifier.fillMaxWidth())
    }
}
