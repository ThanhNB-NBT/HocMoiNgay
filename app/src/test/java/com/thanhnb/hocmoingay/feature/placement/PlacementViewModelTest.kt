package com.thanhnb.hocmoingay.feature.placement

import com.thanhnb.hocmoingay.core.db.PlacementEntity
import com.thanhnb.hocmoingay.core.db.SettingsEntity
import com.thanhnb.hocmoingay.feature.settings.SettingsRepo
import com.thanhnb.hocmoingay.feature.settings.decodeSettings
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlacementViewModelTest {
    private val d = StandardTestDispatcher()
    private var row: SettingsEntity? = null
    private val rows = bank().map { PlacementEntity(it.id, it.level, it.skill, """{"q":"${it.id}","choices":["a","b"],"answer":0}""") }
    private fun settings(pulled: Boolean) = SettingsRepo(flowOf(null), { row }, { row = it }, { "u1" }, {}, observePulled = flowOf(pulled))

    @Before fun setUp() = Dispatchers.setMain(d)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun lamDungHetThiLuuC1() = runTest(d) {
        val vm = PlacementViewModel({ rows }, settings(pulled = true), this, seed = 1)
        advanceUntilIdle()
        assertEquals(30, vm.ui.value.questions.size)
        vm.start()
        repeat(30) { vm.pick(0); vm.next() }
        advanceUntilIdle()
        assertEquals("C1", vm.ui.value.level)
        assertEquals(true, vm.ui.value.saved)
        assertEquals("C1", decodeSettings(row!!.data).englishLevel)
    }

    @Test fun caiDatChuaKeoVeThiKhongGhi() = runTest(d) {
        val vm = PlacementViewModel({ rows }, settings(pulled = false), this, seed = 1)
        advanceUntilIdle()
        vm.start()
        repeat(30) { vm.pick(1); vm.next() }
        advanceUntilIdle()
        assertEquals("A1", vm.ui.value.level)
        assertEquals(false, vm.ui.value.saved)
        assertNull(row)
    }
}
