package com.thanhnb.hocmoingay.feature.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.thanhnb.hocmoingay.core.ui.Tile
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.thanhnb.hocmoingay.core.ui.Pushable

/** Xin POST_NOTIFICATIONS (Android 13+) bằng một thẻ trên Hôm nay; bị từ chối thì dẫn sang cài đặt thông báo của app. */
@Composable
fun NotifyCard(reminders: List<String>, modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT < 33 || reminders.isEmpty()) return
    val ctx = LocalContext.current
    fun check() = ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var granted by remember { mutableStateOf(check()) }
    var denied by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) { granted = check(); onPauseOrDispose { } } // quay lại từ cài đặt hệ thống
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it; denied = !it }
    if (granted) return
    val cs = MaterialTheme.colorScheme
    Pushable(null, cs.surfaceContainerLowest, RoundedCornerShape(16.dp), modifier.padding(top = 14.dp).fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).background(cs.secondaryContainer, Tile), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Notifications, null, tint = cs.onSecondaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Bật nhắc học", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "App nhắc lúc ${reminders.joinToString(", ")} nếu hôm đó bạn chưa học.",
                    style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Pushable(
                    {
                        if (denied) ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                        else ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    cs.secondaryContainer, CircleShape, edge = Color.Transparent,
                ) {
                    Text(
                        if (denied) "Mở cài đặt thông báo" else "Bật thông báo", Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = cs.onSecondaryContainer,
                    )
                }
            }
        }
    }
}
