package com.jericx.trainr.presentation.workout.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.jericx.trainr.R
import com.jericx.trainr.domain.generation.Prescription
import com.jericx.trainr.domain.generation.PrescriptionUnit

// The chip's words. The value says what was prescribed; this says it in
// English, so a set added on the day re-reads rather than going stale.
@Composable
fun Prescription.asText(): String = when (this) {
    Prescription.None -> ""

    is Prescription.Fixed -> sets(setCount, amountText(unit, amount).perSide(perSide))

    is Prescription.Spread -> sets(setCount, rangeText(unit, low, high).perSide(perSide))
}

@Composable
private fun sets(count: Int, amount: String): String =
    pluralStringResource(R.plurals.prescription_sets, count, count, amount)

@Composable
private fun amountText(unit: PrescriptionUnit, amount: Int): String = when (unit) {
    PrescriptionUnit.REPS -> pluralStringResource(R.plurals.prescription_reps, amount, amount)
    PrescriptionUnit.SECONDS ->
        pluralStringResource(R.plurals.prescription_seconds, amount, amount)
    PrescriptionUnit.MINUTES ->
        pluralStringResource(R.plurals.prescription_minutes, amount, amount)
}

@Composable
private fun rangeText(unit: PrescriptionUnit, low: Int, high: Int): String = when (unit) {
    PrescriptionUnit.REPS -> stringResource(R.string.prescription_range_reps, low, high)
    PrescriptionUnit.SECONDS -> stringResource(R.string.prescription_range_seconds, low, high)
    PrescriptionUnit.MINUTES -> stringResource(R.string.prescription_range_minutes, low, high)
}

@Composable
private fun String.perSide(perSide: Boolean): String =
    if (perSide) stringResource(R.string.prescription_per_side, this) else this
