package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanSkeleton
import com.jericx.trainr.domain.generation.SkeletonDay
import com.jericx.trainr.domain.generation.SkeletonSlot

// Everything the model may answer, as plain data rather than the SDK's type,
// so both platforms render it to the same bytes (docs/fixtures) and the same
// tree can become an on-device grammar later. Every property is required.
sealed interface SelectionSchema {
    data class Obj(val properties: List<Pair<String, SelectionSchema>>) : SelectionSchema
    data class OneOf(val values: List<String>, val description: String) : SelectionSchema
    data class Text(val description: String) : SelectionSchema
}

// A session with nothing left to choose is not in it at all, and each open
// slot is an enum of its own candidates, so a movement the slot does not offer
// cannot be written. Slots come before the title, so a session is named after
// what it holds.
fun planSelectionSchema(skeleton: PlanSkeleton): SelectionSchema.Obj = SelectionSchema.Obj(
    skeleton.days.filter { it.openSlots.isNotEmpty() }.map { it.id to daySchema(it) }
)

private fun daySchema(day: SkeletonDay) = SelectionSchema.Obj(
    day.openSlots.map { it.id to slotSchema(it) } +
        ("title" to SelectionSchema.Text("Two to four words naming what this ${day.focus.title.lowercase()} session trains"))
)

// Unreachable, but an enum with no members is an answer no model can give; a
// string lets the repair explain the problem instead.
private fun slotSchema(slot: SkeletonSlot): SelectionSchema =
    if (slot.candidates.isEmpty()) {
        SelectionSchema.Text(slot.label)
    } else {
        SelectionSchema.OneOf(slot.candidates, slot.label)
    }

fun SelectionSchema.toJson(): String = buildString {
    write(this@toJson, 0)
    append('\n')
}

private fun StringBuilder.write(schema: SelectionSchema, depth: Int) {
    val pad = "  ".repeat(depth + 1)
    append("{\n")
    when (schema) {
        is SelectionSchema.Obj -> {
            append(pad).append("\"type\": \"object\",\n")
            append(pad).append("\"properties\": {")
            schema.properties.forEachIndexed { index, (name, child) ->
                append(if (index == 0) "\n" else ",\n")
                append(pad).append("  ").append(quoted(name)).append(": ")
                write(child, depth + 2)
            }
            append(if (schema.properties.isEmpty()) "},\n" else "\n$pad},\n")
            append(pad).append("\"required\": ").append(schema.properties.joinToString(", ", "[", "]") { quoted(it.first) })
            append('\n')
        }
        is SelectionSchema.OneOf -> {
            append(pad).append("\"type\": \"string\",\n")
            append(pad).append("\"description\": ").append(quoted(schema.description)).append(",\n")
            append(pad).append("\"enum\": ").append(schema.values.joinToString(", ", "[", "]") { quoted(it) })
            append('\n')
        }
        is SelectionSchema.Text -> {
            append(pad).append("\"type\": \"string\",\n")
            append(pad).append("\"description\": ").append(quoted(schema.description)).append('\n')
        }
    }
    append("  ".repeat(depth)).append('}')
}

private fun quoted(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
