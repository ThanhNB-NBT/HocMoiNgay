package com.thanhnb.hocmoingay.core.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeSyncTest {
    @Test fun loiThuongBiNuot() = runTest { mergeSafely { error("hàng hỏng") } }

    @Test fun huyThiNemTiep() = runTest {
        val thrown = runCatching { mergeSafely { throw CancellationException("huỷ") } }.exceptionOrNull()
        assertTrue(thrown is CancellationException)
    }
}
