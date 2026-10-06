package com.thanhnb.hocmoingay.feature.reminder

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
import androidx.compose.material3.TextButton
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
    Pushable(null, cs.surfaceContainerHigh, RoundedCornerShape(24.dp), modifier.padding(top = 14.dp).fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Bật thông báo nhắc học", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "App nhắc lúc ${reminders.joinToString(", ")} nếu hôm đó bạn chưa học.",
                style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
            )
            TextButton(onClick = {
                if (denied) ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                else ask.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text(if (denied) "Mở cài đặt thông báo" else "Bật thông báo") }
        }
    }
}
