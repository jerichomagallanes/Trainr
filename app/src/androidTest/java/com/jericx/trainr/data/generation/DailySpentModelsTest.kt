package com.jericx.trainr.data.generation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Instrumented because it is SharedPreferences: the allowance has to outlive the process.
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

    @Test
    fun aFreshInstanceSeesWhatAnEarlierOneRecorded() {
        DailySpentModels(context).markSpent("gemini-3.6-flash")

        assertThat(DailySpentModels(context).spentToday()).isNotEmpty()
    }

    // The quota day is Google's rather than the device's.
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
