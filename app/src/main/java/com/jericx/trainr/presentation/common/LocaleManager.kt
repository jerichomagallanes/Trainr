package com.jericx.trainr.presentation.common

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

// The app is pinned to one language, so this exists to force it rather than to
// switch it: the returned context carries the configuration, and the caller has
// to install it or nothing changes.
object LocaleManager {

    fun updateAppLocale(context: Context, languageCode: String): Context {
        val locale = getLocaleForLanguageCode(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    private fun getLocaleForLanguageCode(languageCode: String): Locale {
        return when (languageCode) {
            "en" -> Locale.ENGLISH
            "tl" -> Locale.forLanguageTag("tl")
            "ja" -> Locale.JAPANESE
            else -> Locale.ENGLISH
        }
    }
}