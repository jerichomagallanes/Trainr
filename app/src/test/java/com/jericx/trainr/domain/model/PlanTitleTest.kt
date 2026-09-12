package com.jericx.trainr.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlanTitleTest {

    // Old plans carry the week number in the title, so a copied week would call itself the first
    @Test
    fun aTrailingWeekNumberIsDropped() {
        assertThat("Beginner Muscle Building - Week 1".withoutWeekNumber())
            .isEqualTo("Beginner Muscle Building")
        assertThat("3-Day Full Body Hypertrophy Program - Week 2".withoutWeekNumber())
            .isEqualTo("3-Day Full Body Hypertrophy Program")
        assertThat("Upper/Lower Split (Week 4)".withoutWeekNumber())
            .isEqualTo("Upper/Lower Split")
    }

    @Test
    fun aLeadingWeekNumberIsDropped() {
        assertThat("Week 3 Strength Progression Program".withoutWeekNumber())
            .isEqualTo("Strength Progression Program")
        assertThat("Week 12: Peak Strength".withoutWeekNumber()).isEqualTo("Peak Strength")
    }

    @Test
    fun aTitleWithoutANumberIsLeftAlone() {
        assertThat("Beginner Muscle Building".withoutWeekNumber())
            .isEqualTo("Beginner Muscle Building")
        // "Week" without a number is a word like any other.
        assertThat("Week of Power".withoutWeekNumber()).isEqualTo("Week of Power")
        assertThat("Weekend Warrior".withoutWeekNumber()).isEqualTo("Weekend Warrior")
    }

    // Blank titles fail validation, so a title that is nothing but its number keeps it
    @Test
    fun aTitleThatIsOnlyANumberKeepsIt() {
        assertThat("Week 2".withoutWeekNumber()).isEqualTo("Week 2")
    }
}
