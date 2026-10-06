package com.thanhnb.hocmoingay.feature.vocab

import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.Quiz
import com.thanhnb.hocmoingay.core.lesson.Vocab
import org.junit.Assert.assertEquals
import org.junit.Test

class VocabBookTest {
    private fun body(vararg words: String) = LessonBody(title = "t", cards = words.map { Vocab("v_$it", word = it) } + Quiz("q", q = "Q", choices = listOf("a"), answer = listOf(0)))

    @Test fun gomTuTheoBaiLayCapTuIdBoTuLap() {
        val es = vocabEntries(listOf("en/A1/c/b1" to body("name", "from"), "en/A2/c/b2" to body("Name", "job")))
        assertEquals(listOf("name", "from", "job"), es.map { it.vocab.word })
        assertEquals(listOf("A1", "A1", "A2"), es.map { it.level })
        assertEquals("en/A2/c/b2#v_job", es.last().ref)
    }

    @Test fun hangDoiTuDenHanTruocRoiTuMoi() {
        val es = vocabEntries(listOf("en/A1/c/b" to body("a", "b", "c", "d")))
        fun card(ref: String, due: Long) = ReviewCardEntity(id = ref, userId = "u", ref = ref, kind = "recall", track = "english", courseId = "en", due = due, updatedAt = 1)
        val cards = listOf(card(es[0].ref, 500), card(es[2].ref, 50)).associateBy { it.ref }
        // a chưa tới hạn → bỏ; c đến hạn lên đầu; b, d mới
        assertEquals(listOf(es[2].ref, es[1].ref, es[3].ref), flashcardQueue(es, cards, now = 100))
        assertEquals(2, flashcardQueue(es, cards, now = 100, n = 2).size)
    }
}
