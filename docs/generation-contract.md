# Generation contract

How a week is made. Two owners now, no model: the **app** decides everything
numeric and which movement fills each slot, and the **catalog** says what every
movement is and how it is done. The executable authority is the code on both
platforms and its tests: `PlanSkeletonBuilder`, `WeekPlanGenerator`,
`PlanExpander` and `GeneratedPlanParser`. This
document is the annotated version.

A remote model used to choose the movements. It was measured against the app's
own ranking on 15 real profiles and did not beat it — see
`docs/programming-evidence.md` for what "better" was measured against — so it
was removed along with the network, the daily allowance and the failure modes
that came with them.

## Who decides what

| Decision | Owner | Where |
| --- | --- | --- |
| The split, which weekdays, how many slots a day holds, what each slot is for | App | `PlanSkeletonBuilder` |
| Each slot's candidates: owned kit only, nothing an injury rules out, ranked, last week's first | App | `PlanSkeletonBuilder`, `InjuryGuard` |
| Which candidate fills each open slot | App: last week's, or one of the slot's best two, seeded from the client's answers | `WeekPlanGenerator` |
| Each session's title | App: the session's focus, or last week's title when the week is carried forward | `PlanSkeletonBuilder`, `WeekPlanGenerator` |
| Sets per slot, rest between them, the session's length | App | `SessionBudget`, `PlanSkeletonBuilder` |
| Reps, seconds and weight for every set | App | `ProgressionEngine`, `SeedLoad`, `LoadStep`, `RepWindow` |
| A lighter week | App | `DeloadCheck` |
| The plan's title | App | `PlanSkeletonBuilder` |
| Name, measure, muscles, how-to, equipment | Catalog | `exercise-catalog.json` |
| The chip ("3 sets of 10 reps") | App, read off the sets | `Prescription` |
| The caution line under a movement | Catalog and app | `InjuryGuard` |
| Dates, ids, completion, what was lifted | App state | — |

Injuries, history, body, budget and equipment reach a week only through the
skeleton. Each was once a rule a model could disobey; each is now a mechanism
nothing reaches around. A movement an injury rules out is on no slot's list, so the
filter is the whole of that obligation, and it has a test per injury.

## One generator, two ways to choose

`WeekPlanGenerator` builds the skeleton once, then:

1. **Carries last week forward** when there is a previous week, the request is
   not for a fresh cast, and every one of last week's movements still has a
   slot that offers it on the same day. A profile edit that rules one out, or a
   different number of days, means it cannot. Regenerating a week asks for new
   movements (`PlanRequest.freshCast`) and never carries.
2. **Chooses** otherwise — a movement for every slot from the catalog, as
   described below.

Either selection is expanded and parsed against the skeleton's own limits. A
carried week the parser turns down falls back to a fresh choice; a chosen week
it turns down is `Failed`, which only an empty or unfillable catalog produces.

**One free week.** Every path that produces a week — the first plan, next
week, regenerating this week, and repeating a week — goes through `ProGate`,
and the first week delivered spends the free generation whichever tier built
it. Everything after that asks for Pro. A generation that failed spends
nothing, because the allowance is spent only once a week has actually arrived.

## The selection

`PlanSelection` is a title per session and one movement key per open slot,
keyed by the skeleton's own ids. An empty selection is a complete answer: the
top of every list. A slot with a single candidate is already decided and is not
chosen for.

`WeekPlanGenerator` fills it so that two clients who answered the same way
do not train the same week for ever, and the same client rebuilding the same
week gets the same movements:

- Each open slot takes one of its best two candidates (`VARIETY_DEPTH`), not
  always its first. Going deeper was measured against the volume and frequency
  the evidence asks for and spread a week's sets thinner for no variety worth
  having.
- Which of the two is decided by an FNV hash of the client's answers — id, age,
  weight, gender, goal, experience, equipment and injuries — together with the
  slot id and the day number. Session length and day count are left out of the
  seed: they reshape the week on their own, and letting them reshuffle every
  movement would let a longer answer buy a shorter session.
- A movement already used that day is skipped, and so is one that trains the
  same muscle with the same pattern as something already in the session, so
  variety never buys a repeat.
- A fresh cast (`freshCast`) rotates each slot's pool by one and adds the week
  number to the seed, so a regenerated week is a different week rather than the
  same one reordered.
- The session's title is its focus (`fallbackTitle`).

The hash is written out by hand so the iOS app can run the same algorithm and
give the same week for the same answers.

## Expansion — how a choice becomes a week

`PlanExpander` turns each slot into an exercise:

- The movement is the selection's if it is on the slot's list and not
  already used that day; otherwise the list's own order decides.
- `ProgressionEngine` is asked for the sets, given the movement's history
  across every stored week, the day's date, whether a lighter week is due,
  whether an injury asks care with it, and, for timed work, the seconds the day
  was fitted around.
- A starting weight lighter than an empty bar, or a movement the client has
  outgrown, is answered with the next candidate on the list, at most three.
- Rest is the skeleton's; timed sets never exceed what the day budgeted.
- The how-to and the chip are not stored with the week: the card reads the
  catalog's summary and derives the chip from the sets.
- A session's title falls back to its focus and is cut to 40 characters.

## Parser — a net over the app's own arithmetic

Every week, whichever tier produced it, goes through `GeneratedPlanParser`
with the limits the skeleton was built to. Its job is now to catch a bug in
the app's arithmetic before a client sees it. It rejects:

- a blank plan title, no days, a repeated day, a day outside 1–7;
- a day with a blank title, no exercises, or more than 12;
- a day with more sets than its session length pays for, or more minutes of
  work and rest than half again the answered length;
- a movement repeated within a day, not offered, not in the catalog, or not a
  `lower_snake_case` key;
- rest outside 5–600 s, no sets or more than 10, reps outside 1–100, seconds
  outside 5–5400, a weight outside 0.5–500 kg, and a set missing the target
  its measure needs;
- a week missing a movement pattern it was dealt (a squat or lunge, an
  upper-body press, an upper-body pull).
