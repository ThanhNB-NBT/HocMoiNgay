package com.thanhnb.hocmoingay

import android.app.Application
import com.thanhnb.hocmoingay.core.auth.AuthRepo
import com.thanhnb.hocmoingay.core.db.HocDb
import com.thanhnb.hocmoingay.core.net.createSupabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class HocApp : Application() {
    val graph by lazy { AppGraph(this) }
}

/** DI viết tay: một đối tượng cho mỗi thứ dùng chung. */
class AppGraph(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val db = HocDb.open(app)
    val supabase = createSupabase()
    val auth = AuthRepo(supabase.auth, db, scope)
}
