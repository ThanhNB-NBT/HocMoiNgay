package com.thanhnb.hocmoingay.feature.player


/**
 * Hàng đợi card của một lượt học. [order] chứa chỉ số card; trả lời sai thì card được thêm lại vào cuối (A4).
 * [results] theo vị trí trong [order]; [firstTry] theo card, chỉ lần trả lời đầu và chỉ card có chấm.
 * [graded]: các vị trí là câu có chấm, để thanh dưới biết có nói "Chính xác" / "Chưa đúng" hay không.
 */
data class PlayerQueue(
    val total: Int,
    val order: List<Int> = List(total) { it },
    val pos: Int = 0,
    val firstTry: Map<Int, Boolean> = emptyMap(),
    val done: Set<Int> = emptySet(),
    val results: Map<Int, Boolean> = emptyMap(),
    val graded: Set<Int> = emptySet(),
) {
    val current: Int? get() = order.getOrNull(pos)
    val finished: Boolean get() = pos >= order.size
    val answered: Boolean get() = pos in results
    val progress: Float get() = if (total == 0) 1f else done.size.toFloat() / total
    val score: Int get() = if (firstTry.isEmpty()) 100 else firstTry.values.count { it } * 100 / firstTry.size

    fun answer(ok: Boolean, graded: Boolean = true): PlayerQueue {
        val i = current ?: return this
        if (answered) return this
        val ft = if (graded && i !in firstTry) firstTry + (i to ok) else firstTry
        val r = results + (pos to ok)
        val g = if (graded) this.graded + pos else this.graded
        return if (ok) copy(firstTry = ft, done = done + i, results = r, graded = g) else copy(firstTry = ft, order = order + i, results = r, graded = g)
    }

    fun next(): PlayerQueue = if (finished) this else copy(pos = pos + 1)
}
