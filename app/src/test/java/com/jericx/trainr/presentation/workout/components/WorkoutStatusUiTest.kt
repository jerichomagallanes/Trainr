package com.jericx.trainr.presentation.workout.components

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.common.theme.StatusCompleted
import com.jericx.trainr.presentation.common.theme.StatusInProgress
import com.jericx.trainr.presentation.common.theme.StatusNotStarted
import org.junit.Test

class WorkoutStatusUiTest {

    @Test
    fun eachStatusMapsToItsOwnLabel() {
        assertThat(WorkoutStatus.COMPLETED.labelRes).isEqualTo(R.string.completed)
        assertThat(WorkoutStatus.IN_PROGRESS.labelRes).isEqualTo(R.string.in_progress)
        assertThat(WorkoutStatus.NOT_STARTED.labelRes).isEqualTo(R.string.not_started)
    }

    @Test
    fun eachStatusMapsToItsOwnColour() {
        assertThat(WorkoutStatus.COMPLETED.chipColor).isEqualTo(StatusCompleted)
        assertThat(WorkoutStatus.IN_PROGRESS.chipColor).isEqualTo(StatusInProgress)
        assertThat(WorkoutStatus.NOT_STARTED.chipColor).isEqualTo(StatusNotStarted)
    }

    @Test
    fun noTwoStatusesShareALabelOrColour() {
        val statuses = WorkoutStatus.entries

        assertThat(statuses.map { it.labelRes }.toSet()).hasSize(statuses.size)
        assertThat(statuses.map { it.chipColor }.toSet()).hasSize(statuses.size)
    }
}
