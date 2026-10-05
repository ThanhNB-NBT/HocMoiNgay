package com.thanhnb.hocmoingay.core.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {
    private fun ratio(a: Color, b: Color): Double {
        val x = a.luminance().toDouble()
        val y = b.luminance().toDouble()
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    @Test fun moiCapChuNenDatTuongPhan() {
        for (dark in listOf(false, true)) {
            for ((ten, s) in listOf("hai-sac" to twoToneScheme(dark), "ide" to ideScheme(dark))) {
                val pairs = mapOf(
                    "primary" to (s.onPrimary to s.primary),
                    "primaryContainer" to (s.onPrimaryContainer to s.primaryContainer),
                    "secondary" to (s.onSecondary to s.secondary),
                    "secondaryContainer" to (s.onSecondaryContainer to s.secondaryContainer),
                    "tertiary" to (s.onTertiary to s.tertiary),
                    "tertiaryContainer" to (s.onTertiaryContainer to s.tertiaryContainer),
                    "surface" to (s.onSurface to s.surface),
                    "chữ phụ trên container cao nhất" to (s.onSurfaceVariant to s.surfaceContainerHighest),
                    "primary trên surface" to (s.primary to s.surface),
                )
                for ((n, p) in pairs) {
                    val r = ratio(p.first, p.second)
                    assertTrue("$ten dark=$dark $n = $r", r >= 4.5)
                }
                val o = ratio(s.outline, s.surface)
                assertTrue("$ten dark=$dark outline = $o", o >= 3.0)
            }
        }
    }

    @Test fun accentMoiMangDatTuongPhan() {
        for (t in listOf(CodeLight, CodeDark, EnglishLight, EnglishDark, AmberLight, AmberDark)) {
            assertTrue("$t accent", ratio(t.onAccent, t.accent) >= 4.5)
            assertTrue("$t container", ratio(t.onContainer, t.container) >= 4.5)
        }
    }

    @Test fun mauNhanPhuDatTuongPhan() {
        for (f in listOf(FunLight, FunDark)) {
            assertTrue("$f coral", ratio(f.onCoral, f.coral) >= 4.5)
            assertTrue("$f coral container", ratio(f.onCoralContainer, f.coralContainer) >= 4.5)
        }
    }

    @Test fun cheDoToi() {
        assertTrue(isDark(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(isDark(ThemeMode.SYSTEM, systemDark = false))
        assertTrue(isDark(ThemeMode.DARK, systemDark = false))
        assertFalse(isDark(ThemeMode.LIGHT, systemDark = true))
    }

    @Test fun mauToCodeDatTuongPhan() {
        for (dark in listOf(false, true)) {
            val s = twoToneScheme(dark)
            for ((ten, c) in listOf("từ khoá" to s.secondary, "chuỗi" to s.tertiary, "số" to s.primary, "comment" to s.onSurfaceVariant)) {
                assertTrue("$dark $ten", ratio(c, s.surfaceContainerLowest) >= 4.5)
            }
        }
    }
}
