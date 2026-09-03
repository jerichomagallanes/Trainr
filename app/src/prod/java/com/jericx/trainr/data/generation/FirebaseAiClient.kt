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

        // Capped, because a model that has not answered in this long is not
        // about to. Without it the SDK waits its own much longer timeout, and
        // with five models in the chain a client can sit through five of those
        // in a row before anything is asked that will actually answer.
        withTimeout(CALL_TIMEOUT_MILLIS) {
            generativeModel.generateContent(userPrompt).text
                ?.let(GeminiResponse::Text)
                ?: GeminiResponse.Failed
        }
    } catch (_: QuotaExceededException) {
        // Its allowance for the day is spent; the next model has its own, and
        // this one will keep saying so until the quota resets.
        GeminiResponse.QuotaSpent
    } catch (_: ServerException) {
        // Overloaded or retired: someone else may still answer.
        GeminiResponse.ModelUnavailable
    } catch (_: RequestTimeoutException) {
        GeminiResponse.ModelUnavailable
    } catch (_: TimeoutCancellationException) {
        // Ours rather than the SDK's, and read the same way: too slow now, but
        // no reason to think it will be tomorrow, so it is not remembered.
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
        // A whole week normally lands in twenty to thirty seconds.
        const val CALL_TIMEOUT_MILLIS = 45_000L

        const val TEMPERATURE = 0.4f
    }
}
