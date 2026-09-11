package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.SpentModels

// Carries last week's movements forward when nothing forces a change. Otherwise
// asks the model through Firebase AI Logic, so no key travels inside the app,
// and builds the week itself whenever the model cannot answer.
internal fun planGenerator(
    catalog: ExerciseCatalog,
    spentModels: SpentModels,
    breadcrumbs: Breadcrumbs
): PlanGenerator = CarryForwardPlanGenerator(
    catalog = catalog,
    next = FallbackPlanGenerator(
        coach = GeminiPlanGenerator(
            client = FirebaseAiClient(),
            promptBuilder = PlanPromptBuilder(),
            catalog = catalog,
            spentModels = spentModels,
            breadcrumbs = breadcrumbs
        ),
        template = TemplatePlanGenerator(catalog)
    )
)
