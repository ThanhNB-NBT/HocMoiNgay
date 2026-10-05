package com.thanhnb.hocmoingay.feature.editor

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val APP_LANGS = listOf("python", "javascript", "typescript", "kotlin", "java", "go", "cpp", "rust", "csharp", "postgres", "sqlite3", "bash")

fun scopeOf(lang: String): String = when (lang) {
    "javascript" -> "source.js"
    "typescript" -> "source.ts"
    "csharp" -> "source.cs"
    "postgres", "sqlite3" -> "source.sql"
    "bash" -> "source.shell"
    else -> "source.$lang"
}

/** Thanh phím phụ: (hiển thị, chèn). Tab chèn 4 dấu cách, vì Python không được trộn tab với dấu cách. */
fun symbolsFor(lang: String): List<Pair<String, String>> = listOf("⇥" to "    ") + when (lang) {
    "python" -> listOf(":", "(", ")", "[", "]", "=", "_", "\"", "'", "#", ".", ",", "+", "-", "*", "<", ">")
    "postgres", "sqlite3" -> listOf("*", "(", ")", ",", ";", "'", "=", "<", ">", ".", "_")
    "bash" -> listOf("$", "{", "}", "(", ")", "|", "\"", "'", ";", "-", "/", "[", "]", "=")
    else -> listOf("{", "}", "(", ")", ";", "=", "[", "]", "<", ">", "\"", ".", ",", ":", "&", "|", "!", "+", "-", "*", "_")
}.map { it to it }

data class EditorColors(
    val dark: Boolean, val bg: Color, val fg: Color, val lineNo: Color, val lineBg: Color, val selection: Color, val cursor: Color,
    val keyword: Color, val string: Color, val comment: Color, val number: Color, val type: Color, val function: Color, val bar: Color,
)

private fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

private fun JsonArrayBuilder.rule(scope: String, c: Color) = addJsonObject {
    put("scope", scope)
    putJsonObject("settings") { put("foreground", hex(c)) }
}

/** Theme TextMate (định dạng VS Code) dựng từ màu app, nên editor đổi theo Hai sắc / Hình nền / IDE và sáng / tối. */
fun tmTheme(c: EditorColors): String = buildJsonObject {
    put("name", "hum")
    put("type", if (c.dark) "dark" else "light")
    putJsonObject("colors") {
        put("editor.background", hex(c.bg))
        put("editor.foreground", hex(c.fg))
        put("editorLineNumber.foreground", hex(c.lineNo))
        put("editorLineNumber.activeForeground", hex(c.fg))
        put("editor.lineHighlightBackground", hex(c.lineBg))
        put("editor.selectionBackground", hex(c.selection))
        put("editorCursor.foreground", hex(c.cursor))
    }
    putJsonArray("tokenColors") {
        rule("comment", c.comment)
        rule("string", c.string)
        rule("constant.numeric", c.number)
        rule("keyword, storage", c.keyword)
        rule("entity.name.type, support.type", c.type)
        rule("entity.name.function, support.function", c.function)
    }
}.toString()

@Composable
fun editorColors(): EditorColors {
    val cs = MaterialTheme.colorScheme
    return EditorColors(
        dark = cs.surface.luminance() < 0.5f, bg = cs.surfaceContainerLowest, fg = cs.onSurface, lineNo = cs.outline,
        lineBg = cs.surfaceContainerLow, selection = cs.secondaryContainer, cursor = cs.primary,
        keyword = cs.secondary, string = cs.tertiary, comment = cs.onSurfaceVariant, number = cs.primary,
        type = cs.tertiary, function = cs.onSurface, bar = cs.surfaceContainer,
    )
}
