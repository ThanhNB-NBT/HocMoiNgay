package com.thanhnb.hocmoingay.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

val LEARNER_TABLES = listOf("progress", "review_cards", "daily_log", "settings")

/** Hàng sync_state giữ user_id chủ dữ liệu người học trên máy. */
const val OWNER_KEY = "_owner"

@Database(
    entities = [
        CourseEntity::class, LessonEntity::class, PlacementEntity::class,
        ProgressEntity::class, ReviewCardEntity::class, DailyLogEntity::class, SettingsEntity::class,
        SyncStateEntity::class, CodeDraftEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HocDb : RoomDatabase() {
    abstract fun curriculum(): CurriculumDao
    abstract fun learner(): LearnerDao
    abstract fun syncState(): SyncStateDao
    abstract fun drafts(): CodeDraftDao

    companion object {
        fun open(ctx: Context): HocDb = Room.databaseBuilder(ctx, HocDb::class.java, "hoc.db").build()
    }
}
