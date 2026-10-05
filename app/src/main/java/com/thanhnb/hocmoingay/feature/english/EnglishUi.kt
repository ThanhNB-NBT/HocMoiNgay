package com.thanhnb.hocmoingay.feature.english

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.thanhnb.hocmoingay.core.speech.LocalTts
import com.thanhnb.hocmoingay.core.speech.Tts
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.pathIcon

// path từ Material Icons "volume_up" và "mic" (Apache 2.0)
val SpeakerIcon = pathIcon("Speaker", "M3,9v6h4l5,5L12,4L7,9L3,9zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z")
val MicIcon = pathIcon("Mic", "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z")

val SPEEDS = listOf(0.8f to "0,8×", 1f to "1×", 1.2f to "1,2×")

/** Không có bộ đọc (hoặc chưa cấp LocalTts) coi như UNAVAILABLE: màn hiện chữ thay cho phần nghe. */
@Composable
fun ttsStatus(): Tts.Status {
    val tts = LocalTts.current ?: return Tts.Status.UNAVAILABLE
    return tts.status.collectAsStateWithLifecycle().value
}

/**
 * Nút nghe + 3 tốc độ (spec §7.2). [autoPlay] đọc một lần khi hiện (truyền `ctx.active` trong trình phát).
 * Máy thiếu giọng: nút mở màn cài giọng. Không có bộ đọc: không vẽ gì, màn gọi [ttsStatus] để hiện chữ.
 */
@Composable
fun SpeakerBar(text: String, autoPlay: Boolean = false, initialRate: Float = 1f) {
    val tts = LocalTts.current ?: return
    val status by tts.status.collectAsStateWithLifecycle()
    val speaking by tts.speaking.collectAsStateWithLifecycle()
    var rate by rememberSaveable { mutableFloatStateOf(initialRate) }
    val owner = remember { Any() }
    val ctx = LocalContext.current
    val t = LocalTrack.current
    LaunchedEffect(status, text, autoPlay) { if (autoPlay && status == Tts.Status.READY) tts.speak(text, rate, owner) }
    DisposableEffect(Unit) { onDispose { tts.stop(owner) } }
    when (status) {
        Tts.Status.UNAVAILABLE -> {}
        Tts.Status.MISSING_DATA -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Máy chưa có giọng đọc tiếng Anh, nên tạm hiện chữ thay cho phần nghe.", style = MaterialTheme.typography.bodyMedium)
            PushButton("Cài giọng đọc", { tts.installVoice(ctx) })
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pushable({ tts.speak(text, rate, owner) }, t.accent, CircleShape, Modifier.size(56.dp), enabled = status == Tts.Status.READY) {
                Icon(
                    if (speaking) Icons.Filled.Refresh else SpeakerIcon, if (speaking) "Nghe lại" else "Nghe",
                    tint = t.onAccent, modifier = Modifier.align(Alignment.Center).size(28.dp),
                )
            }
            SPEEDS.forEach { (r, label) ->
                FilterChip(selected = rate == r, onClick = { rate = r; tts.speak(text, r, owner) }, label = { Text(label) })
            }
        }
    }
}

/** Chạy [action] nếu đã có quyền micro; chưa có thì hỏi, đồng ý thì chạy, từ chối thì [onDenied]. */
@Composable
fun rememberMic(onDenied: () -> Unit, action: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val act by rememberUpdatedState(action)
    val deny by rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) act() else deny() }
    return {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) act()
        else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }
}

@Composable
fun MicButton(on: Boolean, onClick: () -> Unit) {
    val f = LocalFun.current
    val t = LocalTrack.current
    Pushable(onClick, if (on) f.coral else t.accent, CircleShape, Modifier.size(72.dp)) {
        Icon(MicIcon, if (on) "Dừng nói" else "Bắt đầu nói", tint = if (on) f.onCoral else t.onAccent, modifier = Modifier.align(Alignment.Center).size(34.dp))
    }
}
