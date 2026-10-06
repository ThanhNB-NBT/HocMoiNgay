package com.thanhnb.hocmoingay.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.db.CurriculumDao
import com.thanhnb.hocmoingay.core.db.LearnerDao
import com.thanhnb.hocmoingay.core.lesson.CourseStats
import com.thanhnb.hocmoingay.core.lesson.courseStats
import com.thanhnb.hocmoingay.core.lesson.parseOutline
import com.thanhnb.hocmoingay.core.log.active
import com.thanhnb.hocmoingay.core.log.streak
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(
    curriculum: CurriculumDao,
    learner: LearnerDao,
    dayOf: (Long) -> String,
    showSamples: Boolean,
    now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    data class CourseLine(val title: String, val track: String, val stats: CourseStats)
    data class Ui(
        val loading: Boolean = true,
        val streak: Int = 0,
        val totalXp: Int = 0,
        val activeDays: Int = 0,
        val heat: List<List<Int?>> = emptyList(),
        val months: List<String?> = emptyList(),
        val strands: Map<String, Double> = emptyMap(),
        val note: String? = null,
        val courses: List<CourseLine> = emptyList(),
    )

    private val today = LocalDate.parse(dayOf(now()))

    val ui: StateFlow<Ui> = combine(
        curriculum.observeCourses(showSamples), learner.observeAllProgress(),
        learner.observeDailyLog(today.minusWeeks(27).toString()), learner.observeTotalXp(),
    ) { courses, progress, logs, xp ->
        val byId = progress.associateBy { it.lessonId }
        val week = weekStrands(logs, today)
        val active = logs.filter { it.active() }.map { LocalDate.parse(it.day) }.toSet()
        Ui(
            loading = false,
            streak = streak(active, today), // ponytail: chuỗi > 27 tuần bị cắt theo cửa sổ log; đọc thêm khi có người học lâu vậy
            totalXp = xp,
            activeDays = active.size,
            heat = heatmap(logs.associate { LocalDate.parse(it.day) to it.xp }, today),
            months = heatMonths(today),
            strands = week,
            note = strandNote(week),
            courses = courses.mapNotNull { c ->
                courseStats(c.id, parseOutline(c.outline), byId).takeIf { it.done > 0 }?.let { CourseLine(c.title, c.track, it) }
            },
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ui())
}
