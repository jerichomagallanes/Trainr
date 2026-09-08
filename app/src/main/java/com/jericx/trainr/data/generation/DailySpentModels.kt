package com.jericx.trainr.data.generation

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jericx.trainr.domain.generation.SpentModels
import java.util.Calendar
import java.util.TimeZone

// Google resets the daily quota at midnight Pacific, so that is the boundary
// this keeps rather than the device's own midnight.
class DailySpentModels(context: Context) : SpentModels {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun spentToday(): Set<String> {
        if (prefs.getString(KEY_DAY, null) != today()) return emptySet()
        return prefs.getStringSet(KEY_MODELS, emptySet()).orEmpty()
    }

    override fun markSpent(model: String) {
        val current = spentToday()
        prefs.edit {
            putString(KEY_DAY, today())
            putStringSet(KEY_MODELS, current + model)
        }
    }

    private fun today(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(QUOTA_ZONE))
        return "%04d-%02d-%02d".format(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    private companion object {
        const val PREFS_NAME = "trainr_model_allowance"
        const val KEY_DAY = "quotaDay"
        const val KEY_MODELS = "spentModels"
        const val QUOTA_ZONE = "America/Los_Angeles"
    }
}
