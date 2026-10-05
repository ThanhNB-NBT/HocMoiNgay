package com.thanhnb.hocmoingay.feature.learn

import com.thanhnb.hocmoingay.core.db.CourseEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LearnTest {
    @Test fun lapTrinhTruocTiengAnhSauBoNhomRong() {
        val en = CourseEntity("english-work", "english", "Tiếng Anh công việc")
        val py = CourseEntity("python", "code", "Python")
        assertEquals(listOf("Lập trình" to listOf(py), "Tiếng Anh" to listOf(en)), groupCourses(listOf(en, py)) { it.track })
        assertEquals(listOf("Lập trình" to listOf(py)), groupCourses(listOf(py)) { it.track })
    }
}
