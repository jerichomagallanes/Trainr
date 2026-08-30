package com.jericx.trainr.data.generation

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.io.IOException

// Generation goes through Firebase AI Logic rather than straight to the Gemini
// endpoint, so the key never ships inside the app. Every request carries an App
// Check token proving it came from this app on a genuine device; a key lifted
// out of the APK buys nothing without one.
class FirebaseAiClient : PlanModelClient {

    override suspend fun generate(
        model: String,
        systemInstruction: String,
        userPrompt: String
    ): GeminiResponse = try {
        val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = model,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    responseSchema = GENERATED_PLAN_SCHEMA
                    temperature = TEMPERATURE
                },
                systemInstruction = content { text(systemInstruction) }
            )

        generativeModel.generateContent(userPrompt).text
            ?.let(GeminiResponse::Text)
            ?: GeminiResponse.Failed
    } catch (_: QuotaExceededException) {
        // Its allowance for the day is spent; the next model has its own.
        GeminiResponse.ModelUnavailable
    } catch (_: ServerException) {
        // Overloaded or retired: someone else may still answer.
        GeminiResponse.ModelUnavailable
    } catch (_: RequestTimeoutException) {
        GeminiResponse.ModelUnavailable
    } catch (e: Exception) {
        // No route to anything, rather than a quarrel with one model: no other
        // model will do better, so this one stops the list.
        if (e.isNetworkFailure()) GeminiResponse.Unreachable else GeminiResponse.Failed
    }

    private fun Exception.isNetworkFailure(): Boolean {
        var cause: Throwable? = this
        while (cause != null) {
            if (cause is IOException) return true
            cause = cause.cause
        }
        return false
    }

    private companion object {
        // Low enough for disciplined programming, high enough for varied plans.
        const val TEMPERATURE = 0.4f
    }
}
