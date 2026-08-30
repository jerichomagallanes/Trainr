package com.jericx.trainr.data.generation

// What came back from a single call: the model's text, or the reason there
// isn't any. Being unable to reach the model reads differently to the client
// than the model answering with nonsense, so the two are kept apart.
sealed interface GeminiResponse {
    data class Text(val value: String) : GeminiResponse
    data object Unreachable : GeminiResponse

    // This model will not serve us, but another might: its free allowance for
    // the day is spent, it has been retired, or it is overloaded right now.
    // Asking it again is the one thing guaranteed not to help — and on a spent
    // allowance each attempt costs a request we no longer have.
    data object ModelUnavailable : GeminiResponse

    data object Failed : GeminiResponse
}

// One request to one model, answered as JSON. The generator owns the retrying
// and the walking of the model list; this only reports what a single ask did.
interface PlanModelClient {

    suspend fun generate(
        model: String,
        systemInstruction: String,
        userPrompt: String
    ): GeminiResponse

    companion object {
        // Asked in order. The free allowance is counted per model, so a model
        // that has run out for the day says nothing about the next one — these
        // are separate daily buckets, and the plan is worth more than the
        // marginal quality between them. Strongest first; the lite models are
        // the reserve that keeps the app working once it is spent.
        //
        // Every name here was checked against the live API with a
        // schema-constrained request like the real one, because being listed by
        // the API is not the same as being able to do this job. Deliberately
        // absent: the `-latest` aliases, which resolve onto a model already in
        // this list and share its allowance — driving gemini-3.5-flash-lite to
        // its per-minute limit refuses gemini-flash-lite-latest in the same
        // breath, so they add waiting rather than capacity; retired names,
        // which answer "no longer available to new users"; the pro and
        // deep-research models, whose free allowances are far smaller and whose
        // paid rates are far higher; and anything that has been unreachable
        // more than once, since a model that times out reads as the client
        // being offline and stops the whole list.
        val MODELS = listOf(
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3-flash-preview"
        )
    }
}
