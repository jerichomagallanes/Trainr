package com.jericx.trainr.presentation.workout.components

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.R
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.presentation.workout.model.StatusTone
import org.junit.Test

class WorkoutStatusUiTest {

    @Test
    fun eachStatusMapsToItsOwnLabel() {
        assertThat(WorkoutStatus.COMPLETED.labelRes).isEqualTo(R.string.completed)
        assertThat(WorkoutStatus.IN_PROGRESS.labelRes).isEqualTo(R.string.in_progress)
        assertThat(WorkoutStatus.NOT_STARTED.labelRes).isEqualTo(R.string.not_started)
    }

    @Test
    fun eachStatusMapsToItsOwnTone() {
        assertThat(WorkoutStatus.COMPLETED.chipTone).isEqualTo(StatusTone.DONE)
        assertThat(WorkoutStatus.IN_PROGRESS.chipTone).isEqualTo(StatusTone.ACTIVE)
        assertThat(WorkoutStatus.NOT_STARTED.chipTone).isEqualTo(StatusTone.IDLE)
    }

    @Test
    fun noTwoStatusesShareALabelOrTone() {
        val statuses = WorkoutStatus.entries

        assertThat(statuses.map { it.labelRes }.toSet()).hasSize(statuses.size)
        assertThat(statuses.map { it.chipTone }.toSet()).hasSize(statuses.size)
    }
}
