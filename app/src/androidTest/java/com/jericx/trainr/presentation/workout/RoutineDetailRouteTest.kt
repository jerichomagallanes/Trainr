package com.jericx.trainr.presentation.workout

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.purchases.AdjustmentAllowance
import com.jericx.trainr.domain.time.ElapsedClock
import com.jericx.trainr.domain.time.WallClock
import com.jericx.trainr.presentation.Screen
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData
import com.jericx.trainr.presentation.workout.util.WorkoutWeek
import com.jericx.trainr.testing.HiltTestActivity
import com.jericx.trainr.testing.InMemoryAdjustmentRepository
import com.jericx.trainr.testing.OneWeekRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// The view model re-reads the date on every read; only the route says when a
// screen nobody is touching is read again. These drive that trigger.
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RoutineDetailRouteTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

    private class MovingDate(var now: Long = System.currentTimeMillis()) : WallClock {
        override fun nowMillis(): Long = now
    }

    private object NoAllowance : AdjustmentAllowance {
        override fun includedCycleId(): String? = null
        override fun consume(cycleId: String) = Unit
        override fun restore(cycleId: String) = Unit
    }

    private val date = MovingDate()

    // The last day of a week that ends tonight, so one midnight turns the
    // screen into a record.
    private val lastDayOfTheWeek = SampleWorkoutData.weekOne.copy(
        startDateMillis = Calendar.getInstance().apply {
            timeInMillis = WorkoutWeek.startOfDay()
            add(Calendar.DAY_OF_YEAR, -6)
        }.timeInMillis
    )

    private fun setRoute(finishEarlyRequested: Boolean = false): RoutineDetailViewModel {
        val viewModel = RoutineDetailViewModel(
            SavedStateHandle(
                mapOf(
                    Screen.RoutineDetail.ARG_DAY_NUMBER to SampleWorkoutData.DEFAULT_DAY_NUMBER,
                    Screen.RoutineDetail.ARG_WEEK_NUMBER to Screen.RoutineDetail.LATEST_WEEK
                )
            ),
            OneWeekRepository(lastDayOfTheWeek),
            InMemoryAdjustmentRepository(),
            NoAllowance,
            SampleWorkoutData.catalog,
            ElapsedClock { 0L },
            date
        )
        composeTestRule.setContent {
            TrainrTheme {
                RoutineDetailRoute(
                    finishEarlyRequested = finishEarlyRequested,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()
        return viewModel
    }

    private fun comeBackADayLater() {
        date.now += TimeUnit.DAYS.toMillis(1)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeTestRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()
    }

    @Test
    fun aDayLeftOpenAcrossMidnightIsARecordWhenTheAppComesBack() {
        val viewModel = setRoute()
        assertThat(viewModel.uiState.value.isReadOnly).isFalse()

        comeBackADayLater()

        assertThat(viewModel.uiState.value.isReadOnly).isTrue()
    }

    // The confirmation replaces the screen, so a trigger hung on the routine
    // alone would stop watching the clock exactly where it matters most.
    @Test
    fun theFinishEarlyConfirmationNoticesMidnightToo() {
        val viewModel = setRoute(finishEarlyRequested = true)
        assertThat(viewModel.uiState.value.isConfirmingFinishEarly).isTrue()
        assertThat(viewModel.uiState.value.isReadOnly).isFalse()

        comeBackADayLater()

        assertThat(viewModel.uiState.value.isReadOnly).isTrue()
    }
}
