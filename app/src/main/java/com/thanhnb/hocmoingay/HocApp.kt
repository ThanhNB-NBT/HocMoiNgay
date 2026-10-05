package com.thanhnb.hocmoingay

import android.app.Application
import androidx.work.WorkManager
import com.thanhnb.hocmoingay.core.auth.AuthRepo
import com.thanhnb.hocmoingay.core.db.HocDb
import com.thanhnb.hocmoingay.core.net.createSupabase
import com.thanhnb.hocmoingay.core.sync.RealtimeSync
import com.thanhnb.hocmoingay.core.sync.RoomCursors
import com.thanhnb.hocmoingay.core.sync.SupabaseRemote
import com.thanhnb.hocmoingay.core.sync.SyncEngine
import com.thanhnb.hocmoingay.core.sync.SyncScheduler
import com.thanhnb.hocmoingay.core.sync.syncTables
import com.thanhnb.hocmoingay.feature.settings.SettingsRepo
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map

class HocApp : Application() {
    val graph by lazy { AppGraph(this) }
}

/** DI viết tay: một đối tượng cho mỗi thứ dùng chung. */
class AppGraph(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = HocDb.open(app)
    val supabase = createSupabase()
    val auth = AuthRepo(supabase.auth, db, scope)
    val sync = SyncEngine(SupabaseRemote(supabase.postgrest), RoomCursors(db.syncState()), syncTables(db))
    val scheduler = SyncScheduler(WorkManager.getInstance(app))
    val realtime = RealtimeSync(supabase, sync)
    val settings = db.learner().let { l ->
        SettingsRepo(
            l.observeSettings(), l::settings, { l.upsertSettings(listOf(it)) }, auth::currentUserId, scheduler::afterWrite,
            observePulled = db.syncState().observeCursor("settings").map { it != null },
        )
    }
}
