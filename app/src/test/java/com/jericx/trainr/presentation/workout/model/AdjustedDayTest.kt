package com.jericx.trainr.presentation.workout.model

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import org.junit.Test

class AdjustedDayTest {

    private val catalog = InMemoryExerciseCatalog(
        listOf(
            movement("dumbbell_row", Equipment.DUMBBELL),
            movement("dumbbell_curl", Equipment.DUMBBELL),
            movement("kettlebell_swing", Equipment.KETTLEBELL),
            movement("push_up", Equipment.NONE)
        )
    )

    private fun movement(key: String, equipment: Equipment) = CatalogExercise(
        key = key,
        name = key,
        primary = MuscleGroup.UPPER_BACK,
        secondary = emptyList(),
        equipment = equipment,
        measure = ExerciseMeasure.REPS,
        pattern = MovementPattern.HORIZONTAL_PULL,
        staple = false
    )

    private fun exercise(key: String, id: Long, omitted: Int = 0, addedBy: Long? = null) =
        WorkoutExercise(
            id = id,
            exerciseKey = key,
            name = key,
            addedBy = addedBy,
            sets = (1..3).map {
                ExerciseSet(setNumber = it, targetReps = 10, omittedBy = if (it > 3 - omitted) 1L else null)
            }
        )

    private fun day(vararg exercises: WorkoutExercise, equipment: List<String> = listOf("Dumbbells", "Yoga Mat")) =
        WorkoutDay(
            dayNumber = 1,
            title = "Pull",
            duration = 30,
            exerciseCount = exercises.size,
            equipment = equipment,
            exercises = exercises.toList()
        )

    @Test
    fun anUnadjustedDayShowsTheLineAsStored() {
        val day = day(exercise("dumbbell_row", 1), exercise("push_up", 2))

        assertThat(day.derivedEquipment(catalog)).containsExactly("Dumbbells", "Yoga Mat").inOrder()
    }

    // A cut that keeps every exercise changes nothing about what the day needs.
    @Test
    fun fewerSetsLeaveTheLineAlone() {
        val day = day(exercise("dumbbell_row", 1, omitted = 1), exercise("push_up", 2))

        assertThat(day.derivedEquipment(catalog)).containsExactly("Dumbbells", "Yoga Mat").inOrder()
    }

    @Test
    fun anOmittedExerciseTakesItsKitWithItButNotTheMat() {
        val day = day(exercise("dumbbell_row", 1, omitted = 3), exercise("push_up", 2))

        assertThat(day.derivedEquipment(catalog)).containsExactly("Yoga Mat")
    }

    @Test
    fun kitStaysWhileAnotherVisibleExerciseNeedsIt() {
        val day = day(exercise("dumbbell_row", 1, omitted = 3), exercise("dumbbell_curl", 2))

        assertThat(day.derivedEquipment(catalog)).containsExactly("Dumbbells", "Yoga Mat").inOrder()
    }

    @Test
    fun aSubstituteAddsItsKitOnce() {
        val swapped = day(
            exercise("dumbbell_row", 1, omitted = 3),
            exercise("kettlebell_swing", 3, addedBy = 1L),
            exercise("push_up", 2)
        )

        assertThat(swapped.derivedEquipment(catalog)).containsExactly("Yoga Mat", "Kettlebell").inOrder()
        assertThat(swapped.copy(equipment = listOf("Kettlebells")).derivedEquipment(catalog))
            .containsExactly("Kettlebells")
    }

    @Test
    fun aBodyweightSubstituteNeverLeavesTheLineBlank() {
        val day = day(
            exercise("dumbbell_row", 1, omitted = 3),
            exercise("push_up", 3, addedBy = 1L),
            equipment = listOf("Dumbbells")
        )

        assertThat(day.derivedEquipment(catalog)).containsExactly("Dumbbells")
    }
}
