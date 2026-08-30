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
    data object Failed : GeminiResponse
}

// A single generateContent call against the free-tier Gemini API, constrained
// to JSON by a response schema.
class GeminiClient(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
    private val http: OkHttpClient = defaultHttpClient()
) {

    suspend fun generate(
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
                    if (!response.isSuccessful) return@use GeminiResponse.Failed
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
        const val DEFAULT_MODEL = "gemini-3.6-flash"
        // Low enough for disciplined programming, high enough for varied plans.
        private const val TEMPERATURE = 0.4

        private fun defaultHttpClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }
}
