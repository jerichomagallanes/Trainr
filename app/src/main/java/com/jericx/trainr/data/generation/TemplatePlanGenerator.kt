package com.jericx.trainr.data.generation

import com.jericx.trainr.domain.catalog.ExerciseCatalog
import com.jericx.trainr.domain.generation.PlanGenerationResult
import com.jericx.trainr.domain.generation.PlanGenerator
import com.jericx.trainr.domain.generation.PlanRequest
import com.jericx.trainr.domain.generation.PlanSkeletonBuilder

// A whole week with no model at all: the skeleton, the top of every list, and
// the engine's numbers.
class TemplatePlanGenerator(private val catalog: ExerciseCatalog) : PlanGenerator {

    private val builder = PlanSkeletonBuilder(catalog)
    private val assembler = PlanAssembler(catalog)

    override suspend fun generate(request: PlanRequest): PlanGenerationResult {
        if (catalog.all.isEmpty()) return PlanGenerationResult.Failed
        val skeleton = builder.build(request)
        if (!skeleton.isComplete) return PlanGenerationResult.Failed
        return assembler.assemble(skeleton, PlanSelection(), request)
            ?.let { PlanGenerationResult.Generated(it) }
            ?: PlanGenerationResult.Failed
    }
}
