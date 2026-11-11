package com.jericx.trainr.data.ads

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.BuildConfig
import org.junit.Test

// Nothing on screen reaches this: it pins which unit a build is wired to. The
// release half of the guarantee is the build failing when the real id is
// missing, which no unit test can run; see the pull request for the command
// that proves it. What is left is checkable in whichever variant the suite is
// run under, which on CI is devDebug.
class AdsUnitIdTest {

    @Test
    fun `a debug build serves Google's test unit and a release build never does`() {
        val testUnit = "ca-app-pub-3940256099942544/9214589741"

        if (BuildConfig.DEBUG) {
            assertThat(Ads.PLAN_BANNER_UNIT_ID).isEqualTo(testUnit)
        } else {
            assertThat(Ads.PLAN_BANNER_UNIT_ID).isNotEqualTo(testUnit)
        }
    }
}
