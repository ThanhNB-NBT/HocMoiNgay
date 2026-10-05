package com.thanhnb.hocmoingay.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track

/** Khung tạm cho màn chưa làm; plan c/d/e thay bằng màn thật. */
@Composable
fun Placeholder(title: String, note: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun TodayPlaceholder() = Placeholder("Hôm nay", "Hàng đợi học hôm nay — giai đoạn e") {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TrackChip(Track.CODE, "Lập trình")
        TrackChip(Track.ENGLISH, "Tiếng Anh")
    }
}

@Composable
private fun TrackChip(track: Track, label: String) = ProvideTrack(track) {
    val t = LocalTrack.current
    Surface(color = t.container, contentColor = t.onContainer, shape = MaterialTheme.shapes.large) {
        Text(label, Modifier.padding(horizontal = 20.dp, vertical = 12.dp), style = MaterialTheme.typography.titleMedium)
    }
}
