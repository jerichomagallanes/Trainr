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
| Video tutorial URLs | Model-written URLs are routinely dead or wrong. Left null; lookup is a separate concern. |
| Completion state, actuals, notes, ids | A new plan has no history, and only the user logs work. |

Derived rather than accepted, so the app can never contradict itself on
screen: a day's `duration` is the **sum** of its exercises' `durationMinutes`,
`exerciseCount` is the size of its exercise list, and set numbers are the
order the sets arrive in.

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
                "exerciseKey", "name", "measure", "durationMinutes",
                "prescription", "instructions", "sets"
              ],
              "properties": {
                "exerciseKey": { "type": "string", "pattern": "^[a-z][a-z0-9_]*$" },
                "name": { "type": "string", "minLength": 1 },
                "measure": { "enum": ["WEIGHT_AND_REPS", "REPS", "DURATION"] },
                "durationMinutes": { "type": "integer", "minimum": 1 },
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
          "name": "Goblet Squats",
          "measure": "WEIGHT_AND_REPS",
          "durationMinutes": 8,
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
          "name": "Plank",
          "measure": "DURATION",
          "durationMinutes": 6,
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
- **`durationMinutes` vs `prescription`** — both shown on the card, and
  independent: ten allotted minutes of "5 sets of 1 minute" is not five
  minutes. Neither derives from the other.
- **`restSeconds`** — optional, maps to the domain's `restTime`, which is in
  seconds.
- **`dayNumber`** — ISO day of week, 1 = Monday. Weeks start Monday. Each day
  appears at most once; the parser sorts days by it.
- **`equipment`** — display strings in the user's language, only what the
  day's exercises actually use, drawn from the equipment the user said they
  have.

## Validation

The parser returns `Parsed(plan)` or `Invalid(errors)` — every problem, not
just the first, so a retry prompt can quote the full list. It never throws on
model output. Rejected: malformed JSON, blank required text, duplicate or
out-of-range day numbers, malformed or duplicated (within a day) exercise
keys, empty day/exercise/set lists, non-positive `durationMinutes`,
`restSeconds`, `weightKg`, or a set missing the target its measure requires.
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
