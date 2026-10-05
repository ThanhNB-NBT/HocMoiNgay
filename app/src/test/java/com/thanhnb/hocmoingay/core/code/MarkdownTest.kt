package com.thanhnb.hocmoingay.core.code

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {
    @Test fun tachKhoi() {
        val md = "# Tiêu đề\nDòng một\ndòng hai\n\n![Một chấm](assets/_sample-code/dot.png)\n\n```python\n# B1\nx = 1\n```\n- a\n2. b"
        assertEquals(
            listOf(
                Md.Heading(1, "Tiêu đề"), Md.Para("Dòng một dòng hai"), Md.Image("Một chấm", "assets/_sample-code/dot.png"),
                Md.Code("python", "# B1\nx = 1"), Md.Bullet("a", null), Md.Bullet("b", 2),
            ),
            parseMd(md),
        )
    }

    @Test fun khoiCodeChuaDongKhongSap() = assertEquals(listOf(Md.Code("", "x")), parseMd("```\nx"))

    @Test fun inlineCodeVaDam() {
        val s = inlineMd("Gán `tong` **trước** vòng lặp", code = SpanStyle(fontWeight = FontWeight.Light), bold = SpanStyle(fontWeight = FontWeight.Bold))
        assertEquals("Gán tong trước vòng lặp", s.text)
        assertEquals(listOf(4 to 8, 9 to 14), s.spanStyles.map { it.start to it.end })
    }
}
