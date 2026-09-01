package com.jericx.trainr.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Version 1 with no migrations, because nothing has ever shipped. Room builds
// the schema from the entities on a fresh install and never runs a migration on
// one, so the five that used to live here could not execute anywhere. They were
// the upgrade path for databases that do not exist.
//
// This is only true until the first build reaches a device that is not ours.
// From the first upload onward, every schema change needs a migration and this
// version number has to climb with it.
@Database(
    entities = [
        UserEntity::class,
        WeeklyWorkoutPlanEntity::class,
        WorkoutDayEntity::class,
        WorkoutExerciseEntity::class,
        ExerciseSetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TrainrDatabase : RoomDatabase() {
    abstract val userDao: UserDao
}
