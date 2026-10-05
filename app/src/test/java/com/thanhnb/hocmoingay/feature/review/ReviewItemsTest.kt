package com.thanhnb.hocmoingay.feature.review

import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.lesson.PredictOutput
import com.thanhnb.hocmoingay.core.lesson.parseLesson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewItemsTest {
    private val body = parseLesson(
        """{"title":"t","cards":[
          {"key":"v_fix","type":"vocab","review":true,"word":"fix a bug","meaning_vi":"sửa lỗi",
           "examples":["Yesterday I fixed a bug in the login page."],"cloze":"Yesterday I ___ a bug in the login page."},
          {"key":"v_nocloze","type":"vocab","review":true,"word":"on track","meaning_vi":"đúng tiến độ"},
          {"key":"doan","type":"predict_output","review":true,"lang":"python","code":"print(1)","answer":"1"},
          {"key":"two_sum","type":"code","prompt_md":"p"}],
          "review":[{"key":"r_thi","front":"Thì gì?","back":"Quá khứ đơn"}]}""",
    ) { }
    private fun card(key: String) = ReviewCardEntity(
        id = key, userId = "u1", ref = "_sample-en/A2/mau/standup-mau#$key", kind = "recall", track = "english", courseId = "_sample-en", due = 0, updatedAt = 1,
    )

    @Test fun dapAnClozeLaPhanDienVaoCho() {
        assertEquals("fixed", clozeAnswer("Yesterday I ___ a bug in the login page.", listOf("Yesterday I fixed a bug in the login page.")))
        assertEquals("blocked by", clozeAnswer("I'm ___ the staging server.", listOf("Other.", "I'm blocked by the staging server.")))
        assertNull(clozeAnswer("Không có chỗ trống.", listOf("x")))
        assertNull(clozeAnswer("A ___ B.", listOf("Khác hẳn.")))
    }

    @Test fun soDapAnClozeBoHoaThuongVaDauCham() {
        assertTrue(clozeOk("  Fixed. ", "fixed"))
        assertTrue(clozeOk("blocked  by", "blocked by"))
        assertEquals(false, clozeOk("fix", "fixed"))
    }

    @Test fun ghiChuThanhNote() {
        val n = reviewItem(card("r_thi"), body) as ReviewItem.Note
        assertEquals("Thì gì?", n.front)
        assertEquals("Quá khứ đơn", n.back)
    }

    @Test fun vocabCoClozeThanhCloze() {
        val c = reviewItem(card("v_fix"), body) as ReviewItem.Cloze
        assertEquals("fixed", c.answer)
    }

    @Test fun vocabKhongClozeThanhNoteNghiaRaTu() {
        val n = reviewItem(card("v_nocloze"), body) as ReviewItem.Note
        assertEquals("đúng tiến độ", n.front)
        assertEquals("on track", n.back)
    }

    @Test fun cardKhacGiuNguyenDeVeBangCardView() {
        val l = reviewItem(card("doan"), body) as ReviewItem.Lesson
        assertEquals("_sample-en/A2/mau/standup-mau", l.lessonId)
        assertTrue(l.item is PredictOutput)
    }

    @Test fun theMoCoiBoQua() {
        assertNull(reviewItem(card("da_doi_key"), body))
        assertNull(reviewItem(card("two_sum"), body)) // card code ôn ở luồng resolve (giai đoạn e)
        assertNull(reviewItem(card("r_thi"), null))   // bài không còn trên máy
    }
}
