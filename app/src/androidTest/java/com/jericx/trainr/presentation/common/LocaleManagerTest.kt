package com.jericx.trainr.presentation.common

import android.content.Context
import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.presentation.workout.util.WorkoutDateFormatter
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LocaleManagerTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun contextIn(locale: Locale): Context = context.createConfigurationContext(
        Configuration(context.resources.configuration).apply { setLocale(locale) }
    )

    private fun localeOf(context: Context): Locale = context.resources.configuration.locales[0]

    // The app ships English strings alone, so its dates have to be English too.
    // The forcing lives in the context this returns: the Japanese phone it was
    // handed must not survive into it.
    @Test
    fun aJapanesePhoneGetsAnEnglishConfiguration() {
        val forced = LocaleManager.updateAppLocale(contextIn(Locale.JAPANESE), "en")

        assertThat(localeOf(forced).language).isEqualTo("en")
    }

    // What it costs when the configuration is not forced: the weekday beside an
    // English heading comes out as 月曜日.
    @Test
    fun anUnforcedConfigurationIsWhereJapaneseDatesCameFrom() {
        val monday = 1787529600000L // 2026-08-24

        val asShipped = WorkoutDateFormatter.formatWeekday(monday, localeOf(contextIn(Locale.JAPANESE)))
        val forced = WorkoutDateFormatter.formatWeekday(
            monday,
            localeOf(LocaleManager.updateAppLocale(contextIn(Locale.JAPANESE), "en"))
        )

        assertThat(asShipped).isNotEqualTo(forced)
        // Which weekday depends on the device's time zone; that it is named in
        // English does not.
        assertThat(forced).isIn(
            listOf(
                "Monday", "Tuesday", "Wednesday", "Thursday",
                "Friday", "Saturday", "Sunday"
            )
        )
    }
}
