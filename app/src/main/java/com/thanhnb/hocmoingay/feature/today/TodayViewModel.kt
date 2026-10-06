package com.thanhnb.hocmoingay.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.CurriculumDao
import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.db.LearnerDao
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.lesson.LessonBody
import com.thanhnb.hocmoingay.core.lesson.OutlineLesson
import com.thanhnb.hocmoingay.core.lesson.parseOutline
import com.thanhnb.hocmoingay.core.log.active
import com.thanhnb.hocmoingay.core.log.streak
import com.thanhnb.hocmoingay.feature.settings.AppSettings
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** Gom Room + Cài đặt thành hàng đợi Hôm nay (spec §7.4); quyết định nằm ở planToday và TodaySources. */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    curriculum: CurriculumDao,
    private val learner: LearnerDao,
    settings: Flow<AppSettings>,
    private val loadBody: suspend (String) -> LessonBody?,
    private val dayOf: (Long) -> String,
    showSamples: Boolean,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    data class Ui(
        val loading: Boolean = true,
        val plan: List<Planned> = emptyList(),
        val dailyMinutes: Int = 20,
        val doneMinutes: Int = 0,
        val xpToday: Int = 0,
        val streak: Int = 0,
    )

    private data class Snap(val courses: List<CourseEntity>, val progress: List<ProgressEntity>, val settings: AppSettings, val logs: List<DailyLogEntity>)

    private val firstDay = LocalDate.parse(dayOf(now())).minusDays(400).toString()

    val ui: StateFlow<Ui> = combine(
        curriculum.observeCourses(showSamples), learner.observeAllProgress(), settings,
        learner.observeDueCount(now()), // chỉ để chấm thẻ xong thì dựng lại
        learner.observeDailyLog(firstDay),
    ) { courses, progress, s, _, logs -> Snap(courses, progress, s, logs) }
        .mapLatest { build(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Ui())

    private suspend fun build(x: Snap): Ui {
        val t = now()
        val today = dayOf(t)
        val byId = x.progress.associateBy { it.lessonId }
        val done = x.progress.filter { !it.deleted && it.status == "done" }.map { it.lessonId }.toSet()
        val todayLog = x.logs.firstOrNull { it.day == today }
        suspend fun lesson(kind: PickKind, track: String, o: OutlineLesson) = Pick(kind, track, o.id, o.title, loadBody(o.id)?.estimateMin ?: 5)

        val code = activeCodeCourse(x.courses, x.settings.activeCourses, x.progress)
        val practice = x.courses.firstOrNull { it.id == PRACTICE_COURSE }?.let { pc ->
            // ponytail: parse mọi bài luyện ready mỗi lần dựng; đủ nhanh với vài chục bài, cache pattern khi kho lớn
            val ls = parseOutline(pc.outline).flatMap { lv -> lv.chapters.flatMap { it.lessons } }.filter { it.status == "ready" }
            pickPractice(ls.map { it to (loadBody(it.id)?.pattern ?: emptyList()) }, done)?.let { lesson(PickKind.PRACTICE, "code", it) }
        }
        val english = x.courses.firstOrNull { it.id == ENGLISH_COURSE } ?: x.courses.firstOrNull { it.track == "english" }
        val level = x.settings.englishLevel
        val input = TodayInput(
            dailyMinutes = x.settings.dailyMinutes,
            doneMinutes = todayLog?.minutes ?: 0,
            due = learner.dueRecall(t, x.settings.dailyMinutes * 2),
            resolve = learner.dueResolve(t, 1).firstOrNull()?.let { c ->
                val id = c.ref.substringBeforeLast('#')
                val b = loadBody(id)
                Pick(PickKind.RESOLVE, "code", id, b?.title ?: id, b?.estimateMin ?: 10, cardKey = c.ref.substringAfterLast('#'))
            },
            codeCheckpoint = x.courses.filter { it.track == "code" }.firstNotNullOfOrNull { checkpointPick(it, byId, today, dayOf) },
            codeNext = code?.let { c -> nextLesson(parseOutline(c.outline), done)?.let { lesson(PickKind.NEXT, "code", it) } },
            practice = practice,
            englishCheckpoint = english?.let { checkpointPick(it, byId, today, dayOf) },
            englishNext = when {
                english == null -> null
                level == null -> Pick(PickKind.PLACEMENT, "english", "", "Bài xếp lớp tiếng Anh", PLACEMENT_MINUTES)
                else -> nextLesson(parseOutline(english.outline), done, level)?.let { lesson(PickKind.NEXT, "english", it) }
            },
        )
        val days = x.logs.filter { it.active() }.map { LocalDate.parse(it.day) }.toSet()
        return Ui(
            loading = false, plan = planToday(input), dailyMinutes = x.settings.dailyMinutes, doneMinutes = input.doneMinutes,
            xpToday = todayLog?.xp ?: 0, streak = streak(days, LocalDate.parse(today)),
        )
    }
}
