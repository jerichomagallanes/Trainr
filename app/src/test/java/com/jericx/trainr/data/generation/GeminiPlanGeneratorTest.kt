package com.jericx.trainr.data.generation

import com.google.common.truth.Truth.assertThat
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.model.UserProfile
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class GeminiPlanGeneratorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() = server.shutdown()

    private fun generator(apiKey: String = "test-key") = GeminiPlanGenerator(
        client = GeminiClient(
            apiKey = apiKey,
            baseUrl = server.url("/").toString().trimEnd('/')
        ),
        parser = GeneratedPlanParser(),
        promptBuilder = PlanPromptBuilder()
    )

    private fun request(daysPerWeek: Int = 1) = PlanRequest(
        user = UserProfile(id = 7, firstName = "Jericho", age = 30, workoutDaysPerWeek = daysPerWeek),
        weekNumber = 1,
        startDateMillis = 1_000L,
        languageCode = "en"
    )

    private val validPlanJson = """
        {
          "title": "Week 1",
          "days": [
            {
              "dayNumber": 1,
              "title": "Full Body",
              "equipment": ["Dumbbells"],
              "exercises": [
                {
                  "exerciseKey": "goblet_squat",
                  "name": "Goblet Squats",
                  "measure": "WEIGHT_AND_REPS",
                  "durationMinutes": 8,
                  "prescription": "3 sets of 12 reps",
                  "instructions": "Squat holding a dumbbell at your chest.",
                  "restSeconds": 60,
                  "sets": [
                    { "reps": 12, "weightKg": 20 },
                    { "reps": 12, "weightKg": 20 },
                    { "reps": 12, "weightKg": 20 }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()

    private fun envelope(text: String): String = buildJsonObject {
        put("candidates", buildJsonArray {
            add(buildJsonObject {
                put("content", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", text) })
                    })
                })
            })
        })
    }.toString()

    private fun enqueue(text: String) {
        server.enqueue(MockResponse().setBody(envelope(text)))
    }

    @Test
    fun aValidResponseBecomesAPlan() = runTest {
        enqueue(validPlanJson)

        val plan = generator().generate(request())!!

        assertThat(plan.userId).isEqualTo(7)
        assertThat(plan.startDateMillis).isEqualTo(1_000L)
        assertThat(plan.workoutDays.single().exercises.single().exerciseKey)
            .isEqualTo("goblet_squat")

        val sent = server.takeRequest()
        assertThat(sent.path).isEqualTo("/v1beta/models/gemini-2.5-flash:generateContent")
        assertThat(sent.getHeader("x-goog-api-key")).isEqualTo("test-key")
        val body = sent.body.readUtf8()
        assertThat(body).contains("responseSchema")
        assertThat(body).contains("system_instruction")
    }

    @Test
    fun anInvalidResponseIsRetriedWithTheValidationErrors() = runTest {
        enqueue("""{ "title": " ", "days": [] }""")
        enqueue(validPlanJson)

        val plan = generator().generate(request())

        assertThat(plan).isNotNull()
        assertThat(server.requestCount).isEqualTo(2)
        server.takeRequest()
        val second = server.takeRequest().body.readUtf8()
        assertThat(second).contains("rejected")
        assertThat(second).contains("plan: has no days")
    }

    @Test
    fun theWrongNumberOfDaysIsRejectedAndRetried() = runTest {
        enqueue(validPlanJson)
        enqueue(validPlanJson)
        enqueue(validPlanJson)

        val plan = generator().generate(request(daysPerWeek = 3))

        assertThat(plan).isNull()
        assertThat(server.requestCount).isEqualTo(3)
        server.takeRequest()
        assertThat(server.takeRequest().body.readUtf8())
            .contains("asked for exactly 3")
    }

    @Test
    fun persistentGarbageGivesUpAfterThreeAttempts() = runTest {
        repeat(4) { enqueue("not json at all") }

        assertThat(generator().generate(request())).isNull()
        assertThat(server.requestCount).isEqualTo(3)
    }

    @Test
    fun aMissingKeyMeansNoCallAtAll() = runTest {
        assertThat(generator(apiKey = "").generate(request())).isNull()
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun aServerErrorFailsSoftly() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        assertThat(generator().generate(request())).isNull()
    }
}
