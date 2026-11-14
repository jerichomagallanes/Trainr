package com.jericx.trainr.presentation.purchases

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProPaywallComparisonTest {

    @Test
    fun aRowTiesEachMarkToTheColumnItSitsIn() {
        assertThat(
            comparisonRowLabel(
                feature = "Repeat a week",
                freeHead = "Free",
                free = "No",
                proHead = "Pro",
                pro = "Yes",
                format = ROW_FORMAT
            )
        ).isEqualTo("Repeat a week: Free, No. Pro, Yes.")
    }

    @Test
    fun aCountedRowReadsItsAllowanceRatherThanATick() {
        assertThat(
            comparisonRowLabel(
                feature = "Weeks built for you",
                freeHead = "Free",
                free = "1",
                proHead = "Pro",
                pro = "Every week",
                format = ROW_FORMAT
            )
        ).isEqualTo("Weeks built for you: Free, 1. Pro, Every week.")
    }
}

private const val ROW_FORMAT = "%1\$s: %2\$s, %3\$s. %4\$s, %5\$s."
