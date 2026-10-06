package com.thanhnb.hocmoingay.core.log

import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.review.MINUTE_MS
import java.time.LocalDate

/** XP theo spec §7.4. */
const val XP_LESSON = 20
const val XP_CODE_FIRST_TRY = 10
const val XP_REVIEW = 1
const val XP_CHECKPOINT = 30

/** Thẻ mới tối đa mỗi ngày (spec §7.4), đếm theo `daily_log.new_cards`. */
const val NEW_CARDS_PER_DAY = 20

/** Ngày được tính vào chuỗi: có ít nhất một hoạt động (bài hoặc thẻ). */
fun DailyLogEntity.active(): Boolean = !deleted && (lessons > 0 || reviews > 0 || xp > 0)

/** Số ngày liên tiếp có học, tính lùi từ hôm nay; hôm nay chưa học thì chuỗi vẫn còn (tính từ hôm qua) tới hết ngày. */
fun streak(activeDays: Set<LocalDate>, today: LocalDate): Int {
    var d = if (today in activeDays) today else today.minusDays(1)
    var n = 0
    while (d in activeDays) { n++; d = d.minusDays(1) }
    return n
}

/**
 * Gom thời gian học (ms) thành phút nguyên cho `daily_log.minutes` (cột int): đủ phút nào ghi phút đó;
 * [flush] làm tròn phần lẻ ≥ 30 giây lên 1 phút rồi xoá phần lẻ.
 */
class MinuteMeter(private val write: (at: Long, minutes: Int) -> Unit) {
    private var acc = 0L

    fun add(at: Long, ms: Long) {
        acc += ms.coerceAtLeast(0)
        val whole = (acc / MINUTE_MS).toInt()
        if (whole > 0) { acc -= whole * MINUTE_MS; write(at, whole) }
    }

    fun flush(at: Long) {
        if (acc >= MINUTE_MS / 2) write(at, 1)
        acc = 0
    }
}
