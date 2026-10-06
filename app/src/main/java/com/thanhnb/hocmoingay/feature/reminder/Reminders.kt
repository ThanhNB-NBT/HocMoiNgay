package com.thanhnb.hocmoingay.feature.reminder

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/** Thời gian chờ tới lần "HH:mm" kế tiếp sau [now] (đúng giờ thì sang ngày mai). */
fun delayTo(hhmm: String, now: ZonedDateTime): Duration {
    var next = now.toLocalDate().atTime(LocalTime.parse(hhmm)).atZone(now.zone)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next)
}

/** Giờ đang có lịch nhưng không còn trong Cài đặt: phải huỷ. */
fun staleTimes(scheduled: Collection<String>, want: List<String>): Set<String> = scheduled.toSet() - want.toSet()

/**
 * Nhắc học (spec §7.4): mỗi giờ là một OneTimeWorkRequest duy nhất `remind-HH:mm` với initialDelay tới lần kế tiếp;
 * worker chạy xong tự đặt lượt sau. Doze có thể làm lệch vài phút.
 */
class Reminders(private val wm: WorkManager, private val clock: () -> ZonedDateTime = ZonedDateTime::now) {
    fun schedule(hhmm: String, policy: ExistingWorkPolicy) {
        val req = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayTo(hhmm, clock()).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_AT to hhmm))
            .addTag(TAG).addTag(AT + hhmm)
            .build()
        wm.enqueueUniqueWork("$TAG-$hhmm", policy, req)
    }

    /** Khớp lịch với Cài đặt: huỷ giờ đã bỏ; giờ còn giữ thì KEEP để không huỷ lượt đang chạy. */
    suspend fun sync(times: List<String>) {
        val scheduled = wm.getWorkInfosByTagFlow(TAG).first().filterNot { it.state.isFinished }
            .flatMap { it.tags }.filter { it.startsWith(AT) }.map { it.removePrefix(AT) }
        staleTimes(scheduled, times).forEach { wm.cancelUniqueWork("$TAG-$it") }
        times.forEach { schedule(it, ExistingWorkPolicy.KEEP) }
    }

    companion object {
        const val TAG = "remind"
        const val AT = "at:"
        const val KEY_AT = "at"
    }
}
