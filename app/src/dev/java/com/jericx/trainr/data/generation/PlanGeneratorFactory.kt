package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.SpentModels

// Development answers from a canned week. Chosen in the source set rather than
// behind a flag, and FirebaseAiClient lives in the prod source set beside its
// own factory, so the class that can reach the network is not compiled into a
// dev build at all. No run of the app, and no mistake in wiring, can spend the
// day's allowance.
// The store is accepted and ignored: a canned week asks no model, so it has no
// allowance to spend and nothing to remember.
internal fun planGenerator(spentModels: SpentModels): PlanGenerator = CannedPlanGenerator()
