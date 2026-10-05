package com.thanhnb.hocmoingay.core.sync

import androidx.room.withTransaction
import com.thanhnb.hocmoingay.core.db.CourseEntity
import com.thanhnb.hocmoingay.core.db.DailyLogEntity
import com.thanhnb.hocmoingay.core.db.HocDb
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.db.PlacementEntity
import com.thanhnb.hocmoingay.core.db.ProgressEntity
import com.thanhnb.hocmoingay.core.db.ReviewCardEntity
import com.thanhnb.hocmoingay.core.db.SettingsEntity

/** Transaction ghi của Room; SQLite chỉ có một writer nên các khối này không chạy chồng nhau. */
fun dbTx(db: HocDb): suspend (suspend () -> Unit) -> Unit = { block -> db.withTransaction { block() } }

/**
 * Thứ tự = thứ tự đẩy/kéo. Bảng người học trước: máy mới kéo settings về ngay, không phải đợi tải xong giáo trình.
 * onConflict = khoá chính (hoặc unique) trên server.
 */
fun syncTables(db: HocDb): List<TableSync> {
    val c = db.curriculum()
    val l = db.learner()
    return listOf(
        LearnerTable("progress", "user_id,lesson_id", ProgressEntity.serializer(), { it.lessonId },
            l::dirtyProgress, l::progressByKeys, l::upsertProgress, l::cleanProgress, dbTx(db)),
        LearnerTable("review_cards", "id", ReviewCardEntity.serializer(), { it.id },
            l::dirtyReviewCards, l::reviewCardsByKeys, l::upsertReviewCards, l::cleanReviewCard, dbTx(db)),
        LearnerTable("daily_log", "user_id,day", DailyLogEntity.serializer(), { it.day },
            l::dirtyDailyLog, l::dailyLogByKeys, l::upsertDailyLog, l::cleanDailyLog, dbTx(db)),
        LearnerTable("settings", "user_id", SettingsEntity.serializer(), { it.userId },
            l::dirtySettings, l::settingsByKeys, l::upsertSettings, l::cleanSettings, dbTx(db)),
        CurriculumTable("courses", CourseEntity.serializer(), { it.id }, { it.deleted }, c::upsertCourses, c::deleteCourses),
        CurriculumTable("lessons", LessonEntity.serializer(), { it.id }, { it.deleted }, c::upsertLessons, c::deleteLessons),
        CurriculumTable("placement_questions", PlacementEntity.serializer(), { it.id }, { it.deleted }, c::upsertPlacement, c::deletePlacement),
    )
}
