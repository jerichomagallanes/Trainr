package com.jericx.trainr.presentation.common

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.jericx.trainr.R

/**
 * Language preferences manager using SharedPreferences.
 * Handles language selection and persistence across app sessions.
 */
class LanguagePreferences(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    var currentLanguage by mutableStateOf(getStoredLanguage())
        private set
    
    /**
     * Available languages with their codes and display names
     */
    data class Language(
        val code: String,
        val displayName: String,
        val nativeName: String
    )
    
    companion object {
        private const val PREFS_NAME = "language_preferences"
        private const val KEY_LANGUAGE_CODE = "language_code"
        private const val DEFAULT_LANGUAGE = "en"
        
    }
    
    /**
     * Get the current language code from SharedPreferences
     */
    private fun getStoredLanguage(): String {
        return sharedPreferences.getString(KEY_LANGUAGE_CODE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    /**
     * Set the current language and persist it
     */
    fun setLanguage(context: Context, languageCode: String) {
        val availableLanguages = getAvailableLanguages(context)
        if (availableLanguages.any { it.code == languageCode }) {
            sharedPreferences.edit {
                putString(KEY_LANGUAGE_CODE, languageCode)
            }
            currentLanguage = languageCode
        }
    }
    
    /**
     * Get available languages from resources
     */
    fun getAvailableLanguages(context: Context): List<Language> {
        val codes = context.resources.getStringArray(R.array.language_codes)
        val displayNames = context.resources.getStringArray(R.array.language_display_names)
        val nativeNames = context.resources.getStringArray(R.array.language_native_names)
        
        return codes.mapIndexed { index, code ->
            Language(
                code = code,
                displayName = displayNames.getOrNull(index) ?: code,
                nativeName = nativeNames.getOrNull(index) ?: code
            )
        }
    }
    
    /**
     * Get the current language object
     */
    fun getCurrentLanguageObject(context: Context): Language {
        val availableLanguages = getAvailableLanguages(context)
        return availableLanguages.find { it.code == currentLanguage }
            ?: availableLanguages.first()
    }

}