package com.jericx.trainr.data.purchases

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jericx.trainr.domain.purchases.FreeGenerationAllowance

class StoredGenerationAllowance(context: Context) : FreeGenerationAllowance {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasBeenUsed(): Boolean = prefs.getBoolean(KEY_USED, false)

    override fun markUsed() {
        prefs.edit { putBoolean(KEY_USED, true) }
    }

    companion object {
        private const val PREFS_NAME = "purchase_preferences"
        private const val KEY_USED = "free_generation_used"
    }
}
