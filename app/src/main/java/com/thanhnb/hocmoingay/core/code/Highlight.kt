package com.thanhnb.hocmoingay.core.code

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

enum class Tok { KEYWORD, STRING, COMMENT, NUMBER }
data class Span(val start: Int, val end: Int, val tok: Tok)

private fun kw(s: String) = s.split(' ').toSet()
private const val C_LIKE = "if else for while do return break continue switch case default new true false null this class"
private val KEYWORDS: Map<String, Set<String>> = mapOf(
    "python" to kw("def return if elif else for while in not and or is None True False import from as class with try except finally raise pass break continue lambda yield global nonlocal assert del print len range"),
    "javascript" to kw("$C_LIKE function const let var of in typeof instanceof try catch finally throw async await import export from undefined"),
    "typescript" to kw("$C_LIKE function const let var of in typeof instanceof try catch finally throw async await import export from undefined interface type number string boolean"),
    "kotlin" to kw("fun val var if else when for while do return break continue in is as class object interface data true false null this try catch finally throw import package"),
    "java" to kw("$C_LIKE public private protected static final void int long double boolean String import package try catch finally throw throws extends implements"),
    "go" to kw("func package import var const type struct map range if else for return break continue switch case default go defer chan nil true false"),
    "cpp" to kw("$C_LIKE int long double bool void auto const std vector string include using namespace struct template typename"),
    "rust" to kw("fn let mut if else for while loop in match return break continue struct enum impl pub use mod true false Some None Ok Err self Self Vec String"),
    "csharp" to kw("$C_LIKE public private static void int long double bool string var using namespace foreach in List Dictionary"),
    "bash" to kw("if then else elif fi for while do done in case esac function return echo read local export"),
)
private val SQL = setOf("postgres", "sqlite3", "sql")
private val SQL_KW = kw("select from where and or not insert into values update set delete create table drop alter join left right inner outer on group by order having limit offset as distinct count sum avg min max null is in like between union all case when then else end primary key int text")

private fun commentRe(lang: String) = when (lang) {
    "python", "bash" -> "#[^\\n]*"
    in SQL -> "--[^\\n]*"
    else -> "//[^\\n]*|/\\*[\\s\\S]*?\\*/"
}
private const val STR = "\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'|`[^`]*`"
private const val NUM = "\\b\\d+(?:\\.\\d+)?\\b"
private const val ID = "[A-Za-z_][A-Za-z0-9_]*"

/** Tô màu đơn giản cho đoạn code ngắn trong card. Định danh được khớp trước số, nên `a1` không bị tô thành số. */
fun highlight(code: String, lang: String): List<Span> {
    val sql = lang in SQL
    val keys = if (sql) SQL_KW else KEYWORDS[lang].orEmpty()
    val re = Regex("(${commentRe(lang)})|($STR)|($NUM)|($ID)")
    return re.findAll(code).mapNotNull { m ->
        val s = m.range.first
        val e = m.range.last + 1
        when {
            m.groups[1] != null -> Span(s, e, Tok.COMMENT)
            m.groups[2] != null -> Span(s, e, Tok.STRING)
            m.groups[3] != null -> Span(s, e, Tok.NUMBER)
            (if (sql) m.value.lowercase() else m.value) in keys -> Span(s, e, Tok.KEYWORD)
            else -> null
        }
    }.toList()
}

@Composable
fun rememberHighlighted(code: String, lang: String): AnnotatedString {
    val cs = MaterialTheme.colorScheme
    return remember(code, lang, cs) {
        val style = mapOf(
            Tok.KEYWORD to SpanStyle(color = cs.secondary, fontWeight = FontWeight.Bold),
            Tok.STRING to SpanStyle(color = cs.tertiary),
            Tok.NUMBER to SpanStyle(color = cs.primary),
            Tok.COMMENT to SpanStyle(color = cs.onSurfaceVariant),
        )
        buildAnnotatedString {
            append(code)
            highlight(code, lang).forEach { addStyle(style.getValue(it.tok), it.start, it.end) }
        }
    }
}
