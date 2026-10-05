package com.thanhnb.hocmoingay.feature.player

import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.net.CodeApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerViewModelTest {
    private val d = StandardTestDispatcher()
    private val id = "_sample-code/nhap-mon/mau/x"
    private val body = """{"title":"t","cards":[{"key":"e","type":"explain","md":"m"}]}"""
    private val progress = mutableMapOf<String, ProgressEntity>()
    private val gate = CompletableDeferred<Unit>()
    private var loads = 0
    private val repo = LessonRepo(
        lesson = { if (++loads > 1) gate.await(); LessonEntity(id, "_sample-code", body = body) }, track = { "code" },
        getProgress = { progress[it] }, putProgress = { progress[it.lessonId] = it },
        cardsByIds = { emptyList() }, putCards = {}, tx = { it() }, userId = { "u1" }, afterWrite = {}, cpu = d,
    )

    @Before fun setUp() = Dispatchers.setMain(d)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun dongManNgaySauCardCuoiVanGhiXongBai() = runTest(d) {
        val v = PlayerViewModel(id, repo, CodeApi { _, _ -> "{}" }, MutableStateFlow(true), flowOf(null), writeScope = this)
        advanceUntilIdle()
        v.start(); v.answer(true, false); v.next()
        advanceUntilIdle() // finish đang chờ load bài
        v.viewModelScope.cancel() // người dùng đóng màn
        gate.complete(Unit); advanceUntilIdle()
        assertEquals("done", progress.getValue(id).status)
    }
}
