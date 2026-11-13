package com.jericx.trainr.presentation.unstuck

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.repository.AdjustmentRepository
import com.jericx.trainr.domain.unstuck.SessionNote
import com.jericx.trainr.presentation.Screen
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteSavedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(dayId: Long, repository: AdjustmentRepository) = NoteSavedViewModel(
        SavedStateHandle(mapOf(Screen.NoteSaved.ARG_DAY_ID to dayId)),
        repository
    )

    @Test
    fun theNoteIsReadBackByTheDayItWasSavedAgainst() = runTest {
        val repository = mockk<AdjustmentRepository>(relaxed = true).also {
            coEvery { it.getNote(7L) } returns SessionNote(
                id = 3,
                userId = 0,
                workoutDayId = 7L,
                text = "Gym was busy.",
                createdAt = 1L,
                updatedAt = 1L
            )
        }

        val viewModel = viewModel(7L, repository)
        advanceUntilIdle()

        assertThat(viewModel.note.value).isEqualTo("Gym was busy.")
    }

    @Test
    fun aDayWithoutANoteShowsNothing() = runTest {
        val repository = mockk<AdjustmentRepository>(relaxed = true).also {
            coEvery { it.getNote(any()) } returns null
        }

        val viewModel = viewModel(7L, repository)
        advanceUntilIdle()

        assertThat(viewModel.note.value).isEmpty()
    }
}
