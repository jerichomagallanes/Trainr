package com.jericx.trainr.data.purchases

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.purchases.AdjustmentGate
import java.util.UUID
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoredAdjustmentAllowanceTest {

    // Its own preferences file, so the device's real allowance is never read or spent.
    private val prefsName = "allowance-test-${UUID.randomUUID()}"
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences(prefsName, mode)
    }
    private val allowance = StoredAdjustmentAllowance(context)

    @After
    fun tearDown() {
        context.deleteSharedPreferences(prefsName)
    }

    private fun decision(cycleId: String) = AdjustmentGate(
        isPro = { false },
        canSell = { true },
        allowance = allowance
    ).decide(cycleId)

    @Test
    fun aFreshInstallHasSpentNoCycle() {
        assertThat(allowance.includedCycleId()).isNull()
    }

    @Test
    fun consumingRecordsTheCycleAndALaterOneCannotTakeOver() {
        allowance.consume("proposal-1")
        allowance.consume("proposal-2")

        assertThat(allowance.includedCycleId()).isEqualTo("proposal-1")
    }

    @Test
    fun undoingTheIncludedCycleGivesItBack() {
        allowance.consume("proposal-1")
        assertThat(decision("proposal-2")).isEqualTo(AdjustmentGate.Decision.ASK)

        allowance.restore("proposal-1")

        assertThat(allowance.includedCycleId()).isNull()
        assertThat(decision("proposal-2")).isEqualTo(AdjustmentGate.Decision.ALLOWED)
    }

    @Test
    fun undoingAnotherCycleLeavesTheIncludedOneSpent() {
        allowance.restore("proposal-1")
        assertThat(allowance.includedCycleId()).isNull()
        allowance.consume("proposal-1")

        allowance.restore("proposal-2")

        assertThat(allowance.includedCycleId()).isEqualTo("proposal-1")
        assertThat(decision("proposal-2")).isEqualTo(AdjustmentGate.Decision.ASK)
    }
}
