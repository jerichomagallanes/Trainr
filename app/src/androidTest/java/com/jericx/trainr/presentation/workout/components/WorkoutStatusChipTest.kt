package com.jericx.trainr.presentation.workout.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutStatusChipTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private fun setChip(status: WorkoutStatus) {
        composeTestRule.setContent {
            TrainrTheme { WorkoutStatusChip(status = status) }
        }
    }

    @Test
    fun completedChipShowsItsLabel() {
        setChip(WorkoutStatus.COMPLETED)

        composeTestRule.onNodeWithText(string(R.string.completed)).assertIsDisplayed()
    }

    @Test
    fun inProgressChipShowsItsLabel() {
        setChip(WorkoutStatus.IN_PROGRESS)

        composeTestRule.onNodeWithText(string(R.string.in_progress)).assertIsDisplayed()
    }

    @Test
    fun notStartedChipShowsItsLabel() {
        setChip(WorkoutStatus.NOT_STARTED)

        composeTestRule.onNodeWithText(string(R.string.not_started)).assertIsDisplayed()
    }
}
