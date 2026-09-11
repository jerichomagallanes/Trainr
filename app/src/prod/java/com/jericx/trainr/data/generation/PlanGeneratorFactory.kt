package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.SpentModels

// Asks the model through Firebase AI Logic, so no key travels inside the app.
internal fun planGenerator(
    catalog: ExerciseCatalog,
    spentModels: SpentModels,
    breadcrumbs: Breadcrumbs
): PlanGenerator = GeminiPlanGenerator(
    client = FirebaseAiClient(),
    promptBuilder = PlanPromptBuilder(),
    catalog = catalog,
    spentModels = spentModels,
    breadcrumbs = breadcrumbs
)
