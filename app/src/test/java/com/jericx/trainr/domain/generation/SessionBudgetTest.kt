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

    // Three minutes between heavy sets is what the evidence asks for, so the
    // same half hour buys far fewer of them than a circuit does. The numbers
    // are the rest a session really spends: priced at a full rest after every
    // set, including the last of a movement, half an hour bought six and a
    // 90-minute answer was built as an hour.
    @Test
    fun heavyWorkBuysFewerSetsThanTheSameHalfHourOfConditioning() {
        val strength = SessionBudget.maxSetsPerSession(profile(30, FitnessGoal.STRENGTH))
        val weightLoss = SessionBudget.maxSetsPerSession(profile(30, FitnessGoal.WEIGHT_LOSS))

        assertThat(strength).isEqualTo(11)
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

    // A day half again as long as the answer is not that answer.
    @Test
    fun theSessionCeilingSitsAboveTheAnswerWithoutLeavingIt() {
        val user = profile(45, FitnessGoal.MUSCLE_GAIN, days = 3)

        assertThat(SessionBudget.sessionCeilingMinutes(user)).isEqualTo(67)
    }
}
