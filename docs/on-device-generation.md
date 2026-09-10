<!--
Researched 2026-09-10 across six dimensions (Android runtime, iOS runtime, model
licensing, structured output, progression science, coach prompting), with the
decision-relevant low-confidence claims put to adversarial verification. Where a
verifier disagreed with a researcher, the verifier won. Treat the numbers here
as researched, not measured on our own hardware: Stage 0 exists to measure them.
-->

# Trainr: migration from remote Gemini to on-device Gemma — plan

## 1. Feasibility, honestly

**Yes on both platforms, but only if the generation job is reshaped first.** The current job — one call producing a whole week of per-set `{reps, weightKg}` objects plus model-written prose — is ~3.0–3.7k output tokens. That is ~60–90 s on a flagship and 4–7 minutes on a mid-range phone. It is not shippable as-is at any model size. Reshaped to "model picks movements, app computes numbers," it is ~40–120 output tokens per day, which is 5–15 s per day even on weak hardware.

**Single biggest blocker: footprint, not law and not the runtime.** The only artifact that is simultaneously (a) ungated, (b) Apache-2.0, (c) prebuilt for the runtime we want, and (d) plausibly good enough is Gemma 4 E2B at **2.0 GB download and ~0.7–1.7 GB peak RSS**. On a free fitness app that is an onboarding-conversion event, and on 4 GB devices it is a jetsam/low-memory-kill risk. Everything cheaper is either gated (Gemma 3 1B → HTTP 401 anonymous, Gemma Terms of Use) or unproven at this task (Qwen3-0.6B).

Second-order blockers, all real:
- `litertlm-android` ships **arm64-v8a and x86_64 only**. Every armeabi-v7a device in a minSdk-24 install base is permanently excluded. A non-LLM path is mandatory, not optional.
- Measured mid-range numbers exist and are sobering: Pixel 8a (Tensor G3, 8 GB) running a 4B-class int4 model on CPU/XNNPACK does **22 tok/s prefill, 6.3 tok/s decode, 11.9 s TTFT, 4.43 GB peak RSS, 30–50 s engine init, plus a one-time 2.66 GB weight cache on disk**. A budget device (TECNO LJ9) running Qwen3-0.6B int4 does 8.3 tok/s decode.
- `engine.initialize()` is documented at up to 10 s and measured at 30–50 s on mid-range. Generation must run in WorkManager / a background task, not a ViewModel coroutine.

**Honest framing:** the reshape (section 4) is where ~80% of the value is. It is worth doing whether or not the on-device model ever ships. The on-device model is the remaining 20% and carries all the cost.

## 2. Model artifact

**Primary (ship this):**
- Repo: `litert-community/gemma-4-E2B-it-litert-lm`
- File: `gemma-4-E2B-it-gpu.litertlm` (text-only GPU/web build) — **2,008,432,640 bytes ≈ 2.0 GB**. The multimodal CPU build `gemma-4-E2B-it.litertlm` is 2,588 MB; we do not need vision or audio, so do not ship it.
- Licence: **Apache 2.0**, verified at `ai.google.dev/gemma/docs/gemma_4_license` (verbatim Apache text, 2026-04-01) and corroborated by the Gemma Terms of Use explicitly excluding Gemma 4 from its Appendix. `google/gemma-4-E2B-it` itself is `license: apache-2.0`, `gated: false`.
- Ungated: anonymous `GET` returns 302→200 with `accept-ranges: bytes`, so resumable download works with no HF token.
- **Pin to a commit SHA** (`resolve/<sha>/...`), not `main`. The card already warns files were re-uploaded once (pre-2026-05-05 downloads are stale). Ship an expected SHA-256 in the app and verify after download.

**Low-tier candidate (validate in Stage 0, ship only if it passes):**
- `litert-community/Qwen3-0.6B`, mixed int4, **~332–498 MB**, Apache-2.0, ungated. Constraint: the prebuilt is `ekv1280` — a 1,280-token KV cache shared between input and output. Our reshaped per-day prompt must land under ~900 input tokens for this to be usable, which is achievable only after the reshape (today's system instruction alone is 5,940 bytes / 1,390 tokens).

**Explicitly rejected:**
- **Gemma 3 1B** (584 MB, the obvious size sweet spot): every `litert-community` Gemma 3 artifact returns **HTTP 401 GatedRepo** anonymously. Shipping it means self-mirroring and pulling the Gemma Terms of Use §3.1/§3.2 plus the Prohibited Use Policy (which bars "unlicensed practice of… medical" and "automated decisions in… healthcare") into Trainr's own EULA as an enforceable clause. Not worth it for 1.4 GB of savings.
- **ML Kit GenAI / Gemini Nano (AICore)** as primary: zero download and free, but device reach is Pixel-9-class flagships, and its structured-output KSP surface is Alpha with no deprecation policy. Revisit as an opportunistic tier-0 after Stage 4.
- **Apple Foundation Models** as primary: 4,096-token session window (measured: our current week-2 *input* alone is 4,076 tokens) and iPhone 15 Pro+ only. After the reshape a single day would fit comfortably — revisit then, as a fast path, not a replacement.

**Hosting:** fetch from the pinned HF URL; mirror the identical bytes to a Cloudflare R2 free-tier bucket (10 GB-month storage, free egress) as failover. Play Asset Delivery cannot hold it (1.5 GB per-pack cap). GitHub Releases cannot hold it (2 GiB per-asset limit — it would fit Qwen3-0.6B only).

## 3. Runtime library per platform

**Android**
```
com.google.ai.edge.litertlm:litertlm-android:0.17.0   // Google Maven, Apache-2.0
```
- Pin the exact version. Do not use `latest.release`; the project went 0.14→0.17 between July and September 2026 and is pre-1.0.
- minSdk 24 (matches ours). Adds ~21.8 MB uncompressed / 9.5 MB deflated arm64 `.so`. Use ABI splits.
- Transitive: gson 2.14.0, kotlin-reflect 2.4.0, kotlinx-coroutines-android 1.11.0.
- Ships a 2.1 MB `THIRD_PARTY_NOTICE.txt` that must be surfaced in an OSS-licences screen, alongside the Apache-2.0 LICENSE + NOTICE for the Gemma 4 weights.
- GPU backend needs a manifest change inside `<application>`: `<uses-native-library android:name="libOpenCL.so" android:required="false"/>` plus `libvndksupport.so`. **Default to CPU**; GPU is an opt-in after measurement.
- Constrained decoding: `ConversationConfig(enableResponseFormat = true)` + `sendMessage(..., responseFormat = ResponseFormat.json(schema))`. Backed by vendored LLGuidance; enforcement is at the sampler, not post-hoc.

**iOS**
```swift
.package(url: "https://github.com/google-ai-edge/LiteRT-LM", exact: "0.17.0")  // product: LiteRTLM
```
- SPM-native (binaryTarget `CLiteRTLM.xcframework`), iOS 15+; Trainr-iOS is at 18.0 so no floor change. No CocoaPods needed — the existing pure-SPM graph survives.
- Same `ResponseFormat.json(schema:)` surface, so the schema string is shared verbatim between platforms.
- **Swift API is labelled Early Preview** (Kotlin is Stable). Every version bump is a manual xcframework checksum edit. Wrap it behind the existing `PlanModelClient` protocol at `/Users/jericho/StudioProjects/Trainr-iOS/Trainr/Services/Generation/PlanModelClient.swift` and never let LiteRT types leak past that seam.

**Rejected:** MediaPipe `tasks-genai` (maintenance-only; zero constrained-decoding API — verified by class listing; CocoaPods-only on iOS). ONNX Runtime GenAI (guidance is compile-gated behind `--use_guidance`, which the Android AAR build does not enable — the shipped `.so` literally contains the error string "To use guidance, build with use_guidance=true"; no Maven coordinate; same gap in the Obj-C binding). llama.cpp (no AAR, no Swift API, you own the NDK/JNI build). MLX (would fork Android and iOS onto different runtimes and end the 1:1 port). Core ML (no Gemma release, no constrained decoding, Anemll stale since March 2026).

## 4. Reshaping the job — what the model decides vs. what the app computes

**Opinionated position: demote the model from author to selector. It emits movement keys and a day title. Nothing else. The app computes every number and every sentence.**

### The new on-device contract (per day, not per week)

One constrained call per training day. Schema — note LLGuidance fixes property order to schema order, so `exerciseKey` goes first:

```json
{"type":"object","properties":{
  "title":{"type":"string","maxLength":40},
  "exercises":{"type":"array","minItems":3,"maxItems":8,"items":{
    "type":"object","properties":{
      "exerciseKey":{"enum":["…30–40 keys scoped to THIS day's slots…"]},
      "intensity":{"enum":["easy","moderate","hard"]}},
    "required":["exerciseKey","intensity"]}}},
 "required":["title","exercises"]}
```

Output: ~40–120 tokens. Seven of those beat one four-minute silence, each is independently validatable and repairable, and the KV cache stays small (which is what makes an `ekv1280` model even conceivable).

### App computes, deterministically, in Kotlin and mirrored Swift

A new `PlanSkeleton` in `domain/generation/` (next to `/Users/jericho/StudioProjects/Trainr/app/src/main/java/com/jericx/trainr/domain/generation/SessionBudget.kt`) produces, before any inference:
- Split and day count, and which weekday each session lands on.
- Ordered **slot tiers** per day: warm-up → primary compound → secondary compound → accessory → isolation → core → conditioning/mobility. Order matters for strength (whatever is trained first gains most — Nunes 2021, PMID 32077380) and is the loudest credibility signal; hypertrophy is unaffected, so this is a fatigue-allocation rule, enforced by code.
- Per slot, a **ranked candidate list of 5–15 exerciseKeys**, already filtered by owned equipment (`ExerciseShortlist.forRequest` does this today), by injury, by "already used this day/week," and with last week's key ranked first.
- `sets`, `reps`, `restSeconds`, RIR target — from `SessionBudget` plus a role×goal table (see §5).
- `weightKg` — from logged actuals, or the week-1 seed table. **The model never emits a weight.** Invented kilograms are the loudest fake tell and a 1B model has no basis for them.
- `instructions` — from `CatalogExercise.summary` / `.steps`, which are already populated for all 451 movements. The model has been paying tokens to rewrite text we already own; stop.
- `prescription` chip — templated from the computed sets/reps/measure, so the chip can never disagree with the set rows.

**Critical engineering move:** the expander's output is the *existing* `GeneratedPlan` (`/Users/jericho/StudioProjects/Trainr/app/src/main/java/com/jericx/trainr/data/generation/GeneratedPlan.kt`). `GeneratedPlanParser`, `RoutineMapper`, the DB and the UI are untouched. The parser's semantic checks become a safety net over our own arithmetic rather than over a model's.

### Prompt budget: six rules, ~150 tokens, in this order of value

1. The slot list with per-slot candidate keys. 2. Choose only from that slot's list. 3. Never repeat a movement or a movement pattern within a day. 4. Keep last week's movement where one is shown. 5. Title = 2–4 words naming region + focus; never "Day N"/"Week"/"A"/"B". 6. Intensity reflects how hard the slot should feel.

Delete the entire progression-rules block from `PlanPromptBuilder.kt` (lines ~91–99) and the weight/increment instructions. They are arithmetic; they belong in code and are currently duplicated between prompt prose and `GeneratedPlanParser`.

### Failure handling: repair, don't retry

On device a retry costs seconds but a small model rarely fixes a constraint it just violated. So: at most one retry, then **deterministic repair** — invalid or duplicate key → next-ranked candidate for that slot; bad title → template. The skeleton *is* a complete plan minus movement choices, so a zero-model fallback is nearly free. **Generation must never fail.**

### Validators to add (each is one "reads as fake" tell, made executable)

V1 key ∈ slot candidates. V2 ≤2 exercises per MovementPattern and per primary MuscleGroup per day. V3 slot tiers non-decreasing within a day. V4 direct sets per muscle ≤10/day. V5 weekly pattern coverage + push:pull ratio in 0.5–2.0. V6 session minutes ≤ ceiling. V7 **week-over-week ≥80% exerciseKey overlap, ≤1 movement changed per session** — this is the single strongest professional signal and the app has no cross-week continuity check today. V8 copy length + banned phrases ("good form", "engage your core"). V9/V10 reps and rest within the tier's range. V11 title regex.

### Two catalog/schema prerequisites

- **Per-exercise loadable increment** derived from `equipment`. Today `PlanPromptBuilder.incrementKg` is a flat "2.5"/"2.27" string with no equipment input, and `WeightUnit.loadable` is the *identity function in metric* — nothing is snapped and `GeneratedPlanParser` only range-checks 0.5–500 kg. 145 of 451 movements are MACHINE (pin stacks, cables, assisted machines where added load *reduces* difficulty), 13 are kettlebell.
- **Unilateral flag.** "12 reps of Bulgarian split squat" silently doubles the set's time and volume; the time budget's `reps × 3 s` is half the true cost.

**Warm-up ramp sets: defer, and scope narrowly when built.** The aggressive version (2–3 ramp sets on two compounds plus one on an isolation) adds 5–7 sets to a day whose entire budget is 6 sets at 30-min/strength. If built: gate on goal = STRENGTH or top set ≤6 reps, one ramp set on the first compound only, add an `isWarmup` flag to `ExerciseSet` (DB migration off version 3) so ramps are excluded from `maxSetsPerSession`, excluded from the minutes math, and **stripped from `asHistoryLine`** — otherwise week 2's progression rules get applied to warm-up rows.

## 5. Progressive overload as implementable arithmetic

All of this runs on data already persisted (`actualReps`, `actualWeightKg`, completion flags, session date). **No inference required.** Consequence: when the exercise cast is unchanged, week N+1 is generated with **zero model calls** — which is the single largest battery, latency and cross-platform-parity win available.

**Increment model.** `step(equipment, unit)`: barbell 2.5 / 2.27 kg; dumbbell single-arm 2.5 / 2.27, pair 5.0 / 4.54; machine & cable 5.0 / 4.54; kettlebell 4.0 / 8 lb; plate 2.5 / 2.27; bodyweight none. `snap(w) = round(w/step)*step`. Keep the existing rule that **one increment is the floor, not a ceiling** — ACSM 2009 (PMID 19204579) recommends a 2–10% increase *triggered by* performing 1–2 reps over target; it is not a safety cap and a >10% jump is not forbidden. Where one increment exceeds ~10% of current load (16 kg kettlebell, light machine work), use **double progression**: accumulate reps to the top of the range, then take the whole jump. Do not conclude "cannot progress by load."

**Week 1 = calibration, not prescription.** No validated equation predicts a starting load from age/sex/bodyweight; the validated route is a performed rep-max (Reynolds 2006, PMID 16937972: 5RM→1RM R²=0.993 bench, and adding anthropometry/age/sex added little). So: `seed10RM = BW × C(pattern, equipment) × S × A × E × 0.65(novice first 2 sessions)`, with S = 1.00 male / 0.55 upper, 0.70 lower for female (Miller 1993, PMID 8477683), A tapering 1%/yr past 40, E = 1.0/1.3/1.6 by experience. **This coefficient table is an engineering heuristic with no validating study.** Label week-1 loads as an estimate in the UI. Correct in-session: completed ≥ target+5 → next set +20%; +3–4 → +10%; +1–2 → +5%; on target → hold; < target−2 → −15%.

**Weekly update, per exercise, from the last completed session.** Let `minReps = min(actualReps)` over completed sets.
1. Not performed → repeat unchanged. No progression without a stimulus.
2. All sets at reps ≥ range top → if `step/load ≤ 0.10`: `snap(load × (1+p))` with p = 5% lower compound, 3.5% upper compound, 2.5% isolation, **enforcing `new ≥ load + step`** (otherwise imperial 5-lb snapping rounds two consecutive weeks to the same plate and looks like no progression); reset reps to range bottom. Else: rep ladder → +1 rep/set to a widened top, then +1 set (to the `SessionBudget` cap), then a harder catalog variation.
3. All completed but minReps mid-range → hold load, +1 rep target (rep creep).
4. One set 1–2 reps short → repeat unchanged.
5. minReps < bottom−2, or ≥2 sets short, or two consecutive stalls → `snap(load × 0.90)` rounded down, `stallCount++`.
6. DURATION work: +5 s while completed, −10 s after two misses.
7. Bodyweight REPS: +1 rep → +1 set → harder variation.

**Rep windows by goal × role** (not goal alone — a goal-only map is what produces "3×10 on deadlifts, planks and calf raises"): STRENGTH compound [3,6] / isolation [6,10]; MUSCLE_GAIN [6,10]/[8,15]; GENERAL_FITNESS [8,12]/[10,15]; ENDURANCE & WEIGHT_LOSS [12,20]/[15,25]; timed [30,60] s. Age ≥65 or relevant injury: +2 on both bounds, cap load steps at 2.5%.

**RIR overlay, intermediate/advanced only.** Beginners cannot self-report RIR accurately and everyone under-predicts reps-to-failure the further they are from it (Steele 2017 PMID 29204323; Halperin 2022 PMID 34542869) — so drive beginners off reps actually completed. For others, adjust the rule-2 step: RIR 0 → −5%; 1–2 → +0; 3 → +2.5%; 4+ → +5% (APRE, Mann 2010 PMID 20543732; Graham & Cleather 2021 PMID 31009432). Targets differ by slot: heavy compounds 2–3 RIR, isolation 0–2 — hypertrophy improves closer to failure while strength is flat across a wide RIR band (Robinson 2024 PMID 38970765; Refalo 2023 PMID 36334240).

**Time gaps** (from the last completed session of *that exercise*): ≤10 days normal; 11–21 repeat last completed load; 22–42 → ×0.90 and one fewer set, +5%/session for two sessions; >42 → treat as calibration, ×0.70. Justification is Ogasawara 2013 (PMID 23053130), and I am stating its limits: n=14 previously untrained young men, bench press only, and the protocol itself re-measured 1RM around the layoff — it is evidence that a break is not a catastrophe, **not** evidence that you resume at the old absolute load. Bickel 2011 (PMID 21131862) retained *strength* over a long layoff but not fibre-type adaptations, and 60–75-year-olds needed a higher maintenance dose than the young.

**Deload: triggered, and by volume reduction — never a scheduled week off.** The only controlled test of a planned mid-programme deload (Coleman 2024, PeerJ 12:e16777, PMID 38274324) used *complete cessation* and found **worse** isometric and dynamic lower-body strength than training through, with no hypertrophy or power benefit, *more* soreness and *lower* motivation. Practitioners deload ~every 5.6 weeks, mostly reactively, by cutting volume (Rogerson 2024, PMID 38499934). So: fire when any two of — `stallCount ≥ 1` on ≥2 exercises in a week; completion rate <60% over 2 weeks; ≥6 consecutive weeks of compound load increases (≥4 if age ≥50). Prescription: same exercises, **reduced sets at maintained load**, one week, resume at the last successful load. Never deload a beginner in their first 8 weeks.

**Do not ship a weekly volume ramp.** Enes 2024 (PMID 37796222) is a single 12-week trial, n≈10/arm, and two independent RCTs found nothing (Moreno 2025 PMID 39557664; Barsuhn 2025 PMID 39665246 — maintenance actually had the higher squat 1RM), while the 2026 meta-regression (Pelland, PMID 41343037, 67 studies) shows strength saturates with volume faster than hypertrophy does. `SessionBudget.weeklySetsPerMuscle` already caps at 10 and typically resolves to 6–7; leave it there. Keep the fractional set-counting convention (1 for primary, 0.5 per assisted) — Pelland found that quantification best-supported, and `SessionBudget.REGION_SETS_PER_SET_HALVES` already does it.

**Do not encode MEV/MAV/MRV per-muscle tables.** No peer-reviewed source defines them. Encode the shape the evidence supports: floor ~4 direct sets/muscle/week (Iversen 2021 PMID 34125411), rising with diminishing returns to ~10–20, per-session practical cap ~10.

## 6. The existing Gemini path

**Keep it. Demote it. Do not delete it.**

The goal "stop depending on remote Gemini" is achieved by making the **deterministic skeleton the guaranteed floor**, not by deleting the network path. Concretely, three tiers behind the unchanged `PlanGenerator` interface:

1. **On-device LiteRT-LM** — where the device qualifies (arm64-v8a, ≥6 GB RAM, model provisioned, measured decode above threshold) and the user has completed the download.
2. **Deterministic template selection** — zero model. Slot skeleton + staple-ranked, pattern-covering, injury-filtered key picking. Runs offline, instantly, on every device including armeabi-v7a. **This is what actually removes the dependency.**
3. **Remote Gemini** — retained behind a flag, offered on devices that cannot run tier 1, as the quality upgrade over tier 2.

Reasons not to delete: (a) LiteRT-LM has no 32-bit ABI, so a slice of the minSdk-24 base can never run tier 1; (b) a 2 GB download will be declined or will fail; (c) 4 GB devices will be OOM-killed mid-generation; (d) it is the only reference implementation to A/B the on-device output against during Stages 2–4.

Set an explicit removal criterion rather than keeping it forever: **delete the remote path, `PlanModelClient.MODELS`, `DailySpentModels`, `SpentModels`, `GeminiResponse.QuotaSpent` and `PlanGenerationResult.DailyLimitReached` once <5% of generations in a release use tier 3.** Until then that quota machinery stays; on-device it is dead code and should be bypassed, not deleted early.

## 7. Staged delivery

**Stage 0 — spike (riskiest unknown, no app code, ~3 days).** Off-device, on-device, outside the repo. Build the reshaped per-day prompt and JSON schema by hand from the real 451-movement catalog and ~30 real onboarding profiles. Run it through the LiteRT-LM CLI / `litertlm-jvm` on a Mac and, critically, on **one mid-range Android device** (Snapdragon 6/7-gen or Dimensity 7000-class, 8 GB) and **one 4 GB budget device**, against both Gemma 4 E2B and Qwen3-0.6B int4.
Measure: (a) does a human reading the day accept the movement selection and order — target ≥80% of days accepted with zero edits; (b) tokens/s decode and TTFT; (c) engine init time; (d) peak RSS; (e) llguidance mask latency with a 40-key enum (unmeasured anywhere, and it is per-token overhead on top of already slow decode); (f) whether Qwen3-0.6B's `ekv1280` holds the trimmed prompt.
**Kill criterion:** if selection acceptance on Gemma 4 E2B is below ~60%, or mid-range per-day latency exceeds 30 s, stop here and ship only Stage 1. That outcome is not a failure — Stage 1 is most of the value.

**Stage 1 — the reshape, against existing remote Gemini (ships alone).** Slot skeleton, `ProgressionEngine`, catalog-sourced instructions, templated prescription chips, equipment-aware increments, validators V1–V11, deterministic template fallback. Mirror in Swift. Ships: cheaper and faster remote generation, plans that **can never fail**, visible week-to-week continuity, no more invented kilograms. Do this against Gemini, not on-device, so regressions are attributable to the reshape and not to the model swap.

**Stage 2 — on-device engine, Android, dev flavour only.** Add `litertlm-android:0.17.0` behind the existing `dev`/`prod` flavour split; port `generatedPlanSchema` from Firebase's `Schema` builder to a raw JSON Schema string; **model side-loaded via adb, no downloader yet.** Ships internally: real device measurements, A/B against tier 3 on identical profiles.

**Stage 3 — provisioning and Android beta.** Resumable background download (WorkManager, unmetered-only default, SHA-256 verification, pinned commit SHA, R2 mirror failover), free-storage and RAM gating, a first-run benchmark that demotes the device to tier 2/3 when measured decode is below threshold, generation in a foreground service with per-day progress and resumability across process death. Play Console: AI-generated-content in-app reporting affordance and the Health apps declaration.

**Stage 4 — iOS.** `LiteRTPlanModelClient` beside `FirebaseAIPlanModelClient` behind the existing `PlanModelClient` protocol; same schema string; Background Assets for delivery; App Store review note explaining the download plus the 4.2.3(ii) size disclosure and pre-download prompt.

**Stage 5 — demote remote Gemini** per the §6 criterion, and re-evaluate the free zero-download tiers (AICore/Gemini Nano on Android, Apple Foundation Models on iOS — a single reshaped day now fits inside 4,096 tokens, and on iOS 27 LiteRT-LM already ships `LiteRTLanguageModel` conforming to Apple's `LanguageModel` protocol, so both can hide behind one `LanguageModelSession`).

## 8. What could make this a bad idea

- **A 2 GB download on a free fitness app is an onboarding cliff.** On exactly the low-end devices least able to complete it. If Stage 1 lands and tier 2 output is decent, the honest conclusion may be that the LLM is a rounding error of quality for an enormous distribution cost.
- **Constrained decoding guarantees shape, not a good workout.** A 2B model will satisfy the grammar while picking four chest movements and no row. Every guardrail must be code — enum trimming before the call and validators after — which means the model's marginal contribution over ranked template selection may be small. Stage 0 exists to find that out before committing.
- **You are now the author of load prescriptions.** Moving progression from prose-in-a-prompt to deterministic code is better engineering and a sharper medical-adjacent liability surface. The week-1 seed table has no validating study. Injury guardrails, under-18/over-65 rules and substitutions must apply to the computed numbers, not just the model's picks.
- **Two pre-1.0 runtimes maintained by one person.** 0.14→0.17 in three months, Swift API self-described Early Preview, iOS pinned to an xcframework URL + checksum with no major-version signal. Every bump is manual and can change decode behaviour.
- **Battery, thermals and background kills.** 30–50 s engine init plus per-day inference on a phone the user is holding in a gym. Nothing in the current architecture survives process death mid-generation.
- **Scope.** Stage 1 alone touches `GeneratedPlan`, the schema, `GeneratedPlanParser`, `SessionBudget`, the catalog JSON, a DB migration and the workout UI, twice (Kotlin + Swift). This is a bigger change than the backend swap it enables.
- **Gemma's Prohibited Use Policy is linked from the Gemma 4 licence page but not incorporated by the Apache-2.0 text.** Legally the field-of-use restriction almost certainly does not bind Gemma 4; practically, "general fitness information, not medical advice" positioning is forced anyway by App Store 1.4.1, Play health policy and medical-device claim rules. Treat it as a product constraint regardless.

## Remaining uncertainties, stated plainly

1. **Selection quality at 2B is unmeasured.** No benchmark maps to "pick 6 movements for a day." Stage 0 is the only evidence that will exist.
2. **llguidance mask latency over a 30–80-value enum on ARM is unmeasured anywhere.** It is per-token overhead on an already slow decode.
3. **Mid-range decode for Gemma 4 E2B specifically is unmeasured** — the closest proxy is Pixel 8a running a *different* 4B-class model at 6.3 tok/s with 4.43 GB peak RSS and a 2.66 GB on-disk weight cache. The storage cost may be ~4.7 GB total, not 2.0 GB.
4. **`weightKg` semantics are undefined today** — per-hand or per-pair for dumbbells. Neither the schema nor the prompt says. This must be settled before the progression engine does arithmetic on it.
5. **The week-1 seed coefficient table is a heuristic**, not science. Ship it conservative, labelled, and self-correcting.
6. **HF as production CDN has no SLA**, and files there have already been re-uploaded once. Pinned SHA + verified hash + R2 mirror is mitigation, not a guarantee.
7. **Play's AI-generated-content policy** requires in-app reporting of offensive output; whether it applies when inference is on-device is not spelled out. Implement it regardless — it is cheap.
8. **Deload triggers and the 22–42 day regression rest on single small trials.** Ship them as tunable constants, not settled defaults.

Key files this plan touches: `/Users/jericho/StudioProjects/Trainr/app/src/main/java/com/jericx/trainr/data/generation/{GeneratedPlan,GeneratedPlanSchema,GeneratedPlanParser,PlanPromptBuilder,PlanModelClient,GeminiPlanGenerator}.kt`, `/Users/jericho/StudioProjects/Trainr/app/src/main/java/com/jericx/trainr/domain/generation/SessionBudget.kt`, `/Users/jericho/StudioProjects/Trainr/app/src/main/java/com/jericx/trainr/domain/catalog/{ExerciseShortlist,ExerciseCatalog}.kt`, `/Users/jericho/StudioProjects/Trainr/app/src/main/java/com/jericx/trainr/domain/model/UnitSystem.kt`, `/Users/jericho/StudioProjects/Trainr/app/src/main/assets/exercise-catalog.json`, `/Users/jericho/StudioProjects/Trainr/app/src/prod/java/com/jericx/trainr/data/generation/{FirebaseAiClient,PlanGeneratorFactory}.kt`, and their Swift mirrors under `/Users/jericho/StudioProjects/Trainr-iOS/Trainr/Services/Generation/`.
