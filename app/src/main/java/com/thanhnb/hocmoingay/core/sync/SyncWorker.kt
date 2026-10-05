package com.thanhnb.hocmoingay.core.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.thanhnb.hocmoingay.HocApp
import com.thanhnb.hocmoingay.core.auth.AuthState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Tách khỏi Worker để test trên JVM. Lỗi thì retry (WorkManager lo backoff mũ), quá 5 lần thì đợi lượt định kỳ. */
suspend fun runSync(state: AuthState, attempt: Int, sync: suspend () -> Unit): ListenableWorker.Result = when (state) {
    AuthState.SignedOut -> ListenableWorker.Result.success()
    AuthState.Loading -> ListenableWorker.Result.retry()
    is AuthState.SignedIn -> try {
        sync()
        ListenableWorker.Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("SyncWorker", "sync lỗi (lần ${attempt + 1})", e)
        if (attempt < 5) ListenableWorker.Result.retry() else ListenableWorker.Result.failure()
    }
}

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val g = (applicationContext as HocApp).graph
        // Process có thể do WorkManager khởi động: chờ supabase-kt nạp session từ bộ nhớ máy
        val state = withTimeoutOrNull(10_000) { g.auth.state.first { it != AuthState.Loading } } ?: AuthState.Loading
        return runSync(state, runAttemptCount) { g.sync.syncAll() }
    }
}
