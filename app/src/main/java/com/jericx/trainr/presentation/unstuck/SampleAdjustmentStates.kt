package com.jericx.trainr.presentation.unstuck

import com.jericx.trainr.R
import com.jericx.trainr.domain.catalog.MuscleRegion
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.unstuck.InfeasibleReason
import com.jericx.trainr.domain.unstuck.ProposalKind
import com.jericx.trainr.domain.unstuck.TimeScope
import com.jericx.trainr.domain.unstuck.TradeoffCode
import com.jericx.trainr.domain.unstuck.intent.DirectReason
import com.jericx.trainr.presentation.workout.sample.SampleWorkoutData

// Deterministic previews: the decision is written out rather than run, so a
// preview never depends on the policy answering the same way twice.
object SampleAdjustmentStates {

    private val day = SampleWorkoutData.dayFor(SampleWorkoutData.DEFAULT_DAY_NUMBER)

    val time = AdjustmentUiState(
        isLoaded = true,
        day = day,
        dayTitle = day.title,
        plannedMinutes = 45,
        weekdayName = "Wednesday",
        goalLabelRes = R.string.build_muscle_goal,
        reason = DirectReason.LESS_TIME,
        presets = listOf(25, 35, 45),
        selectedMinutes = 35
    )

    val timeWithError = time.copy(
        selectedMinutes = null,
        customMinutesText = "3",
        minutesError = true
    )

    val equipment = AdjustmentUiState(
        isLoaded = true,
        day = day,
        dayTitle = day.title,
        plannedMinutes = 45,
        goalLabelRes = R.string.build_muscle_goal,
        reason = DirectReason.EQUIPMENT,
        exerciseChoices = day.exercises.mapIndexed { index, it ->
            ExerciseChoice(index + 1L, it.name)
        },
        availableEquipment = setOf(Equipment.DUMBBELL)
    )

    val shorterReview = ReviewUi.Proposed(
        kind = ProposalKind.SHORTER_SESSION,
        substituteEquipment = null,
        priorityName = "Overhead Press (Barbell)",
        goalLabelRes = R.string.build_muscle_goal,
        budgetMinutes = 35,
        scope = TimeScope.WHOLE_SESSION,
        hasPerformedWork = false,
        keptNames = listOf("Overhead Press (Barbell)", "Lateral Raise (Dumbbell)"),
        replacedFrom = null,
        replacedTo = null,
        tradeoffs = listOf(
            TradeoffUi(TradeoffCode.LESS_WORK_FOR_REGIONS, listOf(MuscleRegion.ARMS), null)
        ),
        rows = listOf(
            ChangeRowUi.Reduced("Dumbbell Curl", fromSets = 3, toSets = 2),
            ChangeRowUi.Reduced("Triceps Extension", fromSets = 3, toSets = 2)
        )
    )

    val substituteReview = ReviewUi.Proposed(
        kind = ProposalKind.SUBSTITUTE,
        substituteEquipment = Equipment.DUMBBELL,
        priorityName = null,
        goalLabelRes = R.string.build_muscle_goal,
        budgetMinutes = null,
        scope = TimeScope.REMAINING,
        hasPerformedWork = true,
        keptNames = emptyList(),
        replacedFrom = "Lateral Raise (Cable)",
        replacedTo = "Lateral Raise (Dumbbell)",
        tradeoffs = listOf(
            TradeoffUi(TradeoffCode.DIFFERENT_RESISTANCE, emptyList(), "Lateral Raise (Dumbbell)"),
            TradeoffUi(TradeoffCode.SEPARATE_LOAD_HISTORY, emptyList(), "Lateral Raise (Dumbbell)")
        ),
        rows = listOf(
            ChangeRowUi.Replaced(
                fromName = "Lateral Raise (Cable)",
                toName = "Lateral Raise (Dumbbell)",
                sets = 3,
                reps = "10–12"
            )
        )
    )

    val noChangeReview = ReviewUi.NoChange(
        priorityName = null,
        goalLabelRes = R.string.build_muscle_goal,
        plannedMinutes = 28
    )

    val infeasibleReview = ReviewUi.Infeasible(
        reason = InfeasibleReason.TOO_SHORT_FOR_REQUIRED_WORK,
        minimumMinutes = 22
    )
}
