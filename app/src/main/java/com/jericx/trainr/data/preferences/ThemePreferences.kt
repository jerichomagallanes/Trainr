package com.jericx.trainr.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppearanceMode { SYSTEM, LIGHT, DARK }

class ThemePreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _appearance = MutableStateFlow(getStoredAppearance())
    val appearance: StateFlow<AppearanceMode> = _appearance.asStateFlow()

    fun setAppearance(mode: AppearanceMode) {
        sharedPreferences.edit {
            putString(KEY_APPEARANCE, mode.name)
        }
        _appearance.value = mode
    }

    private fun getStoredAppearance(): AppearanceMode {
        val stored = sharedPreferences.getString(KEY_APPEARANCE, null)
        return AppearanceMode.entries.firstOrNull { it.name == stored } ?: AppearanceMode.SYSTEM
    }

    companion object {
        private const val PREFS_NAME = "theme_preferences"
        private const val KEY_APPEARANCE = "appearance_mode"
    }
}
