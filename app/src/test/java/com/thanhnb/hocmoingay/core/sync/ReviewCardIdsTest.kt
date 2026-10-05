package com.thanhnb.hocmoingay.core.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewCardIdsTest {
    @Test fun khopPythonUuid5() {
        assertEquals("f92f0cc2-a89c-5225-b32e-aea230bc2191",
            ReviewCardIds.of("11111111-1111-1111-1111-111111111111", "python/nhap-mon/lam-quen/nhap-du-lieu#doan"))
        assertEquals("d89e98d2-575e-584e-99f8-f8905a98e9be",
            ReviewCardIds.of("22222222-2222-2222-2222-222222222222", "python/nhap-mon/lam-quen/nhap-du-lieu#doan"))
        assertEquals("5e9d9af4-7f43-5b6f-8cd3-091e59317ea6",
            ReviewCardIds.of("11111111-1111-1111-1111-111111111111", "english-work/A2/trao-doi-trong-nhom/phong-van-gioi-thieu#v_currently"))
    }
}
