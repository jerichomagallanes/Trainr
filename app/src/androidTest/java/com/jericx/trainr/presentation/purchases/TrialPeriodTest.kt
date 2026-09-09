package com.jericx.trainr.presentation.purchases

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.revenuecat.purchases.models.Period
import org.junit.Test
import org.junit.runner.RunWith

// The trial length is read from the store rather than written into the copy, so
// it is the one part of a legally required sentence that no translator sees.
@RunWith(AndroidJUnit4::class)
class TrialPeriodTest {

    private fun period(value: Int, unit: Period.Unit) =
        Period(value = value, unit = unit, iso8601 = "")

    @Test
    fun everyLengthReadsAsTheLengthTheStoreReported() {
        val cases = listOf(
            Triple(3, Period.Unit.DAY, "3 days"),
            // The one a normalising formatter would call "1 week", which is the
            // default trial length and so the case most likely to ship.
            Triple(7, Period.Unit.DAY, "7 days"),
            Triple(14, Period.Unit.DAY, "14 days"),
            Triple(1, Period.Unit.WEEK, "1 week"),
            Triple(1, Period.Unit.MONTH, "1 month"),
            Triple(1, Period.Unit.YEAR, "1 year")
        )
        for ((value, unit, reads) in cases) {
            assertThat(ProPaywallViewModel.periodText(period(value, unit))).isEqualTo(reads)
        }
    }

    @Test
    fun anUnreadablePeriodSaysNothingRatherThanGuessing() {
        assertThat(ProPaywallViewModel.periodText(period(7, Period.Unit.UNKNOWN))).isNull()
    }
}
