package com.jericx.trainr.presentation.purchases

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.jericx.trainr.R

// Which paid action was reached for. Carried into the prompt and the paywall so
// both lead with the thing the person actually wanted, rather than presenting
// every Pro feature with equal weight and leaving them to find theirs.
enum class PaywallReason(
    @StringRes val prompt: Int,
    @StringRes val heading: Int,
    @StringRes val detail: Int,
    @DrawableRes val icon: Int
) {
    NEXT_WEEK(
        prompt = R.string.pro_prompt_next_week,
        heading = R.string.pro_feature_next_week_title,
        detail = R.string.pro_feature_next_week_body,
        icon = R.drawable.ic_sparkles
    ),
    REWRITE(
        prompt = R.string.pro_prompt_rewrite,
        heading = R.string.pro_feature_rewrite_title,
        detail = R.string.pro_feature_rewrite_body,
        icon = R.drawable.ic_rewrite
    ),
    FRESH_PLAN(
        prompt = R.string.pro_prompt_fresh,
        heading = R.string.pro_feature_fresh_title,
        detail = R.string.pro_feature_fresh_body,
        icon = R.drawable.ic_fresh_plan
    );

    // The rest of Pro, shown under the one that brought them here.
    val others: List<PaywallReason> get() = entries.filter { it != this }
}
