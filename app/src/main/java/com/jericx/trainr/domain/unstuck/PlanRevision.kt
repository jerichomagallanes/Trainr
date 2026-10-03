package com.jericx.trainr.domain.unstuck

import com.jericx.trainr.domain.model.WorkoutDay
import java.security.MessageDigest

// What the proposal was built against. Apply revalidates against this, so it
// has to move whenever anything the policy read moved, and stay still when
// only the order of a list did.
object PlanRevision {

    fun of(day: WorkoutDay): String = shortDigest(
        day.exercises.sortedWith(compareBy({ it.sortOrder }, { it.id })).joinToString("|") { exercise ->
            val header = listOf(
                exercise.id, exercise.exerciseKey, exercise.sortOrder, exercise.addedBy, exercise.restTime
            ).joinToString(",")
            val rows = exercise.sets.sortedWith(compareBy({ it.setNumber }, { it.id })).joinToString(";") { set ->
                listOf(
                    set.id, set.setNumber, set.targetReps, set.targetWeightKg, set.targetSeconds,
                    set.actualReps, set.actualWeightKg, set.actualSeconds, set.isCompleted, set.omittedBy
                ).joinToString(",")
            }
            "$header/$rows"
        }
    )
}

internal fun shortDigest(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
        .take(DIGEST_BYTES)
        .joinToString("") { "%02x".format(it) }

private const val DIGEST_BYTES = 16
