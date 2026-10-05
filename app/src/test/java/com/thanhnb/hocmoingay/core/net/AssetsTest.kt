package com.thanhnb.hocmoingay.core.net

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AssetsTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun taiMotLanRoiDocCache() = runTest {
        var calls = 0
        val a = Assets(tmp.root) { key -> calls++; assertEquals("_sample-code/dot.png", key); byteArrayOf(1, 2) }
        assertArrayEquals(byteArrayOf(1, 2), a.bytes("assets/_sample-code/dot.png"))
        assertArrayEquals(byteArrayOf(1, 2), a.bytes("assets/_sample-code/dot.png"))
        assertEquals(1, calls)
    }

    @Test fun loiTaiTraNull() = runTest { assertNull(Assets(tmp.root) { error("404") }.bytes("assets/x/y.png")) }

    @Test fun chanDuongDanLeoThuMuc() = runTest { assertNull(Assets(tmp.root) { byteArrayOf(1) }.bytes("assets/../secret")) }
}
