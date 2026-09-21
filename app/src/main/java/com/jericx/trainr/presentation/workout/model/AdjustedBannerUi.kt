package com.jericx.trainr.presentation.workout.model

import androidx.annotation.StringRes
import com.jericx.trainr.domain.catalog.MuscleRegion

data class AdjustedBannerUi(
    @StringRes val messageRes: Int,
    val regions: List<MuscleRegion> = emptyList(),
    val fromName: String = "",
    val toName: String = ""
)
