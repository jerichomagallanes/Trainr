package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.catalog.InjuryGuard
import com.jericx.trainr.domain.generation.Prescription
import com.jericx.trainr.domain.generation.PrescriptionUnit
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.Injury
import java.io.File
import com.jericx.trainr.domain.model.ExerciseSet
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
            assertThat(description).isEqualTo("Alternating elbow-to-knee twists.")
            assertThat(videoUrl).isEqualTo("https://youtu.be/kDPxFoCmb-w")
            assertThat(isCompleted).isTrue()
        }
    }

    // Position is the order they come in, not a stored field, so a routine renumbers itself
    @Test
    fun numbersExercisesByTheirOrder() {
        val routine = day(exercise("First"), exercise("Second"), exercise("Third")).toRoutineUi()

        assertThat(routine.exercises.map { it.position }).containsExactly(1, 2, 3).inOrder()
    }

    // Minutes and prescription are independent: ten minutes of "5 sets of 1 minute" is not five
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

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())

    @Test
    fun theCatalogSaysHowAMovementIsDoneWhateverTheStoredWeekSays() {
        val routine = day(exercise("Goblet Squat", exerciseKey = "goblet_squat", instructions = "Written by a model."))
            .toRoutineUi(catalog = catalog)

        assertThat(routine.exercises.single().description).isEqualTo(catalog["goblet_squat"]!!.summary)
    }

    @Test
    fun theChipIsReadOffTheSetsNotTheStoredText() {
        val routine = day(
            exercise(
                "Squat",
                prescription = "a model's words",
                sets = (1..3).map { ExerciseSet(setNumber = it, targetReps = 10) }
            )
        ).toRoutineUi()

        assertThat(routine.exercises.single().prescription)
            .isEqualTo(Prescription.Fixed(3, PrescriptionUnit.REPS, 10, perSide = false))
    }

    @Test
    fun aOneSidedMovementIsCountedPerSide() {
        val oneSided = catalog.all.first { it.unilateral && it.measure != ExerciseMeasure.DURATION }

        val routine = day(
            exercise(oneSided.name, exerciseKey = oneSided.key, sets = listOf(ExerciseSet(setNumber = 1, targetReps = 8)))
        ).toRoutineUi(catalog = catalog)

        assertThat((routine.exercises.single().prescription as Prescription.Fixed).perSide).isTrue()
    }

    @Test
    fun aMovementAnInjuryAsksCareWithSaysWhichOnlyForThatClient() {
        val squat = catalog.all.first { InjuryGuard.cautionFor(it, listOf(Injury.KNEE)) != null }
        val day = day(exercise(squat.name, exerciseKey = squat.key))

        assertThat(day.toRoutineUi(catalog = catalog, injuries = listOf(Injury.KNEE)).exercises.single().caution)
            .isEqualTo(Injury.KNEE)
        assertThat(day.toRoutineUi(catalog = catalog).exercises.single().caution).isNull()
    }

    // Never lifted before means the weight is the app's guess; once there is
    // history, it is the client's own number moved on.
    @Test
    fun aWeightNeverLiftedBeforeIsMarkedAsAGuess() {
        val weighted = loaded(20f).copy(exerciseKey = "goblet_squat", measure = ExerciseMeasure.WEIGHT_AND_REPS)
        val history = listOf(ExerciseSet(setNumber = 1, actualReps = 10, actualWeightKg = 20f, isCompleted = true))

        assertThat(day(weighted).toRoutineUi().exercises.single().isEstimated).isTrue()
        assertThat(day(weighted).toRoutineUi(previousByKey = mapOf("goblet_squat" to history)).exercises.single().isEstimated)
            .isFalse()
        assertThat(day(exercise("Push Up", sets = listOf(ExerciseSet(setNumber = 1, targetReps = 10))))
            .toRoutineUi().exercises.single().isEstimated).isFalse()
    }
}
