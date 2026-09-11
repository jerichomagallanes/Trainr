package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanSkeleton

sealed interface GeminiResponse {
    data class Text(val value: String) : GeminiResponse
    data object Unreachable : GeminiResponse

    // Durable until the allowance resets, so it is worth remembering.
    data object QuotaSpent : GeminiResponse

    // Transient, and deliberately not remembered.
    data object ModelUnavailable : GeminiResponse

    data object Failed : GeminiResponse
}

// One request to one model. The generator owns the retries and the model list.
interface PlanModelClient {

    // The skeleton becomes the response schema, each open slot an enum of its
    // own candidates, so an answer naming a movement the slot does not offer
    // is not representable. The domain type rather than the SDK's, so an
    // on-device model can build its own grammar from the same thing.
    suspend fun generate(
        model: String,
        systemInstruction: String,
        userPrompt: String,
        skeleton: PlanSkeleton
    ): GeminiResponse

    companion object {
        // Asked in order, strongest first; the free allowance is per model, so
        // each is its own daily bucket and the lite models are the reserve.
        // Deliberately absent: the `-latest` aliases, which resolve onto a
        // model already listed and share its allowance; retired names; and the
        // pro models, whose free allowances are far smaller.
        val MODELS = listOf(
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3-flash-preview"
        )
    }
}
