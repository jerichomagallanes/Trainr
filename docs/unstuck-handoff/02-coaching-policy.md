# 02 · Coaching policy and factual boundaries

**This is a product/engineering policy, not a validated clinical protocol.** Research supports broad principles; exact individual prescriptions require review and tests. `docs/generation-contract.md` and the current deterministic generation tests remain the baseline for existing plan generation.

## Hard invariants

| ID | Required rule |
|---|---|
| C01 | Every exercise is a real, eligible canonical catalog entry; no model-invented movements. |
| C02 | Every set, rep, load, duration, rest, and progression value comes from app rules. |
| C03 | Apply cannot alter actual logged values or mark unperformed work complete. |
| C04 | A constraint is not evidence of physiological failure: lack of time/equipment must not trigger a fatigue deload. |
| C05 | Only validated unperformed work is eligible for modification; a performed set remains attached to its original exercise. |
| C06 | Never automatically make up missed sets on another day or claim equal results from reduced work. |
| C07 | Equipment-only changes do not also reduce volume without a separate declared constraint and preview. |
| C08 | Loads and history are exercise-specific; a substitution does not inherit kilograms as equivalent. |
| C09 | Pain/unclear discomfort bypasses automated prescription and paid gating. No model can clear a person to exercise. |
| C10 | A preference is durable only after explicit confirmation; a note alone is not consent. |
| C11 | No recommendation without adequate data; “keep current”, clarification, or manual fallback is a valid result. |
| C12 | Report uncertainty honestly; no invented confidence, causal improvement, or recovery score. |

## Decision pipeline

1. Snapshot the current workout, actual logged work, broad goal, confirmed specific priority if any, equipment, and relevant history.
2. Run the direct reason/safety routing layer. Pain may be selected directly at any point. Do not depend solely on a model or keywords to detect every unsafe scenario.
3. Resolve structured constraints. Duration must state **whole-session** versus **remaining time**. The prototype asks whole-session time. If already training, ask remaining time instead or obtain reliable elapsed time; do not subtract invented duration from planned minutes.
4. Determine eligibility under reviewed rules. Unknown equipment, injury constraints, unsupported goal, or contradictory facts require clarification or fallback.
5. Produce the smallest feasible change. Prefer no change when the existing plan fits. Preserve the highest goal-relevant work, not a universal compound-over-isolation hierarchy.
6. Estimate with warm-up, set execution, setup/transitions, and rest. Show an estimate/range only when the estimator is validated; never a guarantee. Do not shrink rest just to make the displayed total fit.
7. Validate catalog, injury guard, equipment, session/week exposure, progression constraints, already-logged work, and selected scope.
8. Build a transparent before/after proposal and explicit tradeoff. A deterministic template is sufficient.
9. Revalidate against the latest plan/log revision when applying. If anything material changed, require a new preview.

## Time adjustment

The preferred order is: retain reviewed priority work and required preparation; remove or reduce lower-priority unperformed work where allowed; if an acceptable session cannot fit, explain that and offer saving/finishing rather than manufacturing a “perfect” short workout. User-selected time is a constraint, not a performance target to meet at all costs.

The 25/35/45-minute choices in the prototype are sample choices, not the only supported real durations. Native v1 can keep those presets with an accessible custom whole-minute input if product scope permits; validate input and label it consistently. Exact allowed range remains a reviewed configuration. Do not silently clamp an invalid duration into a different request.

Prototype examples: 35 minutes reduces two arm exercises from three to two sets; 25 minutes keeps two exercises with three sets each. **These are illustrative authored examples, not approved rules or proven timing.** Do not implement `if minutes == 25 then keep two exercises`.

## Equipment adjustment

Select the affected exercise explicitly if entering from the session chooser. Entering from **Need an alternative?** already supplies it. Ask what is available using catalog-backed equipment choices; the current profile's broad categories do not establish a specific machine, bench, or weight inventory.

Candidate ranking must consider goal relevance, movement/muscle intent, technique demands, injury constraints, and available equipment. A cable lateral raise and dumbbell lateral raise may both target shoulders but differ in resistance profile and loading. Dumbbell pressing does not preserve barbell-specific practice exactly. Explain that tradeoff. Candidate compatibility metadata must be curated and reviewed; an LLM similarity judgment is insufficient.

Keep validated set/rep/rest intent when appropriate. Seed a new movement with its own history or a conservative, reviewed calibration flow. Never copy the original movement's load blindly. If partially performed, preserve the original exercise and completed sets; replacement applies only to eligible remaining work, with an explicit preview.

## Guidance and pain

When the problem is “I don't know how”, show the existing exercise-specific demonstration and instructions before proposing a different exercise. Do not replace approved technique content with generic model prose. A tutorial does not inspect or verify the user's actual form.

Pain path: pause the affected exercise, explain the limitation, and allow saving what was done. The shipped copy must be reviewed by a qualified clinician, including what to show for urgent symptoms and region-appropriate help. The prototype's short “seek guidance” copy is incomplete for release. Do not diagnose, prescribe rehab, assert an alternative is safe, or gate the path behind Pro/model download. Uncertain discomfort must not silently become ordinary fatigue. If danger routing is triggered in a note, preserve the note privately and present the reviewed route; do not generate alarming diagnoses.

## Fatigue, missed sessions, and plateau claims

V1 structured adjustment should not expand into a general fatigue/plateau system just because free text mentions being tired. Preserve the existing progression/deload rules and ask a bounded clarification if supported; otherwise offer existing manual choices. Ordinary exertion, soreness, pain, insufficient sleep, illness, and equipment trouble are different contexts. Do not collapse them into one model-inferred readiness score.

Missed work is not a debt. Weekly regeneration/repeat is an existing separate action with its own review and entitlement. Unstuck must not invoke it to implement a local edit.

## Progress and follow-up

Ask about the practical problem first: “Did it fit your time?” or “Did the alternative work with your equipment?” Allow no response. Link the answer to its adjustment ID; display once unless the user revisits it.

Assess performance only from comparable exposures: same canonical exercise, units, measure, relevant technique/range assumptions, and reliable actual entries. Exclude or label inferred legacy actuals. A new substitute starts separate history. Do not use different machines or dumbbells as interchangeable load measurements.

Strength trends and training performance are useful signals, but performance is not a direct measurement of hypertrophy. Fat loss, general health, endurance, and rehabilitation need different outcome models; do not claim this resistance-training adjustment proves those outcomes. The empty state should say more comparable sessions are needed without inventing a universal number. A later numerical threshold must be specified, reviewed, and tested in one shared policy configuration.

## Professional review deliverables

Before enabling real recommendations, obtain a versioned review of eligible populations/goals, substitution mappings, volume/load/rest constraints, time estimation, calibration, partial-session behavior, contraindications, escalation copy, and example cases. Record reviewer role, date, version, changes, and limits. “Uses evidence-based principles” is not proof that every generated session is effective or clinically safe.
