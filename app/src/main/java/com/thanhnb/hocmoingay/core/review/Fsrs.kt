package com.thanhnb.hocmoingay.core.review

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

enum class Rating(val v: Int) { AGAIN(1), HARD(2), GOOD(3), EASY(4) }

/** Số `state` của bảng review_cards (đặt theo ts-fsrs): 0 New, 1 Learning, 2 Review, 3 Relearning. */
object CardState {
    const val NEW = 0
    const val LEARNING = 1
    const val REVIEW = 2
    const val RELEARNING = 3
}

const val MINUTE_MS = 60_000L
const val DAY_MS = 86_400_000L

/** Số ngày trọn, làm tròn xuống như `timedelta.days` của Python. */
fun wholeDays(ms: Long): Long = Math.floorDiv(ms, DAY_MS)

/** Trí nhớ của một thẻ. stability/difficulty null = chưa ôn lần nào. Mốc thời gian là epoch ms. */
data class Memo(
    val state: Int = CardState.NEW,
    val step: Int? = 0,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Long = 0,
    val lastReview: Long? = null,
)

/** 21 tham số mặc định của FSRS-6 (py-fsrs `DEFAULT_PARAMETERS`). Khai báo trước [AppFsrs]: thứ tự khởi tạo top-level theo thứ tự trong file. */
val DEFAULT_W = doubleArrayOf(
    0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001,
    1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629, 1.6483, 0.6014,
    1.8729, 0.5425, 0.0912, 0.0658, 0.1542,
)

/**
 * FSRS-6, port từ py-fsrs `Scheduler.review_card` (fsrs/scheduler.py), bỏ fuzzing.
 * Bước học tính bằng ms; app dùng [AppFsrs] (không bước học), test dùng bước mặc định của py-fsrs để so test vector.
 */
class Fsrs(
    private val w: DoubleArray = DEFAULT_W,
    private val desiredRetention: Double = 0.9,
    private val learningSteps: List<Long> = emptyList(),
    private val relearningSteps: List<Long> = emptyList(),
    private val maximumInterval: Int = 36500,
) {
    private val decay = -w[20]
    private val factor = 0.9.pow(1 / decay) - 1

    fun retrievability(m: Memo, now: Long): Double {
        val s = m.stability ?: return 0.0
        val last = m.lastReview ?: return 0.0
        val days = max(0L, wholeDays(now - last))
        return (1 + factor * days / s).pow(decay)
    }

    /** `round` của Kotlin làm tròn nửa về số chẵn, giống `round` của Python. */
    fun nextInterval(stability: Double): Int =
        round(stability / factor * (desiredRetention.pow(1 / decay) - 1)).toInt().coerceIn(1, maximumInterval)

    fun preview(m: Memo, now: Long): Map<Rating, Memo> = Rating.entries.associateWith { review(m, it, now) }

    fun review(m: Memo, r: Rating, now: Long): Memo {
        val days = m.lastReview?.let { wholeDays(now - it) }
        val shortTerm = days != null && days < 1
        var state = if (m.state == CardState.NEW) CardState.LEARNING else m.state
        var step: Int?
        val s: Double
        val d: Double
        val ivl: Long
        val oldS = m.stability
        val oldD = m.difficulty
        if (state == CardState.LEARNING || state == CardState.RELEARNING) {
            if (oldS == null || oldD == null) {
                s = initS(r); d = initD(r, clamp = true)
            } else if (shortTerm) {
                s = shortS(oldS, r); d = nextD(oldD, r)
            } else {
                s = nextS(oldD, oldS, retrievability(m, now), r); d = nextD(oldD, r)
            }
            val steps = if (state == CardState.LEARNING) learningSteps else relearningSteps
            val st = m.step ?: 0
            val toReview = steps.isEmpty() || (st >= steps.size && r != Rating.AGAIN) ||
                r == Rating.EASY || (r == Rating.GOOD && st + 1 == steps.size)
            if (toReview) {
                state = CardState.REVIEW; step = null; ivl = nextInterval(s) * DAY_MS
            } else when (r) {
                Rating.AGAIN -> { step = 0; ivl = steps[0] }
                Rating.HARD -> {
                    step = st
                    ivl = when {
                        st == 0 && steps.size == 1 -> steps[0] * 3 / 2
                        st == 0 -> (steps[0] + steps[1]) / 2
                        else -> steps[st]
                    }
                }
                else -> { step = st + 1; ivl = steps[st + 1] } // GOOD, chưa tới bước cuối
            }
        } else {
            val s0 = oldS ?: initS(r)
            val d0 = oldD ?: initD(r, clamp = true)
            s = if (shortTerm) shortS(s0, r) else nextS(d0, s0, retrievability(m, now), r)
            d = nextD(d0, r)
            if (r == Rating.AGAIN && relearningSteps.isNotEmpty()) {
                state = CardState.RELEARNING; step = 0; ivl = relearningSteps[0]
            } else {
                step = null; ivl = nextInterval(s) * DAY_MS
            }
        }
        return Memo(state, step, s, d, now + ivl, now)
    }

    private fun clampS(s: Double) = max(s, 0.001)
    private fun clampD(d: Double) = d.coerceIn(1.0, 10.0)
    private fun initS(r: Rating) = clampS(w[r.v - 1])
    private fun initD(r: Rating, clamp: Boolean): Double {
        val d = w[4] - exp(w[5] * (r.v - 1)) + 1
        return if (clamp) clampD(d) else d
    }
    private fun shortS(s: Double, r: Rating): Double {
        var inc = exp(w[17] * (r.v - 3 + w[18])) * s.pow(-w[19])
        if (r != Rating.AGAIN) inc = max(inc, 1.0)
        return clampS(s * inc)
    }
    private fun nextD(d: Double, r: Rating): Double {
        val delta = -(w[6] * (r.v - 3))
        val damped = d + (10.0 - d) * delta / 9.0
        return clampD(w[7] * initD(Rating.EASY, clamp = false) + (1 - w[7]) * damped)
    }
    private fun nextS(d: Double, s: Double, rr: Double, r: Rating): Double = clampS(
        if (r == Rating.AGAIN) {
            min(
                w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp((1 - rr) * w[14]),
                s / exp(w[17] * w[18]),
            )
        } else {
            s * (1 + exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp((1 - rr) * w[10]) - 1) *
                (if (r == Rating.HARD) w[15] else 1.0) * (if (r == Rating.EASY) w[16] else 1.0))
        },
    )
}

/** Cấu hình của app: retention 0.9, không bước học theo phút, không fuzz (Quyết định 2). */
val AppFsrs = Fsrs()

/** Khoảng cách tới lần ôn tiếp, hiện dưới mỗi nút: "10 phút", "3 giờ", "2 ngày", "2 tháng", "1,5 năm". */
fun spanText(ms: Long): String {
    val minutes = ms / MINUTE_MS
    val days = round(ms.toDouble() / DAY_MS).toLong()
    return when {
        minutes < 60 -> "${max(1L, minutes)} phút"
        minutes < 60 * 24 -> "${minutes / 60} giờ"
        days < 30 -> "$days ngày"
        days < 365 -> "${round(days / 30.0).toLong()} tháng"
        else -> "${(days * 10 / 365) / 10.0} năm".replace('.', ',')
    }
}
