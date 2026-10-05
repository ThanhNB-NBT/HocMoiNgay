package com.thanhnb.hocmoingay.core.speech

/** Chuẩn hoá một từ để so: chữ thường, chỉ giữ a–z, 0–9 và dấu nháy bên trong từ. */
fun wordKey(token: String): String =
    token.lowercase().replace('’', '\'').filter { it in 'a'..'z' || it in '0'..'9' || it == '\'' }.trim('\'')

fun words(s: String): List<String> = s.split(Regex("\\s+")).map(::wordKey).filter { it.isNotEmpty() }

data class WordMark(val text: String, val ok: Boolean)
data class SpeechScore(val marks: List<WordMark>, val percent: Int)

/**
 * speak/shadow (spec §7.2): so câu nhận dạng được với câu mẫu bằng LCS theo từ.
 * Điểm = số từ khớp / số từ của câu mẫu; [SpeechScore.marks] giữ chữ gốc của câu mẫu để tô từ sai/thiếu.
 * ponytail: không quy đổi "I am" ↔ "I'm" hay "9" ↔ "nine"; thêm bảng quy đổi nếu người học hay bị trừ oan.
 */
fun scoreSpeech(reference: String, heard: String): SpeechScore {
    val tokens = reference.split(Regex("\\s+")).filter { wordKey(it).isNotEmpty() }
    val a = tokens.map(::wordKey)
    val b = words(heard)
    val dp = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in a.indices.reversed()) for (j in b.indices.reversed()) {
        dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1 else maxOf(dp[i + 1][j], dp[i][j + 1])
    }
    val ok = BooleanArray(a.size)
    var i = 0
    var j = 0
    while (i < a.size && j < b.size) when {
        a[i] == b[j] -> { ok[i] = true; i++; j++ }
        dp[i + 1][j] >= dp[i][j + 1] -> i++
        else -> j++
    }
    val n = ok.count { it }
    return SpeechScore(tokens.mapIndexed { k, t -> WordMark(t, ok[k]) }, if (a.isEmpty()) 0 else n * 100 / a.size)
}

/** timed_talk: cụm `must_use` có được nói liền nhau không. Từ ≥ 3 chữ cái khớp theo tiền tố (fix ~ fixed), từ ngắn phải khớp đúng. */
fun phraseUsed(phrase: String, heard: String): Boolean {
    val p = words(phrase)
    val h = words(heard)
    if (p.isEmpty()) return false
    return (0..h.size - p.size).any { s ->
        p.indices.all { k -> if (p[k].length >= 3) h[s + k].startsWith(p[k]) else h[s + k] == p[k] }
    }
}

fun wordsPerMinute(heard: String, seconds: Int): Int = words(heard).size * 60 / maxOf(1, seconds)
