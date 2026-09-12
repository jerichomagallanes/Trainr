package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.SessionBudget
import com.jericx.trainr.domain.model.Equipment
import com.jericx.trainr.domain.model.ExerciseMeasure
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal
import com.jericx.trainr.domain.model.Injury
import com.jericx.trainr.domain.model.UnitSystem
import com.jericx.trainr.domain.model.UserProfile
import com.jericx.trainr.domain.model.WeeklyWorkoutPlan
import com.jericx.trainr.domain.model.WorkoutExercise
import com.jericx.trainr.domain.model.WorkoutLocation
import com.jericx.trainr.domain.model.WorkoutStatus
import com.jericx.trainr.domain.model.WorkoutType

class PlanPromptBuilder(
    // The video catalog's keys. These movements must use these exact keys or
    // history and tutorials silently split.
    private val canonicalKeys: Collection<String> = emptyList()
) {

    fun systemInstruction(): String = """
        You are an experienced, certified strength and conditioning coach writing a
        one-week training program for a real client. Program like a professional:
        every choice must have a coaching reason, and the week must be one the
        client can actually complete and recover from.

        When the client's answers pull against each other, this is the order that
        decides: injuries first, then the equipment they actually have, then the
        time they have, then their goal, then their preferred style. The style
        decides what the sessions are made of; the goal decides how they are
        loaded.

        Program design rules:
        - Plan exactly the number of training days requested, placed across the
          seven days of the week (1 = the first day of the week .. 7 = the last),
          spacing hard sessions with at least one rest day where possible. The
          week begins on the day the client starts, which may be any weekday.
        - Split by days per week: 1-3 days full body; 4 days upper/lower; 5-6 days
          push/pull/legs style. At 7 days, program at most 5 hard sessions and make
          the others easy mobility or low-intensity work.
        - Train every major muscle group at least twice in the week: the same work
          split over two days beats all of it on one.
        - Reach the weekly set target given below for each major muscle group, and
          never exceed the session set cap given below. The cap is what the
          client's session length pays for once warm-up and rest are counted, so a
          session that exceeds it is a session they will not finish.
        - Include a lower-body push, an upper-body push and an upper-body pull
          every week. Order each session large muscle groups before small,
          multi-joint before single-joint.
        - Each day starts with a short warm-up exercise (DURATION measure): easy
          versions of the movements that follow, not a generic routine and not
          static stretching.
        - Load and reps follow the goal: strength 3-6 reps and 180s rest, heavy;
          muscle gain 6-12 reps, 90-120s rest on multi-joint work and 60-90s on
          isolation; endurance and weight loss 12-20 reps or timed work with
          30-60s rest; general fitness 8-12 reps with 60-90s rest; flexibility
          timed holds of about 60 seconds per muscle group.
        - Leave 1-3 repetitions in reserve on every working set and never program
          a set to failure: failure is not needed for strength or size. A beginner
          or a first week stays at 2-3 in reserve, and technique comes before load.
        - Where no external load is available, a strength goal is served by harder
          leverage - slower tempo, fuller range, one-limb versions - never by
          prescribing a low-rep maximum the client has no weight to reach.
        - Prescribe conditioning by time, never by distance, and meet the weekly
          conditioning minutes given below where there are any. Where the goal is
          muscle or strength, keep conditioning short and low-impact and keep it
          off the day before a hard leg session: running blunts strength and size
          gains where cycling does not.
        - Use ONLY the client's available equipment, and list in each day's
          equipment array only items from that list. Prescribe a weight (measure
          WEIGHT_AND_REPS, weightKg on every set) only for movements loaded by that
          equipment; bodyweight movements are REPS; timed work, holds and cardio
          are DURATION with seconds.
        - Weights are kilograms, whatever the client reads them in. Every
          weightKg must be a multiple of the client's smallest loadable
          increment, given below, or the plan asks for a weight they cannot
          make. For a first week or a beginner, choose conservative loads the
          client can complete with three reps in reserve; progress comes later.
        - Respect injuries strictly: avoid movements that load the injured area
          (e.g. lower back pain: no loaded spinal flexion or heavy hinging from the
          floor; knee problems: no jumps or deep loaded knee flexion; shoulder
          injury: no overhead pressing or dips), substitute a safe alternative, and
          put the relevant form cue in that exercise's instructions.
        - Scale volume to experience: beginners 2-3 sets of simple movements with
          clear form cues; intermediate moderate volume; advanced higher volume and
          intensity.
        - Age 65 and over: include balance work in every session, prefer supported
          or machine versions of each movement, and program no maximal attempts.
          Under 18: bodyweight competence and technique first, moderate loads, and
          no maximal attempts.

        Progression rules when a previous week is provided:
        - Reuse the same exerciseKey for the same movement so history carries over.
        - If every set hit its target, add load: one increment at minimum, and
          2-10% where that is more. An increase smaller than one increment is not
          an increase, because the client cannot load it. Where there is no load,
          add 1-2 reps or 5-10 seconds instead.
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
        - Day titles are short and name the session's focus ("Full Body
          Strength", "Lower Body Power") - never letter or index labels like
          "Full Body A" or "Day 1".
        - The plan title names the block, not its position: "Beginner Muscle
          Building", never "... - Week 2". The app shows which week it is.
        - prescription is a short chip under about 25 characters, shaped like
          "3 sets of 12 reps", "3 sets of 45 seconds" or "5 minutes". Per-side,
          tempo or pacing detail belongs in instructions, never the prescription.
        - instructions are 1-2 sentences of how and why with one form cue.
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
            appendLine(
                "- Session set cap: at most ${SessionBudget.maxSetsPerSession(user)} sets in " +
                    "one day, warm-up included"
            )
            appendLine(
                "- Weekly set target: about ${SessionBudget.weeklySetsPerMuscle(user)} hard sets " +
                    "per major muscle group across the week"
            )
            weeklyConditioningMinutes(user)?.let {
                appendLine("- Weekly conditioning: $it")
            }
            appendLine(
                "- Reads weights in ${user.weightUnits.asWeightWord()}; smallest loadable " +
                    "increment ${incrementKg(user.weightUnits)} kg"
            )
            if (user.injuries.isNotEmpty()) {
                appendLine("- Injuries or areas to protect: ${user.injuries.joinToString { it.asText() }}")
            }
            appendLine("- Write all display copy in: ${request.languageCode.asLanguage()}")
            request.previousWeek?.let { appendHistory(it) }
        }
    }


    // Public-health dose, so the plan reaches it rather than leaving the client
    // to guess: 150-300 minutes a week for health, and more than 250 before
    // weight loss becomes clinically meaningful (WHO 2020; ACSM 2009).
    private fun weeklyConditioningMinutes(user: UserProfile): String? =
        when (user.fitnessGoal) {
            FitnessGoal.WEIGHT_LOSS -> "at least 250 minutes of moderate work across the week"
            FitnessGoal.ENDURANCE -> "150-300 minutes of moderate work across the week"
            FitnessGoal.GENERAL_FITNESS -> "at least 150 minutes of moderate work across the week"
            else -> null
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

    private fun UnitSystem.asWeightWord() = when (this) {
        UnitSystem.METRIC -> "kilograms"
        UnitSystem.IMPERIAL -> "pounds"
    }

    // Kilograms, the unit the contract speaks: 2.27 kg is five pounds, so a
    // client in pounds gets multiples that land on real plates.
    private fun incrementKg(units: UnitSystem): String = when (units) {
        UnitSystem.METRIC -> "2.5"
        UnitSystem.IMPERIAL -> "2.27"
    }

    private fun Injury.asText() = when (this) {
        Injury.LOWER_BACK -> "lower back pain"
        Injury.KNEE -> "knee problems"
        Injury.SHOULDER -> "shoulder injury"
        Injury.WRIST -> "wrist pain"
        Injury.ANKLE -> "ankle issues"
        Injury.HIP -> "hip problems"
        Injury.NECK -> "neck pain"
    }

    private fun ExperienceLevel.asText() = name.lowercase()

    private fun WorkoutType.asText() = when (this) {
        WorkoutType.STRENGTH -> "resistance training"
        WorkoutType.CARDIO -> "cardio"
        WorkoutType.HIIT -> "high-intensity intervals"
        WorkoutType.YOGA -> "mobility and yoga"
        WorkoutType.MIXED -> "a mix of resistance and conditioning"
    }

    private fun WorkoutLocation.asText() = name.lowercase()

    // Named the way a coach would name them, so the model knows what a
    // machine is for rather than guessing from an enum constant.
    private fun List<Equipment>.asText() =
        if (isEmpty() || this == listOf(Equipment.NONE)) {
            "none - bodyweight only"
        } else {
            filterNot { it == Equipment.NONE }.joinToString { it.asText() }
        }

    private fun Equipment.asText() = when (this) {
        Equipment.NONE -> "bodyweight only"
        Equipment.DUMBBELLS -> "dumbbells"
        Equipment.BARBELL -> "barbell and plates"
        Equipment.BENCH -> "adjustable bench"
        Equipment.RESISTANCE_BANDS -> "resistance bands"
        Equipment.PULL_UP_BAR -> "pull-up bar"
        Equipment.KETTLEBELLS -> "kettlebells"
        Equipment.SQUAT_RACK -> "squat rack"
        Equipment.CABLE_MACHINE -> "cable machine"
        Equipment.MACHINES -> "weight machines (lat pulldown, leg press, chest press, leg curl)"
        Equipment.CARDIO_MACHINES -> "cardio machines (treadmill, bike, rower)"
        Equipment.MAT -> "exercise mat"
        Equipment.JUMP_ROPE -> "jump rope"
    }

    private fun String.asLanguage() = when (this) {
        "ja" -> "Japanese"
        "tl" -> "Tagalog (Filipino)"
        else -> "English"
    }
}
