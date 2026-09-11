package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.SkeletonDay
import com.jericx.trainr.domain.generation.SkeletonSlot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface SelectionRepairResult {
    // Repairs counts slots only: a title the app can name itself is no reason
    // to spend another request.
    data class Accepted(val selection: PlanSelection, val repairs: Int) : SelectionRepairResult
    data class Rejected(val problems: List<String>) : SelectionRepairResult
}

// What the schema cannot rule out, repaired from each slot's own ranking. An
// answer goes back only when repairing it would leave more of the week the
// app's choice than the model's. The messages quote the answer, so they are
// for the model and never for the trail.
class PlanSelectionRepair {

    fun repair(json: String, skeleton: PlanSkeleton): SelectionRepairResult {
        val open = skeleton.days.filter { it.openSlots.isNotEmpty() }
        if (open.isEmpty()) return SelectionRepairResult.Accepted(PlanSelection(), 0)
        val answer = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject
            ?: return SelectionRepairResult.Rejected(listOf(NOT_AN_OBJECT))

        val problems = mutableListOf<String>()
        var repairs = 0
        var answered = 0
        val days = linkedMapOf<String, DaySelection>()
        val named = mutableMapOf<String, SkeletonDay>()

        for (day in open) {
            val given = answer[day.id] as? JsonObject
            if (given == null) {
                problems += "You left out ${day.session}. Answer every session in the schema, " +
                    "each with its title and each of its slots."
                repairs += day.openSlots.size
            } else {
                answered++
            }

            val taken = day.slots.filter { it.isDecided }.flatMap { it.candidates }.toMutableSet()
            val slots = linkedMapOf<String, String>()
            for (slot in day.openSlots) {
                val key = given?.text(slot.id)
                if (given != null) {
                    slotProblem(day, slot, key, taken)?.let {
                        problems += it
                        repairs++
                    }
                }
                val chosen = key?.takeIf { it in slot.candidates && it !in taken }
                    ?: slot.candidates.firstOrNull { it !in taken }
                chosen?.let {
                    taken += it
                    slots[slot.id] = it
                }
            }

            var title = day.fallbackTitle
            if (given != null) {
                val written = given.text(TITLE)?.trim().orEmpty()
                val problem = titleProblem(day, written)
                val earlier = named[written.lowercase()]
                when {
                    problem != null -> problems += problem
                    earlier != null -> problems += "${earlier.session} and ${day.session} are both called " +
                        "\"$written\". Each session needs its own name."
                    else -> {
                        title = written
                        named[written.lowercase()] = day
                    }
                }
            }
            days[day.id] = DaySelection(slots, title)
        }

        val openSlots = open.sumOf { it.openSlots.size }
        if (answered == 0 || repairs * 2 > openSlots) return SelectionRepairResult.Rejected(problems)
        return SelectionRepairResult.Accepted(PlanSelection(days), repairs)
    }

    private fun slotProblem(day: SkeletonDay, slot: SkeletonSlot, key: String?, taken: Set<String>): String? = when {
        key == null ->
            "In ${day.session} you left out ${slot.label}. Fill every slot with one movement from that slot's own list."
        key in taken ->
            "In ${day.session}, '$key' is already used earlier in that session. Each slot needs a different movement."
        key !in slot.candidates ->
            "In ${day.session}, ${slot.label} was answered with '$key'. That is not on that slot's list. " +
                "Choose only from the keys listed for the slot you are filling."
        else -> null
    }

    private fun titleProblem(day: SkeletonDay, title: String): String? {
        val words = title.split(WHITESPACE).count { it.isNotEmpty() }
        val filler = FILLER.firstOrNull { title.contains(it, ignoreCase = true) }
        return when {
            title.isEmpty() || title.length > MAX_TITLE_CHARS || words !in 2..4 ->
                "The title for ${day.session} must be two to four words naming the body region and the focus, " +
                    "like \"Upper Body Strength\". \"$title\" is not."
            INDEX_LABEL.containsMatchIn(title) ->
                "The title for ${day.session} is an index label. The app already shows which day and which week " +
                    "it is; name what the session trains."
            filler != null ->
                "The title for ${day.session} uses \"$filler\", which says nothing about this session. " +
                    "Name the region and the focus instead."
            else -> null
        }
    }

    private fun JsonObject.text(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

    private val SkeletonDay.session: String get() = "$id ($fallbackTitle)"

    companion object {
        // Deliberately without the parser's own words: they can quote the
        // answer, which was written from the profile.
        const val NOT_AN_OBJECT = "Your answer was not a JSON object. Reply with only the JSON described by " +
            "the schema, with nothing before or after it."

        private const val TITLE = "title"
        private const val MAX_TITLE_CHARS = 40
        private val WHITESPACE = Regex("\\s+")
        private val INDEX_LABEL = Regex("""(?i)\b(day|week|session|workout|phase)\s*\d|\b[a-z]$""")
        private val FILLER = listOf("good form", "engage your core", "full body workout", "training session")
    }
}
