package com.jericx.trainr.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.local.TrainrDatabase
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainrMigrationsTest {

    private val name = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TrainrDatabase::class.java
    )

    @After
    fun deleteTheFile() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(name)
    }

    @Test
    fun aVersionFourFileKeepsItsRowsWhenOpened() {
        helper.createDatabase(name, 4).apply {
            execSQL(
                """
                INSERT INTO users (id, firstName, age, gender, height, weight, fitnessGoal,
                    experienceLevel, availableEquipment, workoutDaysPerWeek, workoutDuration,
                    injuries, bodyUnitSystem, liftingUnitSystem, createdAt)
                VALUES (1, 'Jeco', 26, 'MALE', 175.0, 72.0, 'MUSCLE_GAIN', 'BEGINNER',
                    '["DUMBBELL"]', 3, 45, '["LOWER_BACK"]', 'METRIC', NULL, 1700000000000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO weekly_workout_plans (id, userId, weekNumber, title, startDateMillis,
                    createdAt, updatedAt)
                VALUES (10, 1, 1, 'Week 1', NULL, 1700000000000, 1700000000000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_days (id, weeklyPlanId, dayNumber, title, status, duration,
                    exerciseCount, equipment, completedAt)
                VALUES (20, 10, 1, 'Push', 'COMPLETED', 45, 1, '["DUMBBELL"]', 1700000100000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_exercises (id, workoutDayId, exerciseKey, name, measure, sets,
                    reps, duration, durationMinutes, restTime, equipment, videoTutorialUrl,
                    isCompleted, notes)
                VALUES (30, 20, 'dumbbell_bench_press', 'Dumbbell Bench Press', 'REPS', 2,
                    '8-12', NULL, 10, 90, '["DUMBBELL"]', NULL, 1, '')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO exercise_sets (id, workoutExerciseId, setNumber, targetReps,
                    targetWeightKg, targetSeconds, actualReps, actualWeightKg, actualSeconds,
                    isCompleted)
                VALUES (40, 30, 1, 10, 20.0, NULL, 10, 20.0, NULL, 1),
                    (41, 30, 2, 10, 20.0, NULL, 8, 22.5, NULL, 1)
                """.trimIndent()
            )
            close()
        }

        val database = helper.runMigrationsAndValidate(name, 4, true, *TrainrDatabase.MIGRATIONS)

        database.query(
            "SELECT id, actualReps, actualWeightKg FROM exercise_sets ORDER BY setNumber"
        ).use { cursor ->
            assertThat(cursor.count).isEqualTo(2)

            assertThat(cursor.moveToNext()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(40)
            assertThat(cursor.getInt(1)).isEqualTo(10)
            assertThat(cursor.getFloat(2)).isEqualTo(20.0f)

            assertThat(cursor.moveToNext()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(41)
            assertThat(cursor.getInt(1)).isEqualTo(8)
            assertThat(cursor.getFloat(2)).isEqualTo(22.5f)
        }
        database.close()
    }

    private fun seedVersionFour() {
        helper.createDatabase(name, 4).apply {
            execSQL(
                """
                INSERT INTO users (id, firstName, age, gender, height, weight, fitnessGoal,
                    experienceLevel, availableEquipment, workoutDaysPerWeek, workoutDuration,
                    injuries, bodyUnitSystem, liftingUnitSystem, createdAt)
                VALUES (1, 'Jeco', 26, 'MALE', 175.0, 72.0, 'MUSCLE_GAIN', 'BEGINNER',
                    '["DUMBBELL"]', 3, 45, '[]', 'METRIC', NULL, 1700000000000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO weekly_workout_plans (id, userId, weekNumber, title, startDateMillis,
                    createdAt, updatedAt)
                VALUES (1, 1, 1, 'Week 1', NULL, 1700000000000, 1700000000000)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_days (id, weeklyPlanId, dayNumber, title, status, duration,
                    exerciseCount, equipment, completedAt)
                VALUES (1, 1, 1, 'Full body', 'IN_PROGRESS', 45, 2, '["DUMBBELL"]', NULL)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO workout_exercises (id, workoutDayId, exerciseKey, name, measure, sets,
                    reps, duration, durationMinutes, restTime, equipment, videoTutorialUrl,
                    isCompleted, notes)
                VALUES (10, 1, 'goblet_squat', 'Goblet Squat', 'WEIGHT_AND_REPS', 3,
                        '8-12', NULL, 12, 90, '["DUMBBELL"]', NULL, 0, ''),
                    (11, 1, 'plank', 'Plank', 'DURATION', 1,
                        NULL, '30s', 2, 60, '[]', NULL, 0, '')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO exercise_sets (id, workoutExerciseId, setNumber, targetReps,
                    targetWeightKg, targetSeconds, actualReps, actualWeightKg, actualSeconds,
                    isCompleted)
                VALUES (100, 10, 1, 10, 40.0, NULL, 8, 40.0, NULL, 1),
                    (101, 10, 2, 10, 40.0, NULL, NULL, NULL, 30, 0),
                    (102, 10, 3, 10, 40.0, NULL, NULL, NULL, NULL, 0),
                    (200, 11, 1, NULL, NULL, 30, NULL, NULL, NULL, 0)
                """.trimIndent()
            )
            close()
        }
    }

    @Test
    fun migratingToFiveKeepsEveryLoggedSetAndOrdersExercises() {
        seedVersionFour()

        val database = helper.runMigrationsAndValidate(name, 5, true, *TrainrDatabase.MIGRATIONS)

        database.query(
            """
            SELECT id, targetReps, targetWeightKg, targetSeconds, actualReps, actualWeightKg,
                actualSeconds, isCompleted, actualOrigin, omittedBy
            FROM exercise_sets ORDER BY id
            """.trimIndent()
        ).use { cursor ->
            assertThat(cursor.count).isEqualTo(4)

            assertThat(cursor.moveToNext()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(100)
            assertThat(cursor.getInt(1)).isEqualTo(10)
            assertThat(cursor.getFloat(2)).isEqualTo(40.0f)
            assertThat(cursor.isNull(3)).isTrue()
            assertThat(cursor.getInt(4)).isEqualTo(8)
            assertThat(cursor.getFloat(5)).isEqualTo(40.0f)
            assertThat(cursor.isNull(6)).isTrue()
            assertThat(cursor.getInt(7)).isEqualTo(1)
            assertThat(cursor.getString(8)).isEqualTo("LEGACY_UNKNOWN")
            assertThat(cursor.isNull(9)).isTrue()

            assertThat(cursor.moveToNext()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(101)
            assertThat(cursor.getInt(1)).isEqualTo(10)
            assertThat(cursor.getFloat(2)).isEqualTo(40.0f)
            assertThat(cursor.isNull(4)).isTrue()
            assertThat(cursor.isNull(5)).isTrue()
            assertThat(cursor.getInt(6)).isEqualTo(30)
            assertThat(cursor.getInt(7)).isEqualTo(0)
            assertThat(cursor.getString(8)).isEqualTo("LEGACY_UNKNOWN")
            assertThat(cursor.isNull(9)).isTrue()

            assertThat(cursor.moveToNext()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(102)
            assertThat(cursor.getInt(1)).isEqualTo(10)
            assertThat(cursor.getFloat(2)).isEqualTo(40.0f)
            assertThat(cursor.isNull(4)).isTrue()
            assertThat(cursor.isNull(5)).isTrue()
            assertThat(cursor.isNull(6)).isTrue()
            assertThat(cursor.getInt(7)).isEqualTo(0)
            assertThat(cursor.getString(8)).isEqualTo("NONE")
            assertThat(cursor.isNull(9)).isTrue()

            assertThat(cursor.moveToNext()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(200)
            assertThat(cursor.isNull(1)).isTrue()
            assertThat(cursor.isNull(2)).isTrue()
            assertThat(cursor.getInt(3)).isEqualTo(30)
            assertThat(cursor.isNull(4)).isTrue()
            assertThat(cursor.isNull(5)).isTrue()
            assertThat(cursor.isNull(6)).isTrue()
            assertThat(cursor.getInt(7)).isEqualTo(0)
            assertThat(cursor.getString(8)).isEqualTo("NONE")
            assertThat(cursor.isNull(9)).isTrue()
        }

        database.query(
            "SELECT id, sortOrder, addedBy FROM workout_exercises ORDER BY sortOrder, id"
        ).use { cursor ->
            assertThat(cursor.count).isEqualTo(2)

            assertThat(cursor.moveToNext()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(10)
            assertThat(cursor.getInt(1)).isEqualTo(0)
            assertThat(cursor.isNull(2)).isTrue()

            assertThat(cursor.moveToNext()).isTrue()
            assertThat(cursor.getLong(0)).isEqualTo(11)
            assertThat(cursor.getInt(1)).isEqualTo(1)
            assertThat(cursor.isNull(2)).isTrue()
        }

        database.query("PRAGMA foreign_key_check").use { cursor ->
            assertThat(cursor.count).isEqualTo(0)
        }
        database.close()
    }

    @Test
    fun migratingToFiveCreatesTheUnstuckTables() {
        seedVersionFour()

        val database = helper.runMigrationsAndValidate(name, 5, true, *TrainrDatabase.MIGRATIONS)

        database.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name"
        ).use { cursor ->
            val tables = generateSequence { if (cursor.moveToNext()) cursor.getString(0) else null }.toList()
            assertThat(tables).containsAtLeast(
                "session_outcomes",
                "applied_adjustments",
                "adjustment_feedback",
                "training_preferences",
                "session_notes"
            )
        }
        database.query("PRAGMA foreign_key_check").use { cursor ->
            assertThat(cursor.count).isEqualTo(0)
        }
        database.close()
    }
}
