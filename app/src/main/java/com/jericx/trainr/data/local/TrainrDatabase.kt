package com.jericx.trainr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        WeeklyWorkoutPlanEntity::class,
        WorkoutDayEntity::class,
        WorkoutExerciseEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrainrDatabase : RoomDatabase() {
    abstract val userDao: UserDao

    companion object {
        // An exercise gained the two numbers the routine screen shows: the
        // minutes it is allotted and the prescription beside them.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_exercises " +
                        "ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE workout_exercises " +
                        "ADD COLUMN prescription TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}
