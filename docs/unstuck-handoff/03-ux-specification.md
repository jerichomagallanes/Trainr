# 03 · UX, navigation, and state specification

## Interaction rules

Keep the existing weekly plan and workout logging as the main experience. There is no mandatory chat, survey, or daily AI check-in. Use one obvious primary action per screen. Saving happens before optional reflection. Show priority, change, tradeoff, and scope before details; exact changes and rationale are progressively disclosed.

The prototype displays every state inside a phone for demonstration. In native apps, use an expanding modal sheet for the short reason chooser and a normal scrollable destination for review, notes, preferences, and results. At large text sizes a sheet can expand to full height. System back/dismiss means cancel the current draft step, not apply. Preserve typed draft text across temporary dismissal/process recreation according to the local retention policy.

## Screen map

Prototype IDs are stable references; implementation route names are proposed, not existing code.

| ID | Entry and contents | Primary / secondary behavior | Persistence |
|---|---|---|---|
| `home` | Existing weekly plan; focus if explicitly set; contextual adjusted/preference card only when relevant | Existing start/view action; training preferences link | Read current plan, no inferred focus |
| `workout` | Existing live routine plus Adjust today; applied banner with scope and undo | Existing finish; Finish early; exercise tutorial/alternative | All real set controls/timers remain |
| `reasons` | “What would help today?”; less time, equipment, guidance, pain, other | Tap one route; Keep today's plan dismisses | Draft reason only |
| `time` | “How much time do you have?” whole-session or remaining-time label; presets; goal priority; unchecked remember option | Show recommendation; Keep today's plan | Draft constraint; preference not persisted yet |
| `review` | Priority, constraint, Today only, change, tradeoff; Exact changes and Why disclosures | Use this workout commits; Keep original cancels | Persist only after successful validated transaction |
| `review` no-change | “Keep your current workout”; reason existing plan fits | Continue workout | No allowance consumed; preference can be saved only if explicitly checked |
| `context` | Optional note and necessary clarification controls | Relevant structured route; Back to workout | Local draft, no autonomous changes |
| `guide` | Exercise-specific existing tutorial/how-to | Back to workout | No plan change |
| `pain` | Reviewed pause/help content; no automated exercise alternative | Save and finish early; Return to workout | No paid gate; no “safe to continue” endorsement |
| `finish` | Summarize genuinely recorded work and unlogged work if any | Save workout | Never fills unlogged sets for Unstuck flow |
| `early` | “Finish early?” / truthful performed-work summary | Save and finish; Continue workout | Session closed as partial; performed work preserved |
| `saved` | Confirmation that saving succeeded; accurate session status | Back to plan; optional Anything to change next time? | Feedback must not block completion |
| `debrief` | Optional note about the completed session | Save note; skip/back | Local note separate from generated exercise notes |
| `noteSaved` | Note saved acknowledgement | Done | No claim model learned or changed future workouts |
| `feedback` | One relevant question for this adjustment | Yes / Not quite / discomfort; Not now | Response tied to cycle/session; idempotent |
| `feedbackDetail` | What still needs changing? | Save answer or open guidance/note | No automatic second plan change |
| `outcome` | Practical answer and separate honest trend state | Done | Future workouts unchanged |
| `preferences` | Confirmed facts with scope/source/date; notes separate; empty state | Edit / Forget / Delete note; Back to plan | User-owned controls free after Pro expiry |
| `editPreference` | Explicit constraint and recurrence scope | Done in frozen prototype | Prototype auto-saves; proposed native Save/Cancel avoids accidental edits; document this deliberate deviation |
| `progress` | Existing weekly history extended with truthful participation vs performance | Back to plan | Not a replacement of existing history |
| `pro` | Contextual explanation and preserved draft request | Existing paywall/restore; Keep original | No simulated successful purchase in native app |

## Exact content hierarchy for recommendation review

1. Screen heading, e.g. **A shorter workout for today** or **Use the dumbbells today**.
2. Small “Your priority” label and the confirmed goal priority. If no specific priority exists, use the broad goal without inventing bench/shoulder focus.
3. Constraint summary, e.g. time available. An estimate of duration is distinct from the user's time budget.
4. Sunken scope chip/row: **Today only · Undo available**. If performed work limits undo, change the copy to truthful “Restore remaining plan” rather than promising full undo.
5. Outlined card: kept work or appropriate alternative; short explanation.
6. Orange vertical-rule **Tradeoff** callout with concrete cost, e.g. less direct arm work, less barbell practice, or independent load history.
7. Expandable **See exact changes**, showing before/after exercise names, set counts and target changes when applicable. Include omitted work and preservation of already-logged sets.
8. Expandable **Why this change?**, grounded in approved rules and real input. A replacement may also show **How to choose the weight** with reviewed calibration guidance.
9. Fixed primary **USE THIS WORKOUT** and secondary **Keep original**. Do not apply from a details disclosure, back gesture, or model completion.

Copy should explain observable changes rather than claiming expert certainty. Use deterministic templates initially. If model-generated explanation is later tested, it may only restate a validated fact bundle and must fall back to templates if unsupported facts/numbers appear.

## Completion behavior

If all work is genuinely logged, normal finish should be as short as the current experience. If work remains, summarize what is recorded and offer finishing with that work; never silently populate remaining actuals. The prototype's count is a fixture, not a query. Show exercise/sets participation truthfully without implying all exercises were complete.

Saving and entitlement checks must not race. The completion confirmation is shown only after persistence succeeds; optional note/follow-up comes after. On save failure, preserve all state and show Retry. Double taps and restored navigation cannot duplicate a completed session or feedback prompt. Session lifecycle is separate from prescribed work completion.

## Preferences and notes

Default checkbox is unchecked. Copy: **Remember this Tuesday time limit** with **Future workouts will still ask before changing.** Show the actual recurrence day from the scheduled local calendar and confirm when rescheduling makes the scope ambiguous. “Tuesday” must never be derived from a UTC date without local-zone handling.

A remembered fact includes source, confirmation date, scope, editable value, and forget action. A raw note is not a fact. Model suggestions to remember something require a separate explicit confirmation. Forgetting a preference stops future use; it does not remove past performed workouts. Deleting a note removes the raw note and dependent inferred candidates/caches; historical applied prescription records retain only the minimum needed to describe performed work. Do not promise complete erasure from device backups until the backup policy is implemented and explained.

## Additional production states absent from the frozen mockup

Use the same spacing, colors, cards, typography, and primary/secondary controls. These are specified behavior, not approved new screenshot compositions; make native previews for design review before release.

| State | Required UI and action |
|---|---|
| Model not installed | “Set up private coaching”; exact download/storage requirements from pinned manifest; Download / Use simple options |
| Downloading | Determinate bytes/progress if known; pause/cancel; Wi-Fi preference; workout remains accessible |
| Insufficient storage | Required vs available space, retry after freeing space; manual controls |
| Verifying/loading | Honest short status, cancel; never a fake percent; off main thread |
| Unsupported device/language | Explain feature limitation before a related purchase; direct structured path remains |
| Parsing/generating | “Reviewing your request”; preserve note and logs; cancel; no streaming prescription fragments |
| Needs clarification | One explicit question with bounded choices; no repeated chat loop |
| Malformed/unsupported model output | “Let's use the simple options”; preserve input; no guessed repair into an actionable plan |
| Timeout/thermal/memory pressure | Stop/release engine; retain request; retry once by user or manual controls |
| No eligible substitution | Explain no supported alternative; tutorial/manual finish; do not invent equipment |
| No feasible short session | Explain limitations; Keep original / Save and finish as appropriate |
| Stale preview | “Your workout changed”; rebuild and re-preview; no silent apply |
| Save/apply error | Explain not applied; retain proposal; Retry; allowance unchanged |
| Purchase cancelled/failed | Keep request and current workout; retry/restore/current plan |
| Entitlement uncertain offline | Respect existing verified cached entitlement policy; no destructive downgrade or trapping active logging |
| Empty history | State insufficient comparable data; no progress percent |

## Accessibility and localization

Minimum hit targets 48 dp Android / 44 pt iOS; visible buttons can be smaller only with appropriate hit regions. Dynamic text expands components, cards, footer, and sheets; no ellipsis for safety, tradeoff, or price copy. Keyboard should not obscure primary actions or text fields. Preserve scroll position after disclosure expansion and return; prototype resets scroll on every render, which is not native production behavior.

Screen reader: announce headings on navigation, selected chips and units, before/after changes in meaningful order, one success/error announcement, and button purpose. Do not move focus on each typed character. Use explicit labels instead of Unicode symbol names. Never communicate state only through orange/green. Respect reduced motion. Verify RTL, longer localized strings, VoiceOver/TalkBack, large text, switch access, and keyboard dismissal. Localize all production copy through existing localization systems.

## Prototype defects and deliberate native deviations

- Common equipment chooser defaults to lateral raise; native must identify the affected exercise.
- Completion/set counts and history are simulated; native uses actual persisted data.
- 25/35/45-minute examples are authored, not coaching logic.
- “Preview Pro continuation” resets simulation state; native must use real entitlements and transactions.
- Feedback equipment copy assumes a cable replacement in one path; production derives the exact exercise names from the applied record.
- Preference editing auto-saves in prototype; native Save/Cancel is the proposed explicit edit contract.
- Existing tutorials replace the mock guide card. Placeholder icons become native assets.
- Model readiness, accessibility failures, and pain escalation require additional work; the prototype does not certify them.
- Scenario sidebar, goal selector, phone chrome, fake time/signal, commentary, and Reset are preview tools and must not ship.
