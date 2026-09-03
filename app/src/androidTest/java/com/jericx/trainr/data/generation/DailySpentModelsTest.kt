package com.jericx.trainr.data.generation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented because it is SharedPreferences, and surviving a cold start is
// the property that matters: the allowance outlives the process, so relearning
// it every launch is exactly what this exists to avoid.
@RunWith(AndroidJUnit4::class)
class DailySpentModelsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clear() {
        context.getSharedPreferences("trainr_model_allowance", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun nothingIsSpentToBeginWith() {
        assertThat(DailySpentModels(context).spentToday()).isEmpty()
    }

    @Test
    fun aSpentModelIsRemembered() {
        DailySpentModels(context).markSpent("gemini-3.6-flash")

        assertThat(DailySpentModels(context).spentToday())
            .containsExactly("gemini-3.6-flash")
    }

    @Test
    fun severalSpentModelsAccumulate() {
        val store = DailySpentModels(context)
        store.markSpent("gemini-3.6-flash")
        store.markSpent("gemini-3.5-flash")

        assertThat(store.spentToday())
            .containsExactly("gemini-3.6-flash", "gemini-3.5-flash")
    }

    // A new process reads what the last one wrote. This is the whole reason it
    // is on disk rather than in memory.
    @Test
    fun aFreshInstanceSeesWhatAnEarlierOneRecorded() {
        DailySpentModels(context).markSpent("gemini-3.6-flash")

        assertThat(DailySpentModels(context).spentToday()).isNotEmpty()
    }

    // Yesterday's refusals say nothing about today's allowance, and the day is
    // Google's rather than the device's: a client in Tokyo whose date rolled
    // over hours ago still shares the same quota window.
    @Test
    fun yesterdaysRefusalsAreForgotten() {
        context.getSharedPreferences("trainr_model_allowance", Context.MODE_PRIVATE)
            .edit()
            .putString("quotaDay", "2020-01-01")
            .putStringSet("spentModels", setOf("gemini-3.6-flash"))
            .commit()

        assertThat(DailySpentModels(context).spentToday()).isEmpty()
    }
}
