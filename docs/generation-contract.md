# Generation contract

How a week is made. Two owners now, no model: the **app** decides everything
numeric and which movement fills each slot, and the **catalog** says what every
movement is and how it is done. The executable authority is the code on both
platforms and its tests: `PlanSkeletonBuilder`, `TemplatePlanGenerator`,
`CarryForwardPlanGenerator`, `PlanExpander` and `GeneratedPlanParser`. This
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
| Which candidate fills each open slot | **Model** | the answer |
| Each session's title | **Model**, repaired to the session's focus | the answer, `PlanSelectionRepair` |
| Sets per slot, rest between them, the session's length | App | `SessionBudget`, `PlanSkeletonBuilder` |
| Reps, seconds and weight for every set | App | `ProgressionEngine`, `SeedLoad`, `LoadStep`, `RepWindow` |
| A lighter week | App | `DeloadCheck` |
| The plan's title | App | `PlanSkeletonBuilder` |
| Name, measure, muscles, how-to, equipment | Catalog | `exercise-catalog.json` |
| The chip ("3 sets of 10 reps") | App, read off the sets | `Prescription` |
| The caution line under a movement | Catalog and app | `InjuryGuard` |
| Dates, ids, completion, what was lifted | App state | — |

The model is never told the client's injuries, history, body, budget or
equipment. Each was once a rule it could disobey; each is now a mechanism it
cannot reach. A movement an injury rules out is on no slot's list, so the
filter is the whole of that obligation, and it has a test per injury.

## Three tiers

1. **Progressed** — `CarryForwardPlanGenerator`. Next week is last week's
   movements, progressed from what was lifted, with **no model call**. Taken
   only when every one of last week's movements still has a slot that offers
   it on the same day; a profile edit that rules one out, or a different
   number of days, hands the week on. Regenerating a week asks for new
   movements (`PlanRequest.freshCast`) and never takes this tier.
2. **Coach** — `GeminiPlanGenerator`. One request per week: the skeleton
   becomes the response schema, the answer is repaired, then expanded.
3. **Template** — `TemplatePlanGenerator`. The top of every list, expanded the
   same way. `FallbackPlanGenerator` hands it over whenever the coach cannot
   answer, carrying the coach's failure in `insteadOf`; the generating screen
   names the reason and waits for the client to read it. Only when the catalog
   cannot build a week either does the coach's own failure reach the screen.

**One free week.** Every path that produces a week — the first plan, next
week, regenerating this week, and repeating a week — goes through `ProGate`,
and the first week delivered spends the free generation whichever tier built
it. Everything after that asks for Pro. A generation that failed spends
nothing, because the allowance is spent only once a week has actually arrived.
Debug builds never reach the network, so no development run spends the model's
daily allowance.

## The selection schema

The model's entire output is a title per session and one movement key per
open slot. The schema is built from the skeleton: one object per session that
still has a choice in it, keyed `day1` … `day7`; inside it one enum per open
slot, listing that slot's candidates in rank order; then a `title` string.
Every property is required. A slot with a single candidate is already decided
and is not asked; a session with nothing left to choose is not in the schema
at all. Slots come before the title, so a session is named after what it
holds.

An excerpt, the first session of the shared fixture (a 34-year-old
intermediate client building muscle, four 60-minute sessions, every kind of
kit):

```json
{
  "day1": {
    "type": "object",
    "properties": {
      "primary": {
        "type": "string",
        "description": "the main lift",
        "enum": ["barbell_bench_press", "barbell_incline_bench_press", "dumbbell_bench_press", "dumbbell_incline_bench_press", "machine_chest_press", "push_up", "band_chest_press", "barbell_bench_press_close_grip"]
      },
      "secondary": {
        "type": "string",
        "description": "the second lift",
        "enum": ["cable_lat_pulldown", "chin_up", "pull_up", "assisted_chin_up", "assisted_pull_up", "band_kneeling_pulldown", "band_lat_pulldown", "band_pull_up"]
      },
      "title": {
        "type": "string",
        "description": "Two to four words naming what this upper body session trains"
      }
    },
    "required": ["primary", "secondary", "accessory", "isolation_1", "isolation_2", "core", "conditioning", "title"]
  }
}
```

The whole of it is `docs/fixtures/plan-selection-schema.json`. Both platforms
render the fixture's week to exactly those bytes, and both test that they do,
so the two apps cannot drift apart in what they ask a model for. The largest
week the setup screen allows stays under 12 KB.

## Worked example

An answer to the fixture's week. It takes the second candidate in a couple of
slots, as a coach would, and names every session. The test
`GenerationContractTest` reads this block, repairs it with no repairs needed,
and assembles it into a week the parser accepts.

```json
{
  "day1": {
    "primary": "barbell_bench_press",
    "secondary": "chin_up",
    "accessory": "barbell_overhead_press",
    "isolation_1": "around_the_world",
    "isolation_2": "dumbbell_incline_chest_fly",
    "core": "crunch",
    "conditioning": "cycling",
    "title": "Chest and Back"
  },
  "day2": {
    "primary": "barbell_squat",
    "secondary": "barbell_hip_thrust",
    "accessory": "dumbbell_bulgarian_split_squat",
    "core": "plank",
    "mobility_1": "stretching",
    "title": "Squats and Hamstrings"
  },
  "day4": {
    "primary": "barbell_bench_press",
    "secondary": "chin_up",
    "accessory": "barbell_overhead_press",
    "isolation_1": "dumbbell_hammer_curl",
    "isolation_2": "barbell_seated_wrist_curl",
    "core": "crunch",
    "conditioning": "jump_rope",
    "title": "Shoulders and Arms"
  },
  "day5": {
    "primary": "barbell_squat",
    "secondary": "barbell_romanian_deadlift",
    "accessory": "dumbbell_bulgarian_split_squat",
    "isolation_2": "machine_seated_leg_curl",
    "core": "crunch",
    "mobility_1": "yoga",
    "title": "Glutes and Quads"
  }
}
```

## Repair — what the schema cannot rule out

`PlanSelectionRepair` checks the answer against the skeleton. A repairable
problem is fixed from the slot's own ranking and noted; an answer goes back to
the model only when it is not a JSON object, answers no session, or would need
more than half of the week's open slots repairing. Title repairs cost no
request: the app can name a session itself. `${session}` below reads
`day1 (Full Body)`.

| # | Fires when | Action | Message sent back |
| --- | --- | --- | --- |
| V0 | the answer is not a JSON object | send back | Your answer was not a JSON object. Reply with only the JSON described by the schema, with nothing before or after it. |
| V1 | a session is missing | every slot from the top of its list | You left out ${session}. Answer every session in the schema, each with its title and each of its slots. |
| V2 | a slot is missing | the top of its list | In ${session} you left out ${slot}. Fill every slot with one movement from that slot's own list. |
| V3 | the key is not on that slot's list | the best candidate not already taken that day | In ${session}, ${slot} was answered with '${key}'. That is not on that slot's list. Choose only from the keys listed for the slot you are filling. |
| V4 | the key is already used that day | the next unused candidate | In ${session}, '${key}' is already used earlier in that session. Each slot needs a different movement. |
| V5 | the title is blank, over 40 characters, or not two to four words | the session's focus | The title for ${session} must be two to four words naming the body region and the focus, like "Upper Body Strength". "${title}" is not. |
| V6 | the title is an index label ("Day 2", "Full Body A") | the session's focus | The title for ${session} is an index label. The app already shows which day and which week it is; name what the session trains. |
| V7 | the title is filler ("good form", "training session") | the session's focus | The title for ${session} uses "${phrase}", which says nothing about this session. Name the region and the focus instead. |
| V8 | two sessions share a title | the later takes its focus | ${sessionA} and ${sessionB} are both called "${title}". Each session needs its own name. |
| V9 | no session answered, or repairs exceed half the open slots | send back, quoting V1–V8 | — |

V0 never quotes the decoder's own words, and the trail records how many
problems there were, never what they said: both can quote text the model
wrote from the profile.

## Expansion — how a choice becomes a week

`PlanExpander` turns each slot into an exercise:

- The movement is the model's choice if it is on the slot's list and not
  already used that day; otherwise the list's own order decides.
- `ProgressionEngine` is asked for the sets, given the movement's history
  across every stored week, the day's date, whether a lighter week is due,
  whether an injury asks care with it, and, for timed work, the seconds the day
  was fitted around.
- A starting weight lighter than an empty bar, or a movement the client has
  outgrown, is answered with the next candidate on the list, at most three.
- Rest is the skeleton's; timed sets never exceed what the day budgeted.
- The how-to is the catalog's summary; the stored prescription is blank,
  because the chip is read off the sets.
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

## Retry

Two attempts. Every answer that cannot be used spends one and moves to the
next model: a safety block on one model is often not one on the next, and the
next is a genuinely different opinion. No route to anything returns
**offline** at once; a model out of its daily allowance is remembered until
the reset and skipped; a model that is merely unavailable is skipped without
being remembered or spending an attempt. When every model is out of allowance
the result is **daily limit reached**; otherwise **failed**. Either way the
fallback builds the week.

## What the model is told

A short brief (under 1,500 characters): the app has decided the split, the
slots and every number; the model picks the candidate that best does each
slot's job for this client, keeps a session coherent, and names it in two to
four English words. The request (under 600 characters) says which week it is,
the client's age, experience and goal, and each session with a choice left in
it, in order, with how many slots it has. The candidates travel in the
schema, not the prompt.
