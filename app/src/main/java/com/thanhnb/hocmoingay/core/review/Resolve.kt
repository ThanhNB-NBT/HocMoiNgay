package com.thanhnb.hocmoingay.core.review

/** Spec §7.4 (B6): thẻ `resolve` chấm theo số gợi ý đã mở khi nộp đạt, và số lần trượt trước đó. */
fun resolveRating(hints: Int, fails: Int): Rating = when {
    hints == 0 && fails == 0 -> Rating.EASY
    hints == 0 -> Rating.GOOD
    hints == 1 -> Rating.HARD
    else -> Rating.AGAIN
}
