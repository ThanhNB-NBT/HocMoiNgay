package com.thanhnb.hocmoingay.core.code

import com.thanhnb.hocmoingay.core.code.Tok.COMMENT
import com.thanhnb.hocmoingay.core.code.Tok.KEYWORD
import com.thanhnb.hocmoingay.core.code.Tok.NUMBER
import com.thanhnb.hocmoingay.core.code.Tok.STRING
import org.junit.Assert.assertEquals
import org.junit.Test

class HighlightTest {
    private fun t(code: String, lang: String) = highlight(code, lang).map { code.substring(it.start, it.end) to it.tok }

    @Test fun pythonDuBonLoai() = assertEquals(
        listOf("def" to KEYWORD, "# hàm" to COMMENT, "return" to KEYWORD, "\"a\"" to STRING, "12" to NUMBER),
        t("def f(x):  # hàm\n    return \"a\" + 12", "python"),
    )

    @Test fun dauThangTrongChuoiKhongPhaiComment() = assertEquals(listOf("\"#a\"" to STRING), t("x = \"#a\"", "python"))

    @Test fun sqlKhongPhanBietHoaThuong() = assertEquals(
        listOf("SELECT" to KEYWORD, "from" to KEYWORD, "-- c" to COMMENT),
        t("SELECT name from t -- c", "postgres"),
    )

    @Test fun kotlinVaSoTrongTen() = assertEquals(
        listOf("val" to KEYWORD, "1" to NUMBER, "// x" to COMMENT),
        t("val a1 = 1 // x", "kotlin"),
    )

    @Test fun ngonNguLaChiToChuoiSoComment() = assertEquals(listOf("2" to NUMBER), t("foo 2", "brainfuck"))
}
