package com.jericx.trainr.presentation.workout

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.data.ads.Ads
import com.jericx.trainr.data.purchases.Entitlements
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdSlotViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        isPro: Boolean,
        hasRead: Boolean,
        canShowAds: Boolean = true
    ): AdSlotViewModel {
        val entitlements = mockk<Entitlements>()
        every { entitlements.isPro } returns MutableStateFlow(isPro)
        every { entitlements.hasRead } returns MutableStateFlow(hasRead)
        val ads = mockk<Ads>()
        every { ads.canShowAds } returns MutableStateFlow(canShowAds)
        every { ads.privacyOptionsRequired } returns MutableStateFlow(false)
        return AdSlotViewModel(entitlements, ads)
    }

    private fun showsAds(viewModel: AdSlotViewModel): Boolean {
        var shown = false
        runTest(testDispatcher) {
            backgroundScope.launch { viewModel.showAds.collect {} }
            advanceUntilIdle()
            shown = viewModel.showAds.value
        }
        return shown
    }

    // The defect, in the state it actually occurs in: between process start and
    // the launch read landing, a subscriber and a free client are the same
    // unread false, and the banner Pro exists to remove was requested in that
    // gap and shown to people who had paid not to see it.
    @Test
    fun `nobody is shown an ad while the store is still answering`() {
        assertThat(showsAds(viewModel(isPro = false, hasRead = false))).isFalse()
    }

    // A read that failed never sets hasRead, so this is the same input as the
    // test above and deliberately the same answer: no ad, for good, on this
    // launch. An impression nobody serves costs less than charging a
    // subscriber the one thing Pro promises to remove.
    @Test
    fun `a read that failed leaves the slot empty rather than guessing`() {
        assertThat(showsAds(viewModel(isPro = false, hasRead = false))).isFalse()
    }

    @Test
    fun `a subscriber is shown no ad once the read confirms pro`() {
        assertThat(showsAds(viewModel(isPro = true, hasRead = true))).isFalse()
    }

    @Test
    fun `a free client is shown an ad once the read lands`() {
        assertThat(showsAds(viewModel(isPro = false, hasRead = true))).isTrue()
    }

    @Test
    fun `consent still decides before any of it`() {
        assertThat(showsAds(viewModel(isPro = false, hasRead = true, canShowAds = false)))
            .isFalse()
    }
}
