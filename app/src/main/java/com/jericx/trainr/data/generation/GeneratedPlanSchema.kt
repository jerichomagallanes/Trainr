package com.jericx.trainr.data.generation

import com.google.firebase.ai.type.Schema

// docs/generation-contract.md in the shape the SDK asks for. Built per request
// rather than once, because the movement vocabulary is the client's own: an
// enum of the keys they can actually perform is what makes an unusable plan
// unrepresentable rather than merely rejected afterwards.
fun generatedPlanSchema(exerciseKeys: List<String>): Schema = Schema.obj(
    properties = mapOf(
        "title" to Schema.string(),
        "days" to Schema.array(items = daySchema(exerciseKeys))
    )
)

private fun daySchema(exerciseKeys: List<String>): Schema = Schema.obj(
    properties = mapOf(
        "dayNumber" to Schema.integer(
            description = "Day within the week, 1 = the first day .. 7 = the last"
        ),
        "title" to Schema.string(),
        "exercises" to Schema.array(items = exerciseSchema(exerciseKeys))
    )
)

private fun exerciseSchema(exerciseKeys: List<String>): Schema = Schema.obj(
    properties = mapOf(
        "exerciseKey" to exerciseKeySchema(exerciseKeys),
        "prescription" to Schema.string(),
        "instructions" to Schema.string(),
        "restSeconds" to Schema.integer(nullable = true),
        "sets" to Schema.array(items = setSchema())
    ),
    optionalProperties = listOf("restSeconds")
)

// An empty vocabulary would make an enum with no members, which no answer can
// satisfy; a plain string lets the parser explain the problem instead.
private fun exerciseKeySchema(exerciseKeys: List<String>): Schema =
    if (exerciseKeys.isEmpty()) {
        Schema.string(description = "Movement key from the list in the request")
    } else {
        Schema.enumeration(values = exerciseKeys)
    }

// A set carries whichever of the three its movement is measured in, so none of
// them is required and the catalog decides what the measure needs.
private fun setSchema(): Schema = Schema.obj(
    properties = mapOf(
        "reps" to Schema.integer(nullable = true),
        "weightKg" to Schema.double(nullable = true),
        "seconds" to Schema.integer(nullable = true)
    ),
    optionalProperties = listOf("reps", "weightKg", "seconds")
)
