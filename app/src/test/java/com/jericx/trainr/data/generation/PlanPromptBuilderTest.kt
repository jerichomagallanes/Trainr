package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExerciseSet
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutStatus
import org.junit.Test

class PlanPromptBuilderTest {

    private val builder = PlanPromptBuilder()

    private fun request(
        languageCode: String = "en",
        previousWeek: WeeklyWorkoutPlan? = null
    ) = PlanRequest(
        user = UserProfile(
            id = 1,
            age = 30,
            height = 170f,
            weight = 70f,
            fitnessGoal = FitnessGoal.MUSCLE_GAIN,
            availableEquipment = listOf(Equipment.DUMBBELLS, Equipment.PULL_UP_BAR),
            workoutDaysPerWeek = 3,
            workoutDuration = 45,
            injuries = listOf("Lower Back Pain")
        ),
        weekNumber = if (previousWeek == null) 1 else 2,
        startDateMillis = 0L,
        languageCode = languageCode,
        previousWeek = previousWeek
    )

    @Test
    fun thePromptCarriesEverythingTheCoachMustRespect() {
        val prompt = builder.userPrompt(request())

        assertThat(prompt).contains("build muscle")
        assertThat(prompt).contains("dumbbells, pull up bar")
        assertThat(prompt).contains("3 (plan EXACTLY this many days)")
        assertThat(prompt).contains("about 45 minutes")
        assertThat(prompt).contains("Lower Back Pain")
        assertThat(prompt).contains("English")
    }

    @Test
    fun displayCopyLanguageFollowsTheAppLanguage() {
        assertThat(builder.userPrompt(request(languageCode = "ja"))).contains("Japanese")
        assertThat(builder.userPrompt(request(languageCode = "tl"))).contains("Tagalog")
    }

    @Test
    fun weekOneCarriesNoHistory() {
        assertThat(builder.userPrompt(request())).doesNotContain("Last week")
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

        val prompt = builder.userPrompt(request(previousWeek = previous))

        assertThat(prompt).contains("Last week (week 1)")
        assertThat(prompt).contains("goblet_squat: prescribed \"2 sets of 12 reps\"")
        assertThat(prompt).contains("20.0kg x 12")
        assertThat(prompt).contains("skipped")
        assertThat(prompt).contains("Skipped Day (skipped)")
    }

    // A near-duplicate key (dumbbell_goblet_squat next to goblet_squat) splits
    // history and loses the tutorial, so the vocabulary must reach the brief.
    @Test
    fun theCanonicalVocabularyIsPinnedInTheBrief() {
        val brief = PlanPromptBuilder(canonicalKeys = setOf("goblet_squat", "plank"))
            .systemInstruction()

        assertThat(brief).contains("goblet_squat, plank")
        assertThat(brief).contains("near-duplicate")
        assertThat(PlanPromptBuilder().systemInstruction()).doesNotContain("near-duplicate")
    }

    // Anchors of the coaching brief the plans' quality hangs on; if one of
    // these leaves the system prompt it should be a deliberate decision.
    @Test
    fun theCoachingBriefKeepsItsLoadBearingRules() {
        val brief = builder.systemInstruction()

        assertThat(brief).contains("lower_snake_case")
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
