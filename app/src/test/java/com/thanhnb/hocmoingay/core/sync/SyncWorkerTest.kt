package com.thanhnb.hocmoingay.core.sync

import androidx.work.ListenableWorker
import com.thanhnb.hocmoingay.core.auth.AuthState
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncWorkerTest {
    private var calls = 0

    @Test fun chuaDangNhapThiKhongSync() = runTest {
        assertEquals(ListenableWorker.Result.success(), runSync(AuthState.SignedOut, 0) { calls++ })
        assertEquals(0, calls)
    }

    @Test fun sessionChuaNapXongThiThuLai() = runTest {
        assertEquals(ListenableWorker.Result.retry(), runSync(AuthState.Loading, 0) { calls++ })
        assertEquals(0, calls)
    }

    @Test fun thanhCong() = runTest {
        assertEquals(ListenableWorker.Result.success(), runSync(AuthState.SignedIn("u1"), 0) { calls++ })
        assertEquals(1, calls)
    }

    @Test fun loiThiThuLaiToiDa5Lan() = runTest {
        val fail: suspend () -> Unit = { throw IOException("mất mạng") }
        assertEquals(ListenableWorker.Result.retry(), runSync(AuthState.SignedIn("u1"), 4, fail))
        assertEquals(ListenableWorker.Result.failure(), runSync(AuthState.SignedIn("u1"), 5, fail))
    }
}
