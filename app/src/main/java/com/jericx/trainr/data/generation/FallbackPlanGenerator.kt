package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSource

// The coach first, and the app's own week whenever the coach cannot answer, so
// nobody is left without a plan. The week carries what went wrong, so the
// screen can say so rather than pass it off as the coach's.
class FallbackPlanGenerator(
    private val coach: PlanGenerator,
    private val template: PlanGenerator
) : PlanGenerator {

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        val coached = coach.generate(request)
        if (coached !is PlanGenerationResult.Failure) return coached
        // Nothing the catalog can build either, so the coach's own reason is
        // the true one to report.
        val built = template.generate(request) as? PlanGenerationResult.Generated ?: return coached
        return built.copy(source = PlanSource.TEMPLATE, insteadOf = coached)
    }
}
