package com.thanhnb.hocmoingay.core.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation3.ui.LocalNavAnimatedContentScope

val LocalShared = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** Phần tử dùng chung giữa hai màn (thẻ khoá → đề cương, dòng bài → trình phát). Nằm ngoài NavDisplay thì không làm gì. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.shared(key: String): Modifier {
    val s = LocalShared.current ?: return this
    val anim = LocalNavAnimatedContentScope.current
    return with(s) { this@shared.sharedBounds(rememberSharedContentState(key), anim) }
}
