package com.jericx.trainr.domain.unstuck.intent

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntentRoutingTest {

    private val painNote = "I have 20 minutes and my knee hurts."
    private val timeNote = "I have 35 minutes."

    private val timeOnly = validated(
        extraction(
            intent = IntentKind.LESS_TIME,
            timeBudget = TimeBudgetMention(minutes = 35, scope = MentionScope.WHOLE_SESSION),
            evidence = listOf(Evidence(EvidenceField.TIME_BUDGET, "35 minutes", start = 7, end = 17))
        ),
        timeNote
    )

    @Test
    fun aStatedDiscomfortRoutesToPainEvenWhenTheIntentIsLessTime() {
        val discomfort = validated(
            extraction(
                intent = IntentKind.LESS_TIME,
                timeBudget = TimeBudgetMention(minutes = 20, scope = MentionScope.WHOLE_SESSION),
                concern = Concern.PAIN_OR_UNCLEAR_DISCOMFORT,
                evidence = listOf(
                    Evidence(EvidenceField.TIME_BUDGET, "20 minutes", start = 7, end = 17),
                    Evidence(EvidenceField.CONCERN, "my knee hurts", start = 22, end = 35)
                )
            ),
            painNote
        )

        assertThat(IntentRouting.routeFor(null, discomfort)).isEqualTo(UnstuckRoute.PAIN)
        assertThat(IntentRouting.routeFor(DirectReason.LESS_TIME, discomfort)).isEqualTo(UnstuckRoute.PAIN)
    }

    @Test
    fun aDirectPainChoiceIsNotClearedByTheModel() {
        assertThat(timeOnly.actionable.painConcern).isFalse()

        assertThat(IntentRouting.routeFor(DirectReason.PAIN, timeOnly)).isEqualTo(UnstuckRoute.PAIN)
    }

    @Test
    fun aDirectReasonOutranksTheModelsIntent() {
        assertThat(IntentRouting.routeFor(DirectReason.EQUIPMENT, timeOnly)).isEqualTo(UnstuckRoute.EQUIPMENT)
        assertThat(IntentRouting.routeFor(DirectReason.GUIDANCE, timeOnly)).isEqualTo(UnstuckRoute.GUIDE)
        assertThat(IntentRouting.routeFor(null, timeOnly)).isEqualTo(UnstuckRoute.TIME)
    }

    @Test
    fun aClarificationTheChooserAnswersBeatsANamedIntent() {
        val unsure = validated(
            extraction(intent = IntentKind.LESS_TIME, clarification = Clarification.PRIMARY_CONSTRAINT),
            timeNote
        )

        assertThat(IntentRouting.routeFor(DirectReason.OTHER, unsure)).isEqualTo(UnstuckRoute.CHOOSER)
    }

    @Test
    fun aRejectedInterpretationFallsBackToTheChooser() {
        val rejected = IntentValidator.validate("{\"schemaVersion\":", timeNote)

        assertThat(rejection(rejected)).isNotEmpty()
        assertThat(IntentRouting.routeFor(null, rejected)).isEqualTo(UnstuckRoute.CHOOSER)
        assertThat(IntentRouting.routeFor(DirectReason.OTHER, null)).isEqualTo(UnstuckRoute.CHOOSER)
        assertThat(IntentRouting.routeFor(null, null)).isEqualTo(UnstuckRoute.CHOOSER)
    }
}
