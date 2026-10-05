package com.thanhnb.hocmoingay.core.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SyncScheduler(private val wm: WorkManager) {
    private val net = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Mở app / quay lại foreground. */
    fun now() = enqueue(0)

    /** Sau mỗi lần ghi local: 10s, REPLACE để gộp các lần ghi sát nhau (spec §7.3). */
    fun afterWrite() = enqueue(10)

    fun periodic() {
        wm.enqueueUniquePeriodicWork(
            "sync-periodic", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS).setConstraints(net).build(),
        )
    }

    private fun enqueue(delaySeconds: Long) {
        wm.enqueueUniqueWork(
            "sync", ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(net)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }
}
