package com.thanhnb.hocmoingay.core.review

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveTest {
    @Test fun khongGoiYKhongTruotLaDe() = assertEquals(Rating.EASY, resolveRating(0, 0))
    @Test fun khongGoiYNhungCoTruotLaNho() = assertEquals(Rating.GOOD, resolveRating(0, 2))
    @Test fun motGoiYLaKho() = assertEquals(Rating.HARD, resolveRating(1, 0))
    @Test fun haiGoiYTroLenLaQuen() {
        assertEquals(Rating.AGAIN, resolveRating(2, 0))
        assertEquals(Rating.AGAIN, resolveRating(3, 1))
    }
}
