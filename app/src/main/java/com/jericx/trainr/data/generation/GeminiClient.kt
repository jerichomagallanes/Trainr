package com.jericx.trainr.data.generation

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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

// A single generateContent call against the free-tier Gemini API, constrained
// to JSON by a response schema.
class GeminiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
    private val http: OkHttpClient = defaultHttpClient()
) {

    suspend fun generate(
        model: String,
        systemInstruction: String,
        userPrompt: String,
        responseSchema: JsonObject
    ): GeminiResponse {
        if (apiKey.isBlank()) return GeminiResponse.Failed

        val body = buildJsonObject {
            put("system_instruction", buildJsonObject {
                put("parts", buildJsonArray {
                    add(buildJsonObject { put("text", systemInstruction) })
                })
            })
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", userPrompt) })
                    })
                })
            })
            put("generationConfig", buildJsonObject {
                put("responseMimeType", "application/json")
                put("responseSchema", responseSchema)
                put("temperature", TEMPERATURE)
            })
        }

        val request = Request.Builder()
            .url("$baseUrl/v1beta/models/$model:generateContent")
            .header("x-goog-api-key", apiKey)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            try {
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@use if (response.code in MODEL_IS_OUT) {
                            GeminiResponse.ModelUnavailable
                        } else {
                            GeminiResponse.Failed
                        }
                    }
                    firstCandidateText(response.body?.string().orEmpty())
                        ?.let(GeminiResponse::Text)
                        ?: GeminiResponse.Failed
                }
            } catch (_: IOException) {
                // No route to the model: no network, a dropped connection, or a
                // request that ran out of time trying.
                GeminiResponse.Unreachable
            }
        }
    }

    private fun firstCandidateText(payload: String): String? = runCatching {
        Json.parseToJsonElement(payload).jsonObject["candidates"]!!
            .jsonArray.first().jsonObject["content"]!!
            .jsonObject["parts"]!!.jsonArray.first()
            .jsonObject["text"]!!.jsonPrimitive.content
    }.getOrNull()

    companion object {
        // Asked in order. The free allowance is counted per model, so a model
        // that has run out for the day says nothing about the next one — these
        // are separate daily buckets, and the plan is worth more than the
        // marginal quality between them. Newest and strongest first; the lite
        // models are the reserve that keeps the app working once it is spent.
        val MODELS = listOf(
            "gemini-3.6-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite"
        )

        // 429 spent, 404 retired, 503 overloaded: reasons to ask someone else.
        private val MODEL_IS_OUT = setOf(429, 404, 503)

        // Low enough for disciplined programming, high enough for varied plans.
        private const val TEMPERATURE = 0.4

        private fun defaultHttpClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
