package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.diagnostics.Breadcrumbs
import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.SpentModels

// Dev builds get the week the app builds with no model at all, which is the
// same assembly prod falls back on, so a dev build exercises the code that
// ships. The class that can reach the network lives in the prod source set,
// so no dev build can spend the day's allowance.
internal fun planGenerator(
    catalog: ExerciseCatalog,
    spentModels: SpentModels,
    breadcrumbs: Breadcrumbs
): PlanGenerator = TemplatePlanGenerator(catalog)
