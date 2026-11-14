package com.jericx.trainr.presentation.unstuck

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.repository.UserRepository
import com.jericx.trainr.domain.unstuck.AdjustmentReason
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.presentation.unstuck.feedback.appliedAdjustment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val preferences = MutableStateFlow(listOf(timeLimit()))
    private val notes = MutableStateFlow(
        listOf(
            SessionNote(
                id = 4,
                userId = 0,
                workoutDayId = preferenceDay.id,
                text = "I had to leave early for work.",
                createdAt = 1L,
                updatedAt = 1L
            )
        )
    )

    private val users: UserRepository = usersWith()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun adjustments(
        reason: AdjustmentReason? = null,
        finished: Boolean = false
    ): AdjustmentRepository = mockk<AdjustmentRepository>(relaxed = true).also { repository ->
        every { repository.observePreferences(any()) } returns preferences
        every { repository.observeNotes(any()) } returns notes
        coEvery { repository.getOutcome(preferenceDay.id) } returns
            if (finished) mockk(relaxed = true) else null
        coEvery { repository.getActiveAdjustment(preferenceDay.id) } returns reason?.let {
            appliedAdjustment(preferenceDay.id).copy(reason = it)
        }
        coEvery { repository.deletePreference(any()) } answers {
            preferences.value = preferences.value.filterNot { it.id == firstArg<Long>() }
        }
        coEvery { repository.deleteNote(any()) } answers {
            notes.value = notes.value.filterNot { it.id == firstArg<Long>() }
        }
    }

    private fun TestScope.viewModel(repository: AdjustmentRepository) =
        PreferencesViewModel(users, repository).also { advanceUntilIdle() }

    @Test
    fun aStoredLimitIsShownAsTheDayItWasConfirmedFor() = runTest {
        val viewModel = viewModel(adjustments())

        with(viewModel.uiState.value.preferences.single()) {
            assertThat(id).isEqualTo(1L)
            assertThat(minutes).isEqualTo(35)
            assertThat(weekdayName).isNotEmpty()
            assertThat(confirmedOn).isNotEmpty()
        }
    }

    @Test
    fun forgettingAPreferenceRemovesItAndSaysSo() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)

        viewModel.forget(1L)
        advanceUntilIdle()

        coVerify { repository.deletePreference(1L) }
        assertThat(viewModel.uiState.value.preferences).isEmpty()
        assertThat(viewModel.uiState.value.hasForgotten).isTrue()
    }

    @Test
    fun deletingANoteRemovesIt() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)

        viewModel.deleteNote(4L)
        advanceUntilIdle()

        coVerify { repository.deleteNote(4L) }
        assertThat(viewModel.uiState.value.notes).isEmpty()
    }

    @Test
    fun todaysAdjustmentIsNamedByItsReason() = runTest {
        assertThat(viewModel(adjustments(AdjustmentReason.LESS_TIME)).uiState.value.todayAdjustment)
            .isEqualTo(TodayAdjustmentKind.SHORTER)
        assertThat(
            viewModel(adjustments(AdjustmentReason.EQUIPMENT_UNAVAILABLE))
                .uiState.value.todayAdjustment
        ).isEqualTo(TodayAdjustmentKind.ALTERNATIVE)
        assertThat(viewModel(adjustments()).uiState.value.todayAdjustment).isNull()
    }

    // The day is over, so nothing about it is still today's adjustment.
    @Test
    fun aFinishedDayReportsNoAdjustmentForToday() = runTest {
        val viewModel = viewModel(adjustments(AdjustmentReason.LESS_TIME, finished = true))

        assertThat(viewModel.uiState.value.todayAdjustment).isNull()
    }
}
