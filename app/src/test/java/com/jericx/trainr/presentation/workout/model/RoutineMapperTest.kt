package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.WeightUnit
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import org.junit.Test

class RoutineMapperTest {

    private fun exercise(
        name: String,
        exerciseKey: String = "",
        durationMinutes: Int = 5,
        prescription: String = "3 sets of 10 reps",
        instructions: String = "Do the thing.",
        videoTutorialUrl: String? = null,
        isCompleted: Boolean = false,
        sets: List<ExerciseSet> = emptyList()
    ) = WorkoutExercise(
        name = name,
        exerciseKey = exerciseKey,
        durationMinutes = durationMinutes,
        prescription = prescription,
        instructions = instructions,
        videoTutorialUrl = videoTutorialUrl,
        isCompleted = isCompleted,
        sets = sets
    )

    private fun loaded(kg: Float) = exercise(
        "Goblet Squat",
        sets = listOf(ExerciseSet(setNumber = 1, targetReps = 10, targetWeightKg = kg))
    )

    private fun day(vararg exercises: WorkoutExercise) = WorkoutDay(
        dayNumber = 3,
        title = "Cardio & Core",
        status = WorkoutStatus.IN_PROGRESS,
        duration = 28,
        exerciseCount = exercises.size,
        equipment = listOf("Yoga Mat"),
        exercises = exercises.toList()
    )

    @Test
    fun carriesEveryFieldTheCardShows() {
        val routine = day(
            exercise(
                name = "Bicycle Crunches",
                durationMinutes = 5,
                prescription = "3 sets of 20 reps",
                instructions = "Alternating elbow-to-knee twists.",
                videoTutorialUrl = "https://youtu.be/kDPxFoCmb-w",
                isCompleted = true
            )
        ).toRoutineUi()

        assertThat(routine.title).isEqualTo("Cardio & Core")
        with(routine.exercises.single()) {
            assertThat(position).isEqualTo(1)
            assertThat(name).isEqualTo("Bicycle Crunches")
            assertThat(minutes).isEqualTo(5)
            assertThat(detail).isEqualTo("3 sets of 20 reps")
            assertThat(description).isEqualTo("Alternating elbow-to-knee twists.")
            assertThat(videoUrl).isEqualTo("https://youtu.be/kDPxFoCmb-w")
            assertThat(isCompleted).isTrue()
        }
    }

    // Position is the order they come in, not a stored field, so a reordered
    // routine renumbers itself rather than showing 1, 4, 2.
    @Test
    fun numbersExercisesByTheirOrder() {
        val routine = day(exercise("First"), exercise("Second"), exercise("Third")).toRoutineUi()

        assertThat(routine.exercises.map { it.position }).containsExactly(1, 2, 3).inOrder()
    }

    // The minutes and the prescription are independent: ten minutes of "5 sets
    // of 1 minute" is not five minutes.
    @Test
    fun keepsTheTotalSeparateFromThePrescription() {
        val routine = day(
            exercise(name = "Intervals", durationMinutes = 10, prescription = "5 sets of 1 minute")
        ).toRoutineUi()

        assertThat(routine.exercises.single().minutes).isEqualTo(10)
        assertThat(routine.totalMinutes).isEqualTo(10)
    }

    @Test
    fun aDayWithNoExercisesMapsToAnEmptyRoutine() {
        val routine = day().toRoutineUi()

        assertThat(routine.exercises).isEmpty()
        assertThat(routine.isComplete).isFalse()
    }

    @Test
    fun anExerciseUnknownToTheCatalogMapsToNoVideo() {
        val routine = day(
            exercise("Basket Weaving", exerciseKey = "basket_weaving", videoTutorialUrl = null)
        ).toRoutineUi()

        assertThat(routine.exercises.single().videoUrl).isNull()
    }

    @Test
    fun anExerciseWithoutAStoredVideoFallsBackToTheCatalog() {
        val routine = day(exercise("Plank", exerciseKey = "plank")).toRoutineUi()

        val videoUrl = routine.exercises.single().videoUrl
        assertThat(videoUrl).isNotNull()
        assertThat(videoUrl).isEqualTo(ExerciseVideoCatalog.urlFor("plank"))
    }

    @Test
    fun attachesPreviousSetsByExerciseKey() {
        val history = listOf(ExerciseSet(setNumber = 1, actualReps = 12, actualWeightKg = 20f))

        val routine = day(exercise("Plank", exerciseKey = "plank"))
            .toRoutineUi(previousByKey = mapOf("plank" to history))

        assertThat(routine.exercises.single().previousSets).isEqualTo(history)
    }

    @Test
    fun anExerciseWithoutHistoryCarriesNone() {
        val routine = day(exercise("Plank", exerciseKey = "plank")).toRoutineUi()

        assertThat(routine.exercises.single().previousSets).isEmpty()
    }

    @Test
    fun aStoredVideoWinsOverTheCatalog() {
        val routine = day(
            exercise(
                "Plank",
                exerciseKey = "plank",
                videoTutorialUrl = "https://youtu.be/abcdefghijk"
            )
        ).toRoutineUi()

        assertThat(routine.exercises.single().videoUrl).isEqualTo("https://youtu.be/abcdefghijk")
    }

    // 20 kg is 44.09 lb, which is not a dumbbell anyone owns.
    @Test
    fun aClientInPoundsIsPrescribedAWeightTheyCanLoad() {
        val routine = day(loaded(20f)).toRoutineUi(units = UnitSystem.IMPERIAL)

        val target = routine.exercises.single().sets.single().targetWeightKg!!
        assertThat(WeightUnit.forDisplay(target, UnitSystem.IMPERIAL)).isEqualTo(45f)
    }

    @Test
    fun aClientInKilogramsKeepsThePrescriptionAsWritten() {
        val routine = day(loaded(12f)).toRoutineUi(units = UnitSystem.METRIC)

        assertThat(routine.exercises.single().sets.single().targetWeightKg).isEqualTo(12f)
    }

    // Ticking an exercise off logs its target, so the number that gets stored
    // has to be the one the client was looking at.
    @Test
    fun loggingAPrescriptionRecordsTheWeightThatWasShown() {
        val logged = day(loaded(20f))
            .toRoutineUi(units = UnitSystem.IMPERIAL)
            .toggleCompleted(1)

        val actual = logged.exercises.single().sets.single().actualWeightKg!!
        assertThat(WeightUnit.forDisplay(actual, UnitSystem.IMPERIAL)).isEqualTo(45f)
    }
}
