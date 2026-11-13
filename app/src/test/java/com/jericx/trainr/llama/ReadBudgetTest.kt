package com.jericx.trainr.llama

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class ReadBudgetTest {

    private fun budget() = ReadBudget(budget = 120.seconds, slice = 1.seconds)

    @Test
    fun aReadThatRunsForItsWholeBudgetIsOutOfTime() {
        val budget = budget()

        repeat(120) { budget.charge(1.seconds) }

        assertThat(budget.isSpent).isTrue()
    }

    @Test
    fun aReadStillRunningInsideItsBudgetHasTimeLeft() {
        val budget = budget()

        repeat(119) { budget.charge(1.seconds) }

        assertThat(budget.isSpent).isFalse()
    }

    // The process is frozen while the call is taken, so the model is given none
    // of that time and is charged for none of it either.
    @Test
    fun timeTheProcessSpentFrozenIsNotChargedAgainstTheRead() {
        val budget = budget()

        repeat(60) { budget.charge(1.seconds) }
        budget.charge(30.minutes)

        assertThat(budget.isSpent).isFalse()

        repeat(58) { budget.charge(1.seconds) }

        assertThat(budget.isSpent).isFalse()

        budget.charge(1.seconds)

        assertThat(budget.isSpent).isTrue()
    }

    @Test
    fun theLastSliceIsOnlyWhatIsLeftOfTheBudget() {
        val budget = ReadBudget(budget = 2500.milliseconds, slice = 1.seconds)

        assertThat(budget.nextSlice).isEqualTo(1.seconds)
        budget.charge(1.seconds)
        budget.charge(1.seconds)

        assertThat(budget.nextSlice).isEqualTo(500.milliseconds)
    }
}
