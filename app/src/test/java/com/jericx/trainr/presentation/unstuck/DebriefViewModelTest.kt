package com.jericx.trainr.presentation.unstuck

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.domain.unstuck.planned
import com.jericx.trainr.domain.unstuck.testDay
import com.jericx.trainr.presentation.Screen
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
class DebriefViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val users = usersWith()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun adjustments(stored: SessionNote? = null): AdjustmentRepository =
        mockk<AdjustmentRepository>(relaxed = true).also {
            coEvery { it.getNote(preferenceDay.id) } returns stored
            coEvery { it.saveNote(any()) } returns NEW_NOTE_ID
        }

    private fun TestScope.viewModel(repository: AdjustmentRepository) = DebriefViewModel(
        SavedStateHandle(
            mapOf(
                Screen.Debrief.ARG_DAY_NUMBER to 1,
                Screen.Debrief.ARG_WEEK_NUMBER to 1
            )
        ),
        users,
        repository
    ).also { advanceUntilIdle() }

    @Test
    fun aBlankNoteIsNeverSaved() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)

        viewModel.typeNote("   ")
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.saveNote(any()) }
        coVerify(exactly = 0) { repository.updateNote(any()) }
    }

    @Test
    fun aNoteIsSavedAgainstTheDayItWasWrittenAbout() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)
        val written = slot<SessionNote>()

        viewModel.typeNote("  Gym was busy.  ")
        viewModel.save()
        advanceUntilIdle()

        coVerify { repository.saveNote(capture(written)) }
        assertThat(written.captured.text).isEqualTo("Gym was busy.")
        assertThat(written.captured.workoutDayId).isEqualTo(preferenceDay.id)
    }

    // One note per session: editing it must not leave a second row behind.
    @Test
    fun aSecondSaveUpdatesTheSameRow() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)
        val updated = slot<SessionNote>()

        viewModel.typeNote("First try.")
        viewModel.save()
        advanceUntilIdle()
        viewModel.typeNote("Second try.")
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.saveNote(any()) }
        coVerify(exactly = 1) { repository.updateNote(capture(updated)) }
        assertThat(updated.captured.id).isEqualTo(NEW_NOTE_ID)
        assertThat(updated.captured.text).isEqualTo("Second try.")
    }

    // Both taps land before the first write returns, and neither may add a row.
    @Test
    fun twoTapsOnSaveWriteOneNote() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)

        viewModel.typeNote("Gym was busy.")
        viewModel.save()
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.saveNote(any()) }
        coVerify(exactly = 0) { repository.updateNote(any()) }
    }

    @Test
    fun anExistingNoteIsEditedRatherThanAddedTo() = runTest {
        val stored = SessionNote(
            id = 12,
            userId = 0,
            workoutDayId = preferenceDay.id,
            text = "Written yesterday.",
            createdAt = 1L,
            updatedAt = 1L
        )
        val repository = adjustments(stored = stored)
        val viewModel = viewModel(repository)

        assertThat(viewModel.note.value).isEqualTo("Written yesterday.")

        viewModel.typeNote("Rewritten.")
        viewModel.save()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.saveNote(any()) }
        coVerify { repository.updateNote(match { it.id == 12L && it.text == "Rewritten." }) }
    }

    @Test
    fun savingEmitsTheDayOnceForTheScreenToFollow() = runTest {
        val viewModel = viewModel(adjustments())
        val seen = mutableListOf<Long>()
        val job = launch { viewModel.savedEvents.collect { seen += it } }

        viewModel.typeNote("Something happened.")
        viewModel.save()
        advanceUntilIdle()
        job.cancel()

        assertThat(seen).containsExactly(preferenceDay.id)
    }

    // One week outline and one note: the whole history is never read to find a day.
    @Test
    fun theDayIsFoundWithoutReadingEveryPlan() = runTest {
        viewModel(adjustments())

        coVerify(exactly = 1) { users.getWeekOutline(any(), 1) }
        coVerify(exactly = 0) { users.getWeeklyWorkoutPlans(any()) }
        coVerify(exactly = 0) { users.getWorkoutDay(any()) }
    }

    // A three-day week stores 1, 3, 5, so the second session it holds is the
    // day numbered 3.
    @Test
    fun theDayIsTheNthTrainingDayNotTheDayNumberedN() = runTest {
        val second = testDay(planned("goblet_squat", sets = 3, id = 4), id = 8).copy(dayNumber = 3)
        val repository = mockk<AdjustmentRepository>(relaxed = true).also {
            coEvery { it.getNote(any()) } returns null
            coEvery { it.saveNote(any()) } returns NEW_NOTE_ID
        }
        val viewModel = DebriefViewModel(
            SavedStateHandle(
                mapOf(
                    Screen.Debrief.ARG_DAY_NUMBER to 2,
                    Screen.Debrief.ARG_WEEK_NUMBER to 1
                )
            ),
            usersWith(planStartingToday().copy(workoutDays = listOf(preferenceDay, second))),
            repository
        ).also { advanceUntilIdle() }

        viewModel.typeNote("Knee felt tight.")
        viewModel.save()
        advanceUntilIdle()

        coVerify { repository.saveNote(match { it.workoutDayId == second.id }) }
    }

    private companion object {
        const val NEW_NOTE_ID = 9L
    }
}
