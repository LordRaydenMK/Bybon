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
  in-memory: the filtered RepDB subset generated at build time, plus four Bybon-only rows in
  [`CatalogExercises.kt`](../app/src/main/java/dev/sanastasov/bybon/workout/domain/CatalogExercises.kt)
  (386 exercises). Import can append unknown exercises to the repository list.
- **`WorkoutPlan`**: `WorkoutPlanId`, `name`, `description?`, ordered `PlanedExercise`s,
  `isArchived`. Built-ins: Full Body A/B (active), Upper Body (legacy) (archived).
- **`PlanedExercise`**: exercise + `warmupSets: Int` + prescribed `sets` + `repRange` +
  `restAfterWorkSet: Duration` (defaults to compound 2:00 / isolation 1:00 / else 0:90) +
  `notes: List<String>`.
- **`WorkoutSession`**: always tied to `planId` + denormalized `planName` + `note?`.
  Identity is `WorkoutSessionId(planId, startedAt)` — no separate UUID, no Strong workout number.
  `WorkoutState` = `NotStarted` | `InProgress` | `Completed(duration)`. Live start copies
  `WorkoutPlan.description` into `note`.
- **`WorkoutExercise`**: definition + target `repRange` + optional `warmupSets` list (null or
  non-empty) + working `sets` + `restAfterWorkSet` + `notes: List<String>`.
- **`ExerciseSet`**: optional `Weight` (positive hundredths when present; `32.5` kg → `3250`;
  Strong `0.0` / empty → `null`), `reps > 0`, `SetState` (`NotStated` / `InProgress` /
  `Completed`), optional `previous` performance. **`oneRm` is null when weight is null.**
- Rest is a per-exercise duration shown between work sets (display-only; not a logged rest event).
- No RPE/RIR, distance, or timed sets. Session `note` and exercise `notes` are first-class.

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
2. Else case-insensitive name match against the catalog (`"Bulgarian Split Squat"` matches
   `Bulgarian Split Squat`).
3. Else the name is left out. The sets are not imported, and no exercise is created.

Strong has no muscle, equipment, or mechanic column. A name that does not resolve keeps the catalog’s
values when an alias or name match hits, and is skipped when it does not. The sample’s
`Crunch (Machine)` aliases to shipped `machine-seated-crunch`, so that file inserts no new exercises.

### 2. Plans

Group Strong sessions by trimmed `Workout Name`. Score each existing Bybon plan:

`0.55 * nameSimilarity + 0.45 * exerciseSimilarity`, threshold **0.70**.

- Name: lowercase, strip punctuation, drop fillers (`workout`, `session`, `training`, `routine`,
  `day`), then Levenshtein. `"Full body A"` matches `"Full Body A"`.
- Exercises: 80% Jaccard on ids + 20% longest common subsequence (order-tolerant, substitution-
  tolerant). `"Full body A"` still matches if one exercise is swapped for Crunch; `"Full body A"`
  does **not** match `"Full Body B"`.

If no plan clears the threshold, create a **new plan**. Archive it when that Strong name was executed
fewer than 5 times (`isArchived = true`); names with 5 or more sessions stay active:

- `id = slugify(trimmed Strong name)` (e.g. `upper-body-a`)
- `name` = trimmed Strong name (keeps Strong casing: `"Upper body A"`)
- `description` = most frequent non-empty `Workout Notes` for that name (last workout on a tie)
- Exercises / warmup count / work-set count / rest / observed `repRange` / `notes` taken from the
  **representative** session = most common exercise-id sequence for that name (ties: first seen)

Sample with only Full Body A/B already in the repo: creates **Upper body A** and **Upper body B**,
both archived (3 sessions each). The import summary counts how many of the inserted plans are
archived.
Sessions keep the Strong exercise list even when matched to a Bybon plan (history is not rewritten
to the plan template).

### 3. Sessions

| Strong                         | Bybon session                                      |
|--------------------------------|----------------------------------------------------|
| `Date`                         | `startedAt`                                        |
| `Duration (sec)`               | `WorkoutState.Completed(duration)`                 |
| Matched/created plan           | `planId` + `planName` (Bybon name if matched)      |
| `Workout Notes`                | Session `note`. New plans also take the most frequent value as `WorkoutPlan.description` (last workout on a tie). Existing Bybon plans keep their description. |
| `Workout #`                    | Used only to group rows; **not stored** (Bybon identity is `planId + startedAt`) |

`importHistory` skips a session when `WorkoutSessionId(planId, startedAt)` is already stored and leaves that session unchanged. Strong `Workout #` is not read at insert time. Two rows in one file that resolve to the same id keep the first. Plans and exercises already stored under the same id are not inserted again. The summary counts inserted sessions, how many of the inserted plans are archived, and when any sessions were skipped it also shows how many were already in history.

### 4. Sets / rest / notes

| Strong row                         | Bybon                                                                 |
|------------------------------------|-----------------------------------------------------------------------|
| `W` with reps > 0                  | `warmupSets` (`SetState.Completed`); `0.0` kg → `weight = null`       |
| Digit `1..N` with reps > 0         | Working `sets`; `0.0` kg → `weight = null`                            |
| `W` or working with `reps` missing/`0` | **Dropped**                                                       |
| `Rest Timer`                       | First `Seconds` value → `restAfterWorkSet`; rest rows themselves discarded |
| No rest rows                       | `exercise.defaultRest`                                                |
| `Note`                             | One item in that exercise’s `notes: List<String>` |
| `RPE` / `Distance` / timed `Seconds` on sets | Parsed on the DTO, unused                                    |

Sample zero-load rows are kept: **29** unassisted pull-up working sets and **15** bodyweight
Bulgarian Split Squat warmups. CSV working sets 854 → imported **854**. Workout #220 still has
pull-ups.

---

## Catalog mapping (sample)

| Strong `Exercise Name`            | Bybon id                    | How                          |
|-----------------------------------|-----------------------------|------------------------------|
| Bench Press (Barbell)             | `bench-press`               | Alias                        |
| Squat (Barbell)                   | `squat`                     | Alias (`Barbell Back Squat`) |
| Squat (Machine)                   | `squat-machine`             | Alias                        |
| Pull Up (Assisted)                | `assisted-pull-ups`         | Alias                        |
| Incline Bench Press (Dumbbell)    | `incline-db-press`          | Alias                        |
| Incline Row (Dumbbell)            | `chest-supported-db-row`    | Alias                        |
| Incline Curl (Dumbbell)           | `incline-db-curl`           | Alias                        |
| Lateral Raise (Dumbbell)          | `lateral-raise`             | Alias                        |
| Lateral Raise (Machine)           | `lateral-raise-machine`     | Alias                        |
| Skullcrusher (Dumbbell)           | `db-skull-crusher`          | Alias                        |
| Upright Row (Dumbbell)            | `dumbbell-upright-row`      | Alias                        |
| Leg Extension (Machine)           | `leg-extension`             | Alias                        |
| Romanian Deadlift (Barbell)       | `romanian-deadlift`         | Alias                        |
| Bulgarian Split Squat             | `bulgarian-split-squat`     | Alias                        |
| Bicep Curl (Machine)              | `machine-bicep-curl`        | Alias                        |
| Seated Leg Curl (Machine)         | `seated-leg-curl`           | Alias                        |
| Triceps Press                     | `triceps-press-machine`     | Alias                        |
| Crunch (Machine)                  | `machine-seated-crunch`     | Alias                        |

Aliases live in `strongExerciseAliases` inside `StrongCsvMapper.kt`. RepDB word order differs from
Strong (`Bench Press (Barbell)` vs `Barbell Bench Press`), so the sample names and the names called
out in the RepDB proposal are aliased. A Strong name with no alias and no case-insensitive
`name_en` match is left out of the import.

Bybon catalog exercises **not** in the 52-session sample include overhead press, lat pulldown, chest
fly variants, dips, calf raise, face pull, lying leg curl, etc. Several of those **do** appear in
the full backup and now match (lat pulldown, both leg curls, leg press and chest fly via alias).

---

## Decisions vs Strong

Statuses from triage. Workout persistence is the remaining TODO.

### Won't do

| Topic | Why it's fine |
|-------|----------------|
| Rest as one `Duration` per exercise | By design. Not rest *events*, not mixed per-set rests |
| RPE / distance / timed sets | Unused in both the sample and the full 252-session export; out of domain (spec wants RIR later, not Strong RPE) |
| Plan template vs session exercises | A plan is a plan. Sessions may drop/swap exercises (busy machine, sore knee, ran out of time) |
| Import `repRange` = min..max logged reps | Fine for import; don't parse Strong “Rep range …” notes into prescription |
| Unknown-exercise metadata | Strong has no muscle, equipment, or mechanic. Unmatched names are left out |
| Hardcoded Strong name aliases | Keep a small map in `StrongCsvMapper` for names that will never equal the catalog string. No alias table. |

### Done

| Topic | What landed |
|-------|-------------|
| Lat pulldown hyphen | Strong `Lat Pulldown (Cable)` aliases to `lat-pulldown` |
| Leg press id collision | Strong `Leg Press` aliases to `leg-press` |
| Seated vs lying leg curl | Catalog has `seated-leg-curl` and `lying-leg-curl`; Full Body A uses seated |
| `"Dumbbell lateral raises"` | Aliases to `lateral-raise` |
| Bare `Chest Fly` | Aliases to `pec-deck` |
| Trim Strong exercise names | Parser strips leading/trailing spaces before alias and name lookup |
| Seed Upper Body A id | Renamed to `upper-body-legacy` / `Upper Body (legacy)` so import can own `upper-body-a` |
| `lateral-raise-machine` equipment | Catalog uses `Equipment.Machine` |
| Zero-load sets | `ExerciseSet.weight` is `Weight?`. Strong `0.0` (and blank kg) import as `null`; `Weight` stays `> 0` when present. 1RM is null when weight is. Sample keeps 29 pull-up work + 15 BSS warmups; full backup 109 rows no longer drop. |
| Session and exercise notes | `Workout Notes` → session `note`. New plan `description` is the most common `Workout Notes` (last on a tie). Each `Set Order=Note` row is one item in `notes`. Existing Bybon plans keep their description and planned-exercise notes. |
| Idempotent import | `importHistory` skips sessions whose `WorkoutSessionId(planId, startedAt)` already exists and does not replace them. The same id inside one file keeps the first session. Strong `Workout #` stays a CSV grouping key. The summary counts inserted sessions and, when some were skipped, how many were already stored. |
| Archive rare unmatched plans | New plans whose Strong name was executed fewer than 5 times are created `isArchived = true`. Names with 5 or more sessions stay in the active list. Matched Bybon plans keep their archive flag. The import summary shows how many inserted plans were archived. Sample Upper body A/B (3 each) are archived. A full backup archives the five names under 5 sessions (`Full body A by JE cut`, `Full body A by JE`, `Upper body A`, `Upper body B`, `Afternoon Workout`) and keeps the six busier new names active. |
| History and summary show warmups | History still headlines each exercise with its top working set (heaviest, then most reps) and lists that exercise's warmups underneath: circled `w`, load × reps, no 1RM. The workout summary lists warmups first, then working sets numbered from 1, with the same warmup row and no checkboxes. The import summary still counts working sets only. |

### TODO

| Topic | Current behavior | Intended |
|-------|------------------|----------|
| Workout persistence | In-memory `MutableStateFlow`; process death loses import | Room (or equivalent) for plans, sessions, exercises. Persist the **domain** (warmup lists, rest `Duration`, weight hundredths), not Strong event rows |

---

## Superseded schema notes

The original doc locked **import fidelity (1A)** (keep notes, RPE, rest events, 0 kg) and **unmatched
names → archived plans**. Implementation kept warmups + rest *duration*, and archives unmatched
names used fewer than 5 times.

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
| `Full body A by JE cut` | 4        | New, archived (< 5 sessions) |
| `Full body A by JE`     | 3        | New, archived (< 5 sessions) |
| `Upper body A`          | 3        | New `upper-body-a`, archived (seed plan is `upper-body-legacy`) |
| `Upper body B `         | 3        | New `upper-body-b`, archived |
| `Afternoon Workout`     | 1        | New, archived (< 5 sessions) |

New plans with fewer than 5 sessions are created archived: `Full body A by JE cut`, `Full body A by
JE`, `Upper body A`, `Upper body B`, `Afternoon Workout`. The other new names (43 down to 6
sessions) stay active.

Zero-load rows grow from 44 in the sample to **109**: BSS warmups 62, pull-up work 34, Back
Extension work 9, BSS work 4. These import as `weight = null` (unassisted / no extra load). Sessions
that previously lost **every** work set for an exercise keep it: Back Extension #15/#17/#21, BSS
#114/#118, pull-up #189/#220.

Rest is still one duration per exercise. Unlike the sample, **20** workout+exercise blocks mix rest
lengths (typically 120s then 60s on laterals). First `Rest Timer` wins; in 16 of those the first
value is the minority. Accepted as by-design.

### Exercise resolution (full)

After catalog/alias fixes: lat pulldown, both machine leg curls, `Leg Press`, and bare `Chest Fly`
attach to catalog ids. `"Dumbbell lateral raises"` aliases to `lateral-raise`. `Crunch (Machine)`
aliases to `machine-seated-crunch`. `Reverse Lunges` aliases to `reverse-lunge`. `Back Extension`
matches the shipped name. Created names are trimmed.

Names that match neither an alias nor the catalog are left out of the import. In this export that
includes Standing Calf Raise (Barbell), Chest Fly (Band), Cable Pushdown (rope), Triceps Extension
(Cable), and Hip Thrust (Barbell).

Catalog rows that **never appear** in this export: `db-bench-press`, `assisted-dips`,
`incline-bench-press`.

Bare Strong `Chest Fly` aliases to `pec-deck`. `Chest Fly (Cable)` aliases to `cable-fly`.
`Chest Fly (Band)` does not resolve and is left out.

| Strong name | Import |
|-------------|--------|
| `Chest Fly` | Alias → catalog Machine |
| `Leg Press` | Alias → catalog Machine |
| `Triceps Press` | Alias → catalog Machine |
| `Bulgarian Split Squat` | Alias → catalog Dumbbell |
| `Back Extension` | Name match → catalog |
| `Reverse Lunges` | Alias → `reverse-lunge` |

---

## Mapping summary

**Bybon live session:** plan (warmup count, work-set count, rep range, rest) → session →
`WorkoutExercise` with warmup list + work sets + `restAfterWorkSet`. Set lifecycle
`NotStated` / `InProgress` / `Completed`.

**Strong import (today):**

1. Parse `;` CSV into `StrongCsvRow`.
2. Resolve exercises (alias / name). Unmatched names are left out.
3. Fuzzy-match plan or insert a new plan from the most common exercise sequence. Unmatched names
   executed fewer than 5 times are archived; busier names stay active. The summary counts archived
   plans.
4. Build `WorkoutSession(Completed)` with Strong date/duration, that session’s `Workout Notes` in
   `note`, warmups + work sets (`Weight?` + reps; Strong `0.0` → `null` weight), rest duration from
   the first rest-timer row, and `Set Order=Note` rows as `notes`.
5. `importHistory` appends only plans, exercises, and sessions whose ids are not already stored.
   Session identity is `WorkoutSessionId(planId, startedAt)`. A repeat of the same file inserts nothing.
