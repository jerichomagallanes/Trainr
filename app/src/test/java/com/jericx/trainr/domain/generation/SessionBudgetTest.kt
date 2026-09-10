package com.jericx.trainr.domain.generation

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jericx.trainr.common.Constants
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
        assertThat(SessionBudget.weeklySetsPerMuscle(profile(60, FitnessGoal.MUSCLE_GAIN, days = 5)))
            .isEqualTo(10)
        assertThat(SessionBudget.weeklySetsPerMuscle(profile(45, FitnessGoal.MUSCLE_GAIN, days = 2)))
            .isLessThan(10)
    }

    // The cap is enforced and the target is not, so a target the sessions
    // cannot hold is the rule the model quietly drops. Every answer the setup
    // screen allows must be able to satisfy both at once.
    @Test
    fun theWeeklyTargetIsNeverMoreThanTheSessionsCanHold() {
        val goals = FitnessGoal.entries
        val durations = Constants.Workout.DURATION_OPTIONS
        val everyAnswer = goals.flatMap { goal ->
            durations.flatMap { duration ->
                Constants.Workout.DAYS_PER_WEEK_OPTIONS.map { profile(duration, goal, it) }
            }
        }

        everyAnswer.forEach { user ->
            val demanded = SessionBudget.weeklySetsPerMuscle(user) * TRAINABLE_REGIONS
            val afforded = SessionBudget.maxSetsPerSession(user) * user.workoutDaysPerWeek * 3 / 2

            assertWithMessage(
                "${user.fitnessGoal} ${user.workoutDaysPerWeek}d x ${user.workoutDuration}min"
            ).that(demanded).isAtMost(afforded)
        }
    }

    // A day half again as long as the answer is not that answer.
    @Test
    fun theSessionCeilingSitsAboveTheAnswerWithoutLeavingIt() {
        val user = profile(45, FitnessGoal.MUSCLE_GAIN, days = 3)

        assertThat(SessionBudget.sessionCeilingMinutes(user)).isEqualTo(67)
    }

    private companion object {
        const val TRAINABLE_REGIONS = 9
    }
}
