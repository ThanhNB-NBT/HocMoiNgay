package com.thanhnb.hocmoingay.feature.player

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.log.DailyLogRepo
import com.thanhnb.hocmoingay.core.net.CodeApi
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerStrandsTest {
    private val d = StandardTestDispatcher()
    private val id = "_sample-en/A2/mau/standup-mau"
    private val body = """{"title":"t","kind":"task","cards":[
        {"key":"v","type":"vocab","word":"w"},
        {"key":"l","type":"listen","text":"Hi team.","questions":[]}]}"""
    private val progress = mutableMapOf<String, ProgressEntity>()
    private val logs = mutableMapOf<String, DailyLogEntity>()
    private var clock = 0L
    private val log = DailyLogRepo({ logs[it] }, { logs[it.day] = it }, { it() }, { "u1" }, {}, zone = { ZoneOffset.UTC })

    private fun repo(track: String) = LessonRepo(
        lesson = { LessonEntity(id, "_sample-en", body = body) }, track = { track },
        getProgress = { progress[it] }, putProgress = { progress[it.lessonId] = it },
        cardsByIds = { emptyList() }, putCards = {}, tx = { it() }, userId = { "u1" }, afterWrite = {}, now = { clock }, cpu = d,
    )

    private fun kotlinx.coroutines.test.TestScope.vm(track: String) =
        PlayerViewModel(id, repo(track), CodeApi { _, _ -> "{}" }, MutableStateFlow(true), flowOf(null), this, log, now = { clock })

    @Before fun setUp() = Dispatchers.setMain(d)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun baiTiengAnhGhiPhutTheoMachVaChanNamPhut() = runTest(d) {
        val v = vm("english"); advanceUntilIdle()
        v.start()
        clock += 90_000; v.answer(true, false); v.next()       // vocab 1,5 phút → language
        clock += 30 * 60_000; v.answer(true, false); v.next()  // listen 30 phút, chỉ tính 5 → input
        advanceUntilIdle()
        val s = Json.parseToJsonElement(logs.getValue("1970-01-01").strands).jsonObject
        assertEquals(1.5, s.getValue("language").jsonPrimitive.double, 1e-9)
        assertEquals(5.0, s.getValue("input").jsonPrimitive.double, 1e-9)
    }

    @Test fun baiLapTrinhKhongGhiMach() = runTest(d) {
        val v = vm("code"); advanceUntilIdle()
        v.start()
        clock += 60_000; v.answer(true, false); v.next()
        advanceUntilIdle()
        assertTrue(logs.isEmpty())
    }
}
