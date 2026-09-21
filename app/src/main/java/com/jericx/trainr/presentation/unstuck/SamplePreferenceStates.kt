package com.jericx.trainr.presentation.unstuck

import com.jericx.trainr.domain.unstuck.SessionNote

object SamplePreferenceStates {

    val empty = PreferencesUiState(isLoaded = true)

    val filled = PreferencesUiState(
        isLoaded = true,
        preferences = listOf(
            PreferenceCardUi(
                id = 1,
                weekdayName = "Tuesday",
                minutes = 35,
                confirmedOn = "12 Sept 2026"
            )
        ),
        notes = listOf(
            SessionNote(
                id = 1,
                userId = 1,
                workoutDayId = 7,
                text = "I had to leave early for work.",
                createdAt = 0L,
                updatedAt = 0L
            )
        ),
        todayAdjustment = TodayAdjustmentKind.SHORTER
    )

    val editing = EditPreferenceUiState(
        isLoaded = true,
        weekdayName = "Tuesday",
        presets = listOf(25, 35, 45),
        selectedMinutes = 35
    )

    val editingWithError = editing.copy(
        selectedMinutes = null,
        customMinutesText = "3",
        minutesError = true
    )
}
