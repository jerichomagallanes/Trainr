package com.jericx.trainr.presentation.unstuck

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutDay
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AppliedAdjustment
import com.jericx.trainr.domain.unstuck.ApplyRejection
import com.jericx.trainr.domain.unstuck.ApplyResult
import com.jericx.trainr.domain.unstuck.PolicyDecision
import com.jericx.trainr.domain.unstuck.PreferenceKind
import com.jericx.trainr.domain.unstuck.TimeScope
import com.jericx.trainr.domain.unstuck.TrainingPreference
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.domain.unstuck.intent.IntentInterpreter
import com.jericx.trainr.domain.unstuck.intent.UnavailableInterpreter
import com.jericx.trainr.domain.unstuck.intent.UnstuckRoute
import com.jericx.trainr.domain.unstuck.planned
import com.jericx.trainr.domain.unstuck.testCatalog
import com.jericx.trainr.domain.unstuck.testDay
import com.jericx.trainr.domain.unstuck.testUser
import com.jericx.trainr.presentation.Screen
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.util.Calendar
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdjustmentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val fullDay = testDay(
        planned("warm_up", sets = 1, id = 1),
        planned("barbell_bench_press", sets = 4, id = 2),
        planned("barbell_bent_over_row", sets = 3, id = 3),
        planned("dumbbell_bicep_curl", sets = 3, id = 4, reps = 10),
        planned("bicycle_crunch", sets = 3, id = 5, reps = 12)
    )

    private val partlyDoneDay = testDay(
        planned("warm_up", sets = 1, id = 1, performed = 1),
        planned("barbell_bench_press", sets = 4, id = 2, performed = 2),
        planned("dumbbell_bicep_curl", sets = 3, id = 4, reps = 10)
    )

    private fun repositoryWith(day: WorkoutDay, startDateMillis: Long = 0L): UserRepository =
        mockk<UserRepository>(relaxed = true).also {
            coEvery { it.getCurrentUser() } returns testUser()
            every { it.getWeeklyWorkoutPlans(any()) } returns flowOf(
                listOf(
                    WeeklyWorkoutPlan(
                        id = 1,
                        userId = 0,
                        weekNumber = 1,
                        title = "Week 1",
                        startDateMillis = startDateMillis,
                        workoutDays = listOf(day)
                    )
                )
            )
            coEvery { it.getWorkoutDay(day.id) } returns day
        }

    // A relaxed mock answers every nullable read with a stand-in, which would
    // make each weekday look as though it already had a stored limit.
    private fun adjustments(): AdjustmentRepository =
        mockk<AdjustmentRepository>(relaxed = true).also {
            coEvery { it.getPreference(any(), any(), any()) } returns null
        }

    private fun TestScope.viewModel(
        day: WorkoutDay = fullDay,
        reason: DirectReason = DirectReason.LESS_TIME,
        exerciseId: Long = Screen.Adjust.NO_EXERCISE,
        minutes: Int = Screen.Adjust.NO_MINUTES,
        repository: UserRepository = repositoryWith(day),
        adjustmentRepository: AdjustmentRepository = adjustments(),
        interpreter: IntentInterpreter = UnavailableInterpreter,
        breadcrumbs: Breadcrumbs = mockk(relaxed = true)
    ) = AdjustmentViewModel(
        SavedStateHandle(
            mapOf(
                Screen.Adjust.ARG_DAY_NUMBER to day.dayNumber,
                Screen.Adjust.ARG_WEEK_NUMBER to 1,
                Screen.Adjust.ARG_REASON to reason.name,
                Screen.Adjust.ARG_EXERCISE_ID to exerciseId,
                Screen.Adjust.ARG_MINUTES to minutes
            )
        ),
        repository,
        adjustmentRepository,
        testCatalog,
        interpreter,
        breadcrumbs
    ).also { advanceUntilIdle() }

    @Test
    fun aShortBudgetProducesAProposalAndItsId() = runTest {
        val viewModel = viewModel()
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes - 10)

        val proposalId = viewModel.showRecommendation()

        assertThat(proposalId).isNotNull()
        assertThat(viewModel.uiState.value.decision).isInstanceOf(PolicyDecision.Proposed::class.java)
        assertThat(viewModel.uiState.value.review).isInstanceOf(ReviewUi.Proposed::class.java)
    }

    @Test
    fun aBudgetThatFitsIsANoChange() = runTest {
        val viewModel = viewModel()
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes + 5)

        val proposalId = viewModel.showRecommendation()

        assertThat(proposalId).isNull()
        assertThat(viewModel.uiState.value.review).isInstanceOf(ReviewUi.NoChange::class.java)
    }

    @Test
    fun performedWorkSwitchesToRemainingScope() = runTest {
        assertThat(viewModel().uiState.value.scope).isEqualTo(TimeScope.WHOLE_SESSION)
        assertThat(viewModel(day = partlyDoneDay).uiState.value.scope)
            .isEqualTo(TimeScope.REMAINING)
    }

    @Test
    fun anInvalidMinuteValueDisablesTheRecommendation() = runTest {
        val viewModel = viewModel()

        viewModel.typeMinutes("3")

        assertThat(viewModel.uiState.value.minutesError).isTrue()
        assertThat(viewModel.uiState.value.canShowRecommendation).isFalse()
        assertThat(viewModel.showRecommendation()).isNull()
    }

    // A preset the policy would refuse is never offered, and never enables the button.
    @Test
    fun aSessionTooShortToShortenOffersNoPreset() = runTest {
        val viewModel = viewModel(
            day = testDay(planned("dumbbell_bicep_curl", sets = 1, id = 4, reps = 10))
        )

        assertThat(viewModel.uiState.value.presets).isEmpty()

        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes)

        assertThat(viewModel.uiState.value.canShowRecommendation).isFalse()
    }

    @Test
    fun applyingEmitsTheProposalIdOnce() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustmentRepository = repository)
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes - 10)
        val proposalId = viewModel.showRecommendation()
        coEvery { repository.apply(any(), any(), any(), any()) } returns ApplyResult.Applied(
            AppliedAdjustment(
                id = 1,
                workoutDayId = fullDay.id,
                proposal = proposed(viewModel).proposal,
                reason = com.jericx.trainr.domain.unstuck.AdjustmentReason.LESS_TIME,
                appliedAt = 0L
            ),
            null
        )

        val seen = mutableListOf<String>()
        val job = launch { viewModel.appliedEvents.collect { seen += it } }
        viewModel.apply()
        advanceUntilIdle()
        job.cancel()

        assertThat(seen).containsExactly(proposalId)
    }

    @Test
    fun aStalePreviewIsRebuiltNotApplied() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustmentRepository = repository)
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes - 10)
        viewModel.showRecommendation()
        coEvery { repository.apply(any(), any(), any(), any()) } returns
            ApplyResult.Rejected(ApplyRejection.BEFORE_SNAPSHOT_MISMATCH)

        val seen = mutableListOf<String>()
        val job = launch { viewModel.appliedEvents.collect { seen += it } }
        viewModel.apply()
        advanceUntilIdle()
        job.cancel()

        assertThat(seen).isEmpty()
        assertThat(viewModel.uiState.value.applyError).isEqualTo(ApplyErrorUi.STALE_REBUILT)
        assertThat(viewModel.uiState.value.review).isInstanceOf(ReviewUi.Proposed::class.java)
    }

    // The day that came back has been worked on since, so the request is
    // rebuilt against what is left of it and not against the whole session.
    @Test
    fun aRebuiltPreviewAsksAboutWhatIsLeft() = runTest {
        val repository = repositoryWith(fullDay)
        val adjustmentRepository = adjustments()
        val viewModel = viewModel(
            repository = repository,
            adjustmentRepository = adjustmentRepository
        )
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes - 10)
        viewModel.showRecommendation()
        coEvery { repository.getWorkoutDay(fullDay.id) } returns partlyDoneDay
        coEvery { adjustmentRepository.apply(any(), any(), any(), any()) } returns
            ApplyResult.Rejected(ApplyRejection.BEFORE_SNAPSHOT_MISMATCH)

        viewModel.apply()
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            assertThat(hasPerformedWork).isTrue()
            assertThat(scope).isEqualTo(TimeScope.REMAINING)
            assertThat(exerciseChoices.map { it.id }).containsExactly(2L, 4L)
        }
    }

    @Test
    fun aFailedApplyKeepsTheProposalAndReportsIt() = runTest {
        val repository = adjustments()
        val breadcrumbs = mockk<Breadcrumbs>(relaxed = true)
        val viewModel = viewModel(adjustmentRepository = repository, breadcrumbs = breadcrumbs)
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes - 10)
        val proposalId = viewModel.showRecommendation()
        coEvery { repository.apply(any(), any(), any(), any()) } returns
            ApplyResult.Failed(IllegalStateException("disk"))

        viewModel.apply()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.applyError).isEqualTo(ApplyErrorUi.NOT_APPLIED)
        assertThat(proposed(viewModel).proposal.proposalId).isEqualTo(proposalId)
        coVerify { breadcrumbs.record("adjust_apply_failed") }
    }

    @Test
    fun theContextRouteNeverCallsAnUnavailableInterpreter() = runTest {
        val interpreter = mockk<IntentInterpreter>(relaxed = true).also {
            every { it.availability } returns
                com.jericx.trainr.domain.unstuck.intent.InterpreterAvailability.NOT_INSTALLED
        }
        val viewModel = viewModel(reason = DirectReason.OTHER, interpreter = interpreter)
        viewModel.typeNote("my shoulder feels off and the rack is taken")

        val seen = mutableListOf<UnstuckRoute>()
        val job = launch { viewModel.routeEvents.collect { seen += it } }
        viewModel.chooseFromContext(DirectReason.LESS_TIME)
        advanceUntilIdle()
        job.cancel()

        assertThat(seen).containsExactly(UnstuckRoute.TIME)
        coVerify(exactly = 0) { interpreter.interpret(any(), any(), any()) }
    }

    @Test
    fun anEquipmentRequestNeedsAnExerciseAndSomethingToUse() = runTest {
        val viewModel = viewModel(reason = DirectReason.EQUIPMENT, exerciseId = 2L)

        assertThat(viewModel.uiState.value.canShowRecommendation).isFalse()

        viewModel.toggleEquipment(Equipment.DUMBBELL)

        assertThat(viewModel.uiState.value.canShowRecommendation).isTrue()
        assertThat(viewModel.showRecommendation()).isNotNull()
    }

    // A whole-session limit is no answer to how much time is left, so the
    // carried value is dropped rather than shown as one.
    @Test
    fun aCarriedLimitIsDroppedOnceTheSessionIsUnderWay() = runTest {
        val viewModel = viewModel(day = partlyDoneDay, minutes = CARRIED_MINUTES)

        with(viewModel.uiState.value) {
            assertThat(scope).isEqualTo(TimeScope.REMAINING)
            assertThat(selectedMinutes).isNull()
            assertThat(canShowRecommendation).isFalse()
        }
    }

    // Nothing may be recommended off an answer the screen never showed.
    @Test
    fun aCarriedLimitThatIsNoPresetIsShownInTheField() = runTest {
        val viewModel = viewModel(minutes = CARRIED_MINUTES)

        with(viewModel.uiState.value) {
            assertThat(presets).doesNotContain(CARRIED_MINUTES)
            assertThat(selectedMinutes).isEqualTo(CARRIED_MINUTES)
            assertThat(customMinutesText).isEqualTo(CARRIED_MINUTES.toString())
        }
    }

    // Remaining time is a different question, and its answer is not a limit
    // for the whole weekday.
    @Test
    fun aSessionUnderWayOffersNothingToRemember() = runTest {
        assertThat(viewModel().uiState.value.canRemember).isTrue()
        assertThat(viewModel(day = partlyDoneDay).uiState.value.canRemember).isFalse()
    }

    // T21: the box is the only thing that makes a limit durable.
    @Test
    fun rememberUncheckedWritesNoPreference() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustmentRepository = repository)
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes - 10)
        viewModel.showRecommendation()
        applies(repository, viewModel)

        viewModel.apply()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.savePreference(any()) }
        coVerify(exactly = 0) { repository.updatePreference(any()) }
    }

    // T22: ticking the box is a request, not the confirmation itself.
    @Test
    fun rememberCheckedButKeepOriginalWritesNothing() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustmentRepository = repository)
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes - 10)
        viewModel.toggleRemember()
        viewModel.showRecommendation()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.savePreference(any()) }
        coVerify(exactly = 0) { repository.updatePreference(any()) }
    }

    @Test
    fun rememberCheckedAndAppliedWritesTheWeekdayLimit() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustmentRepository = repository)
        val budget = viewModel.uiState.value.plannedMinutes - 10
        viewModel.selectMinutes(budget)
        viewModel.toggleRemember()
        viewModel.showRecommendation()
        applies(repository, viewModel)
        val written = slot<TrainingPreference>()

        viewModel.apply()
        advanceUntilIdle()

        coVerify { repository.savePreference(capture(written)) }
        with(written.captured) {
            assertThat(kind).isEqualTo(PreferenceKind.TIME_LIMIT)
            assertThat(minutes).isEqualTo(budget)
            assertThat(sourceAdjustmentId).isEqualTo(APPLIED_ID)
            assertThat(confirmedAt).isEqualTo(updatedAt)
        }
    }

    @Test
    fun rememberCheckedAndNoChangeContinueWritesTheLimit() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustmentRepository = repository)
        val budget = viewModel.uiState.value.plannedMinutes + 5
        viewModel.selectMinutes(budget)
        viewModel.toggleRemember()
        viewModel.showRecommendation()
        val written = slot<TrainingPreference>()

        viewModel.continueWorkout()
        advanceUntilIdle()

        coVerify { repository.savePreference(capture(written)) }
        assertThat(written.captured.minutes).isEqualTo(budget)
        assertThat(written.captured.sourceAdjustmentId).isNull()
    }

    @Test
    fun aLimitAlreadyStoredForThatWeekdayIsReplacedInPlace() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(adjustmentRepository = repository)
        coEvery { repository.getPreference(any(), any(), any()) } returns TrainingPreference(
            id = 12,
            userId = 0,
            kind = PreferenceKind.TIME_LIMIT,
            minutes = 40,
            weekday = 1,
            sourceAdjustmentId = null,
            confirmedAt = 1L,
            updatedAt = 1L
        )
        viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes + 5)
        viewModel.toggleRemember()
        viewModel.showRecommendation()
        val written = slot<TrainingPreference>()

        viewModel.continueWorkout()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.savePreference(any()) }
        coVerify { repository.updatePreference(capture(written)) }
        assertThat(written.captured.id).isEqualTo(12L)
    }

    // T30: the same instant is a different weekday in UTC either side of
    // midnight, and the day belongs to the person's own calendar.
    @Test
    fun theWeekdayComesFromTheLocalDate() = runTest {
        val original = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
        try {
            val sundayEveningInUtc = utcMillis(2026, Calendar.SEPTEMBER, 20, 23)
            val repository = adjustments()
            val viewModel = viewModel(
                repository = repositoryWith(fullDay, startDateMillis = sundayEveningInUtc),
                adjustmentRepository = repository
            )
            viewModel.selectMinutes(viewModel.uiState.value.plannedMinutes + 5)
            viewModel.toggleRemember()
            viewModel.showRecommendation()
            val written = slot<TrainingPreference>()

            viewModel.continueWorkout()
            advanceUntilIdle()

            coVerify { repository.savePreference(capture(written)) }
            assertThat(written.captured.weekday).isEqualTo(MONDAY)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    private fun applies(repository: AdjustmentRepository, viewModel: AdjustmentViewModel) {
        coEvery { repository.apply(any(), any(), any(), any()) } returns ApplyResult.Applied(
            AppliedAdjustment(
                id = APPLIED_ID,
                workoutDayId = fullDay.id,
                proposal = proposed(viewModel).proposal,
                reason = com.jericx.trainr.domain.unstuck.AdjustmentReason.LESS_TIME,
                appliedAt = 0L
            ),
            null
        )
    }

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int) =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
            clear()
            set(year, month, day, hour, 0)
            timeInMillis
        }

    private fun proposed(viewModel: AdjustmentViewModel) =
        viewModel.uiState.value.decision as PolicyDecision.Proposed

    private companion object {
        const val APPLIED_ID = 1L
        const val MONDAY = 1
        const val CARRIED_MINUTES = 20
    }
}
