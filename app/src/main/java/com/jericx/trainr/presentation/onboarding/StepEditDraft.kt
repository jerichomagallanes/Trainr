package com.jericx.trainr.presentation.onboarding

import androidx.lifecycle.ViewModel

// What an edited step has in it before SAVE turns it into an answer. Scoped to
// that step's own back stack entry rather than to the onboarding view model: an
// edit has to survive a configuration change and be gone once it is closed or
// saved, and popping the entry is exactly both of those. The walk's answers
// cannot live here, because stepping back pops an entry whose answer has to
// outlive it.
class StepEditDraft : ViewModel() {
    var basicInfo: BasicInfoEntry? = null
    var bodyMetrics: BodyMetricsEntry? = null
    var fitnessGoal: FitnessGoalEntry? = null
    var workoutSetup: WorkoutSetupEntry? = null
    var limitations: LimitationsEntry? = null
}
