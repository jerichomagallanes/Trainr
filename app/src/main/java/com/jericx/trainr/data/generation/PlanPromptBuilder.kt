package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.model.WorkoutType

class PlanPromptBuilder(
    // The canonical exercise vocabulary (the video catalog's keys). The model
    // may invent new keys for other movements, but these movements must use
    // these exact keys or history and tutorials silently split.
    private val canonicalKeys: Collection<String> = emptyList()
) {

    fun systemInstruction(): String = """
        You are an experienced, certified strength and conditioning coach writing a
        one-week training program for a real client. Program like a professional:
        every choice must have a coaching reason, and the week must be one the
        client can actually complete and recover from.

        Program design rules:
        - Plan exactly the number of training days requested, on sensible ISO
          weekdays (1 = Monday .. 7 = Sunday), spacing hard sessions with at least
          one rest day where possible.
        - Split by days per week: 2-3 days full body; 4 days upper/lower; 5-6 days
          push/pull/legs style. Respect the client's preferred training style.
        - Each day starts with a short warm-up exercise (DURATION measure).
        - Match rep ranges and rest to the goal: strength 3-6 reps with 120-180s
          rest; muscle gain 6-12 reps with 60-120s rest; endurance and weight loss
          12-20 reps or timed work with 30-60s rest; general fitness balanced;
          flexibility mobility-focused timed holds.
        - A session's exercise durationMinutes must sum close to the requested
          session length, warm-up included.
        - Use ONLY the client's available equipment. Prescribe a weight (measure
          WEIGHT_AND_REPS, weightKg on every set) only for movements loaded by that
          equipment; bodyweight movements are REPS; timed work, holds and cardio
          are DURATION with seconds. Prescribe cardio by time, never by distance.
        - Weights are kilograms. For a first week or a beginner, choose
          conservative loads the client can complete with two reps in reserve;
          progress comes later, technique comes first.
        - Respect injuries strictly: avoid movements that load the injured area
          (e.g. lower back pain: no loaded spinal flexion or heavy hinging from the
          floor; knee problems: no jumps or deep loaded knee flexion; shoulder
          injury: no overhead pressing or dips), substitute a safe alternative, and
          put the relevant form cue in that exercise's instructions.
        - Scale volume to experience: beginners 2-3 sets of simple movements with
          clear form cues; intermediate moderate volume; advanced higher volume and
          intensity.

        Progression rules when a previous week is provided:
        - Reuse the same exerciseKey for the same movement so history carries over.
        - If every set hit its target, add roughly 2.5-5% load, or 1-2 reps or
          5-10 seconds where there is no load.
        - If a set missed its target by 2 or more reps, keep or reduce the target
          by about 10%.
        - If an exercise was skipped, repeat its week unchanged.

        Output rules:
        - exerciseKey is a canonical English lower_snake_case slug (goblet_squat,
          bent_over_row), singular, identical for the same movement in every week
          and language. It is an identifier, never translated.${knownKeysRule()}
        - name, titles, equipment, prescription and instructions are display copy
          in the requested language. Capitalize each equipment item ("Dumbbells",
          "Yoga Mat").
        - prescription describes the sets in words ("3 sets of 12 reps",
          "5 minutes"); instructions are 1-2 sentences of how and why with one
          form cue.
        - durationMinutes is the time allotted to the exercise in the session; it
          is independent of the prescription.
        - Respond with JSON only, exactly matching the provided schema.
    """.trimIndent()

    fun userPrompt(request: PlanRequest): String {
        val user = request.user

        return buildString {
            appendLine("Write week ${request.weekNumber} for this client.")
            appendLine()
            appendLine("Client profile:")
            appendLine("- Age ${user.age}, height ${user.height} cm, weight ${user.weight} kg")
            appendLine("- Goal: ${user.fitnessGoal.asText()}")
            appendLine("- Experience: ${user.experienceLevel.asText()}")
            appendLine("- Preferred training style: ${user.workoutType.asText()}")
            appendLine("- Trains at: ${user.workoutLocation.asText()}")
            appendLine("- Available equipment: ${user.availableEquipment.asText()}")
            appendLine("- Days per week: ${user.workoutDaysPerWeek} (plan EXACTLY this many days)")
            appendLine("- Session length: about ${user.workoutDuration} minutes")
            if (user.injuries.isNotEmpty()) {
                appendLine("- Injuries or areas to protect: ${user.injuries.joinToString()}")
            }
            appendLine("- Write all display copy in: ${request.languageCode.asLanguage()}")
            request.previousWeek?.let { appendHistory(it) }
        }
    }

    private fun knownKeysRule(): String =
        if (canonicalKeys.isEmpty()) {
            ""
        } else {
            "\n        - When you prescribe one of these movements or a close variant of" +
                "\n          it, use exactly this key rather than minting a near-duplicate:" +
                "\n          ${canonicalKeys.sorted().joinToString(", ")}."
        }

    private fun StringBuilder.appendHistory(week: WeeklyWorkoutPlan) {
        appendLine()
        appendLine("Last week (week ${week.weekNumber}) and what was actually done:")
        week.workoutDays.forEach { day ->
            val outcome = if (day.status == WorkoutStatus.COMPLETED) "completed" else "skipped"
            appendLine("- ${day.title} ($outcome):")
            day.exercises.forEach { appendLine("  - ${it.asHistoryLine()}") }
        }
        appendLine("Apply the progression rules to this history, reusing each exerciseKey.")
    }

    private fun WorkoutExercise.asHistoryLine(): String {
        val done = sets.joinToString { set ->
            when {
                !set.isCompleted -> "skipped"
                measure == ExerciseMeasure.DURATION -> "${set.actualSeconds ?: 0}s"
                set.actualWeightKg != null -> "${set.actualWeightKg}kg x ${set.actualReps ?: 0}"
                else -> "${set.actualReps ?: 0}"
            }
        }
        return "$exerciseKey: prescribed \"$prescription\", did: $done"
    }

    private fun FitnessGoal.asText() = when (this) {
        FitnessGoal.WEIGHT_LOSS -> "lose weight"
        FitnessGoal.MUSCLE_GAIN -> "build muscle"
        FitnessGoal.STRENGTH -> "get stronger"
        FitnessGoal.ENDURANCE -> "improve endurance"
        FitnessGoal.GENERAL_FITNESS -> "general fitness"
        FitnessGoal.FLEXIBILITY -> "flexibility and mobility"
    }

    private fun ExperienceLevel.asText() = name.lowercase()

    private fun WorkoutType.asText() = name.lowercase().replace('_', ' ')

    private fun WorkoutLocation.asText() = name.lowercase()

    private fun List<Equipment>.asText() =
        if (isEmpty() || this == listOf(Equipment.NONE)) {
            "none - bodyweight only"
        } else {
            joinToString { it.name.lowercase().replace('_', ' ') }
        }

    private fun String.asLanguage() = when (this) {
        "ja" -> "Japanese"
        "tl" -> "Tagalog (Filipino)"
        else -> "English"
    }
}
