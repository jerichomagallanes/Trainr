package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.model.ExperienceLevel
import com.jericx.trainr.domain.model.FitnessGoal

// Only what the choice needs. Everything the old brief asked for in words is
// now decided before the model is asked or computed after it answers, so none
// of it is a rule the model can break. Injuries are not mentioned at all: a
// movement they rule out is on no slot's list.
class PlanPromptBuilder {

    fun systemInstruction(): String = """
        You are a strength coach choosing which movements a client trains this week.

        The app has already decided the split, which days they train, how many slots
        each day holds and what each slot is for, and it computes every set, load,
        rest and instruction afterwards. Your job is which movement fills each slot,
        and what to call each session.

        Every slot offers only movements this client can perform, with the kit they
        own, that are safe for them. Choose one of them for each slot.

        - Take the candidate that best does that slot's job for this client. Where
          two do it equally well, prefer the one their experience and age suit.
        - A day is one session, not six separate choices: no two slots should take
          near-versions of the same movement, and a day of free weights should not
          send the client across four machines to finish it.
        - Titles are English, two to four words, and name the region and the focus:
          "Upper Body Strength", "Legs and Core" - never "Day 2", "Week 3" or
          "Full Body A".
        - Answer with JSON only, in the shape given.
    """.trimIndent()

    fun userPrompt(request: PlanRequest, skeleton: PlanSkeleton): String {
        val user = request.user
        return buildString {
            appendLine("Choose the movements for week ${request.weekNumber}.")
            appendLine()
            appendLine(
                "Client: ${user.age}, ${user.experienceLevel.asText()}, " +
                    "training to ${user.fitnessGoal.asText()}."
            )
            appendLine()
            appendLine("Sessions, in the order they are trained:")
            skeleton.days.filter { it.openSlots.isNotEmpty() }.forEach { day ->
                val count = day.openSlots.size
                appendLine("- ${day.id}, ${day.focus.title.lowercase()}, $count ${if (count == 1) "slot" else "slots"}")
            }
            appendLine()
            appendLine("Each slot names the job it does and carries its own list of movements.")
            appendLine("Slots that are already settled are not shown; leave the rest of the week alone.")
        }
    }

    private fun FitnessGoal.asText() = when (this) {
        FitnessGoal.WEIGHT_LOSS -> "lose weight"
        FitnessGoal.MUSCLE_GAIN -> "build muscle"
        FitnessGoal.STRENGTH -> "get stronger"
        FitnessGoal.ENDURANCE -> "build endurance"
        FitnessGoal.GENERAL_FITNESS -> "get generally fitter"
        FitnessGoal.FLEXIBILITY -> "move more freely"
    }

    private fun ExperienceLevel.asText() = name.lowercase()
}
