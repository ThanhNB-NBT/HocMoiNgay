package com.thanhnb.hocmoingay.feature.settings

import com.thanhnb.hocmoingay.core.db.SettingsEntity
import com.thanhnb.hocmoingay.core.theme.ThemeMode
import com.thanhnb.hocmoingay.core.theme.ThemeStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsTest {
    private val row = MutableStateFlow<SettingsEntity?>(null)
    private var writes = 0
    private var scheduled = 0
    private val pulled = MutableStateFlow(true)

    private fun repo(uid: String? = "u1", clock: Long = 1_000L) = SettingsRepo(
        observeRow = row, getRow = { row.value }, putRow = { row.value = it; writes++ },
        userId = { uid }, afterWrite = { scheduled++ }, now = { clock }, observePulled = pulled,
    )

    @Test fun chuaCoHangThiTraMacDinhVaKhongGhi() = runTest {
        assertEquals(AppSettings(), repo().settings.first())
        assertEquals("cài lại app không được ghi mặc định đè server", 0, writes)
    }

    @Test fun capNhatGhiHangDirtyVaHenSync() = runTest {
        repo().update { it.copy(dailyMinutes = 30) }
        val r = row.value!!
        assertTrue(r.dirty)
        assertEquals(1_000L, r.updatedAt)
        assertEquals("u1", r.userId)
        assertEquals(30, decodeSettings(r.data).dailyMinutes)
        assertEquals(1, scheduled)
    }

    @Test fun updatedAtVuotBanCuKhiDongHoLui() = runTest {
        row.value = SettingsEntity(userId = "u1", data = "{}", updatedAt = 5_000)
        repo(clock = 1_000).update { it.copy(dailyMinutes = 10) }
        assertEquals(5_001L, row.value!!.updatedAt)
    }

    @Test fun giuKhoaCuaBanAppMoiHon() = runTest {
        row.value = SettingsEntity(userId = "u1", data = """{"daily_minutes":20,"future_key":1}""", updatedAt = 1)
        repo().update { it.copy(dailyMinutes = 10) }
        val o = Json.parseToJsonElement(row.value!!.data).jsonObject
        assertEquals(JsonPrimitive(1), o["future_key"])
        assertEquals(JsonPrimitive(10), o["daily_minutes"])
    }

    @Test fun giaTriLaKhongLamHongCaiDat() {
        val s = decodeSettings("""{"theme":"neon","daily_minutes":10}""")
        assertEquals(ThemeStyle.TWO_TONE, s.theme)
        assertEquals(10, s.dailyMinutes)
        assertEquals(AppSettings(), decodeSettings("không phải json"))
    }

    @Test fun khongDoiThiKhongGhi() = runTest {
        repo().update { it }
        assertEquals(0, writes)
        assertEquals(0, scheduled)
    }

    @Test fun chuaDangNhapThiKhongGhi() = runTest {
        repo(uid = null).update { it.copy(dailyMinutes = 30) }
        assertEquals(0, writes)
    }

    @Test fun chonIdeKhiDangTheoHeThongThiChuyenToi() {
        assertEquals(ThemeMode.DARK, AppSettings().withTheme(ThemeStyle.IDE).mode)
        assertEquals(ThemeMode.LIGHT, AppSettings(mode = ThemeMode.LIGHT).withTheme(ThemeStyle.IDE).mode)
        assertEquals(ThemeMode.SYSTEM, AppSettings().withTheme(ThemeStyle.WALLPAPER).mode)
    }

    @Test fun gioNhacSapXepKhongTrung() {
        assertEquals(listOf("07:30", "20:00"), addReminder(listOf("20:00"), "07:30"))
        assertEquals(listOf("20:00"), addReminder(listOf("20:00"), "20:00"))
    }

    // Máy mới/cài lại: sửa trước lần kéo đầu tiên sẽ ghi mặc định + 1 thay đổi, mốc mới hơn nên đè hết cài đặt trên server
    @Test fun chuaKeoLanNaoThiKhongChoSua() = runTest {
        pulled.value = false
        val r = repo()
        r.update { it.copy(dailyMinutes = 30) }
        assertEquals("mặc định không được đè cài đặt trên server", 0, writes)
        assertFalse(r.loaded.first())
    }

    @Test fun daCoHangThiSuaDuocDuChuaDanhDauKeo() = runTest {
        pulled.value = false
        row.value = SettingsEntity(userId = "u1", data = "{}", updatedAt = 1)
        repo().update { it.copy(dailyMinutes = 30) }
        assertEquals(1, writes)
    }
}
