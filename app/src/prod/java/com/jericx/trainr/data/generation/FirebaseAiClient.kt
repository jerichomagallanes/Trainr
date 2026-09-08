package com.jericx.trainr.data.generation

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

// Goes through Firebase AI Logic rather than the Gemini endpoint, so no key ships
// in the app. Every request carries an App Check token attesting its origin.
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

        // Capped because the SDK's own timeout is far longer, and a chain of
        // five models can make a client wait through five of them.
        withTimeout(CALL_TIMEOUT_MILLIS) {
            generativeModel.generateContent(userPrompt).text
                ?.let(GeminiResponse::Text)
                ?: GeminiResponse.Failed
        }
    } catch (_: QuotaExceededException) {
        // Spent for the day until the quota resets; the next model has its own.
        GeminiResponse.QuotaSpent
    } catch (_: ServerException) {
        // Overloaded or retired: someone else may still answer.
        GeminiResponse.ModelUnavailable
    } catch (_: RequestTimeoutException) {
        GeminiResponse.ModelUnavailable
    } catch (_: TimeoutCancellationException) {
        // Too slow now, but no reason to think tomorrow, so it is not remembered.
        GeminiResponse.ModelUnavailable
    } catch (e: Exception) {
        // No route to anything, so no other model will do better: stop the list.
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
        // A whole week normally lands in twenty to thirty seconds.
        const val CALL_TIMEOUT_MILLIS = 45_000L

        const val TEMPERATURE = 0.4f
    }
}
