package com.thanhnb.hocmoingay.feature.review

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.lesson.parseLesson
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import com.thanhnb.hocmoingay.core.review.CardState
import com.thanhnb.hocmoingay.core.review.Rating
import com.thanhnb.hocmoingay.core.review.ReviewRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewViewModelTest {
    private val d = StandardTestDispatcher()
    private val lesson = "_sample-en/A2/mau/standup-mau"
    private val body = """{"title":"t","cards":[{"key":"v","type":"vocab","review":true,"word":"w","examples":["I fixed it."],"cloze":"I ___ it."}],
        "review":[{"key":"r","front":"F","back":"B"}]}"""
    private val cards = mutableMapOf<String, ReviewCardEntity>()
    private val logs = mutableMapOf<String, DailyLogEntity>()
    private var loads = 0
    private val repo = ReviewRepo(
        due = { now, limit -> cards.values.filter { it.due <= now }.sortedBy { it.due }.take(limit) },
        byIds = { ids -> ids.mapNotNull { cards[it] } }, put = { cards[it.id] = it },
        log = DailyLogRepo({ logs[it] }, { logs[it.day] = it }, { it() }, { "u1" }, {}),
        tx = { it() }, afterWrite = {}, now = { 1_000_000 },
    )

    private fun card(key: String, due: Long) = ReviewCardEntity(
        id = key, userId = "u1", ref = "$lesson#$key", kind = "recall", track = "english", courseId = "_sample-en", due = due, updatedAt = 1,
    )

    @Before fun setUp() = Dispatchers.setMain(d)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun napTheDenHanMotLanMoiBaiRoiChamXong() = runTest(d) {
        cards["r"] = card("r", 10)
        cards["v"] = card("v", 20)
        cards["mo_coi"] = card("mo_coi", 5)
        val vm = ReviewViewModel(repo, { loads++; parseLesson(body) { } }, this, flowOf(null))
        advanceUntilIdle()
        val items = vm.ui.value.items
        assertEquals(listOf("r", "v"), items.map { it.card.id }) // thẻ mồ côi bị bỏ, thứ tự theo due
        assertEquals(1, loads)
        assertTrue(items[0] is ReviewItem.Note)
        vm.rate(Rating.GOOD)
        advanceUntilIdle()
        assertEquals(1, vm.ui.value.pos)
        assertEquals(CardState.REVIEW, cards.getValue("r").state)
    }
}
