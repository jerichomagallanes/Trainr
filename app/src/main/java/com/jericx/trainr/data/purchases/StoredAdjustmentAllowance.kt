package com.jericx.trainr.data.purchases

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jericx.trainr.domain.purchases.AdjustmentAllowance

class StoredAdjustmentAllowance(context: Context) : AdjustmentAllowance {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun includedCycleId(): String? = prefs.getString(KEY_CYCLE, null)

    override fun consume(cycleId: String) {
        if (prefs.contains(KEY_CYCLE)) return
        prefs.edit {
            putString(KEY_CYCLE, cycleId)
            // Kept so support can say when the cycle went, never read to decide.
            putLong(KEY_CONSUMED_AT, System.currentTimeMillis())
        }
    }

    companion object {
        private const val PREFS_NAME = "purchase_preferences"
        private const val KEY_CYCLE = "adjustment_included_cycle"
        private const val KEY_CONSUMED_AT = "adjustment_included_consumed_at"
    }
}
