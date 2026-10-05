package com.thanhnb.hocmoingay.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.feature.ScreenHeader

/** Khung tab Hồ sơ. Plan e thêm streak, XP, heatmap vào thẻ "Thành tích". */
@Composable
fun ProfileScreen(email: String?, onOpenSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())) {
        ScreenHeader("Hồ sơ")
        Surface(color = cs.surfaceContainerLow, shape = RoundedCornerShape(28.dp), modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = cs.primary, contentColor = cs.onPrimary, shape = CircleShape) {
                    Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                        Text(email?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(email ?: "Chưa đăng nhập", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Đồng bộ trên mọi thiết bị", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        MenuRow(Icons.Filled.Star, "Thành tích", "Chuỗi ngày, XP và lịch học sẽ hiện ở đây.", onClick = null)
        Spacer(Modifier.height(12.dp))
        MenuRow(Icons.Filled.Settings, "Cài đặt", "Giao diện, thời lượng, giờ nhắc, ngôn ngữ", onClick = onOpenSettings)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MenuRow(icon: ImageVector, title: String, note: String, onClick: (() -> Unit)?) {
    val cs = MaterialTheme.colorScheme
    val content: @Composable () -> Unit = {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = cs.secondaryContainer, contentColor = cs.onSecondaryContainer, shape = RoundedCornerShape(14.dp)) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(icon, null) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(note, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            if (onClick != null) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = cs.onSurfaceVariant)
        }
    }
    val m = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
    val shape = RoundedCornerShape(24.dp)
    if (onClick != null) Surface(onClick = onClick, color = cs.surfaceContainerLow, shape = shape, modifier = m, content = content)
    else Surface(color = cs.surfaceContainerLow, shape = shape, modifier = m, content = content)
}
