package com.thanhnb.hocmoingay.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.thanhnb.hocmoingay.core.net.InstantMillis
import com.thanhnb.hocmoingay.core.net.JsonText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/** Hàng người học: LWW theo updatedAt (client đặt), dirty = chưa đẩy lên. */
interface LearnerRow {
    val updatedAt: Long
    val dirty: Boolean
}

// ===== Giáo trình (chỉ kéo về; server luôn thắng, hàng deleted thì xoá local)

@Serializable
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val track: String,
    val title: String,
    val description: String = "",
    val sort: Int = 0,
    @Serializable(with = JsonText::class) val outline: String = "[]",
    val deleted: Boolean = false,
)

@Serializable
@Entity(tableName = "lessons", indices = [Index("courseId")])
data class LessonEntity(
    @PrimaryKey val id: String,
    @SerialName("course_id") val courseId: String,
    val sort: Int = 0,
    val version: Int = 1,
    @Serializable(with = JsonText::class) val body: String,
    val deleted: Boolean = false,
)

@Serializable
@Entity(tableName = "placement_questions")
data class PlacementEntity(
    @PrimaryKey val id: String,
    val level: String,
    val skill: String,
    @Serializable(with = JsonText::class) val body: String,
    val deleted: Boolean = false,
)

// ===== Người học (giống bảng server + cờ dirty chỉ có ở local)

@Serializable
@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey @SerialName("lesson_id") val lessonId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
    val score: Int? = null,
    @SerialName("hints_used") val hintsUsed: Int = 0,
    val language: String? = null,
    @SerialName("completed_at") @Serializable(with = InstantMillis::class) val completedAt: Long? = null,
    @SerialName("card_state") @Serializable(with = JsonText::class) val cardState: String = "{}",
    @SerialName("updated_at") @Serializable(with = InstantMillis::class) override val updatedAt: Long,
    val deleted: Boolean = false,
    @Transient override val dirty: Boolean = false,
) : LearnerRow

@Serializable
@Entity(tableName = "review_cards", indices = [Index(value = ["ref"], unique = true)])
data class ReviewCardEntity(
    @PrimaryKey val id: String,
    @SerialName("user_id") val userId: String,
    val ref: String,
    val kind: String,
    val track: String,
    @SerialName("course_id") val courseId: String,
    @Serializable(with = InstantMillis::class) val due: Long,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    @SerialName("elapsed_days") val elapsedDays: Int = 0,
    @SerialName("scheduled_days") val scheduledDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val state: Int = 0,
    @SerialName("last_review") @Serializable(with = InstantMillis::class) val lastReview: Long? = null,
    @SerialName("updated_at") @Serializable(with = InstantMillis::class) override val updatedAt: Long,
    val deleted: Boolean = false,
    @Transient override val dirty: Boolean = false,
) : LearnerRow

@Serializable
@Entity(tableName = "daily_log")
data class DailyLogEntity(
    @PrimaryKey val day: String, // "2026-10-05"
    @SerialName("user_id") val userId: String,
    val xp: Int = 0,
    val minutes: Int = 0,
    val reviews: Int = 0,
    val lessons: Int = 0,
    @SerialName("new_cards") val newCards: Int = 0,
    @Serializable(with = JsonText::class) val strands: String = "{}",
    @SerialName("updated_at") @Serializable(with = InstantMillis::class) override val updatedAt: Long,
    val deleted: Boolean = false,
    @Transient override val dirty: Boolean = false,
) : LearnerRow

@Serializable
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey @SerialName("user_id") val userId: String,
    @Serializable(with = JsonText::class) val data: String = "{}",
    @SerialName("updated_at") @Serializable(with = InstantMillis::class) override val updatedAt: Long,
    val deleted: Boolean = false,
    @Transient override val dirty: Boolean = false,
) : LearnerRow

// ===== Chỉ có ở local

/** Mốc synced_at lần kéo trước của từng bảng; hàng "_owner" giữ user_id chủ dữ liệu (AuthRepo). */
@Entity(tableName = "sync_state")
data class SyncStateEntity(@PrimaryKey val tableName: String, val cursor: String)

/** Code nháp, không đồng bộ. Khoá có cardKey vì một bài có thể có nhiều card code. */
@Entity(tableName = "code_drafts", primaryKeys = ["lessonId", "cardKey", "language"])
data class CodeDraftEntity(val lessonId: String, val cardKey: String, val language: String, val code: String, val updatedAt: Long)
