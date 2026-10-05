package com.thanhnb.hocmoingay

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavTest {
    @Test fun chonTabKhacThiHomNayNamDuoi() {
        val s = mutableListOf<NavKey>(Today, Learn, CourseDetail("python"))
        s.selectTab(Profile)
        assertEquals(listOf<NavKey>(Today, Profile), s)
    }

    @Test fun chonHomNayChiConMotMuc() {
        val s = mutableListOf<NavKey>(Today, Review)
        s.selectTab(Today)
        assertEquals(listOf<NavKey>(Today), s)
    }
}
