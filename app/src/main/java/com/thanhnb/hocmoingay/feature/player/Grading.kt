package com.thanhnb.hocmoingay.feature.player

import com.thanhnb.hocmoingay.core.lesson.Issue
import kotlin.random.Random

/** So đáp án gõ tay: bỏ khoảng trắng hai đầu, gộp khoảng trắng liền nhau. */
fun norm(s: String) = s.trim().replace(Regex("\\s+"), " ")

fun quizOk(picked: Set<Int>, answer: List<Int>) = picked == answer.toSet()
fun predictOk(given: String, answer: String) = norm(given) == norm(answer)
fun fillOk(given: List<String>, answers: List<List<String>>) =
    given.size == answers.size && given.indices.all { i -> answers[i].any { norm(it) == norm(given[i]) } }
/** Parsons: đúng khi đã đặt đủ các dòng thật, đúng thứ tự, không có dòng nhiễu. So theo chữ vì hai dòng giống hệt nhau đổi chỗ vẫn đúng. */
fun orderOk(placed: List<String>, lines: List<String>) = placed == lines
fun fadeOk(fills: Map<Int, String>, fades: Map<Int, String>) = fades.all { (i, t) -> norm(fills[i].orEmpty()) == norm(t) }
/** find_bug: số dòng đếm từ 1. */
fun linesOk(picked: Set<Int>, bugLines: List<Int>) = picked == bugLines.toSet()
/** code_review: mỗi vấn đề có ít nhất một dòng được chọn, và không chọn dòng ngoài các vấn đề. */
fun reviewOk(picked: Set<Int>, issues: List<Issue>): Boolean {
    val all = issues.flatMap { it.lines }.toSet()
    return picked.isNotEmpty() && picked.all { it in all } && issues.all { i -> i.lines.any { it in picked } }
}
/** Hoán vị cố định theo seed (`<bài>#<card>#<vị trí>`): xoay màn hay recompose không đổi thứ tự lựa chọn. */
fun permutation(n: Int, seed: String): List<Int> = (0 until n).shuffled(Random(seed.hashCode()))
/** "t = ___ g" → ["t = ", " g"]: n chỗ trống thì n + 1 đoạn. */
fun blankParts(text: String): List<String> = text.split("___")
