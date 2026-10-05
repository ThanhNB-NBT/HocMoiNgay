package com.thanhnb.hocmoingay.core.lesson

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonTest {
    // rút gọn từ content/_sample/_sample-code/nhap-mon/mau/moi-loai-card.yaml sau push.py (concept giữ solutions)
    private val body = """
    {"title":"Mọi loại card","kind":"concept","estimate_min":8,"can_do":"Nhận ra mọi loại card",
     "cards":[
      {"key":"ngu_canh","type":"explain","md":"Xin chào"},
      {"key":"doan","type":"predict_output","review":true,"lang":"python","code":"print(50)","answer":"50","choices":["50","12308"]},
      {"key":"chay","type":"run_example","lang":"python","code":"print(1)"},
      {"key":"hoi","type":"quiz","q":"?","choices":["0","None"],"answer":[0],"why":["Đúng","Sai"]},
      {"key":"tim_loi","type":"find_bug","lang":"python","code":"a\nb","bug_lines":[2],"why":"vì"},
      {"key":"ghep","type":"match","pairs":[["a","b"],["c","d"]]},
      {"key":"sua","type":"code","kind":"stdout","prompt_md":"Sửa","starter":{"python":"n = 0\n"},
       "tests":[{"stdin":"3\n","stdout":"6\n"}],"langs":["python","bash"],"solutions":{"python":"x","bash":"y"}},
      {"key":"xep_mo","type":"order_lines","lang":"python","lines":["a","b","c"],"fade":[[2,"> 10"]]},
      {"key":"dien","type":"fill_blank","lang":"python","text":"t = ___","answers":[["0"]]},
      {"key":"review_code","type":"code_review","lang":"python","code":"x","issues":[{"lines":[2],"why":"chia 0"}]},
      {"key":"giai_thich","type":"free_text","mode":"explain","prompt":"Vì sao?","rubric":"r","sample":"s"}
     ],
     "review":[{"key":"r1","front":"F","back":"B"}]}
    """.trimIndent()

    @Test fun docDuMoiLoaiCardCode() {
        val l = parseLesson(body) { }!!
        assertEquals("Nhận ra mọi loại card", l.canDo)
        assertEquals(8, l.estimateMin)
        assertEquals(
            listOf("Explain", "PredictOutput", "RunExample", "Quiz", "FindBug", "Match", "CodeTask", "OrderLines", "FillBlank", "CodeReview", "FreeText"),
            l.cards.map { it::class.simpleName },
        )
        assertTrue((l.cards[1] as PredictOutput).review)
        assertEquals(listOf(2), (l.cards[4] as FindBug).bugLines)
        assertEquals(mapOf(2 to "> 10"), (l.cards[7] as OrderLines).fades)
        val code = l.cards[6] as CodeTask
        assertEquals("n = 0\n", code.starterFor("python"))
        assertNull(code.starterFor("bash"))
        assertEquals(listOf(ReviewNote("r1", "F", "B")), l.review)
    }

    @Test fun starterLaChuoiThiDungChoMoiNgonNgu() {
        val c = parseLesson("""{"title":"t","cards":[{"key":"k","type":"code","starter":"abc"}]}""") { }!!.cards[0] as CodeTask
        assertEquals("abc", c.starterFor("go"))
    }

    @Test fun cardLaBoQuaVaGhiLog() {
        val logs = mutableListOf<String>()
        val l = parseLesson(
            """{"title":"t","cards":[{"key":"a","type":"hologram"},{"key":"b","type":"quiz","q":"thiếu choices"},{"key":"c","type":"explain","md":"ok"}]}""",
        ) { logs += it }!!
        assertEquals(listOf("c"), l.cards.map { it.key })
        assertEquals(2, logs.size)
        assertTrue(logs[0].contains("a"))
    }

    @Test fun bodyHongTraNull() {
        assertNull(parseLesson("[1,2]") { })
        assertNull(parseLesson("không phải json") { })
    }
}
