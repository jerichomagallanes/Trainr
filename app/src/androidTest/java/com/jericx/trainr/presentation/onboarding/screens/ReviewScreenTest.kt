package com.jericx.trainr.presentation.onboarding.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WorkoutType
import com.jericx.trainr.presentation.common.theme.TrainrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    private val sampleProfile = UserProfile(
        firstName = "Jericho",
        age = 30,
        height = 175f,
        weight = 72f
    )

    private fun setScreen(profile: UserProfile) {
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(userProfile = profile, onConfirmClick = {}, onBackClick = {})
            }
        }
    }

    private fun routineSentence(profile: UserProfile) =
        composeTestRule.activity.getString(
            R.string.ai_routine_description,
            composeTestRule.activity.getString(
                R.string.program_length_format,
                profile.workoutDaysPerWeek
            ),
            composeTestRule.activity.getString(
                when (profile.workoutType) {
                    WorkoutType.STRENGTH -> R.string.program_strength
                    WorkoutType.CARDIO -> R.string.program_cardio
                    WorkoutType.HIIT -> R.string.program_hiit
                    WorkoutType.YOGA -> R.string.program_flexibility
                    WorkoutType.MIXED -> R.string.program_mixed
                }
            ),
            composeTestRule.activity.getString(
                when (profile.fitnessGoal) {
                    FitnessGoal.WEIGHT_LOSS -> R.string.goal_focus_lose_weight
                    FitnessGoal.MUSCLE_GAIN -> R.string.goal_focus_build_muscle
                    FitnessGoal.STRENGTH -> R.string.goal_focus_get_stronger
                    FitnessGoal.ENDURANCE -> R.string.goal_focus_improve_endurance
                    FitnessGoal.GENERAL_FITNESS -> R.string.goal_focus_general_fitness
                    FitnessGoal.FLEXIBILITY -> R.string.goal_focus_flexibility
                }
            )
        )

    // The screen used to name "general fitness" whatever the user picked, which
    // made the last screen before generation identical for everybody.
    @Test
    fun theRoutinePreviewNamesTheGoalTheUserChose() {
        val profile = sampleProfile.copy(
            fitnessGoal = FitnessGoal.ENDURANCE,
            workoutType = WorkoutType.CARDIO,
            workoutDaysPerWeek = 4
        )
        setScreen(profile)

        composeTestRule.onNodeWithText(routineSentence(profile))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.goal_focus_general_fitness), substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun theRoutinePreviewNamesTheWorkoutStyleTheUserChose() {
        val profile = sampleProfile.copy(
            fitnessGoal = FitnessGoal.MUSCLE_GAIN,
            workoutType = WorkoutType.STRENGTH,
            workoutDaysPerWeek = 3
        )
        setScreen(profile)

        composeTestRule.onNodeWithText(routineSentence(profile))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // Two profiles that differ only in goal must not read the same.
    @Test
    fun twoDifferentGoalsProduceTwoDifferentSentences() {
        val endurance = sampleProfile.copy(fitnessGoal = FitnessGoal.ENDURANCE)
        val strength = sampleProfile.copy(fitnessGoal = FitnessGoal.STRENGTH)

        assertThat(routineSentence(endurance)).isNotEqualTo(routineSentence(strength))
    }

    @Test
    fun displaysScreenTitle() {
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    onConfirmClick = {},
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.your_fitness_profile))
            .assertIsDisplayed()
    }

    @Test
    fun everySectionOffersAnEdit() {
        var personal = false
        var goals = false
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    onConfirmClick = {},
                    onBackClick = {},
                    onEditPersonal = { personal = true },
                    onEditGoals = { goals = true }
                )
            }
        }

        composeTestRule.onAllNodesWithText(string(R.string.edit))
            .assertCountEquals(4)
        composeTestRule.onAllNodesWithText(string(R.string.edit))[0].performClick()
        composeTestRule.onAllNodesWithText(string(R.string.edit))[1].performClick()
        assertThat(personal).isTrue()
        assertThat(goals).isTrue()
    }

    // Entered from the plan, the review is a detour: it closes, it does not
    // promise a step back through a flow that is not there.
    @Test
    fun regeneratingShowsACloseInsteadOfABack() {
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    onConfirmClick = {},
                    onBackClick = {},
                    isRegenerating = true
                )
            }
        }

        composeTestRule.onNodeWithContentDescription(string(R.string.close)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(string(R.string.back)).assertDoesNotExist()
    }

    @Test
    fun confirmButtonFiresCallback() {
        var confirmed = false
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    onConfirmClick = { confirmed = true },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.generate_my_workout_plan))
            .performClick()

        assertThat(confirmed).isTrue()
    }
    private fun setupProgress() = composeTestRule.onAllNodes(
        hasProgressBarRangeInfo(ProgressBarRangeInfo(current = 6f, range = 0f..7f))
    )

    @Test
    fun theLastStepOfSetupSaysHowFarThroughItIs() {
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(userProfile = sampleProfile, onConfirmClick = {}, onBackClick = {})
            }
        }

        setupProgress().assertCountEquals(1)
    }

    // Reached from the plan there is no seven-step run to be six sevenths of.
    @Test
    fun reviewingFromThePlanCountsNothing() {
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    isProfileUpdate = true,
                    onConfirmClick = {},
                    onBackClick = {}
                )
            }
        }

        setupProgress().assertCountEquals(0)
    }

    // Saving the profile writes no routine, so the screen must not promise one.
    @Test
    fun theProfileUpdateSavesInsteadOfGenerating() {
        var saved = false
        composeTestRule.setContent {
            TrainrTheme {
                ReviewScreen(
                    userProfile = sampleProfile,
                    isProfileUpdate = true,
                    onConfirmClick = { saved = true },
                    onBackClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(string(R.string.generate_my_workout_plan))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.ai_routine_preview_label))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.review_profile_description))
            .assertIsDisplayed()

        composeTestRule.onNodeWithText(string(R.string.save_profile)).performClick()

        assertThat(saved).isTrue()
    }


    // The profile is stored in centimetres and kilograms whichever units were
    // typed, so a client in pounds was shown their own weight as a number they
    // had never entered.
    @Test
    fun measurementsReadBackInTheUnitsTheyWereEnteredIn() {
        setScreen(sampleProfile.copy(unitSystem = UnitSystem.IMPERIAL))

        composeTestRule.onNodeWithText("159 lbs").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("5'9\"").assertIsDisplayed()
    }

    @Test
    fun aMetricProfileStillReadsInCentimetresAndKilograms() {
        setScreen(sampleProfile)

        composeTestRule.onNodeWithText(string(R.string.weight_kg_format).format(72f))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(string(R.string.height_cm_format).format(175))
            .assertIsDisplayed()
    }
}
