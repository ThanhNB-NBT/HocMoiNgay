package com.thanhnb.hocmoingay.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTest {
    @Test fun soTungSoKhongSoChuoi() {
        assertTrue(isNewer("0.10.0", "0.9.9"))
        assertTrue(isNewer("1.0", "0.9.9"))
        assertTrue(isNewer("0.2.0", "0.1.0"))
    }

    @Test fun bangHoacCuHonThiKhongHoi() {
        assertFalse(isNewer("0.1.0", "0.1.0"))
        assertFalse(isNewer("0.1", "0.1.0"))
        assertFalse(isNewer("0.1.0", "0.2.0"))
        assertFalse(isNewer("", "0.1.0")) // tag lạ, không có số
    }
}
