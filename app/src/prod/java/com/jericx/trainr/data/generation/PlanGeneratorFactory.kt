package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.generation.SpentModels
import com.jericx.trainr.presentation.workout.model.ExerciseVideoCatalog

// The shipped build asks the model, through Firebase AI Logic so no key travels
// inside the app.
internal fun planGenerator(
    spentModels: SpentModels,
    breadcrumbs: Breadcrumbs
): PlanGenerator = GeminiPlanGenerator(
    client = FirebaseAiClient(),
    parser = GeneratedPlanParser(),
    promptBuilder = PlanPromptBuilder(canonicalKeys = ExerciseVideoCatalog.videoIds.keys),
    spentModels = spentModels,
    breadcrumbs = breadcrumbs
)
