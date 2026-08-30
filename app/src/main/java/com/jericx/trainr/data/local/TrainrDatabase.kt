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
        WorkoutExerciseEntity::class,
        ExerciseSetEntity::class
    ],
    version = 5,
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

        // Per-set logging: how an exercise is measured, and a row per set
        // holding both what was asked for and what was done.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE workout_exercises " +
                        "ADD COLUMN measure TEXT NOT NULL DEFAULT 'REPS'"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exercise_sets (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        workoutExerciseId INTEGER NOT NULL,
                        setNumber INTEGER NOT NULL,
                        targetReps INTEGER,
                        targetWeightKg REAL,
                        targetSeconds INTEGER,
                        actualReps INTEGER,
                        actualWeightKg REAL,
                        actualSeconds INTEGER,
                        isCompleted INTEGER NOT NULL,
                        FOREIGN KEY(workoutExerciseId) REFERENCES workout_exercises(id)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_exercise_sets_workoutExerciseId " +
                        "ON exercise_sets (workoutExerciseId)"
                )
            }
        }

        // The generation contract: a plan knows the Monday it starts, and an
        // exercise carries the stable key its history is matched on.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE weekly_workout_plans ADD COLUMN startDateMillis INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE workout_exercises " +
                        "ADD COLUMN exerciseKey TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        // Duplicate weeks were reachable before the index below existed, so
        // they are cleared out first: the index cannot be created while any
        // remain. The earliest of each set is kept, being the one the client
        // was shown, and its children go with the rows that lose.
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM weekly_workout_plans WHERE id NOT IN (" +
                        "SELECT MIN(id) FROM weekly_workout_plans GROUP BY userId, weekNumber)"
                )
                db.execSQL(
                    "DELETE FROM workout_days WHERE weeklyPlanId NOT IN " +
                        "(SELECT id FROM weekly_workout_plans)"
                )
                db.execSQL(
                    "DELETE FROM workout_exercises WHERE workoutDayId NOT IN " +
                        "(SELECT id FROM workout_days)"
                )
                db.execSQL(
                    "DELETE FROM exercise_sets WHERE workoutExerciseId NOT IN " +
                        "(SELECT id FROM workout_exercises)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "index_weekly_workout_plans_userId_weekNumber " +
                        "ON weekly_workout_plans (userId, weekNumber)"
                )
            }
        }
    }
}
