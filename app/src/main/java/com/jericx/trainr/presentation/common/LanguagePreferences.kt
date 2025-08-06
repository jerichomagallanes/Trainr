package com.jericx.trainr.presentation.common

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

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
        
        val AVAILABLE_LANGUAGES = listOf(
            Language("en", "English", "English"),
            Language("tl", "Tagalog", "Tagalog"),
            Language("ja", "Japanese", "日本語")
        )
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
    fun setLanguage(languageCode: String) {
        if (AVAILABLE_LANGUAGES.any { it.code == languageCode }) {
            sharedPreferences.edit {
                putString(KEY_LANGUAGE_CODE, languageCode)
            }
            currentLanguage = languageCode
        }
    }
    
    /**
     * Get the current language object
     */
    fun getCurrentLanguageObject(): Language {
        return AVAILABLE_LANGUAGES.find { it.code == currentLanguage }
            ?: AVAILABLE_LANGUAGES.first()
    }

}