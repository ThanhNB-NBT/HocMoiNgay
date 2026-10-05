package com.thanhnb.hocmoingay.core.auth

import com.thanhnb.hocmoingay.core.db.HocDb
import com.thanhnb.hocmoingay.core.db.LEARNER_TABLES
import com.thanhnb.hocmoingay.core.db.OWNER_KEY
import com.thanhnb.hocmoingay.core.db.SyncStateEntity
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String) : AuthState
}

/**
 * RefreshFailure = session hết hạn và thư viện đang thử làm mới (thường do mất mạng). App offline-first nên vẫn cho vào nếu đã biết chủ máy.
 * Chỉ NotAuthenticated (refresh token bị từ chối, hoặc chưa đăng nhập) mới về màn đăng nhập; dữ liệu local giữ nguyên.
 */
fun authStateOf(status: SessionStatus, owner: String?): AuthState = when (status) {
    SessionStatus.Initializing -> AuthState.Loading
    is SessionStatus.NotAuthenticated -> AuthState.SignedOut
    is SessionStatus.RefreshFailure -> owner?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut
    is SessionStatus.Authenticated ->
        (status.session.user?.id ?: owner)?.let { AuthState.SignedIn(it) } ?: AuthState.Loading
}

fun needsWipe(owner: String?, userId: String) = owner != null && owner != userId

fun loginError(e: Throwable): String = when {
    e is AuthRestException && e.errorCode == AuthErrorCode.InvalidCredentials -> "Sai email hoặc mật khẩu."
    e is AuthRestException -> "Máy chủ từ chối đăng nhập: ${e.errorDescription}"
    e is HttpRequestException || e is HttpRequestTimeoutException || e is IOException ->
        "Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại."
    else -> "Đăng nhập lỗi: ${e.message ?: e::class.simpleName}"
}

class AuthRepo(auth: Auth, private val db: HocDb, scope: CoroutineScope) {
    val state: StateFlow<AuthState> = auth.sessionStatus
        .map { s ->
            authStateOf(s, db.syncState().cursor(OWNER_KEY)).also { if (it is AuthState.SignedIn) claim(it.userId) }
        }
        .stateIn(scope, SharingStarted.Eagerly, AuthState.Loading)

    fun currentUserId(): String? = (state.value as? AuthState.SignedIn)?.userId

    /** Máy đổi tài khoản: xoá dữ liệu người học cũ TRƯỚC khi phát SignedIn. Nếu không, upsert bị RLS chặn và SyncWorker kẹt mãi. */
    private suspend fun claim(userId: String) {
        val owner = db.syncState().cursor(OWNER_KEY)
        if (owner == userId) return
        if (needsWipe(owner, userId)) {
            db.learner().wipeAll()
            db.syncState().clear(LEARNER_TABLES)
        }
        db.syncState().put(SyncStateEntity(OWNER_KEY, userId)) // ghi sau cùng: lỡ chết giữa chừng thì lần sau xoá lại
    }
}
