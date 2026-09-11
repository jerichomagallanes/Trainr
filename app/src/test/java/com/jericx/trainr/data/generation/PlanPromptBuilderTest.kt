package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder
import com.jericx.trainr.domain.generation.SessionFocus
import com.jericx.trainr.domain.generation.SkeletonDay
import com.jericx.trainr.domain.generation.SkeletonSlot
import com.jericx.trainr.domain.generation.SlotTier
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import java.io.File
import org.junit.Test

class PlanPromptBuilderTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())
    private val skeletons = PlanSkeletonBuilder(catalog)
    private val builder = PlanPromptBuilder()

    private fun user(
        goal: FitnessGoal = FitnessGoal.MUSCLE_GAIN,
        days: Int = 4,
        minutes: Int = 60,
        experience: ExperienceLevel = ExperienceLevel.INTERMEDIATE
    ) = UserProfile(
        id = 1, age = 34, height = 170f, weight = 70f, fitnessGoal = goal, experienceLevel = experience,
        availableEquipment = Equipment.entries.toList(), workoutDaysPerWeek = days, workoutDuration = minutes,
        injuries = listOf(Injury.LOWER_BACK)
    )

    private fun request(user: UserProfile = user(), history: List<WeeklyWorkoutPlan> = emptyList()) =
        PlanRequest(user, weekNumber = 2, startDateMillis = 0L, history = history)

    private fun prompt(request: PlanRequest = request()) = builder.userPrompt(request, skeletons.build(request))

    @Test
    fun thePromptNamesTheWeekTheClientAndEverySessionWithAChoiceLeft() {
        val request = request()
        val skeleton = skeletons.build(request)
        val prompt = builder.userPrompt(request, skeleton)

        assertThat(prompt).contains("Choose the movements for week 2.")
        assertThat(prompt).contains("Client: 34, intermediate, training to build muscle.")
        skeleton.days.filter { it.openSlots.isNotEmpty() }.forEach { day ->
            assertThat(prompt).contains("- ${day.id}, ${day.focus.title.lowercase()}, ${day.openSlots.size} slots")
        }
    }

    // Each was a rule the model could disobey. None of them is now: the
    // schema holds the movements, the skeleton the budget, and the injury
    // guard every list.
    @Test
    fun thePromptCarriesNoVocabularyNoBudgetNoBodyAndNoInjuries() {
        val prompt = prompt()

        listOf("_", "goblet", "kg", "minutes", "sets", "170", "70 ", "lower back", "injur", "dumbbell", "Last week")
            .forEach { assertWithMessage(it).that(prompt).doesNotContain(it) }
    }

    @Test
    fun historyNeverReachesThePrompt() {
        val lastWeek = WeeklyWorkoutPlan(userId = 1, weekNumber = 1, title = "Week 1", workoutDays = emptyList())

        assertThat(prompt(request(history = listOf(lastWeek)))).doesNotContain("week 1")
    }

    @Test
    fun theLargestWeekStillAsksInAFewLines() {
        FitnessGoal.entries.forEach { goal ->
            (1..7).forEach { days ->
                val prompt = prompt(request(user(goal, days, 90, ExperienceLevel.ADVANCED)))
                assertWithMessage("$goal ${days}d").that(prompt.length).isLessThan(600)
            }
        }
    }

    @Test
    fun aSessionWithNothingLeftToChooseIsNotListed() {
        fun slot(id: String, vararg candidates: String) = SkeletonSlot(
            id = id, label = "the $id", tier = SlotTier.ACCESSORY, patterns = emptyList(), muscles = emptySet(),
            candidates = candidates.toList(), sets = 3, restSeconds = 60
        )
        val skeleton = PlanSkeleton(
            title = "Test Week",
            days = listOf(
                SkeletonDay(1, SessionFocus.FULL_BODY, listOf(slot("warm_up", "arm_circles"), slot("primary", "a", "b"))),
                SkeletonDay(4, SessionFocus.MOBILITY_FLOW, listOf(slot("mobility", "stretching")))
            ),
            units = UnitSystem.METRIC, maxSetsPerSession = 20, sessionCeilingMinutes = 90,
            weeklySetsByRegion = emptyMap(), uncoveredPatterns = emptySet()
        )

        val prompt = builder.userPrompt(request(), skeleton)

        assertThat(prompt).contains("- day1, full body, 1 slot\n")
        assertThat(prompt).doesNotContain("day4")
    }

    @Test
    fun theBriefNoLongerPricesSetsSplitsTheWeekRestatesInjuriesOrWritesCopy() {
        val brief = builder.systemInstruction()

        listOf("weightKg", "reps", "prescription", "injur", "kilograms", "push/pull/legs", "reserve", "exactly")
            .forEach { assertWithMessage(it).that(brief).doesNotContain(it) }
        assertThat(brief.length).isLessThan(1_500)
    }

    @Test
    fun theBriefKeepsWhatOnlyTheModelCanDo() {
        val brief = builder.systemInstruction()

        assertThat(brief).contains("which movement fills each slot")
        assertThat(brief).contains("near-versions of the same movement")
        assertThat(brief).contains("never \"Day 2\"")
        assertThat(brief).contains("JSON only")
    }
}
