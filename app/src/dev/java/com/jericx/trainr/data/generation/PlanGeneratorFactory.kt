package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerator

// Development answers from a canned week. Chosen in the source set rather than
// behind a flag, and FirebaseAiClient lives in the prod source set beside its
// own factory, so the class that can reach the network is not compiled into a
// dev build at all. No run of the app, and no mistake in wiring, can spend the
// day's allowance.
internal fun planGenerator(): PlanGenerator = CannedPlanGenerator()
