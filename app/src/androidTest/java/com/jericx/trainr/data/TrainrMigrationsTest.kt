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
}
