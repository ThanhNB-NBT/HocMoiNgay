package com.thanhnb.hocmoingay

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable sealed interface Screen : NavKey
@Serializable data object Today : Screen
@Serializable data object Learn : Screen
@Serializable data object Review : Screen
@Serializable data object Profile : Screen
@Serializable data object Settings : Screen
@Serializable data class CourseDetail(val courseId: String) : Screen
@Serializable data class LessonPlayer(val lessonId: String) : Screen
@Serializable data class CodeEditor(val lessonId: String, val cardKey: String) : Screen

val TABS: List<NavKey> = listOf(Today, Learn, Review, Profile)

/** Chồng luôn là [Today] hoặc [Today, tab]: Back từ tab khác về Hôm nay, Back ở Hôm nay thì thoát. */
fun MutableList<NavKey>.selectTab(tab: NavKey) {
    clear()
    add(Today)
    if (tab != Today) add(tab)
}
