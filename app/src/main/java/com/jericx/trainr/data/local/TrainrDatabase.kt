package com.jericx.trainr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

@Database(
    entities = [
        UserEntity::class,
        WeeklyWorkoutPlanEntity::class,
        WorkoutDayEntity::class,
        WorkoutExerciseEntity::class,
        ExerciseSetEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TrainrDatabase : RoomDatabase() {
    abstract val userDao: UserDao

    companion object {
        val MIGRATIONS: Array<Migration> = emptyArray()

        // Predate the 2026-09-13 store upload: the only versions that may be wiped.
        val LEGACY_VERSIONS = intArrayOf(1, 2, 3)
    }
}
