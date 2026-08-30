package com.jericx.trainr.data.generation

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// docs/generation-contract.md in the dialect Gemini's responseSchema speaks
// (an OpenAPI subset — no additionalProperties, patterns or bounds; the
// parser enforces those).
val GENERATED_PLAN_SCHEMA: JsonObject = buildJsonObject {
    put("type", "OBJECT")
    put("required", buildJsonArray { add("title"); add("days") })
    put("properties", buildJsonObject {
        put("title", buildJsonObject { put("type", "STRING") })
        put("days", buildJsonObject {
            put("type", "ARRAY")
            put("items", daySchema())
        })
    })
    put("propertyOrdering", buildJsonArray { add("title"); add("days") })
}

private fun daySchema(): JsonObject = buildJsonObject {
    put("type", "OBJECT")
    put("required", buildJsonArray {
        add("dayNumber"); add("title"); add("equipment"); add("exercises")
    })
    put("properties", buildJsonObject {
        put("dayNumber", buildJsonObject {
            put("type", "INTEGER")
            put("description", "Day within the week, 1 = the first day .. 7 = the last")
        })
        put("title", buildJsonObject { put("type", "STRING") })
        put("equipment", buildJsonObject {
            put("type", "ARRAY")
            put("items", buildJsonObject { put("type", "STRING") })
        })
        put("exercises", buildJsonObject {
            put("type", "ARRAY")
            put("items", exerciseSchema())
        })
    })
    put("propertyOrdering", buildJsonArray {
        add("dayNumber"); add("title"); add("equipment"); add("exercises")
    })
}

private fun exerciseSchema(): JsonObject = buildJsonObject {
    put("type", "OBJECT")
    put("required", buildJsonArray {
        add("exerciseKey"); add("name"); add("measure"); add("durationMinutes")
        add("prescription"); add("instructions"); add("sets")
    })
    put("properties", buildJsonObject {
        put("exerciseKey", buildJsonObject {
            put("type", "STRING")
            put("description", "Canonical lower_snake_case slug, stable across weeks")
        })
        put("name", buildJsonObject { put("type", "STRING") })
        put("measure", buildJsonObject {
            put("type", "STRING")
            put("enum", buildJsonArray {
                add("WEIGHT_AND_REPS"); add("REPS"); add("DURATION")
            })
        })
        put("durationMinutes", buildJsonObject { put("type", "INTEGER") })
        put("prescription", buildJsonObject { put("type", "STRING") })
        put("instructions", buildJsonObject { put("type", "STRING") })
        put("restSeconds", buildJsonObject {
            put("type", "INTEGER")
            put("nullable", true)
        })
        put("sets", buildJsonObject {
            put("type", "ARRAY")
            put("items", setSchema())
        })
    })
    put("propertyOrdering", buildJsonArray {
        add("exerciseKey"); add("name"); add("measure"); add("durationMinutes")
        add("prescription"); add("instructions"); add("restSeconds"); add("sets")
    })
}

private fun setSchema(): JsonObject = buildJsonObject {
    put("type", "OBJECT")
    put("properties", buildJsonObject {
        put("reps", buildJsonObject { put("type", "INTEGER"); put("nullable", true) })
        put("weightKg", buildJsonObject { put("type", "NUMBER"); put("nullable", true) })
        put("seconds", buildJsonObject { put("type", "INTEGER"); put("nullable", true) })
    })
}
