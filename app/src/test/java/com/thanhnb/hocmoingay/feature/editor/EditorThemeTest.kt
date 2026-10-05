package com.thanhnb.hocmoingay.feature.editor

import androidx.compose.ui.graphics.Color
import com.thanhnb.hocmoingay.core.lesson.LessonJson
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EditorThemeTest {
    private val c = EditorColors(
        dark = false, bg = Color(0xFFFFFDF7), fg = Color(0xFF12171B), lineNo = Color(0xFF81878D), lineBg = Color(0xFFF3F0E6),
        selection = Color(0xFFEAD9FF), cursor = Color(0xFF7A5700), keyword = Color(0xFF773AC1), string = Color(0xFF006CA2),
        comment = Color(0xFF4F565E), number = Color(0xFF7A5700), type = Color(0xFF006CA2), function = Color(0xFF12171B), bar = Color(0xFFEEEBDF),
    )

    @Test fun themeCoDuKhoaMau() {
        val j = LessonJson.parseToJsonElement(tmTheme(c)).jsonObject
        assertEquals("light", j["type"]!!.jsonPrimitive.content)
        assertEquals("#FFFDF7", j["colors"]!!.jsonObject["editor.background"]!!.jsonPrimitive.content)
        assertEquals(6, j["tokenColors"]!!.jsonArray.size)
    }

    @Test fun scopeTheoNgonNgu() {
        assertEquals("source.cs", scopeOf("csharp"))
        assertEquals("source.sql", scopeOf("sqlite3"))
        assertEquals("source.shell", scopeOf("bash"))
    }

    @Test fun phimPhuCoTabVaNgoac() {
        assertEquals("⇥" to "    ", symbolsFor("python").first())
        assertTrue(symbolsFor("rust").any { it.first == "{" })
        assertTrue(symbolsFor("postgres").any { it.first == ";" })
    }

    // thư mục làm việc của test JVM là app/
    @Test fun moiNgonNguChayDuocDeuCoGrammar() {
        val langs = LessonJson.parseToJsonElement(File("src/main/assets/textmate/languages.json").readText()).jsonObject["languages"]!!.jsonArray
        val scopes = langs.map { it.jsonObject["scopeName"]!!.jsonPrimitive.content }.toSet()
        for (l in APP_LANGS) assertTrue("$l → ${scopeOf(l)}", scopeOf(l) in scopes)
        for (g in langs) assertTrue(File("src/main/assets/" + g.jsonObject["grammar"]!!.jsonPrimitive.content).exists())
    }
}
