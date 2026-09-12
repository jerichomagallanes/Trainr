package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.UserProfile
import org.junit.Test

class SessionBudgetTest {

    private fun profile(minutes: Int, goal: FitnessGoal, days: Int = 3) = UserProfile(
        workoutDuration = minutes,
        fitnessGoal = goal,
        workoutDaysPerWeek = days
    )

    // Three minutes between heavy sets is what the evidence asks for, and half
    // an hour only pays for six of them. A plan with twelve is a plan the
    // client abandons halfway.
    @Test
    fun heavyWorkBuysFewerSetsThanTheSameHalfHourOfConditioning() {
        val strength = SessionBudget.maxSetsPerSession(profile(30, FitnessGoal.STRENGTH))
        val weightLoss = SessionBudget.maxSetsPerSession(profile(30, FitnessGoal.WEIGHT_LOSS))

        assertThat(strength).isEqualTo(6)
        assertThat(weightLoss).isGreaterThan(strength)
    }

    @Test
    fun aLongerSessionBuysMoreSets() {
        val short = SessionBudget.maxSetsPerSession(profile(30, FitnessGoal.MUSCLE_GAIN))
        val long = SessionBudget.maxSetsPerSession(profile(90, FitnessGoal.MUSCLE_GAIN))

        assertThat(long).isGreaterThan(short)
    }

    // Whatever the arithmetic says, a session with nothing in it is not a
    // session.
    @Test
    fun theBudgetNeverFallsBelowAWorkableSession() {
        assertThat(SessionBudget.maxSetsPerSession(profile(30, FitnessGoal.STRENGTH)))
            .isAtLeast(4)
    }

    // Ten sets a muscle a week needs days to spread over; one or two days a
    // week cannot hold it, so it is not promised.
    @Test
    fun theWeeklyTargetOnlyReachesTenWhenThereAreDaysToSpreadItOver() {
        assertThat(SessionBudget.weeklySetsPerMuscle(profile(45, FitnessGoal.MUSCLE_GAIN, days = 4)))
            .isEqualTo(10)
        assertThat(SessionBudget.weeklySetsPerMuscle(profile(45, FitnessGoal.MUSCLE_GAIN, days = 2)))
            .isEqualTo(6)
    }
}
