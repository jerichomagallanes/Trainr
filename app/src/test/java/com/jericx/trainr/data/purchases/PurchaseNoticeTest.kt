package com.jericx.trainr.data.purchases

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import org.junit.Test
import java.io.IOException

// A purchase that did not end in Pro used to end in silence, which reads as a
// broken button. Now each outcome says what to do, except the one the buyer
// chose themselves.
class PurchaseNoticeTest {

    private fun storeError(code: PurchasesErrorCode, cancelled: Boolean = false) =
        PurchasesTransactionException(PurchasesError(code, null), cancelled)

    @Test
    fun `a cancelled purchase stays silent`() {
        val cancelled = storeError(PurchasesErrorCode.PurchaseCancelledError, cancelled = true)
        assertThat(Entitlements.noticeFor(cancelled)).isNull()

        val cancelledOutsideATransaction =
            PurchasesException(PurchasesError(PurchasesErrorCode.PurchaseCancelledError, null))
        assertThat(Entitlements.noticeFor(cancelledOutsideATransaction)).isNull()
    }

    @Test
    fun `a pending payment says Pro is on its way rather than failed`() {
        assertThat(Entitlements.noticeFor(storeError(PurchasesErrorCode.PaymentPendingError)))
            .isEqualTo(R.string.pro_purchase_pending_google)
    }

    @Test
    fun `every other failure explains what to do next`() {
        assertThat(Entitlements.noticeFor(storeError(PurchasesErrorCode.NetworkError)))
            .isEqualTo(R.string.pro_purchase_failed)
        assertThat(Entitlements.noticeFor(storeError(PurchasesErrorCode.StoreProblemError)))
            .isEqualTo(R.string.pro_purchase_failed)
        assertThat(Entitlements.noticeFor(IOException("offline")))
            .isEqualTo(R.string.pro_purchase_failed)
    }
}
