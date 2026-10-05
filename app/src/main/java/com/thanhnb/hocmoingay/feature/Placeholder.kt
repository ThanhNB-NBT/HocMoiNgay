package com.thanhnb.hocmoingay.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.ui.Flower
import com.thanhnb.hocmoingay.core.theme.TrackColors

/** Khung tạm cho màn chưa làm; plan c/d/e thay bằng màn thật. */
@Composable
fun Placeholder(
    title: String,
    note: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().safeDrawingPadding().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Box(Modifier.size(112.dp).background(MaterialTheme.colorScheme.primaryContainer, Flower), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.height(24.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

/** Tiêu đề lớn đầu mỗi tab, cùng kiểu chữ với màn đăng nhập. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, eyebrow: String? = null, modifier: Modifier = Modifier, inset: Dp = 20.dp) {
    Column(modifier.padding(start = inset, end = inset, top = 24.dp, bottom = 16.dp)) {
        eyebrow?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
        }
        Text(title, style = MaterialTheme.typography.displaySmall)
        subtitle?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Nhãn viên thuốc mang màu của một mảng ("Lập trình", "Tiếng Anh"). */
@Composable
fun TrackLabel(text: String, t: TrackColors) =
    Surface(color = t.accent, contentColor = t.onAccent, shape = CircleShape) {
        Text(text, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
