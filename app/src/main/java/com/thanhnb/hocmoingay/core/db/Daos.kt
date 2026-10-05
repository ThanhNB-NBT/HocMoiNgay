package com.thanhnb.hocmoingay.core.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CurriculumDao {
    @Upsert suspend fun upsertCourses(rows: List<CourseEntity>)
    @Upsert suspend fun upsertLessons(rows: List<LessonEntity>)
    @Upsert suspend fun upsertPlacement(rows: List<PlacementEntity>)
    @Query("DELETE FROM courses WHERE id IN (:ids)") suspend fun deleteCourses(ids: List<String>)
    @Query("DELETE FROM lessons WHERE id IN (:ids)") suspend fun deleteLessons(ids: List<String>)
    @Query("DELETE FROM placement_questions WHERE id IN (:ids)") suspend fun deletePlacement(ids: List<String>)

    /** Khoá `_…` (_sample-code, _sample-en) chỉ hiện ở bản debug (Kết quả f1). */
    @Query("SELECT * FROM courses WHERE :showSamples OR substr(id, 1, 1) <> '_' ORDER BY sort, id")
    fun observeCourses(showSamples: Boolean): Flow<List<CourseEntity>>
    @Query("SELECT * FROM lessons WHERE id = :id") suspend fun lesson(id: String): LessonEntity?
    @Query("SELECT * FROM courses WHERE id = :id") suspend fun course(id: String): CourseEntity?
    @Query("SELECT * FROM courses WHERE id = :id") fun observeCourse(id: String): Flow<CourseEntity?>
}

@Dao
abstract class LearnerDao {
    @Query("SELECT * FROM progress WHERE dirty = 1") abstract suspend fun dirtyProgress(): List<ProgressEntity>
    @Query("SELECT * FROM progress WHERE lessonId IN (:keys)") abstract suspend fun progressByKeys(keys: List<String>): List<ProgressEntity>
    @Upsert abstract suspend fun upsertProgress(rows: List<ProgressEntity>)
    /** Chỉ bỏ dirty nếu hàng vẫn là bản vừa đẩy; lần sửa mới hơn giữ dirty cho lượt sau. */
    @Query("UPDATE progress SET dirty = 0 WHERE lessonId = :key AND updatedAt = :updatedAt") abstract suspend fun cleanProgress(key: String, updatedAt: Long)

    @Query("SELECT * FROM review_cards WHERE dirty = 1") abstract suspend fun dirtyReviewCards(): List<ReviewCardEntity>
    @Query("SELECT * FROM review_cards WHERE id IN (:keys)") abstract suspend fun reviewCardsByKeys(keys: List<String>): List<ReviewCardEntity>
    @Upsert abstract suspend fun upsertReviewCards(rows: List<ReviewCardEntity>)
    @Query("UPDATE review_cards SET dirty = 0 WHERE id = :key AND updatedAt = :updatedAt") abstract suspend fun cleanReviewCard(key: String, updatedAt: Long)

    @Query("SELECT * FROM daily_log WHERE dirty = 1") abstract suspend fun dirtyDailyLog(): List<DailyLogEntity>
    @Query("SELECT * FROM daily_log WHERE day IN (:keys)") abstract suspend fun dailyLogByKeys(keys: List<String>): List<DailyLogEntity>
    @Upsert abstract suspend fun upsertDailyLog(rows: List<DailyLogEntity>)
    @Query("UPDATE daily_log SET dirty = 0 WHERE day = :key AND updatedAt = :updatedAt") abstract suspend fun cleanDailyLog(key: String, updatedAt: Long)

    @Query("SELECT * FROM settings WHERE dirty = 1") abstract suspend fun dirtySettings(): List<SettingsEntity>
    @Query("SELECT * FROM settings WHERE userId IN (:keys)") abstract suspend fun settingsByKeys(keys: List<String>): List<SettingsEntity>
    @Upsert abstract suspend fun upsertSettings(rows: List<SettingsEntity>)
    @Query("UPDATE settings SET dirty = 0 WHERE userId = :key AND updatedAt = :updatedAt") abstract suspend fun cleanSettings(key: String, updatedAt: Long)
    @Query("SELECT * FROM settings LIMIT 1") abstract fun observeSettings(): Flow<SettingsEntity?>
    @Query("SELECT * FROM settings LIMIT 1") abstract suspend fun settings(): SettingsEntity?

    @Query("DELETE FROM progress") abstract suspend fun wipeProgress()
    @Query("DELETE FROM review_cards") abstract suspend fun wipeReviewCards()
    @Query("DELETE FROM daily_log") abstract suspend fun wipeDailyLog()
    @Query("DELETE FROM settings") abstract suspend fun wipeSettings()

    /** Đổi tài khoản trên cùng máy: xoá dữ liệu người học, giữ giáo trình. */
    @Transaction
    open suspend fun wipeAll() {
        wipeProgress()
        wipeReviewCards()
        wipeDailyLog()
        wipeSettings()
    }
    @Query("SELECT * FROM progress WHERE lessonId = :lessonId") abstract fun observeProgress(lessonId: String): Flow<ProgressEntity?>
    @Query("SELECT * FROM progress WHERE deleted = 0") abstract fun observeAllProgress(): Flow<List<ProgressEntity>>
}

@Dao
interface SyncStateDao {
    @Query("SELECT cursor FROM sync_state WHERE tableName = :table") suspend fun cursor(table: String): String?
    @Query("SELECT cursor FROM sync_state WHERE tableName = :table") fun observeCursor(table: String): Flow<String?>
    @Upsert suspend fun put(row: SyncStateEntity)
    @Query("DELETE FROM sync_state WHERE tableName IN (:tables)") suspend fun clear(tables: List<String>)
}

@Dao
interface CodeDraftDao {
    @Query("SELECT code FROM code_drafts WHERE lessonId = :lessonId AND cardKey = :cardKey AND language = :language")
    suspend fun get(lessonId: String, cardKey: String, language: String): String?
    @Upsert suspend fun put(draft: CodeDraftEntity)
}
