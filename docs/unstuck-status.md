# Unstuck: what shipped, what deviates, what is open

Trainr Unstuck is the **Adjust today** flow inside a live workout. This is the
report on what the Android build does at this commit, where it departs from the
frozen prototype in `docs/unstuck-handoff/prototype/`, and which release gates
are not passed. The specification is `docs/unstuck-handoff/`; this does not
restate it.

Nothing here has been reviewed by a clinician or a coach, and no usability
study has been run. Read section 5 before treating the feature as releasable.

## 1. What ships

One option row inside a running session asks what would help: less time,
missing equipment, show me how, something hurts, or something else. Each leads
to the smallest question that route needs, then to a review of what changes and
what it costs. Nothing is applied until the person taps apply, and an applied
change can be undone from the workout screen. After a saved session that used
an adjustment, one follow-up question is asked once. Confirmed time limits and
typed notes are listed, editable and deletable on a preferences screen.

| Prototype screen | Native |
| --- | --- |
| `reasons` | `AdjustTodaySheet`, opened from the `adjust_today` option row in `RoutineDetailScreen` |
| `time` | `AdjustTimeScreen` at `Screen.AdjustTime` |
| (`review-equipment` question) | `AdjustEquipmentScreen` at `Screen.AdjustEquipment` |
| `review` | `AdjustReviewScreen` at `Screen.AdjustReview` |
| `review` no-change | the same screen, `ReviewUi.NoChange` |
| `context` | `AdjustContextScreen` at `Screen.AdjustContext` |
| `guide` | no screen: `RoutineDetailViewModel.showHowTo` expands that card's existing `HowToSection` / `VideoTutorial` and scrolls to it |
| `pain` | `AdjustPainScreen` at `Screen.AdjustPain`, and again at `Screen.FeedbackPain` |
| `early` | `FinishEarlyContent` in `RoutineDetailScreen` |
| `saved` | `SessionSavedScreen` at `Screen.SessionSaved` |
| `debrief` | `DebriefScreen` at `Screen.Debrief` |
| `noteSaved` | `NoteSavedScreen` at `Screen.NoteSaved` |
| `feedback` | `AdjustmentFeedbackScreen` at `Screen.AdjustmentFeedback` |
| `feedbackDetail` | `FeedbackDetailScreen` at `Screen.FeedbackDetail` |
| `outcome` | `FeedbackOutcomeScreen` at `Screen.FeedbackOutcome` |
| `preferences` | `PreferencesScreen` at `Screen.Preferences` |
| `editPreference` | `EditPreferenceScreen` at `Screen.EditPreference` |
| `home` cards | `AdjustedTodayCard` and `WeekdayPreferenceCard` in `WeeklyPlanScreen` |
| `workout` banner | `AdjustedBanner` in `RoutineDetailScreen` |

`Screen.Adjust` and `Screen.Feedback` are nested graphs, so a draft survives
moving between steps and is dropped when the graph pops.

## 2. What the deterministic rules decide

`UnstuckPolicy` decides, and its version string is
`unstuck-policy-2026.09-unreviewed`. It is pure: no persistence, no UI, no
model. Every set, rep, load, rest and substitution it names comes from the
generation rules the app already shipped and the 451-entry catalog
(`app/src/main/assets/exercise-catalog.json`, catalog version 4).

| Decision | Owned by |
| --- | --- |
| How long a session or its remaining work takes | `SessionEstimate`, over `SessionMinutes` |
| Rest between sets | `SessionBudget` |
| How many sets a slot may lose, and the goal's drop order | `SessionShape` |
| Which slot each stored exercise came from | `SessionTiers` |
| Which exercise may replace another | `ExerciseCatalog`, `InjuryGuard` |
| A substitute's reps | `RepWindow` |
| A substitute's starting weight | `SeedLoad`, `LoadStep` |
| Which time limits may be asked for | `TimePresets` |

The prototype's 25/35/45-minute choices and its authored set counts are
illustrative and none of them are implemented. `TimePresets.forPlanned` derives
three choices from the session length the person answered for, ten minutes
apart, floored at ten; `isSupported` accepts 5–180 minutes and refuses anything
outside that rather than clamping it.

## 3. Deviations from the frozen prototype

| Reference | Native behaviour | Why | Where |
| --- | --- | --- | --- |
| `editPreference` saves on every tap ("This preference editor saves changes immediately") | Save and Cancel; leaving writes nothing | A stray tap must not rewrite a fact the person confirmed | `EditPreferenceScreen`, `EditPreferenceViewModel` |
| Sidebar goal selector, scenario nav, theme switch, "Reset example", "Preview Pro continuation", fake `9:41` status row and phone frame | None ship; real system insets, and `PaywallReason.ADJUST` opens the existing RevenueCat paywall | Preview tooling, not product | `docs/unstuck-handoff/prototype/index.html`; `AdjustGraph`, `ProPaywallScreen` |
| `review` shows an estimated duration | No estimate is shown; the review states the budget the person chose | Handoff 02 step 6: show an estimate only once the estimator is validated. The estimate is still computed to fit the session | `ReviewUi`, `SessionEstimate` |
| `render()` sets `#screen.scrollTop = 0` on every render | Scroll position is preserved across disclosure expansion | `TrainrScreenContent` holds one `rememberScrollState`; expanding is state, not a new screen | `AdjustReviewScreen` |
| Equipment flow starts on one fixed exercise | The chooser asks which exercise unless `need_an_alternative` on an exercise card supplied it | The profile's broad equipment categories do not identify a specific machine (handoff 02) | `AdjustEquipmentScreen` |
| Feedback copy is authored around a cable lateral raise | Both exercise names are derived from the applied record's proposal | Copy must describe the change that happened | `AdjustmentFeedbackViewModel` |
| `[25,35,45]` presets | Presets derived from the answered session length | See section 2 | `TimePresets` |
| `early` and `saved-partial` present finishing early as a state of its own | The day is stored as `WorkoutStatus.COMPLETED`; the kind of finish lives in the additive `session_outcomes` row and only the label reads **Finished early** | Widening the stored status enum would have meant migrating a stored string and auditing every consumer of `COMPLETED` (PR #156) | `SessionOutcome`, `WorkoutDayCard` |
| Prototype's follow-up returns through `outcome` | "Something else" ends at the note acknowledgement instead | Sequencing both needs a next-destination argument on the shared debrief route; the answer is recorded either way (PR #165) | `FeedbackGraph` |
| Prototype card counts and minutes are authored | An adjusted day derives its exercise count and minutes from the remaining sets; the stored `duration`, `exerciseCount` and `equipment` columns are not rewritten | Recomputing them on apply would make a faithful undo impossible (PR #159) | `RoutineDetailViewModel`, `AdjustedDay` |

## 4. Accessibility

Done: every tappable row and chip is at least `ComponentHeight.Medium` (48dp),
and `TrainrOptionRow` is 56dp; all type is `MaterialTheme.typography` in sp, so
it scales; the adjusted banner and the preferences confirmations are
`LiveRegionMode.Polite`, so one announcement follows an apply, an undo and a
forget; the review's disclosures carry `Role.Button` and a `stateDescription`
of expanded or collapsed; every new screen has light and dark `@Preview`s fed
by `SampleAdjustmentStates`, `SamplePreferenceStates` and
`SampleFeedbackStates`, except `AdjustTodaySheet`, which carries no `@Preview`,
and `SessionSavedScreen`, whose two are both light.

Not done: no real-device TalkBack pass, no switch-access pass, no large-text or
narrow-screen pass beyond previews, no RTL pass (the build filters resources to
`en`), and the two screens named above have not been seen in dark at all. One
known gap: the shared `TrainrToggleChip` the time presets use marks
selection with fill and text colour and carries no `selected` semantics, so a
screen reader is not told which preset is chosen.

**The brand contrast gate is open.** Handoff 04 marks the reference's white
16px primary label on `#D37200` as needing contrast review, and no measurement
has been made. The token is `brandLarge`, which is `Orange500` (`#D37200`) in
light and `#E8963A` in dark. If it fails, `brandLarge` in the light palette is
what changes, and every `TrainrButton` fill and the review's tradeoff rule
change with it.

## 5. Release gates still open

- [ ] Clinician review of the pain and urgent-symptom copy and of the eligible populations (D11, C09) — a qualified clinician.
- [ ] Coach review of the substitution mappings, the volume and set-drop rules, and the time estimator (D11) — a qualified coach; the policy version says `unreviewed` until then.
- [ ] Contrast measurement of the white label on `brandLarge` (handoff 04) — whoever owns the design system.
- [ ] Anti-abuse and reset policy for the included adjustment cycle — D06 is a proposal, not a decision; a commercial decision, not an implementation one.
- [ ] Retention, backup and export policy for notes and preferences — see section 7.
- [ ] Telemetry consent design — nothing content-bearing is sent today and no consented event exists.
- [ ] Usability study as described in handoff 07 — product.

**Automated coaching must not be enabled for the public until the reviewed
items above pass.** The feature is behind no remote flag — there is no
`RemoteConfig` and no feature flag in the build — so "enabled" means "merged
and shipped". Rollback is removing five entry points: the `adjust_today`
option row, the `need_an_alternative` link on an exercise card, the home
`WeekdayPreferenceCard`, the home `training_preferences` action shown when
`isHome && state.hasMemory`, and `AnythingToChangeCard`, whose **Leave a note**
is unconditional on `SessionSavedScreen` and `DayCompletedScreen` and reaches
the note editor and, through `NoteSavedScreen`, the preferences screen again.
That leaves every `applied_adjustments` row, every
preference, every note and every logged set intact. No rollback deletes data.

## 6. What the model does not do

No model ships. `IntentInterpreter` is bound to `UnavailableInterpreter` in
`AppModule`, which always answers unavailable; `AdjustmentViewModel` injects it
and returns before calling it unless availability is `READY`, which nothing
sets. The strict `IntentValidator` and the handoff's fixtures
(`intent-valid.json`, `intent-invalid-extra-key.json`, `behavior-cases.json`)
exist and are tested, so a future spike has a gate to pass rather than one to
negotiate. A validated extraction can name an intent, quote an explicitly
stated time or equipment mention with verified code-point offsets, flag a
concern, or request one bounded clarification — nothing else. It can never
write a set, choose an exercise, clear a pain concern, or decide entitlement.
The evaluation plan is `docs/unstuck-handoff/06-local-model.md`; no benchmark
has been run.

## 7. Data and privacy

Five tables were added to the app's own Room database on the device at schema
version 5: `session_outcomes`, `applied_adjustments`, `adjustment_feedback`,
`training_preferences` and `session_notes`. The included-cycle allowance is one
string in the existing preferences file (`StoredAdjustmentAllowance`), separate
from `StoredGenerationAllowance`.

No code in the app sends a note anywhere, and nothing from a note or a
preference reaches Crashlytics or any analytics: the only breadcrumb this
feature records is the content-free string `adjust_apply_failed`, beside the
route names `MainActivity` already recorded, and there is no analytics SDK.
Android's own backup is the exception, below. The ad, crash and purchase data
flows are unchanged and are described in `docs/privacy-policy.md`.

Not decided: backup inclusion. `AndroidManifest.xml` sets
`android:allowBackup="true"`, and the two files it points at,
`@xml/data_extraction_rules` and `@xml/backup_rules`, are still the empty IDE
templates with no `include` or `exclude`. Android backup therefore covers the
database and the preferences file, notes included, by omission rather than by
decision, which puts them in the person's Google account. Retention and export
remain an open gate, and `docs/privacy-policy.md` carries the same gap.

## 8. Test coverage

Against the matrix in `docs/unstuck-handoff/07-delivery-and-testing.md`. "Not
covered" means no automated test in this repository asserts it.

| ID | Covered by |
| --- | --- |
| T01 | `RoutineDetailScreenTest.aFinishedSessionIsNotOfferedAnAdjustment`; `FeedbackPromptViewModelTest.anActiveAdjustmentWithoutFeedbackIsTheOnlyPendingCase` |
| T02 | `UnstuckPolicyTimeTest.aSessionThatAlreadyFitsIsLeftAlone`; `AdjustmentViewModelTest.aBudgetThatFitsIsANoChange`; `AdjustmentGateTest.applyingIsWhatClosesTheIncludedCycle` |
| T03 | `UnstuckPolicyTimeTest.strengthAndMuscleGainShedDifferentWorkForTheSameBudget`; `UnstuckPolicyTimeTest.aConfirmedPriorityIsKeptWhole` |
| T04 | `UnstuckPolicyEquipmentTest.anEquipmentSwapKeepsTheSetCount`; `UnstuckPolicyEquipmentTest.onlyTheAffectedExerciseChangesAndPerformedSetsAreListed` |
| T05 | `UnstuckPolicyEquipmentTest.aSubstituteNeverInheritsTheOriginalWeight`; `AdjustmentApplyTest.aReplacementNeverCopiesTheOriginalWeight` |
| T06 | `AdjustmentApplyTest.applyingKeepsEveryPerformedSetsIdAndValues`; `AdjustmentApplyTest.aPerformedSetCanNeverBeOmitted` |
| T07 | `AdjustmentApplyTest.aProposalBuiltBeforeASetWasLoggedIsStale`; `AdjustmentViewModelTest.aStalePreviewIsRebuiltNotApplied`; `AdjustmentViewModelTest.aRebuiltPreviewAsksAboutWhatIsLeft` |
| T08 | `AdjustmentApplyTest.aRejectedChangeLeavesNothingBehind`; `AdjustmentViewModelTest.aFailedApplyKeepsTheProposalAndReportsIt` |
| T09 | `AdjustmentApplyTest.applyingTheSameProposalTwiceWritesOnce`; `AdjustmentViewModelTest.applyingEmitsTheProposalIdOnce`; `AdjustmentGateTest.theIncludedCycleStaysFreeAfterItIsSpent` |
| T10 | `AdjustmentApplyTest.undoBeforeAnyWorkRestoresTheWholeRemainingPlan`; `AdjustmentApplyTest.undoAfterPerformingTheSubstituteKeepsWhatWasLogged` |
| T11 | `RoutineDetailViewModelTest.finishingEarlyMarksTheDayCompleteWithoutFillingASet`; `WorkoutDayCardTest.aDayFinishedEarlySaysSoInsteadOfCompleted` |
| T12 | `RoutineDetailViewModelTest.finishingEarlyReportsAFailedSaveAndRetries`; `AdjustReviewScreenTest.anApplyThatFailedSaysSoAndOffersARetry` |
| T13 | `RoutineDetailViewModelTest.theSavedEventFiresOnce`; `RoutineDetailViewModelTest.aSecondTapWhileSavingIsIgnored` |
| T14 | `IntentValidatorTest.anUnknownEnumValueIsRejected`; `IntentValidatorTest.aTimeBudgetWithoutEvidenceIsNotActionable`; `UnstuckPolicyEquipmentTest.noCandidateIsReportedNotInvented` |
| T15 | `AdjustPainScreenTest.nothingHereAsksForPro`; `IntentRoutingTest.aDirectPainChoiceIsNotClearedByTheModel`; `IntentRoutingTest.aStatedDiscomfortRoutesToPainEvenWhenTheIntentIsLessTime` |
| T16 | `AdjustTodaySheetTest.showMeHowPicksAnExerciseInsteadOfARoute`; `RoutineDetailViewModelTest.tutorialsStartClosedAndToggleOpenAndShut` |
| T17 | `IntentRoutingTest.aStatedDiscomfortRoutesToPainEvenWhenTheIntentIsLessTime`; `IntentValidatorTest.anUnknownScopeLeavesMinutesUnactionable` |
| T18 | `IntentRoutingTest.aClarificationTheChooserAnswersBeatsANamedIntent` |
| T19 | `IntentValidatorTest.instructionTextInTheNoteIsJustAString`; `NoteSavedScreenTest.markupInANoteStaysLiteralText` |
| T20 | `IntentValidatorTest.truncatedJsonIsMalformed`; `IntentValidatorTest.aWrongSchemaVersionIsRejected`; `IntentRoutingTest.aRejectedInterpretationFallsBackToTheChooser` |
| T21 | `AdjustmentViewModelTest.rememberUncheckedWritesNoPreference` |
| T22 | `AdjustmentViewModelTest.rememberCheckedButKeepOriginalWritesNothing` |
| T23 | Partly. Editing and forgetting are covered by `EditPreferenceViewModelTest.savingWritesTheNewMinutesAndKeepsTheConfirmation` and `PreferencesViewModelTest.forgettingAPreferenceRemovesItAndSaysSo`; no gate is consulted anywhere outside `AdjustGraph`, but no test drives an expired entitlement |
| T24 | `PreferencesViewModelTest.deletingANoteRemovesIt`; `UnstuckPersistenceTest.aNoteRoundTripsThroughSaveUpdateAndDelete`. Nothing is derived from a note, so there is no cache to invalidate |
| T25 | `AdjustmentGateTest.theIncludedCycleStaysFreeAfterItIsSpent`; `AdjustmentGateTest.aLaterCycleCannotTakeOverTheIncludedOne` |
| T26 | Not covered. The draft is held in the graph-scoped `AdjustmentViewModel` so it survives the paywall, but no test returns from a cancelled purchase |
| T27 | Not covered. No device-eligibility check exists, because no model and no AI-specific purchase offer ship |
| T28 | Not covered. There is no download path to interrupt |
| T29 | Only 4→5: `TrainrMigrationsTest.migratingToFiveKeepsEveryLoggedSetAndOrdersExercises` and `.migratingToFiveCreatesTheUnstuckTables`. Versions 1–3 are handled by destructive fallback and are covered only by `StaleDatabaseTest.aFileFromTheOldSchemaOpensEmptyRatherThanRefusingToOpen` |
| T30 | `AdjustmentViewModelTest.theWeekdayComesFromTheLocalDate`; `FeedbackPromptViewModelTest.anEarlierWeeksDayIsNotAskedAboutTheNewestWeeksAdjustment` |
| T31 | ``UserMapperTest.`a set with actuals but no origin is stored as legacy unknown` ``; `RoutineUiTest.typingANumberMarksTheSetAsTyped`; `FeedbackOutcomeScreenTest.theTrendIsHonestAboutWhatOneSessionShows` and `.nothingOnScreenIsAPercentage` |
| T32 | Not covered. Both themes are covered by previews only, and not for the two screens named in section 4; there is no automated screen-reader, large-text, narrow-screen or hardware-keyboard test |
| T33 | `UnstuckPolicyTimeTest.fiveMinutesIsReportedAsInfeasibleNotShrunk`; `AdjustReviewScreenTest.anImpossibleBudgetOffersFinishingEarly`; `AdjustReviewScreenTest.anUnworkableRequestNamesNoMinutes` |
| T34 | `PlanSkeletonBuilderTest`, `PlanExpanderTest` and `GeneratedPlanParserTest`, all unmodified by this stack, plus `WeekPlanGeneratorTest`, which this stack extended with `aSubstituteAddedForOneDayIsNotCarriedIntoNextWeek`, and `SessionShapeTest`, which this stack added |
