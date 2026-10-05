package com.thanhnb.hocmoingay.core.sync

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LwwTest {
    private fun row(u: Long, dirty: Boolean) = DailyLogEntity(day = "2026-10-05", userId = "u1", updatedAt = u, dirty = dirty)

    @Test fun localDirtyMoiHonThiGiu() = assertNull(pickRemote(row(200, true), row(100, false)))
    @Test fun localDirtyCuHonThiLayServer() = assertEquals(row(300, false), pickRemote(row(200, true), row(300, false)))
    // keep_newer dùng <=: server đã từ chối bản bằng mốc, nên lấy bản server
    @Test fun bangMocThiLayServer() = assertEquals(row(200, false), pickRemote(row(200, true), row(200, false)))
    // bản server cũ hơn bản local (sự kiện realtime đến trễ) thì không được lùi dữ liệu
    @Test fun localSachMoiHonThiGiu() = assertNull(pickRemote(row(200, false), row(100, false)))
    @Test fun localSachCuHonThiLayServer() = assertEquals(row(300, false), pickRemote(row(200, false), row(300, false)))
    @Test fun chuaCoLocalThiLayServer() = assertEquals(row(100, false), pickRemote(null, row(100, false)))

    @Test fun updatedAtLuonTangKeCaKhiDongHoLui() {
        assertEquals(5_001L, nextUpdatedAt(5_000L, now = 1_000L))
        assertEquals(9_000L, nextUpdatedAt(5_000L, now = 9_000L))
        assertEquals(9_000L, nextUpdatedAt(null, now = 9_000L))
    }
}
