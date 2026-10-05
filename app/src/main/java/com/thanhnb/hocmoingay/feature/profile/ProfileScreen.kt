package com.thanhnb.hocmoingay.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.theme.LocalFun
import com.thanhnb.hocmoingay.core.ui.Clover
import com.thanhnb.hocmoingay.core.ui.Cookie
import com.thanhnb.hocmoingay.core.ui.Flower
import com.thanhnb.hocmoingay.core.ui.Pushable
import com.thanhnb.hocmoingay.core.ui.rise
import com.thanhnb.hocmoingay.feature.ScreenHeader

/** Khung tab Hồ sơ. Plan e thêm streak, XP, heatmap vào khối "Thành tích". */
@Composable
fun ProfileScreen(email: String?, onOpenSettings: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val fun_ = LocalFun.current
    val card = BorderStroke(2.dp, cs.outlineVariant)
    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        ScreenHeader("Hồ sơ", modifier = Modifier.rise(0), inset = 0.dp)
        Row(Modifier.rise(1), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp).background(cs.primaryContainer, Cookie), contentAlignment = Alignment.Center) {
                Text(email?.take(1)?.uppercase() ?: "?", style = MaterialTheme.typography.headlineMedium, color = cs.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(email ?: "Chưa đăng nhập", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Tiến độ đồng bộ trên mọi thiết bị", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(24.dp))
        // coral: khoảnh khắc mạnh duy nhất của màn
        Pushable(null, fun_.coralContainer, RoundedCornerShape(28.dp), Modifier.fillMaxWidth().rise(2), edge = fun_.coral) {
            Column(Modifier.padding(20.dp)) {
                Box(Modifier.size(48.dp).background(fun_.coral, Flower), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Star, null, tint = fun_.onCoral)
                }
                Spacer(Modifier.height(12.dp))
                Text("Thành tích", style = MaterialTheme.typography.headlineSmall, color = fun_.onCoralContainer)
                Text(
                    "Học xong bài đầu tiên là chuỗi ngày, XP và lịch học bắt đầu đếm.",
                    style = MaterialTheme.typography.bodyMedium, color = fun_.onCoralContainer,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Pushable(
            onOpenSettings, cs.surfaceContainerLowest, RoundedCornerShape(24.dp), Modifier.fillMaxWidth().rise(3),
            edge = cs.outlineVariant, border = card,
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).background(cs.secondaryContainer, Clover), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, null, tint = cs.onSecondaryContainer)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cài đặt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Giao diện, thời lượng, giờ nhắc, ngôn ngữ", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
