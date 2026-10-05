package com.thanhnb.hocmoingay.core.sync

import com.thanhnb.hocmoingay.core.db.LearnerRow

/**
 * Mốc updated_at cho một lần ghi local: luôn lớn hơn bản trước, kể cả khi đồng hồ máy bị lùi.
 * Nếu không, trigger keep_newer trên server sẽ âm thầm bỏ qua lần ghi này.
 */
fun nextUpdatedAt(prev: Long?, now: Long = System.currentTimeMillis()): Long = maxOf(now, (prev ?: 0L) + 1)

/** Gộp một hàng kéo về. Trả null nghĩa là giữ bản local (đang dirty và mới hơn); còn lại lấy bản server. */
fun <E : LearnerRow> pickRemote(local: E?, remote: E): E? =
    if (local != null && local.dirty && local.updatedAt > remote.updatedAt) null else remote
