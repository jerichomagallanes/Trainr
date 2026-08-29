package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import org.junit.Test

class RoutineMapperTest {

    private fun exercise(
        name: String,
        durationMinutes: Int = 5,
        prescription: String = "3 sets of 10 reps",
        instructions: String = "Do the thing.",
        videoTutorialUrl: String? = null,
        isCompleted: Boolean = false
    ) = WorkoutExercise(
        name = name,
        durationMinutes = durationMinutes,
        prescription = prescription,
        instructions = instructions,
        videoTutorialUrl = videoTutorialUrl,
        isCompleted = isCompleted
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
    fun anExerciseWithNoVideoMapsToNoVideo() {
        val routine = day(exercise("Plank", videoTutorialUrl = null)).toRoutineUi()

        assertThat(routine.exercises.single().videoUrl).isNull()
    }
}
