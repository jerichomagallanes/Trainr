package com.jericx.trainr.presentation

import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrientationTest {

    // Every screen is drawn for a phone held upright — the plan, the routine
    // and its set table have no landscape layout to fall back on. Read from the
    // installed manifest rather than by turning a device, so the guarantee
    // holds whether or not anything ever rotates.
    @Test
    fun theAppIsLockedToPortrait() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activity = ComponentName(context, MainActivity::class.java)

        val info = context.packageManager.getActivityInfo(activity, 0)

        assertThat(info.screenOrientation).isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
    }
}
