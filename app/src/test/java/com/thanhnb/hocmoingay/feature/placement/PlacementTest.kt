package com.thanhnb.hocmoingay.feature.placement

import com.thanhnb.hocmoingay.core.db.PlacementEntity
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 5 cấp × 3 kỹ năng × 6 câu, giống content/english/placement.yaml; [skip] bỏ cả một ô. */
fun bank(skip: (String, String) -> Boolean = { _, _ -> false }): List<PlacementQ> = LEVELS.flatMap { l ->
    SKILLS.flatMap { s ->
        if (skip(l, s)) emptyList() else (1..6).map { i -> PlacementQ("$l-$s-0$i", l, s, PlacementBody("q", listOf("a", "b", "c"))) }
    }
}

class PlacementTest {
    @Test fun rutHaiCauMoiOCapDeTruoc() {
        val p = pickPlacement(bank(), Random(7))
        assertEquals(30, p.size)
        assertEquals(30, p.map { it.id }.toSet().size)
        assertTrue(p.groupBy { it.level to it.skill }.values.all { it.size == 2 })
        assertEquals(LEVELS.flatMap { l -> List(6) { l } }, p.map { it.level })
        assertEquals(p, pickPlacement(bank(), Random(7)))
    }

    @Test fun thieuOThiLayPhanCon() {
        assertEquals(28, pickPlacement(bank { l, s -> l == "B2" && s == "listening" }, Random(1)).size)
    }

    @Test fun cauHongBiBo() {
        val rows = listOf(
            PlacementEntity("ok", "A1", "listening", """{"q":"Q","choices":["a","b"],"answer":0,"text":"Hi"}"""),
            PlacementEntity("ngoai", "A1", "vocab", """{"q":"Q","choices":["a"],"answer":3}"""),
            PlacementEntity("hong", "A1", "vocab", "{khong phai json"),
            PlacementEntity("xoa", "A1", "vocab", """{"q":"Q","choices":["a"]}""", deleted = true),
        )
        val p = parsePlacement(rows)
        assertEquals(listOf("ok"), p.map { it.id })
        assertEquals("Hi", p.single().body.text)
    }

    @Test fun dungHetThiC1() {
        val a = pickPlacement(bank(), Random(1))
        assertEquals("C1", placementLevel(levelScores(a, a.map { it.id }.toSet())))
    }

    @Test fun capDuoiTruotThiKhongTinhCapTren() {
        val a = pickPlacement(bank(), Random(1))
        val correct = a.filter { it.level != "B1" }.map { it.id } + a.filter { it.level == "B1" }.take(4).map { it.id }
        assertEquals("A2", placementLevel(levelScores(a, correct.toSet()))) // B1 4/6 trượt, B2/C1 đúng hết vẫn không tính
    }

    @Test fun saiHetThiA1() {
        assertEquals("A1", placementLevel(levelScores(pickPlacement(bank(), Random(1)), emptySet())))
    }

    @Test fun nguong70PhanTram() {
        assertTrue(LevelScore("A1", 7, 10).passed)
        assertFalse(LevelScore("A1", 6, 10).passed)
        assertTrue(LevelScore("A1", 5, 6).passed)
        assertFalse(LevelScore("A1", 4, 6).passed)
    }

    @Test fun capKhongCoCauThiBoQua() {
        val s = listOf(LevelScore("A1", 6, 6), LevelScore("A2", 0, 0), LevelScore("B1", 6, 6), LevelScore("B2", 1, 6), LevelScore("C1", 0, 6))
        assertEquals("B1", placementLevel(s))
    }
}
