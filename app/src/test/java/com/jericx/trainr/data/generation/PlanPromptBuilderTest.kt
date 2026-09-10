package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.catalog.CatalogExercise
import com.jericx.trainr.domain.catalog.MovementPattern
import com.jericx.trainr.domain.catalog.MuscleGroup
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.model.UnitSystem
import org.junit.Test

class PlanPromptBuilderTest {

    private fun catalogExercise(
        key: String,
        muscle: MuscleGroup,
        measure: ExerciseMeasure,
        pattern: MovementPattern,
        equipment: Equipment = Equipment.NONE
    ) = CatalogExercise(key, key, key, muscle, null, equipment, measure, pattern, staple = true)

    private val shortlist = listOf(
        catalogExercise(
            "goblet_squat", MuscleGroup.QUADRICEPS,
            ExerciseMeasure.WEIGHT_AND_REPS, MovementPattern.SQUAT, Equipment.DUMBBELL
        ),
        catalogExercise("push_up", MuscleGroup.CHEST, ExerciseMeasure.REPS, MovementPattern.HORIZONTAL_PUSH),
        catalogExercise("plank", MuscleGroup.ABDOMINALS, ExerciseMeasure.DURATION, MovementPattern.CORE)
    )

    private val builder = PlanPromptBuilder()

    private fun request(
        languageCode: String = "en",
        previousWeek: WeeklyWorkoutPlan? = null,
        units: UnitSystem = UnitSystem.METRIC
    ) = PlanRequest(
        user = UserProfile(
            id = 1,
            age = 30,
            height = 170f,
            weight = 70f,
            fitnessGoal = FitnessGoal.MUSCLE_GAIN,
            availableEquipment = listOf(Equipment.DUMBBELL, Equipment.MACHINE),
            workoutDaysPerWeek = 3,
            workoutDuration = 45,
            injuries = listOf(Injury.LOWER_BACK),
            bodyUnitSystem = units
        ),
        weekNumber = if (previousWeek == null) 1 else 2,
        startDateMillis = 0L,
        languageCode = languageCode,
        previousWeek = previousWeek
    )

    @Test
    fun thePromptCarriesEverythingTheCoachMustRespect() {
        val prompt = builder.userPrompt(request(), shortlist)

        assertThat(prompt).contains("build muscle")
        assertThat(prompt).contains("dumbbells, machines and cables")
        assertThat(prompt).contains("3 (plan EXACTLY this many days)")
        assertThat(prompt).contains("about 45 minutes")
        assertThat(prompt).contains("lower back pain")
        assertThat(prompt).contains("English")
    }

    @Test
    fun displayCopyLanguageFollowsTheAppLanguage() {
        assertThat(builder.userPrompt(request(languageCode = "ja"), shortlist)).contains("Japanese")
        assertThat(builder.userPrompt(request(languageCode = "tl"), shortlist)).contains("Tagalog")
    }

    @Test
    fun weekOneCarriesNoHistory() {
        assertThat(builder.userPrompt(request(), shortlist)).doesNotContain("Last week")
    }

    @Test
    fun historyReportsWhatWasActuallyDonePerSet() {
        val previous = WeeklyWorkoutPlan(
            userId = 1,
            weekNumber = 1,
            title = "Week 1",
            workoutDays = listOf(
                WorkoutDay(
                    dayNumber = 1,
                    title = "Full Body",
                    status = WorkoutStatus.COMPLETED,
                    duration = 45,
                    exerciseCount = 1,
                    equipment = emptyList(),
                    exercises = listOf(
                        WorkoutExercise(
                            exerciseKey = "goblet_squat",
                            name = "Goblet Squats",
                            measure = ExerciseMeasure.WEIGHT_AND_REPS,
                            prescription = "2 sets of 12 reps",
                            durationMinutes = 8,
                            sets = listOf(
                                ExerciseSet(
                                    setNumber = 1,
                                    targetReps = 12,
                                    actualReps = 12,
                                    actualWeightKg = 20f,
                                    isCompleted = true
                                ),
                                ExerciseSet(setNumber = 2, targetReps = 12)
                            )
                        )
                    )
                ),
                WorkoutDay(
                    dayNumber = 3,
                    title = "Skipped Day",
                    status = WorkoutStatus.NOT_STARTED,
                    duration = 30,
                    exerciseCount = 0,
                    equipment = emptyList()
                )
            )
        )

        val prompt = builder.userPrompt(request(previousWeek = previous), shortlist)

        assertThat(prompt).contains("Last week (week 1)")
        assertThat(prompt).contains("goblet_squat: prescribed \"2 sets of 12 reps\"")
        assertThat(prompt).contains("20.0kg x 12")
        assertThat(prompt).contains("skipped")
        assertThat(prompt).contains("Skipped Day (skipped)")
    }

    // The vocabulary is the client's own, so it belongs in the request and not
    // in a brief that is identical for everyone.
    @Test
    fun theVocabularyIsListedByHowEachMovementIsMeasured() {
        val prompt = builder.userPrompt(request(), shortlist)

        assertThat(prompt).contains("Movements you may prescribe")
        assertThat(prompt).contains("Weighted - every set needs reps and weightKg:")
        assertThat(prompt).contains("QUADRICEPS: goblet_squat")
        assertThat(prompt).contains("Bodyweight - every set needs reps:")
        assertThat(prompt).contains("CHEST: push_up")
        assertThat(builder.systemInstruction()).doesNotContain("goblet_squat")
    }

    // Programming around what the client owns is the app's job, so the demand
    // is only made where the shortlist can meet it.
    @Test
    fun theWeekIsToldWhichPatternsItMustCover() {
        val prompt = builder.userPrompt(request(), shortlist)

        assertThat(prompt).contains("a squat or lunge")
        assertThat(prompt).contains("an upper-body press")
        assertThat(builder.userPrompt(request(), emptyList()))
            .doesNotContain("The week must include")
    }

    // In pounds the gym's step is 5 lb, so a 2.5% rise on 20 kg reads back as the same 45 lb
    @Test
    fun theBriefNamesTheIncrementTheClientCanActuallyLoad() {
        val metric = PlanPromptBuilder().userPrompt(request(units = UnitSystem.METRIC), shortlist)
        assertThat(metric).contains("Reads weights in kilograms")
        assertThat(metric).contains("increment 2.5 kg")

        val imperial = PlanPromptBuilder().userPrompt(request(units = UnitSystem.IMPERIAL), shortlist)
        assertThat(imperial).contains("Reads weights in pounds")
        assertThat(imperial).contains("increment 2.27 kg")
    }

    @Test
    fun theContractStaysInKilogramsWhicheverTheClientReads() {
        val imperial = PlanPromptBuilder().systemInstruction()

        assertThat(imperial).contains("Weights are kilograms")
        assertThat(imperial).contains("multiple of the client's smallest loadable")
    }

    @Test
    fun theCoachingBriefKeepsItsLoadBearingRules() {
        val brief = builder.systemInstruction()

        assertThat(brief).contains("chosen from the movement list")
        assertThat(brief).contains("warm-up")
        assertThat(brief).contains("kilograms")
        assertThat(brief).contains("strength 3-6 reps")
        assertThat(brief).contains("never by distance")
        assertThat(brief).contains("injuries strictly")
        assertThat(brief).contains("JSON only")
        assertThat(brief).contains("never letter or index labels")
        assertThat(brief).contains("under about 25 characters")
    }
}
