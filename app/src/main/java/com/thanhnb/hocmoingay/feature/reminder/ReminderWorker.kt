package com.thanhnb.hocmoingay.feature.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.thanhnb.hocmoingay.HocApp
import com.thanhnb.hocmoingay.MainActivity
import com.thanhnb.hocmoingay.R
import com.thanhnb.hocmoingay.core.log.active
import kotlinx.coroutines.flow.first

class ReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val at = inputData.getString(Reminders.KEY_AT) ?: return Result.success()
        val g = (applicationContext as HocApp).graph
        val now = System.currentTimeMillis()
        val l = g.db.learner()
        // Có hàng settings = đã đăng nhập (đăng xuất thì wipeAll; AuthState chưa kịp nạp khi WorkManager đánh thức tiến trình).
        // Hôm nay đã học thì không nhắc (spec §7.4).
        if (l.settings() != null && g.log.day(now)?.active() != true) showReminder(applicationContext, l.observeDueCount(now).first())
        g.reminders.schedule(at, ExistingWorkPolicy.APPEND_OR_REPLACE) // lượt sau chạy sau khi lượt này xong
        return Result.success()
    }
}

private const val CHANNEL = "nhac-hoc"

fun showReminder(ctx: Context, due: Int) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    val nm = NotificationManagerCompat.from(ctx)
    if (!nm.areNotificationsEnabled()) return
    nm.createNotificationChannel(NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT).setName("Nhắc học").build())
    // như bấm icon app: đưa task đang có lên trước, không dựng lại MainActivity
    val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName) ?: Intent(ctx, MainActivity::class.java)
    val open = PendingIntent.getActivity(ctx, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    val text = if (due > 0) "Có $due thẻ đến hạn ôn và bài mới đang chờ. Vài phút là xong." else "Bài hôm nay đang chờ. Vài phút là xong."
    val n = NotificationCompat.Builder(ctx, CHANNEL)
        .setSmallIcon(R.drawable.ic_notify)
        .setContentTitle("Đến giờ học rồi")
        .setContentText(text)
        .setContentIntent(open)
        .setAutoCancel(true)
        .build()
    try { nm.notify(1, n) } catch (_: SecurityException) { } // quyền bị thu hồi giữa hai lần kiểm
}
