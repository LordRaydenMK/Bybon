# Strong CSV ↔ Bybon

Living comparison of Strong’s CSV export vs Bybon’s workout domain **as implemented today**.
Import is live (History empty-state → file picker). Workout data is still in-memory — Room only
persists body-weight tables.

Reference export in repo:
[`app/src/test/resources/strong-backup-sample.csv`](../app/src/test/resources/strong-backup-sample.csv)
(52 sessions, Strong workout #201–252, 2053 rows, 2026-02-17 → 2026-08-20).

A full Strong backup (workout #1–252, 8170 rows, 2024-04-17 → 2026-08-20, **42** exercise names,
**13** workout names) was also checked. It is the same format as the sample; the sample is the last
52 sessions. Extra findings are in [Full backup](#full-backup-252-sessions).

Code:

- Parser [`StrongCsvParser.kt`](../app/src/main/java/dev/sanastasov/bybon/strong/StrongCsvParser.kt)
- Mapper [`StrongCsvMapper.kt`](../app/src/main/java/dev/sanastasov/bybon/strong/StrongCsvMapper.kt)
- History UI [`WorkoutHistoryViewModel.kt`](../app/src/main/java/dev/sanastasov/bybon/workout/ui/history/WorkoutHistoryViewModel.kt)

---

## Strong CSV schema

Delimiter: `;`. One row per set-like event (working set, warmup, rest timer, or note). Workout-level
fields are repeated on every row.

| Column              | Type in CSV           | Meaning                                                                  |
|---------------------|-----------------------|--------------------------------------------------------------------------|
| `Workout #`         | int                   | Session identity (201…252 in the sample)                                 |
| `Date`              | `yyyy-MM-dd HH:mm:ss` | Session start                                                            |
| `Workout Name`      | string                | Free-text session/plan name (may have trailing space, e.g. `"Upper body B "`) |
| `Duration (sec)`    | int                   | Total session duration                                                   |
| `Exercise Name`     | string                | Display name only (no id)                                                |
| `Set Order`         | string enum-ish       | `W` warmup, `1`…`N` working set, `Rest Timer`, `Note`                    |
| `Weight (kg)`       | decimal or empty      | Load. `0.0` is used (bodyweight warmup / unassisted pull-up)             |
| `Reps`              | int or empty          | Reps                                                                     |
| `RPE`               | decimal or empty      | Column present; **0 uses in this sample**                                |
| `Distance (meters)` | decimal or empty      | Column present; **0 uses**                                               |
| `Seconds`           | decimal or empty      | Rest duration when `Set Order=Rest Timer`; also available for timed work |
| `Notes`             | string                | Per-exercise note when `Set Order=Note`                                  |
| `Workout Notes`     | string                | Session note (repeated per row; filled on all 52 sample workouts)        |

**Row kinds in the sample:** `Rest Timer` 854, `W` 299, working `1`/`2`/`3`/`4` 854, `Note` 46.
Every rest row has `Seconds` (`120` / `60` / `90`). No RPE, distance, or timed-work rows. Rest
seconds are consistent per exercise within a workout (never mixed 60 and 120 on the same block).

### Sample names

| Strong `Workout Name` | Sessions |
|-----------------------|----------|
| `Full body A`         | 23       |
| `Full body B`         | 23       |
| `Upper body A`        | 3        |
| `Upper body B `       | 3        |

18 unique Strong exercise names (see [Catalog mapping](#catalog-mapping-sample)).

### Example: Workout #252

- **Session:** `#252`, `2026-08-20 17:59:34`, name `"Upper body B "` (trailing space), duration
  `3367`s, notes `"Full body B without legs"`
- **Exercises (order):** Incline Bench Press (Dumbbell) → Incline Row (Dumbbell) → Incline Curl
  (Dumbbell) → Lateral Raise (Machine) → Crunch (Machine)
- **Per exercise pattern (bench):** optional `Note` → `W` warmups → working `1..N` interleaved with
  `Rest Timer` (e.g. 120s). Weight/reps live on set rows only.

---

## Bybon domain (current)

From
[`WorkoutPlan.kt`](../app/src/main/java/dev/sanastasov/bybon/workout/domain/WorkoutPlan.kt),
[`WorkoutSession.kt`](../app/src/main/java/dev/sanastasov/bybon/workout/domain/WorkoutSession.kt),
[`WorkoutExercise.kt`](../app/src/main/java/dev/sanastasov/bybon/workout/domain/WorkoutExercise.kt),
[`ExerciseSet.kt`](../app/src/main/java/dev/sanastasov/bybon/workout/domain/ExerciseSet.kt),
[`ExerciseDefinition.kt`](../app/src/main/java/dev/sanastasov/bybon/workout/domain/ExerciseDefinition.kt):

- **`ExerciseDefinition`**: stable `id`, `name`, `primaryMuscleGroup`, `equipment`. Catalog is
  in-memory ([`CatalogExercises.kt`](../app/src/main/java/dev/sanastasov/bybon/workout/domain/CatalogExercises.kt),
  32 exercises). Import can append unknown exercises to the repository list.
- **`WorkoutPlan`**: `WorkoutPlanId`, `name`, `description?`, ordered `PlanedExercise`s,
  `isArchived`. Built-ins: Full Body A/B (active), Upper Body (legacy) (archived).
- **`PlanedExercise`**: exercise + `warmupSets: Int` + prescribed `sets` + `repRange` +
  `restAfterWorkSet: Duration` (defaults to compound 2:00 / isolation 1:00 / else 0:90).
- **`WorkoutSession`**: always tied to `planId` + denormalized `planName` / `planDescription`.
  Identity is `WorkoutSessionId(planId, startedAt)` — no separate UUID, no Strong workout number.
  `WorkoutState` = `NotStarted` | `InProgress` | `Completed(duration)`.
- **`WorkoutExercise`**: definition + target `repRange` + optional `warmupSets` list (null or
  non-empty) + working `sets` + `restAfterWorkSet`.
- **`ExerciseSet`**: optional `Weight` (positive hundredths when present; `32.5` kg → `3250`;
  Strong `0.0` / empty → `null`), `reps > 0`, `SetState` (`NotStated` / `InProgress` /
  `Completed`), optional `previous` performance. **`oneRm` is null when weight is null.**
- Rest is a per-exercise duration shown between work sets (display-only; not a logged rest event).
- No RPE/RIR, distance, timed sets, per-exercise notes, or first-class session notes. Strong workout
  notes are currently stuffed into `planDescription`.

Persistence: [`BybonDatabase`](../app/src/main/java/dev/sanastasov/bybon/data/BybonDatabase.kt) is
Room v2 with `WeightEntryEntity` + `DietPhaseEntity` only.
[`WorkoutsRepositoryImpl`](../app/src/main/java/dev/sanastasov/bybon/workout/data/WorkoutsRepositoryImpl.kt)
holds exercises, plans, and sessions in `MutableStateFlow`s.

---

## Implemented import

History empty screen picks a CSV → parse → `toStrongImport(existingPlans, existingExercises)` →
`repository.importHistory(...)`. Existing plans are loaded with `WorkoutPlansFilter.AllPlans`
(archived plans participate in matching).

### 1. Exercises

Resolve each Strong `Exercise Name`:

1. Hardcoded aliases (normalized lowercase name → Bybon id) — see table below.
2. Else case-insensitive name match against the catalog (`"Bench Press (Barbell)"` →
   `"Bench Press (barbell)"`).
3. Else create a new `ExerciseDefinition`: `id = slugify(name)`, Strong display name, equipment
   inferred from the name (`barbell` / `dumbbell` / `assisted` / `machine|cable` / else bodyweight),
   muscle group `Core` if the name looks like crunch/plank/sit-up else `Other`.

Only **new** (non-catalog) exercises are returned for insert. Sample: **`Crunch (Machine)`** →
`crunch-machine` / Core / Machine.

**Strong has no equipment column.** Equipment is only words inside `Exercise Name`, usually
`Exercise (Equipment)` (`Bench Press (Barbell)`, `Chest Fly (Cable)`). A bare name (`Chest Fly`,
`Leg Press`, `Triceps Press`) is not a missing field — Strong stored whatever display string the
library used. Catalog matches and aliases take Bybon’s `equipment`; guessing happens **only** when
creating a new exercise. `Band` is not a Bybon `Equipment` value and is not in the keyword list, so
`Chest Fly (Band)` is created as Bodyweight.

### 2. Plans

Group Strong sessions by trimmed `Workout Name`. Score each existing Bybon plan:

`0.55 * nameSimilarity + 0.45 * exerciseSimilarity`, threshold **0.70**.

- Name: lowercase, strip punctuation, drop fillers (`workout`, `session`, `training`, `routine`,
  `day`), then Levenshtein. `"Full body A"` matches `"Full Body A"`.
- Exercises: 80% Jaccard on ids + 20% longest common subsequence (order-tolerant, substitution-
  tolerant). `"Full body A"` still matches if one exercise is swapped for Crunch; `"Full body A"`
  does **not** match `"Full Body B"`.

If no plan clears the threshold, create a **new plan** (today `isArchived = false`; **TODO:** archive
unmatched imports):

- `id = slugify(trimmed Strong name)` (e.g. `upper-body-a`)
- `name` = trimmed Strong name (keeps Strong casing: `"Upper body A"`)
- `description = null`
- Exercises / warmup count / work-set count / rest / observed `repRange` taken from the
  **representative** session = most common exercise-id sequence for that name (ties: first seen)

Sample with only Full Body A/B already in the repo: creates **Upper body A** and **Upper body B**.
Sessions keep the Strong exercise list even when matched to a Bybon plan (history is not rewritten
to the plan template).

### 3. Sessions

| Strong                         | Bybon session                                      |
|--------------------------------|----------------------------------------------------|
| `Date`                         | `startedAt`                                        |
| `Duration (sec)`               | `WorkoutState.Completed(duration)`                 |
| Matched/created plan           | `planId` + `planName` (Bybon name if matched)      |
| `Workout Notes`                | `planDescription` (else the plan’s description)    |
| `Workout #`                    | Used only to group rows; **not stored** (Bybon identity is `planId + startedAt`) |

Re-import appends again (**TODO:** idempotent on `WorkoutSessionId`, not Strong `Workout #`).

### 4. Sets / rest / notes

| Strong row                         | Bybon                                                                 |
|------------------------------------|-----------------------------------------------------------------------|
| `W` with weight > 0 and reps > 0   | `warmupSets` (`SetState.Completed`)                                   |
| Digit `1..N` with weight > 0, reps > 0 | Working `sets`                                                    |
| `W` or working with `Weight = 0.0` | **Dropped** (`Weight` must be positive)                               |
| `Rest Timer`                       | First `Seconds` value → `restAfterWorkSet`; rest rows themselves discarded |
| No rest rows                       | `exercise.defaultRest`                                                |
| `Note`                             | **Dropped** (including `"Rep range 11-15"`)                           |
| `RPE` / `Distance` / timed `Seconds` on sets | Parsed on the DTO, unused                                    |

On the sample this drops **29** unassisted pull-up working sets (`0.0` kg) and **15** bodyweight
Bulgarian Split Squat warmups. CSV working sets 854 → imported **825**. One Full Body A session
(#220) loses every pull-up working set.

---

## Catalog mapping (sample)

| Strong `Exercise Name`            | Bybon id                 | How                          |
|-----------------------------------|--------------------------|------------------------------|
| Bench Press (Barbell)             | `bench-press-bb`         | Normalized name              |
| Squat (Barbell)                   | `squat-bb`               | Normalized name              |
| Squat (Machine)                   | `squat-machine`          | Normalized name              |
| Pull Up (Assisted)                | `pullup-assisted`        | Normalized name              |
| Incline Bench Press (Dumbbell)    | `incline-bench-press-db` | Normalized name              |
| Incline Row (Dumbbell)            | `incline-row-db`         | Normalized name              |
| Incline Curl (Dumbbell)           | `incline-curl-db`        | Normalized name              |
| Lateral Raise (Dumbbell)          | `lateral-raise-db`       | Normalized name              |
| Lateral Raise (Machine)           | `lateral-raise-machine`  | Normalized name              |
| Skullcrusher (Dumbbell)           | `skullcrusher-db`        | Normalized name              |
| Upright Row (Dumbbell)            | `upright-row-db`         | Normalized name              |
| Leg Extension (Machine)           | `leg-extension`          | Normalized name              |
| Romanian Deadlift (Barbell)       | `rdl-bb`                 | Alias (Bybon name includes `(RDL)`) |
| Bulgarian Split Squat             | `split-squat-db`         | Alias (Bybon adds `(dumbbell)`) |
| Bicep Curl (Machine)              | `biceps-curl-machine`    | Alias (`Curl (machine)`)     |
| Seated Leg Curl (Machine)         | `seated-leg-curl`        | Normalized name              |
| Triceps Press                     | `triceps-press-machine`  | Alias                        |
| Crunch (Machine)                  | `crunch-machine`         | **Created on import**        |

Aliases live in `strongExerciseAliases` inside `StrongCsvMapper.kt` for Strong strings that are not
a case-insensitive match for the catalog (RDL, Bulgarian split squat, bicep curl, triceps press,
bare `Leg Press`, `"Dumbbell lateral raises"`, bare `Chest Fly`). Extra Strong names still become
new exercises.

Bybon catalog exercises **not** in the 52-session sample include overhead press, lat pulldown, chest
fly variants, dips, calf raise, face pull, lying leg curl, etc. Several of those **do** appear in
the full backup and now match (lat pulldown, both leg curls, leg press and chest fly via alias).

---

## Decisions vs Strong

Statuses from triage. **TODO** items are for work **before** Room workout persistence, except
persistence itself.

### Won't do

| Topic | Why it's fine |
|-------|----------------|
| Session notes in `planDescription` | No separate session-notes field needed; stuffing Strong `Workout Notes` into the denormalized plan blurb is OK |
| Rest as one `Duration` per exercise | By design. Not rest *events*, not mixed per-set rests |
| RPE / distance / timed sets | Unused in both the sample and the full 252-session export; out of domain (spec wants RIR later, not Strong RPE) |
| Plan template vs session exercises | A plan is a plan. Sessions may drop/swap exercises (busy machine, sore knee, ran out of time) |
| Import `repRange` = min..max logged reps | Fine for import; don't parse Strong “Rep range …” notes into prescription |
| Unknown-exercise metadata | Creating Crunch as Core/Machine (guessed) is OK |
| Hardcoded Strong name aliases | Keep a small map in `StrongCsvMapper` for names that will never equal the catalog string. No alias table. |

### Done

| Topic | What landed |
|-------|-------------|
| Lat pulldown hyphen | Catalog name is `Lat Pulldown (cable)` so Strong `Lat Pulldown (Cable)` matches `lat-pull-down` |
| Leg press id collision | Catalog id is `leg-press-machine`; Strong `Leg Press` aliases onto it |
| Seated vs lying leg curl | Catalog has `seated-leg-curl` and `lying-leg-curl`; Full Body A uses seated |
| `"Dumbbell lateral raises"` | Aliases to `lateral-raise-db` |
| Bare `Chest Fly` | Aliases to `chest-fly-peck-deck` (`Chest Fly (machine)`) |
| Trim Strong exercise names | Parser and created definitions strip leading/trailing spaces |
| Seed Upper Body A id | Renamed to `upper-body-legacy` / `Upper Body (legacy)` so import can own `upper-body-a` |
| `lateral-raise-machine` equipment | Catalog uses `Equipment.Machine` |
| Zero-load sets | `ExerciseSet.weight` is `Weight?`. Strong `0.0` (and blank kg) import as `null`; `Weight` stays `> 0` when present. 1RM is null when weight is. Sample keeps 29 pull-up work + 15 BSS warmups; full backup 109 rows no longer drop. |

### TODO (before Room)

| Topic | Current behavior | Intended |
|-------|------------------|----------|
| Exercise notes | `Set Order=Note` rows discarded (46 sample / 90 full) | Store per-exercise notes (rep-range hints, “Right knee slight pain”, …) |
| Idempotent import | Re-picking the CSV appends duplicate sessions | Skip sessions whose `WorkoutSessionId(planId, startedAt)` already exists. Keep Bybon identity; do **not** key off Strong `Workout #` |
| Archive unmatched plans | New plans are `isArchived = false` and show in the list | Unmatched Strong names should be created **archived**. Full backup adds ~11 extra names (`Chest, back side delts`, JE variants, Upper/Lower Body 1–2, …) on top of Upper body A/B |
| History / summary hide warmups | Warmups import; history is top **work** set, summary lists work sets only | Show warmups on those screens |

### TODO (after the above)

| Topic | Current behavior | Intended |
|-------|------------------|----------|
| Workout persistence | In-memory `MutableStateFlow`; process death loses import | Room (or equivalent) for plans, sessions, exercises. Persist the **domain** (warmup lists, rest `Duration`, weight hundredths), not Strong event rows |

---

## Superseded schema notes

The original doc locked **import fidelity (1A)** (keep notes, RPE, rest events, 0 kg) and **unmatched
names → archived plans**. Implementation only kept warmups + rest *duration*; archive-on-create is
still TODO.

The proposed Room event-row schema (`workout_set.kind`, `exercise_alias`, `strong_workout_number`,
weight tenths) is **not** the persistence target. When Room happens, persist the current domain.

---

## Full backup (252 sessions)

Same CSV schema as the sample. 8170 rows, workout #1–252, 42 unique `Exercise Name`s, 13 workout
names. Still **no** RPE, distance, or timed-work rows. Identity timestamps are unique (252 dates for
252 sessions).

| Strong `Workout Name`   | Sessions | Import vs current Bybon plans |
|-------------------------|----------|-------------------------------|
| `Full body A`           | 80       | Matches Full Body A (all 80) |
| `Full body B`           | 77       | Matches Full Body B (all 77) |
| `Chest, back side delts`| 43       | New plan `chest-back-side-delts` |
| `Lower Body 1`          | 9        | New |
| `Upper Body 1`          | 9        | New (`upper-body-1`; does **not** match seed Upper Body (legacy), score 0.64) |
| `Full body B by JE`     | 8        | New (score 0.60 vs Full Body B) |
| `Lower Body 2`          | 6        | New |
| `Upper Body 2`          | 6        | New |
| `Full body A by JE cut` | 4        | New |
| `Full body A by JE`     | 3        | New |
| `Upper body A`          | 3        | New `upper-body-a` (seed plan is now `upper-body-legacy`) |
| `Upper body B `         | 3        | New `upper-body-b` |
| `Afternoon Workout`     | 1        | New |

Zero-load rows grow from 44 in the sample to **109**: BSS warmups 62, pull-up work 34, Back
Extension work 9, BSS work 4. These import as `weight = null` (unassisted / no extra load). Sessions
that previously lost **every** work set for an exercise keep it: Back Extension #15/#17/#21, BSS
#114/#118, pull-up #189/#220.

Rest is still one duration per exercise. Unlike the sample, **20** workout+exercise blocks mix rest
lengths (typically 120s then 60s on laterals). First `Rest Timer` wins; in 16 of those the first
value is the minority. Accepted as by-design.

### Exercise resolution (full)

After catalog/alias fixes: lat pulldown, both machine leg curls, `Leg Press`, and bare `Chest Fly`
attach to catalog ids. `"Dumbbell lateral raises"` aliases to `lateral-raise-db`. Created names are
trimmed.

High-volume names that still **create** new exercises:

| Strong name | Workouts | Work sets | Created id | Inferred |
|-------------|----------|-----------|------------|----------|
| Standing Calf Raise (Barbell) | 15 | 45 | `standing-calf-raise-barbell` | Other / Barbell |
| Chest Fly (Band) | 11 | 30 | `chest-fly-band` | Other / Bodyweight |
| Cable Pushdown (rope) | 10 | 30 | `cable-pushdown-rope` | Other / Machine |
| Back Extension | 10 | 21 | `back-extension` | Other / Bodyweight |
| Triceps Extension (Cable) | 7 | 21 | `triceps-extension-cable` | Other / Machine |
| Hip Thrust (Barbell) | 6 | 18 | `hip-thrust-barbell` | Other / Barbell |
| Reverse Lunges | 6 | 18 | `reverse-lunges` | Other / Bodyweight |
| Crunch (Machine) | 6 | 12 | `crunch-machine` | Core / Machine |
| + others with ≤4 workouts | | | | |

Catalog rows that **never appear** in this export: `bench-press-db`, `chest-dip`,
`incline-bench-press-bb`.

Bare Strong `Chest Fly` aliases to `chest-fly-peck-deck` (`Chest Fly (machine)` / Machine).
`Chest Fly (Cable)` matches `chest-fly-cable`. `Chest Fly (Band)` is still created (Bodyweight —
`band` is not an infer keyword).

Bare names in this export that **lack** an equipment word in the string:

| Strong name | Import |
|-------------|--------|
| `Chest Fly` | Alias → catalog Machine |
| `Leg Press` | Alias → catalog Machine |
| `Triceps Press` | Alias → catalog Machine |
| `Bulgarian Split Squat` | Alias → catalog Dumbbell |
| `Back Extension` | Created Bodyweight |
| `Reverse Lunges` | Created Bodyweight |

Genuine new movements (Hip Thrust, Reverse Lunges, Cable Pushdown, Iso-Lateral Chest Press,
Skullcrusher (Barbell), Standing Calf Raise (Barbell), …) creating new exercises is **won't do** to
force onto the current catalog.

---

## Mapping summary

**Bybon live session:** plan (warmup count, work-set count, rep range, rest) → session →
`WorkoutExercise` with warmup list + work sets + `restAfterWorkSet`. Set lifecycle
`NotStated` / `InProgress` / `Completed`.

**Strong import (today):**

1. Parse `;` CSV into `StrongCsvRow`.
2. Resolve exercises (alias / name / create).
3. Fuzzy-match plan or insert a new plan from the most common exercise sequence (unarchived today;
   **TODO:** archive unmatched names).
4. Build `WorkoutSession(Completed)` with Strong date/duration, notes in `planDescription`, warmups
   + work sets (`Weight?` + reps; Strong `0.0` → `null` weight), rest duration from the first
   rest-timer row.
