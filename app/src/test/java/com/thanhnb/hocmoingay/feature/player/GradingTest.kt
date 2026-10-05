package com.thanhnb.hocmoingay.feature.player

import com.thanhnb.hocmoingay.core.lesson.Issue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradingTest {
    @Test fun quizSoTapHop() {
        assertTrue(quizOk(setOf(0), listOf(0)))
        assertFalse(quizOk(setOf(0, 1), listOf(0)))
        assertTrue(quizOk(setOf(2, 0), listOf(0, 2)))
    }

    @Test fun predictBoKhoangTrangThua() = assertTrue(predictOk("  50\n", "50"))

    @Test fun fillNhanMotTrongCacDapAn() {
        assertTrue(fillOk(listOf(" 0 ", "+="), listOf(listOf("0"), listOf("+=", "= tong +"))))
        assertFalse(fillOk(listOf("0"), listOf(listOf("0"), listOf("+="))))
    }

    @Test fun xepDongDungThuTuVaKhongLanDongNhieu() {
        val lines = listOf("tong = 0", "for g in a:", "    tong += g")
        assertTrue(orderOk(lines, lines))
        assertFalse(orderOk(listOf("tong = 0", "for g in a:", "    tong = g"), lines))
        assertFalse(orderOk(lines.dropLast(1), lines))
    }

    @Test fun dienTokenBiChe() {
        assertTrue(fadeOk(mapOf(2 to " > 10", 3 to "+="), mapOf(2 to "> 10", 3 to "+=")))
        assertFalse(fadeOk(mapOf(2 to "> 10"), mapOf(2 to "> 10", 3 to "+=")))
    }

    @Test fun reviewCanMoiVanDeMotDongVaKhongChonThua() {
        val issues = listOf(Issue(listOf(2), "chia 0"), Issue(listOf(3, 4), "tên"))
        assertTrue(reviewOk(setOf(2, 4), issues))
        assertFalse(reviewOk(setOf(2), issues))
        assertFalse(reviewOk(setOf(2, 3, 1), issues))
    }

    @Test fun hoanViOnDinhTheoSeedVaDuPhanTu() {
        val a = permutation(5, "bai#k")
        assertEquals(a, permutation(5, "bai#k"))
        assertEquals((0 until 5).toSet(), a.toSet())
        assertNotEquals((0 until 20).toList(), permutation(20, "bai#k"))
    }

    @Test fun tachChoTrong() = assertEquals(listOf("t = ", "\nu ", " g"), blankParts("t = ___\nu ___ g"))
}
