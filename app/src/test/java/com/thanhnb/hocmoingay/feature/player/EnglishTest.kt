package com.thanhnb.hocmoingay.feature.player

import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.lesson.Dialogue
import com.thanhnb.hocmoingay.core.lesson.FreeText
import com.thanhnb.hocmoingay.core.lesson.Listen
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.lesson.PredictOutput
import com.thanhnb.hocmoingay.core.lesson.Shadow
import com.thanhnb.hocmoingay.core.lesson.Vocab
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishTest {
    private fun objs(s: String) = Json.parseToJsonElement(s).jsonArray.map { it.jsonObject }

    @Test fun machTheoLoaiCard() {
        assertEquals("language", strandOf(Vocab("v", word = "w")))
        assertEquals("input", strandOf(Listen("l", text = "t")))
        assertEquals("fluency", strandOf(Shadow("s", text = "t")))
        assertEquals("output", strandOf(Dialogue("d", turns = emptyList())))
        assertEquals("output", strandOf(FreeText("f", mode = "writing", prompt = "p")))
        assertNull(strandOf(PredictOutput("p", lang = "python", code = "", answer = "")))
    }

    @Test fun cauHoiNgheLaMangChiSoBoCauHong() {
        val qs = parseQuestions(objs("""[
            {"q":"What?","choices":["The server","PR"],"answer":[0],"why":["Đúng","Sai"]},
            {"q":"Hỏng","choices":["a"],"answer":[5]},
            {"q":"Thiếu choices"}]"""))
        assertEquals(1, qs.size)
        assertEquals(listOf(0), qs[0].answer)
        assertEquals("Sai", qs[0].why[1])
    }

    @Test fun hoiThoaiTachLuotNoiVaLuotChon() {
        val t = parseTurns(objs("""[
            {"who":"Lan","text":"Morning!","vi":"Chào buổi sáng!"},
            {"who":"me","options":[{"text":"I fixed it.","ok":true},{"text":"I fix it tomorrow.","ok":false,"why":"Sai thì"}]},
            {"who":"Lan","text":"Great."},
            {"who":"me","options":[{"text":"không có đáp án đúng","ok":false}]}]"""))
        assertEquals(3, t.size) // lượt chọn không có đáp án đúng bị bỏ, không làm kẹt bài
        assertEquals(Turn.Line("Lan", "Morning!", "Chào buổi sáng!"), t[0])
        val p = t[1] as Turn.Pick
        assertEquals("Sai thì", p.options[1].why)
        assertEquals("", (t[2] as Turn.Line).vi)
    }

    @Test fun capAmToiThieuMoiCapMotLuotCoDinhTheoSeed() {
        val r = minimalRounds(listOf(listOf("ship", "sheep"), listOf("fill", "feel"), listOf("lẻ")), "seed")
        assertEquals(2, r.size)
        assertTrue(r.all { it.second in 0..1 })
        assertEquals(r, minimalRounds(listOf(listOf("ship", "sheep"), listOf("fill", "feel")), "seed"))
    }

    @Test fun napBaiTraVeMang() = runTest {
        val repo = LessonRepo(
            lesson = { LessonEntity("e/A2/c/b", "english-work", body = """{"title":"t","cards":[]}""") }, track = { "english" },
            getProgress = { null }, putProgress = {}, cardsByIds = { emptyList() }, putCards = {}, tx = { it() }, userId = { "u1" }, afterWrite = {},
        )
        assertEquals("english", repo.load("e/A2/c/b")!!.track)
    }
}
