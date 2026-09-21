# 01 · Product definition and decision register

## User problem and promise

A planned workout can fail because the person has less time, cannot access equipment, does not understand an exercise, or needs to stop. A static plan leaves them choosing between abandoning the day and guessing. Trainr should offer the smallest understandable adjustment that respects their current goal and the work already performed.

Proposed customer-facing promise: **“Make today's workout fit your situation, with your goal in mind.”** Do not promise guaranteed results, equivalent gains from less training, injury prevention, diagnosis, or a feature no competitor has. Day-level adaptation exists in other products. Trainr's intended differentiation is the combination of private context, transparent tradeoffs, continuity with actual logs, editable memory, and restrained follow-up.

## Core experience

1. Open the current workout and choose **Adjust today**.
2. Pick less time, equipment unavailable, exercise guidance, pain, or another reason.
3. For common needs, use direct controls. For other reasons, optionally type a short note that is interpreted locally.
4. Ask only the next necessary clarification. Preserve every earlier answer.
5. Show the proposed change, goal priority, tradeoff, today-only scope, and expandable exact differences.
6. **Use this workout** commits the validated proposal. **Keep original** leaves the plan unchanged.
7. Keep normal logging, timers, and tutorials. Offer safe undo without deleting performed work.
8. Save actual work even when the person finishes early. Feedback is optional and comes after saving.
9. Ask once whether the relevant adjustment helped. Distinguish practical fit from long-term results.
10. Remember an explicitly confirmed preference only when the user opts in; future changes still require preview/apply.

Example: a person prioritizing bench strength has less time. The app may preserve bench practice and reduce lower-priority work if reviewed rules allow it. A person prioritizing shoulder growth may instead retain lateral raises. The specific prototype set counts do not constitute the policy.

## Initial scope

Include current-day time adjustments, equipment substitution within the catalog, existing exercise guidance, partial completion, optional local notes, one adjustment-specific follow-up, and explicit recurring time preferences. Include manual structured paths even when no model is installed. Initial model language support and eligible device cohorts are determined by evaluation, not vendor language lists.

Initial automated coaching eligibility should be scoped to cohorts the rules have been reviewed for, starting with healthy adults doing beginner/intermediate strength or general resistance training. Existing Trainr goals remain available; do not imply every goal, age, disability, pregnancy/postpartum situation, injury, or clinical condition is covered by one rule set. Users outside validated cohorts can still log, finish, use catalog guidance, and manage their own data. A coach/clinician must review the support policy before release. Do not infer clinical eligibility from a model response.

## Explicit non-goals

- An open-ended personal-trainer chat or medical assistant.
- A model generating whole weeks, exercise catalog entries, weights, reps, set counts, rest times, or calorie prescriptions.
- Automatic compensation for missed work, permanent plan rewrites, or changing future sessions without review.
- Camera form scoring, voice input, nutrition coaching, social features, wearables, or cloud inference in v1.
- A universal medical screening or diagnostic system.
- A promise that private notes make the entire app network-free; existing ads, crash reporting, purchases, and video services have separate data flows.

## Entitlement direction

Use the existing Trainr Pro entitlement and paywall infrastructure. Proposed allowance: **one complete adjustment cycle free**, including its later follow-up. Consume the allowance only when a useful changed proposal is successfully applied. Merely opening a screen, downloading, parsing, generating, failing validation, cancelling, keeping the original, or accepting “no change needed” must not consume it.

A cycle is identified by an application-issued adjustment/cycle ID. Retrying a transaction is idempotent. Undo and reapply within that same saved cycle do not consume a second allowance. A genuinely new paid recommendation after that cycle follows the gate. The exact anti-abuse/reset policy is a business decision; do not tie it silently to the existing free-week allowance, reset it on every process launch, or claim cross-device enforcement without an account design.

Keep these free: existing logging/manual functions, finish/save, pain guidance, catalog tutorials, reading historical adjustments, feedback for the included first cycle, viewing/editing/forgetting preferences, and deleting notes. Pro expiration cannot trap a person behind a paywall when saving an ongoing workout. An applied workout remains usable.

Check compatible hardware, runtime availability, and model readiness before offering Pro specifically for local AI. If another Pro feature is offered on an unsupported device, its copy must explain that Unstuck's local AI is unavailable there. Load localized real prices, trial eligibility, restore, and cancellation information from existing purchase infrastructure. Prototype purchase buttons are not production behavior.

Existing entitlement: `trainr_workout_planner_pro`. Do not create a new SKU or change prices from this document. `docs/store-products.md` is the repository reference for existing product configuration.

## Success measures

Primary product question: can a user make a useful, understandable adjustment and continue training without losing logs or trust?

Measure with consented, content-free events where appropriate: opened chooser, selected reason enum, preview shown, kept original, successful apply, undo, finished/finished-early, optional feedback answer enum, model failure reason, and duration buckets. Never include raw notes, prompt payloads, symptoms, weights, model text, or preference text in analytics/crash breadcrumbs. Local usability studies can examine comprehension separately with explicit participant consent.

Track practical fit and saved sessions separately from performance trends and conversion. Conversion alone must not optimize the app toward excessive changes. Do not label session completion as muscle gain or attribute a progress change causally to Unstuck from one session.

## Decision register

| ID | Status | Decision / consequence |
|---|---|---|
| D01 | Accepted direction | Embedded **Adjust today**, no main chatbot tab. |
| D02 | Hard invariant | Deterministic training engine owns prescriptions; model has no write privileges. |
| D03 | Hard invariant | Preview and explicit apply; today-only by default; preserve logs. |
| D04 | Accepted direction | Optional post-save context, explicit editable preferences, practical follow-up. |
| D05 | Hard invariant | Pain route is free, separate, no model-approved safe replacement. |
| D06 | Proposed commercial policy | One free full adjustment cycle, then existing Pro; verify allowance details before release. |
| D07 | Provisional technical choice | Evaluate LFM2.5-1.2B-Instruct Q4_K_M first; no model selected for shipping. |
| D08 | Required new capability | Optional specific goal priority; current profile only has a broad goal. |
| D09 | Hard invariant | Data-preserving Android and iOS migrations, no destructive fallback for this rollout. |
| D10 | Design reference | Frozen interactive prototype is the visual target, with listed native adaptations. |
| D11 | Review gate | Exact training thresholds, contraindication policy, urgent-symptom copy require professional review. |
| D12 | Proposed engineering defaults | Contracts, table names, timing targets in this package are implementation proposals, not existing APIs. |

## Open decisions and safe defaults

If unanswered, implement fixtures/interfaces without enabling unreviewed automation. Resolve: eligible populations; exercise substitution compatibility metadata; timing estimator and minimum viable session rules; supported languages/devices; exact model/runtime/artifact license and checksum; whether recurring preference attaches to weekday or training slot after rescheduling; retention/backup/export policy; allowance restoration; telemetry consent. Do not block visual scaffolding or logging fixes on these decisions. Do block public automated coaching until the corresponding release gates pass.
