# Strong CSV ↔ Bybon

Living comparison of Strong’s CSV export vs Bybon’s workout domain **as implemented today**.
Import is live (History empty-state → file picker). Workout data is still in-memory — Room only
persists body-weight tables.

Reference export in repo:
[`app/src/test/resources/strong-backup-sample.csv`](../app/src/test/resources/strong-backup-sample.csv)
(52 sessions, Strong workout #201–252, 2053 rows, 2026-02-17 → 2026-08-20).

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
  31 exercises). Import can append unknown exercises to the repository list.
- **`WorkoutPlan`**: `WorkoutPlanId`, `name`, `description?`, ordered `PlanedExercise`s,
  `isArchived`. Built-ins: Full Body A/B (active), Upper Body A (archived).
- **`PlanedExercise`**: exercise + `warmupSets: Int` + prescribed `sets` + `repRange` +
  `restAfterWorkSet: Duration` (defaults to compound 2:00 / isolation 1:00 / else 0:90).
- **`WorkoutSession`**: always tied to `planId` + denormalized `planName` / `planDescription`.
  Identity is `WorkoutSessionId(planId, startedAt)` — no separate UUID, no Strong workout number.
  `WorkoutState` = `NotStarted` | `InProgress` | `Completed(duration)`.
- **`WorkoutExercise`**: definition + target `repRange` + optional `warmupSets` list (null or
  non-empty) + working `sets` + `restAfterWorkSet`.
- **`ExerciseSet`**: `Weight` (positive hundredths of a kg; `32.5` kg → `3250`), `reps > 0`,
  `SetState` (`NotStated` / `InProgress` / `Completed`), optional `previous` performance. **Weight
  cannot be 0.**
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
| Seated Leg Curl (Machine)         | `leg-curl`               | Alias (`Leg Curl (machine)`) |
| Triceps Press                     | `triceps-press-machine`  | Alias                        |
| Crunch (Machine)                  | `crunch-machine`         | **Created on import**        |

Aliases live in `strongExerciseAliases` inside `StrongCsvMapper.kt`. They exist only because five
Strong strings are not a case-insensitive match for the Bybon catalog name (extra `(RDL)`, missing
`(dumbbell)` / `(machine)`, “Bicep Curl” vs “Curl”, “Seated Leg Curl” vs “Leg Curl”). The sample
fully resolves; Crunch is created rather than aliased.

Bybon catalog exercises **not** in this sample include overhead press, lat pull-down, chest fly
variants, dips, calf raise, face pull, etc.

---

## Decisions vs Strong

Statuses from triage. **TODO** items are for work **before** Room workout persistence, except
persistence itself.

### Won't do

| Topic | Why it's fine |
|-------|----------------|
| Session notes in `planDescription` | No separate session-notes field needed; stuffing Strong `Workout Notes` into the denormalized plan blurb is OK |
| Rest as one `Duration` per exercise | By design. Not rest *events*, not mixed per-set rests |
| RPE / distance / timed sets | Unused in this export; out of domain (spec wants RIR later, not Strong RPE) |
| Plan template vs session exercises | A plan is a plan. Sessions may drop/swap exercises (busy machine, sore knee, ran out of time) |
| Import `repRange` = min..max logged reps | Fine for import; don't parse Strong “Rep range …” notes into prescription |
| Unknown-exercise metadata | Creating Crunch as Core/Machine (guessed) is OK |

### TODO (before Room)

| Topic | Current behavior | Intended |
|-------|------------------|----------|
| Zero-load sets | `Weight` must be `> 0`; importer drops `0.0` kg rows | Keep unassisted pull-ups / BW warmups (29 pull-up work sets + 15 BSS warmups lost in the sample; #220 loses every pull-up work set) |
| Exercise notes | `Set Order=Note` rows discarded (46 in sample) | Store per-exercise notes (rep-range hints, “Right knee slight pain”) |
| Idempotent import | Re-picking the CSV appends duplicate sessions | Skip sessions whose `WorkoutSessionId(planId, startedAt)` already exists. Keep Bybon identity; do **not** key off Strong `Workout #` |
| Archive unmatched plans | New plans are `isArchived = false` and show in the list | Unmatched Strong names (Upper body A/B) should be created **archived** |
| Remove/rename built-in Upper Body A | Seed plan `upper-body-a` is archived but still occupies the id Strong will slug | Temporary plan — remove or rename it so import can own `upper-body-a` |
| History / summary hide warmups | Warmups import; history is top **work** set, summary lists work sets only | Show warmups on those screens |
| `lateral-raise-machine` equipment | Catalog uses `Equipment.Dumbbell` | Bug: should be `Machine` (increments / default warmup follow the wrong equipment) |

### TODO (after the above)

| Topic | Current behavior | Intended |
|-------|------------------|----------|
| Workout persistence | In-memory `MutableStateFlow`; process death loses import | Room (or equivalent) for plans, sessions, exercises. Persist the **domain** (warmup lists, rest `Duration`, weight hundredths), not Strong event rows |

### Open — hardcoded aliases

Not a sample bug: all 18 Strong names resolve (13 by case-insensitive catalog name, 5 via the map,
Crunch created). The map is only needed when the Strong string ≠ Bybon catalog string ignoring case.

This is only a product question if we want either:

- Users to map a Strong name onto an existing catalog exercise without a code change, or
- A larger Strong export whose names aren't in the map / catalog

Otherwise leave the map in `StrongCsvMapper`. No `exercise_alias` table required.

---

## Superseded schema notes

The original doc locked **import fidelity (1A)** (keep notes, RPE, rest events, 0 kg) and **unmatched
names → archived plans**. Implementation only kept warmups + rest *duration*; archive-on-create is
still TODO.

The proposed Room event-row schema (`workout_set.kind`, `exercise_alias`, `strong_workout_number`,
weight tenths) is **not** the persistence target. When Room happens, persist the current domain.

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
   + work sets with positive weight/reps, rest duration from the first rest-timer row.
