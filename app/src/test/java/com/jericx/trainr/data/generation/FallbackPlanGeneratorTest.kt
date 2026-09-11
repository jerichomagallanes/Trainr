package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.catalog.ExerciseCatalogReader
import com.jericx.trainr.domain.catalog.InMemoryExerciseCatalog
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSource
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FallbackPlanGeneratorTest {

    private val catalog = ExerciseCatalogReader.read(File("src/main/assets/exercise-catalog.json").readText())
    private val request = PlanRequest(UserProfile(id = 7, age = 30, weight = 80f), weekNumber = 1, startDateMillis = 0L)
    private val coachedWeek = WeeklyWorkoutPlan(userId = 7, weekNumber = 1, title = "Coached Week", workoutDays = emptyList())

    private class Answering(private val result: PlanGenerationResult) : PlanGenerator {
        var asked = 0
        override suspend fun generate(request: PlanRequest): PlanGenerationResult {
            asked++
            return result
        }
    }

    @Test
    fun aCoachedWeekIsHandedOverAsTheCoachsAndNothingIsBuilt() = runTest {
        val template = Answering(PlanGenerationResult.Failed)

        val result = FallbackPlanGenerator(Answering(PlanGenerationResult.Generated(coachedWeek)), template)
            .generate(request)

        assertThat(result).isEqualTo(PlanGenerationResult.Generated(coachedWeek, PlanSource.COACH))
        assertThat(template.asked).isEqualTo(0)
    }

    @Test
    fun everyWayTheCoachCanFailStillEndsInAWeekThatSaysWhy() = runTest {
        listOf(PlanGenerationResult.Offline, PlanGenerationResult.Failed, PlanGenerationResult.DailyLimitReached)
            .forEach { failure ->
                val result = FallbackPlanGenerator(Answering(failure), TemplatePlanGenerator(catalog))
                    .generate(request) as PlanGenerationResult.Generated

                assertThat(result.source).isEqualTo(PlanSource.TEMPLATE)
                assertThat(result.insteadOf).isEqualTo(failure)
                assertThat(result.plan.workoutDays).isNotEmpty()
            }
    }

    // Standing in for the coach is a dev build's arrangement; a week that
    // replaced a failed answer is always named for what it is.
    @Test
    fun aWeekBuiltInsteadIsATemplateWhateverTheTemplateCallsItself() = runTest {
        val result = FallbackPlanGenerator(
            Answering(PlanGenerationResult.Offline),
            TemplatePlanGenerator(catalog, source = PlanSource.COACH)
        ).generate(request) as PlanGenerationResult.Generated

        assertThat(result.source).isEqualTo(PlanSource.TEMPLATE)
    }

    @Test
    fun withNothingToBuildFromTheCoachsOwnReasonIsReported() = runTest {
        val result = FallbackPlanGenerator(
            Answering(PlanGenerationResult.DailyLimitReached),
            TemplatePlanGenerator(InMemoryExerciseCatalog(emptyList()))
        ).generate(request)

        assertThat(result).isEqualTo(PlanGenerationResult.DailyLimitReached)
    }
}
