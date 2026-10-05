package com.thanhnb.hocmoingay.feature.settings

import com.thanhnb.hocmoingay.core.db.SettingsEntity
import com.thanhnb.hocmoingay.core.sync.nextUpdatedAt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SettingsRepo(
    observeRow: Flow<SettingsEntity?>,
    private val getRow: suspend () -> SettingsEntity?,
    private val putRow: suspend (SettingsEntity) -> Unit,
    private val userId: () -> String?,
    private val afterWrite: () -> Unit,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val lock = Mutex() // đọc–sửa–ghi; hai lần chạm nhanh không được mất một lần

    /** Chưa có hàng thì trả mặc định và KHÔNG ghi: cài lại app phải kéo hàng trên server về, không để mặc định đè lên. */
    val settings: Flow<AppSettings> =
        observeRow.map { r -> r?.takeUnless { it.deleted }?.let { decodeSettings(it.data) } ?: AppSettings() }

    suspend fun update(change: (AppSettings) -> AppSettings) = lock.withLock {
        val uid = userId() ?: return@withLock
        val row = getRow()
        val old = row?.takeUnless { it.deleted }?.let { decodeSettings(it.data) } ?: AppSettings()
        val new = change(old)
        if (new == old) return@withLock
        putRow(SettingsEntity(userId = uid, data = encodeSettings(new, row?.data), updatedAt = nextUpdatedAt(row?.updatedAt, now()), dirty = true))
        afterWrite()
    }
}
