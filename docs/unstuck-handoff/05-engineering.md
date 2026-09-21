# 05 · Engineering, data model, and integration

Everything in “proposed” sections is new design, not an API that already exists. Recheck current code at the baseline or newer commit. Keep platform domain behavior equivalent with shared JSON fixtures; source code need not be shared.

## Verified current architecture

**Android root:** `app/src/main/java/com/jericx/trainr/`.

| Area | Existing source / consequence |
|---|---|
| Navigation | `presentation/MainActivity.kt`, `presentation/Screen.kt`; add feature routes deliberately |
| Models | `domain/model/WorkoutRoutine.kt`; weekly plan → days → exercises → sets |
| Profile | `domain/model/UserProfile.kt`; broad fitness goal, experience, equipment, duration, injuries; no confirmed bench/shoulder priority field |
| Generation | `domain/generation/PlanSkeletonBuilder.kt`, `SessionBudget.kt`, `ProgressionEngine.kt` and neighboring generation components; deterministic existing pipeline |
| Purchase gate | `domain/purchases/ProGate.kt`, `FreeGenerationAllowance.kt`; `presentation/purchases/PaywallReason.kt` currently NEXT_WEEK, REWRITE, FRESH_PLAN |
| Database | `data/local/TrainrDatabase.kt` version 4, schema export disabled at inspection; `data/local/UserEntity.kt` contains relational entities |
| Migration risk | `di/AppModule.kt` uses `fallbackToDestructiveMigration(dropAllTables = true)`; do not ship a feature migration that deletes history |
| Persistence | User DAO/repository/repository implementation hold current records; add targeted transaction APIs rather than rewriting a week |
| Routine | `RoutineDetailViewModel.completeRoutine()` fills remaining work; existing completion transitions in RoutineDetailScreen need separation for finish-early |
| Weekly plan | WeeklyPlanScreen includes progress and day cards; integrate contextual card without permanent coach banner |
| Next week | NextWeekViewModel repeat/regenerate/generate paths are not current-day adjustment APIs |

**iOS root:** `Trainr/` inside sibling `Trainr-iOS`.

| Area | Existing source / consequence |
|---|---|
| Navigation | `App/Route.swift`, `App/RootView.swift`, `App/TrainrApp.swift` |
| Models | `Models/WorkoutPlan.swift`, `Models/UserProfile.swift`; UUID/Date/Double instead of Android Long/millis/Float |
| Workouts | `Features/Workouts/RoutineDetailView.swift`, `RoutineDetailModel.swift`, WeeklyPlanView/Model, WeeklyProgressView/Model, NextWeekModel, CompletionView |
| Generation | `Services/Generation/` deterministic equivalents; read current generation contracts |
| Persistence | `Services/Persistence/TrainingStore.swift`, `TrainingRecords.swift`; SwiftData ModelContainer with five record types |
| Mutation risk | `savePlan` replaces an existing same-week plan; `saveUser` deletes existing user data; `updateUser` preserves it. Do not use replace-style APIs for targeted adjustment |
| Purchases | `Services/Purchases/GenerationAllowance.swift`, `Entitlements.swift`; `Features/Purchases/PaywallReason.swift` cases nextWeek/rewrite/freshPlan |

Current model does **not** ship an LLM. The Sept 12 generation spike concluded deterministic selection beat/tied tested model choices; `docs/on-device-generation.md` retains the old plan below an explicit outcome. Its historical “ship this” wording is superseded. Unstuck is a new interpretation task, not permission to restore model-generated weekly plans.

## Existing data semantics to preserve

`WeeklyWorkoutPlan` includes IDs, user, week number, optional start date, days, created/updated timestamps. `WorkoutDay` includes status, planned duration, exercise count/equipment, exercises and completedAt. `WorkoutExercise` has a canonical `exerciseKey`, measure, sets, target metadata, rest, tutorial URL, completion flag, and generated/catalog notes. **Those notes are not a user debrief field.** `ExerciseSet` separates target reps/kg/seconds from actual values plus completion.

Existing statuses are NOT_STARTED, IN_PROGRESS, COMPLETED. A checkmark can populate targets into actuals; historical data lacks explicit entry provenance. Planned duration is not measured elapsed session duration. Android entities cascade through user → week → day → exercise → set; replacing parents can destroy children. iOS similarly uses relational records and replacement helpers.

## Proposed domain records

Use platform-native IDs internally; serialize as opaque strings at contract boundaries. Never expose database IDs as model-generated authority. Use a versioned policy/model/schema bundle on each proposal.

| Record | Minimum fields / purpose |
|---|---|
| SessionNote | id, user/day/session ID, raw text, locale, created/updated, source; separate user-owned text |
| GoalPriority | id, broad goal reference, priority kind (movement/muscle), catalog-backed key, explicit confirmedAt, active scope; optional |
| TrainingPreference | id, typed value, units, recurrence scope, source reference, confirmedAt, updatedAt; only confirmed facts used |
| AdjustmentRequest | id, day/session ID, reason enum, structured constraints, optional note reference, current revision, draft status |
| AdjustmentProposal | id, request, base plan/log revision, policy version, generated diff, evidence references, tradeoff code, created/expiry, status |
| AppliedAdjustment | id/cycle ID, proposal ID, before/after patches, stable exercise/set IDs, appliedAt, actor=user, policy/model versions, undo state |
| AdjustmentFeedback | id, cycle/session ID, practical answer enum, optional note, answeredAt/dismissedAt; one current response |
| SessionOutcome | session ID, lifecycle active/finished, finishKind full/partial, finishedAt, performed-set references; not inferred set completion |
| ActualValueOrigin | typed, explicitlyConfirmedTarget, legacyUnknown; per set or field as appropriate |
| AdjustmentAllowance | durable included cycle ID/consumedAt and purchase gating state; separate from free-week allowance |
| ModelInstallation | artifact ID, pinned hash, file path, runtime version, install state; no prompt content |

Proposed table/schema version is intentionally not hardcoded to “5” until current HEAD is checked. Add records transactionally and update schema versions/migration plans appropriately. Do not relabel legacy inferred entries as typed. If exact provenance is unknowable, mark legacyUnknown and exclude/label it in trend logic.

## State machine

Request: draft → structured/needsClarification → eligible → proposalReady → applying → applied.
Alternative terminals: cancelled, unsupported, noChange, failedRecoverable, safetyRoute. No terminal besides a committed `applied` mutates a plan or consumes allowance.

An applied cycle supports feedbackPending → answered/dismissed and remainingPlanRestored. Session lifecycle active → finished is independent. A finished session can be partial. Purchasing Pro resumes a preserved request only after rebuilding/revalidating its revision; it does not silently apply the old proposal.

## Atomic apply algorithm

1. Receive user action with proposal ID and a stable idempotency key, not raw model JSON.
2. In a single transaction, check proposal belongs to current user/day and current editable session; current plan/log revision matches the preview.
3. Re-run hard policy validation, catalog membership, allowable operations, entitlement/free-cycle availability, and preserved performed-set identity/value checks.
4. Compute and persist a patch to eligible unperformed work. Keep stable IDs and historical actual values. Store enough before/after metadata for transparent history and safe inverse patch.
5. Persist AppliedAdjustment and consume the included cycle only if this is a new successfully applied useful change. Persist confirmed preference only if the user explicitly opted in.
6. Commit, then emit success/navigation. On failure roll back all changes and allowance consumption. Retry with same idempotency key returns the existing application result.

Do not call whole-week regenerate, replace-plan, fill-all-sets, or model execution inside the database transaction. A workout edited while inference runs invalidates the preview. A goal/preference/catalog/policy change that materially affects eligibility also requires revalidation.

## Undo algorithm

Undo is not deletion of history. Before any further work, restoring the original remaining plan is straightforward via the inverse patch. Once the person has performed substituted/changed work, keep it exactly as logged and restore only compatible unperformed work. Explain what cannot be undone and preview if restoring remaining work would alter the person's workload unexpectedly. Never create duplicate already-performed sets when restoring an original exercise.

Keep the audit event and cycle entitlement; do not use destructive parent deletion. A second undo or retry is idempotent. Finished sessions retain historical adjustment records; editing old plans is outside v1's scope.

## Partial finish

Add an explicit finish-session command distinct from current `completeRoutine()`. It saves the already-recorded set values and marks the session finished/partial, leaving unperformed sets unperformed. Do not overload `COMPLETED` to mean all sets were done without updating every consumer. Prefer an additive SessionOutcome record and derived display state until a versioned status change can be migrated across platforms.

Audit weekly progress, next-week history aggregation, day cards, reordering, reopen behavior, progression, week-completion navigation, ads, and analytics for assumptions that a finished day means every set completed. Existing automatic completion side effects must fire once after a committed save. No adjusted or partial session should be lost when the process dies between saving and navigation.

## Migration and privacy requirements

Android: export the current Room schema, inspect all shipped versions, author and test explicit migrations to the new version with real representative fixtures. Remove reliance on destructive fallback for supported upgrade paths. Test parent/child IDs, counts, target/actual values, foreign keys, dates, entitlements, and unknown legacy values before/after. A new database that works is not a migration test.

iOS: version the SwiftData schema and migration plan as needed; test upgrading stores created by released app versions. Do not rebuild a store by deleting/reinserting users/weeks. Preserve UUIDs, relationship integrity, existing raw enum values, and history. Reinstalling a test app does not validate migration.

Keep notes/context/model caches on device with appropriate platform protection. Specify backup inclusion/exclusion, lock-state access, retention, export, and deletion before release. User note deletion also invalidates derived unconfirmed memory and prompt caches. Do not log raw input/output in Crashlytics, analytics, or model debug logs. Inference must not silently fall back to a cloud endpoint. Downloading model bytes is a distinct network action; do not include notes in requests or URL parameters.

## Suggested new module boundaries

`UnstuckIntentInterpreter` (runtime adapter, no mutation), `UnstuckPolicy` (pure validated decisions), `AdjustmentRepository` (atomic persistence), `AdjustmentCoordinator` (state), `ModelReadinessService` (installation/capabilities), `AdjustmentEntitlementPolicy` (existing Pro + separate allowance), and platform UI. These names are proposals. Keep vendor APIs behind an interface and provide a fake implementation for previews/tests. Share contracts, policy fixtures, and copy keys across platforms; avoid allowing Android/iOS numerical rules to drift.
