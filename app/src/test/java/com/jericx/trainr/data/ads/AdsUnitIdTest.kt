package com.jericx.trainr.data.ads

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.BuildConfig
import org.junit.Test

// Nothing on screen reaches this: it pins which unit a build is wired to.
// The release half of the guarantee is the build failing, which no unit test
// can run; see the pull request for the command that proves it.
class AdsUnitIdTest {

    @Test
    fun `a debug build serves Google's test unit`() {
        assertThat(BuildConfig.DEBUG).isTrue()
        assertThat(Ads.PLAN_BANNER_UNIT_ID).isEqualTo("ca-app-pub-3940256099942544/9214589741")
    }

    @Test
    fun `the unit comes from the build and is never blank`() {
        assertThat(Ads.PLAN_BANNER_UNIT_ID).isEqualTo(BuildConfig.ADMOB_PLAN_BANNER_ID)
        assertThat(Ads.PLAN_BANNER_UNIT_ID).isNotEmpty()
    }
}
