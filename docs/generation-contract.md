# Generation contract

The shape a generated weekly workout plan must arrive in. The AI does not
choose this shape — the app does, by sending it as a structured-output JSON
schema with the prompt. The executable authority is
`data/generation/GeneratedPlanParser.kt` and its tests; this document is the
annotated version, plus the guidance the prompt must carry.

## Division of responsibility

The model writes **the plan**: which days, which exercises, what to aim for,
and every piece of display copy. The app supplies **everything it already
knows better**, as parameters to `GeneratedPlanParser.parse()`:

| The app supplies | Why the model must not |
| ---------------- | ---------------------- |
| `userId`, `weekNumber` | App state. |
| `startDateMillis` (local midnight of the plan's Monday) | The model has no clock; dates it writes are guesses. |
| Video tutorial URLs | Model-written URLs are routinely dead or wrong. Resolved at render time from the hand-verified `ExerciseVideoCatalog`, keyed on `exerciseKey`. |
| Completion state, actuals, notes, ids | A new plan has no history, and only the user logs work. |
| `durationMinutes` | Arithmetic on the prescription, not a fourth number the model has to keep in agreement with sets, reps and rest. |
| `name`, `measure`, and a day's `equipment` | Facts about the movement, not about this client's week. The catalog owns them, so a plan cannot rename a movement, measure it wrongly, or claim kit the client does not have. |

Derived rather than accepted, so the app can never contradict itself on
screen: an exercise's `durationMinutes` is its prescribed work plus its rest
(a repetition costs about three seconds at the moderate velocity ACSM asks
for), a day's `duration` is the sum of those, `exerciseCount` is the size of
its exercise list, and set numbers are the order the sets arrive in.

## Schema

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "additionalProperties": false,
  "required": ["title", "days"],
  "properties": {
    "title": { "type": "string", "minLength": 1 },
    "days": {
      "type": "array",
      "minItems": 1,
      "maxItems": 7,
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["dayNumber", "title", "equipment", "exercises"],
        "properties": {
          "dayNumber": { "type": "integer", "minimum": 1, "maximum": 7 },
          "title": { "type": "string", "minLength": 1 },
          "equipment": { "type": "array", "items": { "type": "string" } },
          "exercises": {
            "type": "array",
            "minItems": 1,
            "items": {
              "type": "object",
              "additionalProperties": false,
              "required": [
                "exerciseKey", "prescription", "instructions", "sets"
              ],
              "properties": {
                "exerciseKey": { "enum": ["<the movements this client can perform>"] },
                "prescription": { "type": "string", "minLength": 1 },
                "instructions": { "type": "string", "minLength": 1 },
                "restSeconds": { "type": ["integer", "null"], "minimum": 1 },
                "sets": {
                  "type": "array",
                  "minItems": 1,
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "properties": {
                      "reps": { "type": ["integer", "null"], "minimum": 1 },
                      "weightKg": { "type": ["number", "null"], "exclusiveMinimum": 0 },
                      "seconds": { "type": ["integer", "null"], "minimum": 1 }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

## Example

```json
{
  "title": "Week 1",
  "days": [
    {
      "dayNumber": 1,
      "title": "Full Body Strength",
      "equipment": ["Dumbbells", "Yoga Mat"],
      "exercises": [
        {
          "exerciseKey": "goblet_squat",
          "prescription": "3 sets of 12 reps",
          "instructions": "Squat holding a dumbbell at your chest to build the legs and brace the core.",
          "restSeconds": 60,
          "sets": [
            { "reps": 12, "weightKg": 20 },
            { "reps": 12, "weightKg": 20 },
            { "reps": 12, "weightKg": 20 }
          ]
        },
        {
          "exerciseKey": "plank",
          "prescription": "3 sets of 45 seconds",
          "instructions": "Hold a straight line from head to heels to brace the whole core.",
          "sets": [
            { "seconds": 45 },
            { "seconds": 45 },
            { "seconds": 45 }
          ]
        }
      ]
    }
  ]
}
```

## Field notes

- **`exerciseKey`** — the field the whole progression loop hangs on. A
  canonical `lower_snake_case` slug, singular, in English
  (`goblet_squat`, `bent_over_row`), identical for the same movement in every
  week and every locale. History — the PREVIOUS column, progress over time —
  is matched on this key, never on `name`: prose names drift ("Goblet Squats"
  one week, "Dumbbell Goblet Squat" the next) and a drifted name breaks
  history silently. Unique within a day; recurring across days is normal.
- **`name`, `prescription`, `instructions`, titles** — display copy, written
  in the user's language. `prescription` is free text ("3 sets of 12 reps",
  "5 minutes"), not assembled from the sets: assembling it would mean
  inventing and translating a unit vocabulary for text the generator writes
  anyway.
- **`measure`** — decides which columns a set row renders and which target
  each set must carry: `WEIGHT_AND_REPS` and `REPS` require `reps` (weight
  optional — a new user has no baseline), `DURATION` requires `seconds`.
  A stray target the measure doesn't render (a weight on a jog) is stripped,
  so nothing lingers invisibly in the log. An unknown `measure` value
  degrades to `REPS`, matching the database mapper's fallback — and the plan
  is then rejected anyway if its sets carry no reps.
- **`durationMinutes`** — computed, not written. The card shows it beside
  the prescription, and it is the prescribed work plus the rest between sets.
- **`restSeconds`** — optional, maps to the domain's `restTime`, which is in
  seconds.
- **`dayNumber`** — ISO day of week, 1 = Monday. Weeks start Monday. Each day
  appears at most once; the parser sorts days by it.
- **`equipment`** — not written. Derived as the union of what the day's
  movements require, which the catalog already records.
- **`exerciseKey`** — an enum, not a string. The values are the movements this
  client owns the equipment for, capped and spread across muscle regions by
  `ExerciseShortlist`, plus every key their previous week used so progression
  keeps its thread. A movement outside that set is unrepresentable rather than
  rejected afterwards, which is what makes a small on-device model viable.

## Validation

The parser returns `Parsed(plan)` or `Invalid(errors)` — every problem, not
just the first, so a retry prompt can quote the full list. It never throws on
model output. Rejected: malformed JSON, blank required text, duplicate or
out-of-range day numbers, malformed or duplicated (within a day) exercise
keys, empty day/exercise/set lists, and numbers no client could perform: reps
outside 1–100, seconds outside 5–5400, `restSeconds` outside 5–600, `weightKg`
outside 0.5–500 kg, more than 12 exercises in a day or 10 sets in an exercise,
or a day whose total sets exceed what the client's session length pays for
(see `SessionBudget`). A set missing the target its measure requires is
rejected too, as is a key outside the offered vocabulary and a week missing a
movement pattern it was told to cover.
Tolerated: unknown JSON keys (ignored), unknown `measure` (degrades to
`REPS`), stray set targets (stripped).

## Prompt guidance

The prompt that requests a plan must tell the model, alongside this schema:

- Write all display copy in the user's language (en / ja / tl). Keep
  `exerciseKey` English slugs regardless of locale.
- Use the same `exerciseKey` for the same movement every week. When
  regenerating, the keys from previous weeks arrive in the prompt — reuse
  them for recurring movements rather than minting near-duplicates.
- Plan only `days.length == workoutDaysPerWeek` days, on sensible weekdays,
  using only the user's available equipment, respecting injuries.
- Give every set an explicit target; the app shows targets as placeholders
  the user logs against, and next week's generation is fed what was actually
  done (e.g. "last week: 40kg × 12, 12, 9").
- `WEIGHT_AND_REPS` only where the user has the kit to load the movement;
  bodyweight work is `REPS`, timed work is `DURATION`. Distance work (a 5 km
  run) has no representation yet — prescribe cardio by time.
- The session's set cap and the weekly set target per muscle group, both
  computed by `SessionBudget` from the user's answers. What the rules
  themselves rest on is in docs/programming-evidence.md.
- The movements it may prescribe, grouped by how each is measured so the model
  reads off which targets to write, and the movement patterns the week must
  cover. Both come from the shortlist, so both are already filtered to this
  client's equipment.
