package com.jericx.trainr.presentation.unstuck

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.unstuck.TrainingPreference
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
class EditPreferenceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val stored = timeLimit(id = 3, minutes = 35, confirmedAt = 1_000L, updatedAt = 1_000L)

    private val users = usersWith()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun adjustments(
        preferences: List<TrainingPreference> = listOf(stored)
    ): AdjustmentRepository = mockk<AdjustmentRepository>(relaxed = true).also {
        coEvery { it.getPreferences(any()) } returns preferences
    }

    private fun TestScope.viewModel(
        repository: AdjustmentRepository = adjustments(),
        id: Long = stored.id
    ) = EditPreferenceViewModel(
        SavedStateHandle(mapOf(Screen.EditPreference.ARG_ID to id)),
        users,
        repository
    ).also { advanceUntilIdle() }

    @Test
    fun theStoredLimitIsTheStartingAnswer() = runTest {
        val viewModel = viewModel()

        with(viewModel.uiState.value) {
            assertThat(isLoaded).isTrue()
            assertThat(selectedMinutes).isEqualTo(35)
            assertThat(weekdayName).isNotEmpty()
            assertThat(presets).isNotEmpty()
            assertThat(canSave).isTrue()
        }
    }

    // A limit that is not one of today's presets still has to be visible.
    @Test
    fun aStoredLimitThatIsNoPresetIsShownInTheField() = runTest {
        val viewModel = viewModel(adjustments(listOf(stored.copy(minutes = 30))))

        with(viewModel.uiState.value) {
            assertThat(presets).doesNotContain(30)
            assertThat(customMinutesText).isEqualTo("30")
            assertThat(isPresetSelected).isFalse()
            assertThat(canSave).isTrue()
        }
    }

    // Editing the value is not the person agreeing to remember it again.
    @Test
    fun savingWritesTheNewMinutesAndKeepsTheConfirmation() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)
        val written = slot<TrainingPreference>()

        viewModel.selectMinutes(25)
        viewModel.save()
        advanceUntilIdle()

        coVerify { repository.updatePreference(capture(written)) }
        with(written.captured) {
            assertThat(id).isEqualTo(stored.id)
            assertThat(minutes).isEqualTo(25)
            assertThat(confirmedAt).isEqualTo(stored.confirmedAt)
            assertThat(updatedAt).isGreaterThan(stored.updatedAt)
        }
    }

    @Test
    fun leavingWithoutSavingWritesNothing() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)

        viewModel.selectMinutes(25)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.updatePreference(any()) }
        coVerify(exactly = 0) { repository.savePreference(any()) }
    }

    @Test
    fun anUnsupportedValueBlocksTheSave() = runTest {
        val repository = adjustments()
        val viewModel = viewModel(repository)

        viewModel.typeMinutes("3")
        viewModel.save()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.minutesError).isTrue()
        assertThat(viewModel.uiState.value.canSave).isFalse()
        coVerify(exactly = 0) { repository.updatePreference(any()) }
    }

    @Test
    fun aPreferenceThatIsNoLongerStoredSavesNothing() = runTest {
        val repository = adjustments(preferences = emptyList())
        val viewModel = viewModel(repository)

        viewModel.selectMinutes(25)
        viewModel.save()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoaded).isTrue()
        coVerify(exactly = 0) { repository.updatePreference(any()) }
    }

    @Test
    fun savingEmitsOneEventForTheScreenToFollow() = runTest {
        val viewModel = viewModel()
        val seen = mutableListOf<Unit>()
        val job = launch { viewModel.savedEvents.collect { seen += it } }

        viewModel.selectMinutes(25)
        viewModel.save()
        advanceUntilIdle()
        job.cancel()

        assertThat(seen).hasSize(1)
    }
}
