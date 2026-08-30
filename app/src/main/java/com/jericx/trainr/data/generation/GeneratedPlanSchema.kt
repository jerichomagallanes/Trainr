package com.jericx.trainr.data.generation

import com.google.firebase.ai.type.Schema

// docs/generation-contract.md, in the shape the SDK asks for. Everything is
// required unless named in optionalProperties; the parser still enforces the
// rules a schema cannot express (bounds, key vocabulary, day counts).
val GENERATED_PLAN_SCHEMA: Schema = Schema.obj(
    properties = mapOf(
        "title" to Schema.string(),
        "days" to Schema.array(items = daySchema())
    )
)

private fun daySchema(): Schema = Schema.obj(
    properties = mapOf(
        "dayNumber" to Schema.integer(
            description = "Day within the week, 1 = the first day .. 7 = the last"
        ),
        "title" to Schema.string(),
        "equipment" to Schema.array(items = Schema.string()),
        "exercises" to Schema.array(items = exerciseSchema())
    )
)

private fun exerciseSchema(): Schema = Schema.obj(
    properties = mapOf(
        "exerciseKey" to Schema.string(
            description = "Canonical lower_snake_case slug, stable across weeks"
        ),
        "name" to Schema.string(),
        "measure" to Schema.enumeration(
            values = listOf("WEIGHT_AND_REPS", "REPS", "DURATION")
        ),
        "durationMinutes" to Schema.integer(),
        "prescription" to Schema.string(),
        "instructions" to Schema.string(),
        "restSeconds" to Schema.integer(nullable = true),
        "sets" to Schema.array(items = setSchema())
    ),
    optionalProperties = listOf("restSeconds")
)

// A set carries whichever of the three the exercise is measured in, so none of
// them is required and the parser decides what the measure needs.
private fun setSchema(): Schema = Schema.obj(
    properties = mapOf(
        "reps" to Schema.integer(nullable = true),
        "weightKg" to Schema.double(nullable = true),
        "seconds" to Schema.integer(nullable = true)
    ),
    optionalProperties = listOf("reps", "weightKg", "seconds")
)
