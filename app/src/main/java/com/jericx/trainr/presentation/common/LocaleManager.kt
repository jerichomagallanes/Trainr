package com.jericx.trainr.presentation.common

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.activity.ComponentActivity
import com.jericx.trainr.R
import java.util.Locale

/**
 * Manager for runtime locale changes in the app.
 * Handles updating app configuration when language is changed.
 */
object LocaleManager {
    
    /**
     * Update app locale and return new context with updated configuration
     */
    fun updateAppLocale(context: Context, languageCode: String): Context {
        val locale = getLocaleForLanguageCode(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Set app locale for the current activity and save navigation state
     */
    fun setAppLocale(activity: ComponentActivity, languageCode: String) {
        val locale = getLocaleForLanguageCode(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration()
        config.setLocale(locale)
        
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)

        activity.recreate()
    }
    
    /**
     * Get Locale object for language code
     */
    private fun getLocaleForLanguageCode(languageCode: String): Locale {
        return when (languageCode) {
            "en" -> Locale.ENGLISH
            "tl" -> Locale("tl")
            "ja" -> Locale.JAPANESE
            else -> Locale.ENGLISH
        }
    }
    
    /**
     * Get available language codes from resources
     */
    fun getAvailableLanguageCodes(context: Context): Array<String> {
        return context.resources.getStringArray(R.array.language_codes)
    }

}