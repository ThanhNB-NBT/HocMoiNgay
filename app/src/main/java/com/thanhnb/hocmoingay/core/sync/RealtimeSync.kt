package com.thanhnb.hocmoingay.core.sync

import android.util.Log
import com.thanhnb.hocmoingay.core.db.LEARNER_TABLES
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/** postgres_changes trên các bảng người học khi app ở foreground; mất kết nối thì SyncWorker vẫn bù. */
class RealtimeSync(private val supabase: SupabaseClient, private val engine: SyncEngine) {
    suspend fun run(userId: String): Nothing = coroutineScope {
        val ch = supabase.channel("learner-$userId")
        for (table in LEARNER_TABLES) {
            ch.postgresChangeFlow<PostgresAction>(schema = "public") {
                this.table = table
                filter("user_id", FilterOperator.EQ, userId)
            }.onEach { a ->
                val row: JsonObject? = when (a) {
                    is PostgresAction.Insert -> a.record
                    is PostgresAction.Update -> a.record
                    else -> null // xoá mềm là UPDATE; DELETE thật không xảy ra
                }
                if (row != null) {
                    mergeSafely { engine.mergeRemote(table, row) }
                }
            }.launchIn(this)
        }
        try {
            ch.subscribe()
            awaitCancellation()
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(ch) }
        }
    }
}

/** Một hàng hỏng không được làm sập kênh; nhưng huỷ coroutine thì phải nổi lên để kênh dừng đúng. */
internal suspend fun mergeSafely(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w("RealtimeSync", "gộp hàng realtime lỗi", e)
    }
}
