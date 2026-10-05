package com.thanhnb.hocmoingay.core.auth

import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTest {
    private fun session(uid: String?) = UserSession(
        accessToken = "a", refreshToken = "r", expiresIn = 3600, tokenType = "bearer",
        user = uid?.let { UserInfo(aud = "authenticated", id = it) },
    )

    @Test fun dangKhoiDong() = assertEquals(AuthState.Loading, authStateOf(SessionStatus.Initializing, owner = "u1"))
    @Test fun daDangNhap() = assertEquals(AuthState.SignedIn("u2"), authStateOf(SessionStatus.Authenticated(session("u2")), owner = "u1"))
    @Test fun chuaDangNhap() = assertEquals(AuthState.SignedOut, authStateOf(SessionStatus.NotAuthenticated(), owner = "u1"))

    @Suppress("DEPRECATION")
    @Test fun refreshFailureVanVaoAppNeuBietChuMay() {
        val s = SessionStatus.RefreshFailure(RefreshFailureCause.NetworkError(IOException("offline")))
        assertEquals(AuthState.SignedIn("u1"), authStateOf(s, owner = "u1"))
        assertEquals(AuthState.SignedOut, authStateOf(s, owner = null))
    }

    @Test fun needsWipeKhiDoiTaiKhoan() = assertTrue(needsWipe(owner = "u1", userId = "u2"))
    @Test fun needsWipeKhongKhiCungTaiKhoan() = assertFalse(needsWipe(owner = "u1", userId = "u1"))
    @Test fun needsWipeKhongKhiMayMoi() = assertFalse(needsWipe(owner = null, userId = "u1"))

    @Test fun loiMang() = assertEquals("Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.", loginError(IOException("x")))
    @Test fun loiKhac() = assertEquals("Đăng nhập lỗi: lạ", loginError(IllegalStateException("lạ")))
}
