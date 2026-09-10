package com.jericx.trainr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Version 1 with no migrations because nothing has shipped. From the first
// upload onward, every schema change needs a migration and this number climbs.
@Database(
    entities = [
        UserEntity::class,
        WeeklyWorkoutPlanEntity::class,
        WorkoutDayEntity::class,
        WorkoutExerciseEntity::class,
        ExerciseSetEntity::class
    ],
    // Bumped without a migration on purpose: the version has to move for the
    // destructive fallback to fire, or an old file fails its identity check.
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrainrDatabase : RoomDatabase() {
    abstract val userDao: UserDao
}
