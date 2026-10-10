package com.jericx.trainr.domain.unstuck

sealed interface ApplyResult {
    data class Applied(val adjustment: AppliedAdjustment, val addedExerciseId: Long?) : ApplyResult
    data class AlreadyApplied(val adjustment: AppliedAdjustment) : ApplyResult
    data class Stale(val currentRevision: String) : ApplyResult
    data class Rejected(val reason: ApplyRejection) : ApplyResult
    data class Failed(val cause: Throwable) : ApplyResult
}

enum class ApplyRejection {
    UNKNOWN_DAY,
    UNKNOWN_EXERCISE,
    BEFORE_SNAPSHOT_MISMATCH,
    PERFORMED_SETS_MISMATCH,
    AFTER_NOT_A_SUBSET,
    UNKNOWN_CATALOG_KEY,
    BAD_PLACEHOLDER,
    EMPTY_CHANGES,
    WRONG_SCOPE
}

sealed interface UndoResult {
    data class Restored(
        val adjustment: AppliedAdjustment,
        val keptPerformedSubstituteSets: Int
    ) : UndoResult

    data object AlreadyUndone : UndoResult
    data object Unknown : UndoResult
    data class Failed(val cause: Throwable) : UndoResult
}
