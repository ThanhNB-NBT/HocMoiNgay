package com.thanhnb.hocmoingay.feature.editor

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import com.thanhnb.hocmoingay.core.db.CodeDraftDao
import com.thanhnb.hocmoingay.core.db.CodeDraftEntity
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.lesson.cardStateOf
import com.thanhnb.hocmoingay.core.net.CodeApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val d = StandardTestDispatcher()
    private val id = "_sample-code/nhap-mon/mau/bai-problem"
    private val body = """{"title":"t","kind":"problem","hints":["Ý tưởng","Các bước"],"cards":[
        {"key":"two_sum","type":"code","prompt_md":"p","langs":["python","rust"],"tests":[]}]}"""
    private val progress = mutableMapOf<String, ProgressEntity>()
    private val draftMap = mutableMapOf<Triple<String, String, String>, String>()
    private val calls = mutableListOf<String>()
    private var gate: CompletableDeferred<Unit>? = null
    private var submitResult = """{"compiled":true,"tests":[{"name":"a","pass":true,"time_ms":1,"hidden":false}],"time_ms":1}"""

    private val drafts = object : CodeDraftDao {
        override suspend fun get(lessonId: String, cardKey: String, language: String) = draftMap[Triple(lessonId, cardKey, language)]
        override suspend fun put(draft: CodeDraftEntity) { draftMap[Triple(draft.lessonId, draft.cardKey, draft.language)] = draft.code }
    }
    private val api = CodeApi { _, body ->
        val mode = body["mode"]!!.jsonPrimitive.content
        calls += mode
        when (mode) {
            "starter" -> """{"code":"fn two_sum() {}"}"""
            "hint" -> """{"lines":["b","a"]}"""
            else -> { gate?.await(); submitResult }
        }
    }

    private fun TestScope.vm(preferred: String = "rust") = EditorViewModel(
        id, "two_sum",
        LessonRepo(
            lesson = { LessonEntity(id, "_sample-code", body = body) }, track = { "code" },
            getProgress = { progress[it] }, putProgress = { progress[it.lessonId] = it },
            cardsByIds = { emptyList() }, putCards = {}, tx = { it() }, userId = { "u1" }, afterWrite = {}, cpu = d,
        ),
        drafts, api, MutableStateFlow(true), flowOf(preferred), flushScope = this,
    )

    @Before fun setUp() = Dispatchers.setMain(d)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun roiManKhiDangNopVanGhiKetQua() = runTest(d) {
        val v = vm(); advanceUntilIdle()
        gate = CompletableDeferred()
        v.submit(); advanceUntilIdle() // đang chờ server chấm
        v.viewModelScope.cancel() // người dùng bấm back
        gate!!.complete(Unit); advanceUntilIdle()
        assertEquals("true", (cardStateOf(progress[id])["two_sum"] as? JsonObject)?.get("pass")?.toString())
    }

    private fun state() = cardStateOf(progress[id])["two_sum"] as JsonObject

    @Test fun chonNgonNguUaThichVaNapStarterTuServer() = runTest(d) {
        val v = vm(); advanceUntilIdle()
        assertEquals("rust", v.ui.value.lang)
        assertEquals("fn two_sum() {}", v.ui.value.code)
        assertEquals("fn two_sum() {}", draftMap[Triple(id, "two_sum", "rust")]) // starter thành nháp = cache
    }

    @Test fun uaThichKhongCoTrongBaiThiLayNgonNguDau() = runTest(d) {
        val v = vm(preferred = "go"); advanceUntilIdle()
        assertEquals("python", v.ui.value.lang)
    }

    @Test fun nhapCoSanThiKhongGoiServer() = runTest(d) {
        draftMap[Triple(id, "two_sum", "rust")] = "nháp cũ"
        val v = vm(); advanceUntilIdle()
        assertEquals("nháp cũ", v.ui.value.code)
        assertEquals(emptyList<String>(), calls)
    }

    @Test fun nopDatGhiCardState() = runTest(d) {
        val v = vm(); advanceUntilIdle()
        v.submit(); advanceUntilIdle()
        assertEquals("true", state()["pass"]!!.jsonPrimitive.content)
        assertEquals("rust", state()["lang"]!!.jsonPrimitive.content)
        assertEquals("rust", progress.getValue(id).language)
    }

    @Test fun nopTruotTangFails() = runTest(d) {
        submitResult = """{"compiled":true,"tests":[{"name":"a","pass":false,"time_ms":1,"hidden":false}],"time_ms":1}"""
        val v = vm(); advanceUntilIdle()
        v.submit(); advanceUntilIdle()
        v.submit(); advanceUntilIdle()
        assertEquals("2", state()["fails"]!!.jsonPrimitive.content)
    }

    @Test fun goiYNac3GoiServerMotLanRoiDungCache() = runTest(d) {
        val v = vm(); advanceUntilIdle()
        repeat(3) { v.openHint(); advanceUntilIdle() }
        assertEquals(listOf("b", "a"), v.ui.value.hintLines)
        assertEquals(3, progress.getValue(id).hintsUsed)
        val v2 = vm(); advanceUntilIdle() // mở lại màn: nấc 3 đọc từ card_state
        calls.clear()
        v2.openHint(); advanceUntilIdle()
        assertEquals(listOf("b", "a"), v2.ui.value.hintLines)
        assertEquals(emptyList<String>(), calls)
    }

    @Test fun roiManVanLuuNhapCuoi() = runTest(d) {
        val v = vm(); advanceUntilIdle()
        v.onEdit("fn two_sum() { todo!() }")
        v.flush(); advanceUntilIdle()
        assertEquals("fn two_sum() { todo!() }", draftMap[Triple(id, "two_sum", "rust")])
    }

    @Test fun daDatRoiNopTruotKhongTangFails() = runTest(d) {
        val v = vm(); advanceUntilIdle()
        v.submit(); advanceUntilIdle() // đạt
        submitResult = """{"compiled":true,"tests":[{"name":"a","pass":false,"time_ms":1,"hidden":false}],"time_ms":1}"""
        v.submit(); advanceUntilIdle() // học lại, nộp trượt
        assertEquals(null, state()["fails"])
        assertEquals("true", state()["pass"]!!.jsonPrimitive.content)
    }
}
