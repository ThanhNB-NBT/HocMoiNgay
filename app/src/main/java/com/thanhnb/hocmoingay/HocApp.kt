package com.thanhnb.hocmoingay

import android.app.Application
import androidx.work.WorkManager
import com.thanhnb.hocmoingay.core.auth.AuthRepo
import com.thanhnb.hocmoingay.core.db.HocDb
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import com.thanhnb.hocmoingay.core.net.ApiHttp
import com.thanhnb.hocmoingay.core.net.Assets
import com.thanhnb.hocmoingay.core.net.CodeApi
import com.thanhnb.hocmoingay.core.net.NetState
import com.thanhnb.hocmoingay.core.net.createSupabase
import com.thanhnb.hocmoingay.core.review.ReviewRepo
import com.thanhnb.hocmoingay.core.sync.RealtimeSync
import com.thanhnb.hocmoingay.core.sync.RoomCursors
import com.thanhnb.hocmoingay.core.sync.SupabaseRemote
import com.thanhnb.hocmoingay.core.sync.SyncEngine
import com.thanhnb.hocmoingay.core.sync.SyncScheduler
import com.thanhnb.hocmoingay.core.sync.dbTx
import com.thanhnb.hocmoingay.core.sync.syncTables
import com.thanhnb.hocmoingay.feature.settings.SettingsRepo
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import java.io.File
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
    val lessons = LessonRepo(
        lesson = db.curriculum()::lesson,
        track = { db.curriculum().course(it)?.track },
        getProgress = { db.learner().progressByKeys(listOf(it)).firstOrNull() },
        putProgress = { db.learner().upsertProgress(listOf(it)) },
        cardsByIds = db.learner()::reviewCardsByKeys,
        putCards = db.learner()::upsertReviewCards,
        tx = dbTx(db),
        userId = auth::currentUserId,
        afterWrite = scheduler::afterWrite,
    )
    val log = db.learner().let { l ->
        DailyLogRepo({ l.dailyLogByKeys(listOf(it)).firstOrNull() }, { l.upsertDailyLog(listOf(it)) }, dbTx(db), auth::currentUserId, scheduler::afterWrite)
    }
    val reviews = db.learner().let { l ->
        ReviewRepo(l::dueRecall, l::reviewCardsByKeys, { l.upsertReviewCards(listOf(it)) }, log, dbTx(db), scheduler::afterWrite)
    }
    val net = NetState(app, scope)
    val code = CodeApi { fn, body ->
        try {
            supabase.functions.invoke(fn, body).bodyAsText()
        } catch (e: RestException) {
            throw ApiHttp(e.statusCode, "${e.error} ${e.description.orEmpty()}")
        }
    }
    val assets = Assets(File(app.cacheDir, "assets")) { supabase.storage.from("content").downloadAuthenticated(it) }
}
