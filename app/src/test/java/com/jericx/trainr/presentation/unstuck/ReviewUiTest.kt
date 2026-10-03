package com.jericx.trainr.presentation.unstuck

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.unstuck.ProposalKind
import com.jericx.trainr.domain.unstuck.ProposalSummary
import org.junit.Test

class ReviewUiTest {

    private fun summary(after: Int?, budget: Int?) = ProposalSummary(
        kind = ProposalKind.SHORTER_SESSION,
        keptPriorityKey = null,
        tradeoffs = emptyList(),
        rows = emptyList(),
        estimateBeforeMinutes = 40,
        estimateAfterMinutes = after,
        budgetMinutes = budget
    )

    // The review must not echo a number the day cannot meet.
    @Test
    fun aResultPastTheRequestIsNamedAsTheShortestVersion() {
        assertThat(summary(after = 23, budget = 17).shortestMinutes).isEqualTo(23)
    }

    @Test
    fun aResultWithinTheRequestLeavesTheRequestStanding() {
        assertThat(summary(after = 17, budget = 17).shortestMinutes).isNull()
        assertThat(summary(after = 15, budget = 17).shortestMinutes).isNull()
        assertThat(summary(after = null, budget = 17).shortestMinutes).isNull()
        assertThat(summary(after = 23, budget = null).shortestMinutes).isNull()
    }
}
