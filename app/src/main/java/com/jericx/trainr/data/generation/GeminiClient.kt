package com.jericx.trainr.data.generation

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

// A single generateContent call against the free-tier Gemini API, constrained
// to JSON by a response schema. Returns the model's text or null on any
// failure — a plan the app cannot get is never worth an error screen.
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
    ): String? {
        if (apiKey.isBlank()) return null

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
            runCatching {
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    firstCandidateText(response.body?.string().orEmpty())
                }
            }.getOrNull()
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
