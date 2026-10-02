package com.jericx.trainr.domain.unstuck.intent

enum class UnstuckRoute { TIME, EQUIPMENT, GUIDE, PAIN, CHOOSER }

enum class DirectReason { LESS_TIME, EQUIPMENT, GUIDANCE, PAIN, OTHER }

object IntentRouting {

    // A model returning none_stated is not a safety clearance, so a direct
    // pain choice is answered before anything the extraction says.
    fun routeFor(directReason: DirectReason?, validation: IntentValidation?): UnstuckRoute {
        if (directReason == DirectReason.PAIN) return UnstuckRoute.PAIN

        val valid = validation as? IntentValidation.Valid
        if (valid?.actionable?.painConcern == true) return UnstuckRoute.PAIN

        val chosen = when (directReason) {
            DirectReason.LESS_TIME -> UnstuckRoute.TIME
            DirectReason.EQUIPMENT -> UnstuckRoute.EQUIPMENT
            DirectReason.GUIDANCE -> UnstuckRoute.GUIDE
            else -> null
        }
        if (chosen != null) return chosen

        if (valid == null) return UnstuckRoute.CHOOSER
        return when (valid.extraction.clarification) {
            Clarification.PRIMARY_CONSTRAINT, Clarification.MEANING -> UnstuckRoute.CHOOSER
            else -> when (valid.extraction.intent) {
                IntentKind.LESS_TIME -> UnstuckRoute.TIME
                IntentKind.EQUIPMENT_UNAVAILABLE -> UnstuckRoute.EQUIPMENT
                IntentKind.EXERCISE_GUIDANCE -> UnstuckRoute.GUIDE
                IntentKind.PAIN_CONCERN -> UnstuckRoute.PAIN
                IntentKind.OTHER_OR_UNCLEAR -> UnstuckRoute.CHOOSER
            }
        }
    }
}
