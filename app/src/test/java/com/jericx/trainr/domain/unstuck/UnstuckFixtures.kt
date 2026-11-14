package com.jericx.trainr.domain.unstuck

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Gender
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import java.io.File

internal val testCatalog: ExerciseCatalog =
    ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())

internal fun testUser(
    goal: FitnessGoal = FitnessGoal.MUSCLE_GAIN,
    kit: List<Equipment> = Equipment.entries,
    injuries: List<Injury> = emptyList(),
    minutes: Int = 45
) = UserProfile(
    age = 30,
    gender = Gender.MALE,
    weight = 80f,
    fitnessGoal = goal,
    experienceLevel = ExperienceLevel.INTERMEDIATE,
    availableEquipment = kit,
    workoutDaysPerWeek = 3,
    workoutDuration = minutes,
    injuries = injuries
)

internal fun testDay(vararg exercises: WorkoutExercise, id: Long = 7L) = WorkoutDay(
    id = id,
    dayNumber = 1,
    title = "Day 1",
    duration = 45,
    exerciseCount = exercises.size,
    equipment = emptyList(),
    exercises = exercises.toList()
)

internal fun planned(
    key: String,
    sets: Int,
    id: Long,
    sortOrder: Int = id.toInt(),
    performed: Int = 0,
    omitted: Int = 0,
    reps: Int = 8,
    seconds: Int = 300,
    rest: Int? = null,
    weightKg: Float? = null
): WorkoutExercise {
    val entry = testCatalog[key]
    val timed = entry?.measure == ExerciseMeasure.DURATION
    return WorkoutExercise(
        id = id,
        exerciseKey = key,
        name = entry?.name ?: key,
        measure = entry?.measure ?: ExerciseMeasure.REPS,
        restTime = rest,
        sortOrder = sortOrder,
        sets = (1..sets).map { number ->
            ExerciseSet(
                id = id * SET_ID_STRIDE + number,
                setNumber = number,
                targetReps = if (timed) null else reps,
                targetSeconds = if (timed) seconds else null,
                targetWeightKg = weightKg,
                isCompleted = number <= performed,
                omittedBy = if (number > sets - omitted) OMITTED_BY else null
            )
        }
    )
}

internal fun WorkoutExercise.logged(setNumber: Int, reps: Int): WorkoutExercise = copy(
    sets = sets.map {
        if (it.setNumber == setNumber) it.copy(actualReps = reps, isCompleted = true) else it
    }
)

internal fun assertWellFormed(proposal: AdjustmentProposal, catalog: ExerciseCatalog) {
    assertThat(proposal.changes).isNotEmpty()
    assertThat(proposal.policyVersion).isEqualTo(UnstuckPolicy.VERSION)
    assertThat(ProposalJson.decode(ProposalJson.encode(proposal))).isEqualTo(proposal)
    proposal.changes.forEach { change ->
        assertThat(change.before.sets).isNotEmpty()
        assertThat(catalog[change.before.catalogKey]).isNotNull()
        when (change.kind) {
            ChangeKind.REDUCE_UNPERFORMED -> {
                val after = checkNotNull(change.after)
                assertThat(after.catalogKey).isEqualTo(change.before.catalogKey)
                assertThat(after.exerciseInstanceId).isEqualTo(change.before.exerciseInstanceId)
                assertThat(after.sets.size).isLessThan(change.before.sets.size)
                assertThat(change.before.sets.map { it.setId })
                    .containsAtLeastElementsIn(after.sets.map { it.setId })
            }

            ChangeKind.OMIT_UNPERFORMED -> assertThat(change.after).isNull()

            ChangeKind.REPLACE_UNPERFORMED -> {
                val after = checkNotNull(change.after)
                assertThat(catalog[after.catalogKey]).isNotNull()
                assertThat(after.exerciseInstanceId).isEqualTo("new")
            }
        }
    }
}

private const val SET_ID_STRIDE = 100L
private const val OMITTED_BY = 1L
