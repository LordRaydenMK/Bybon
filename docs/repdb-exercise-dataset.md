# RepDB exercise dataset — proposal

Status: proposal only. No implementation in this change.

Snapshot reviewed: [RepDB/exercise-dataset](https://github.com/RepDB/exercise-dataset) `main` @ `9ed9357f09c7566ea0256c57ebd6374ebb8b575e` (2026-09-16). `exercises.json` is schema 3, **601** exercises.

## Request

- Exercises ship inside the app and are available on first launch, with no download step.
- The RepDB snapshot replaces the hardcoded catalog in `CatalogExercises.kt`.
- Full Body A, Full Body B, and Upper Body (legacy) keep their prescriptions and point at RepDB exercises.
- Strong CSV import still resolves exercises, matches plans, and creates sessions.
- Where an exercise’s identity, name, muscles, or equipment disagree, RepDB wins. Bybon stays the source of truth for plans, sessions, sets, and progression.

## Bybon today

Workout exercises, plans, and sessions live in memory (`WorkoutsRepositoryImpl`). Room persists body weight only, so swapping exercise ids does not migrate stored workouts.

The catalog is 32 `ExerciseDefinition` values: `id`, `name`, `primaryMuscleGroup`, `equipment`. Ids are Bybon slugs (`bench-press-bb`, `squat-bb`, `rdl-bb`). Names put the equipment in parentheses (`Bench Press (barbell)`).

`MuscleGroup` is Arms, Back, Chest, Core, FullBody, Legs, Shoulders, Other. `Equipment` is Barbell, Dumbbell, Machine, Bodyweight, AssistedBodyWeight. That enum drives progression increments (barbell 2.5 kg, dumbbell 2 kg, machine and assisted 2.5 kg, bodyweight none), default warm-up and work weights, and the library filter chips.

Default rest is a hardcoded id list: 2:00 for a compound set, 1:00 for an isolation set, 1:30 otherwise. Face pull is on the isolation list.

The exercise library groups by `MuscleGroup` and filters by muscle and equipment. There is no search and no exercise-detail screen. There is no in-app credits screen.

Strong import (`StrongCsvMapper`) resolves a Strong `Exercise Name` in this order:

1. A 7-entry alias map (`romanian deadlift (barbell)` → `rdl-bb`, and similar).
2. Case-insensitive match on the catalog display name.
3. Otherwise it creates a new `ExerciseDefinition` (`id` = slug of the Strong name, equipment guessed from words in the name, muscle `Core` or `Other`).

Plans embed a full `ExerciseDefinition` on each `PlanedExercise`, plus warm-up count, work-set count, rep range, rest, and notes.

## RepDB free tier

`exercises.json` is about 2.1 MB. Each record has `id`, `name_en` / `name_de` / `name_es`, descriptions, instructions, and tips in those three languages, `category`, `force_type`, `mechanic` (`compound` / `isolation`), `difficulty`, `equipment` (absent on bodyweight moves), `body_part`, `primary_muscles`, `secondary_muscles`, `goals`, `tags`, `met`, `is_unilateral`, `is_bodyweight`, and `images.flat`.

| Field | Counts |
| --- | --- |
| `category` | strength 491, stretching 76, cardio 16, olympic 14, plyometrics 4 |
| `mechanic` | compound 416, isolation 185 |
| `body_part` | upper_legs 156, back 104, shoulders 73, core 70, upper_arms 65, chest 61, full_body 43, lower_legs 18, lower_arms 11 |
| `equipment` | absent (bodyweight) 179; otherwise ~50 slugs, led by dumbbell 79, barbell 68, kettlebell 61, cable 27 |

Images under `images/flat/` are 512×512 WebP: **1056** files, **16.7 MB**. Most exercises have `start` and `peak`; 134 have a single `main`. A few variants share a file. `images/equipment/` (55 files, 0.3 MB) and `images/muscles/` (27 files, 0.2 MB) are separate icon sets.

License ([LICENSE-DATA.md](https://github.com/RepDB/exercise-dataset/blob/main/LICENSE-DATA.md)): free in-app use, including commercial; visible attribution **"Exercise data by RepDB (repdb.co)"** in an about/credits screen, the project README, or a website footer; no redistribution as a dataset or API; `premium-samples/` is evaluation-only and stays out. Bybon’s GitHub repo is public.

## What this change would do

These follow from the request. They stay out of the code until the open questions below are answered.

1. **Bundle a pinned snapshot in the APK.** Parse it at startup into the existing in-memory exercise list. No network fetch, no Room table for the catalog. Record the RepDB commit SHA next to the asset so a later refresh is a deliberate bump.
2. **Delete `catalogExercises`.** Exercise `id` becomes the RepDB id. Display name becomes `name_en`. Plans, the library, and Strong resolution all read that list.
3. **Retarget the three built-in plans** once each current exercise has a confirmed RepDB id. Set counts, rep ranges, and warm-up counts stay as they are in `WorkoutPlans.kt`. Plan ids and names stay (`full-body-a`, `Full Body A`, and the legacy upper-body plan).
4. **Retarget Strong import** at that same list: alias, then case-insensitive match on `name_en`, then the unmatched-name rule from question 6. Plan fuzzy-matching stays. Session identity stays `planId + startedAt`.
5. **Keep prescription and progression in Bybon.** RepDB does not define sets, reps, rest, or load increments. `mechanic` is the input available for default rest (question 5).

`ExerciseDefinition` grows only with fields the UI or progression actually needs. The raw JSON can stay the asset; the domain type stays the type plans and sessions already embed.

## Name mismatch (why Strong aliases have to grow)

RepDB puts equipment in the name and uses its own word order. After the switch, case-insensitive equality with Strong’s `Exercise Name` fails for almost every current catalog hit.

| Strong `Exercise Name` | RepDB `name_en` | RepDB `id` |
| --- | --- | --- |
| Bench Press (Barbell) | Barbell Bench Press | `bench-press` |
| Squat (Barbell) | Barbell Back Squat | `squat` |
| Incline Bench Press (Dumbbell) | Incline Dumbbell Press | `incline-db-press` |
| Romanian Deadlift (Barbell) | Romanian Deadlift | `romanian-deadlift` |
| Lat Pulldown (Cable) | Lat Pulldown | `lat-pulldown` |
| Pull Up (Assisted) | Assisted Pull Ups | `assisted-pull-ups` |
| Skullcrusher (Dumbbell) | Dumbbell Skull Crusher | `db-skull-crusher` |
| Lateral Raise (Dumbbell) | Dumbbell Lateral Raise | `lateral-raise` |
| Overhead Press (Barbell) | Barbell Overhead Press | `ohp` |
| Deadlift (Barbell) | Barbell Deadlift | `deadlift` |

A few Strong strings already equal `name_en`: `Bulgarian Split Squat` (`bulgarian-split-squat`), `Leg Press` (`leg-press`), `Seated Leg Curl` (`seated-leg-curl`), `Back Extension` (`back-extension`).

The implementation should include a reviewed Strong-name → RepDB-id table covering the names in the sample and the full backup described in `docs/strong-import.md`, not a guess inside the matcher.

## Plan exercises

Clear RepDB counterparts (same movement and equipment):

| Current id | RepDB id | RepDB name |
| --- | --- | --- |
| `bench-press-bb` | `bench-press` | Barbell Bench Press |
| `incline-bench-press-bb` | `incline-bench-press` | Incline Barbell Bench Press |
| `bench-press-db` | `db-bench-press` | Dumbbell Bench Press |
| `incline-bench-press-db` | `incline-db-press` | Incline Dumbbell Press |
| `chest-press-machine` | `chest-press-machine` | Machine Chest Press |
| `chest-fly-cable` | `cable-fly` | Cable Fly |
| `squat-bb` | `squat` | Barbell Back Squat |
| `rdl-bb` | `romanian-deadlift` | Romanian Deadlift |
| `split-squat-db` | `bulgarian-split-squat` | Bulgarian Split Squat |
| `seated-leg-curl` | `seated-leg-curl` | Seated Leg Curl |
| `lying-leg-curl` | `leg-curl` | Lying Leg Curl |
| `leg-press-machine` | `leg-press` | Leg Press |
| `leg-extension` | `leg-extension` | Leg Extension |
| `standing-calf-raise-machine` | `standing-calf-raise` | Standing Calf Raise |
| `lateral-raise-db` | `lateral-raise` | Dumbbell Lateral Raise |
| `lateral-raise-cable` | `cable-lateral-raise` | Cable Lateral Raise |
| `upright-row-db` | `dumbbell-upright-row` | Dumbbell Upright Row |
| `overhead-press-bb` | `ohp` | Barbell Overhead Press |
| `deadlift-barbell` | `deadlift` | Barbell Deadlift |
| `pullup-assisted` | `assisted-pull-ups` | Assisted Pull Ups |
| `lat-pull-down` | `lat-pulldown` | Lat Pulldown |
| `incline-curl-db` | `incline-db-curl` | Incline Dumbbell Curl |
| `skullcrusher-db` | `db-skull-crusher` | Dumbbell Skull Crusher |

No equally named RepDB exercise exists for the rows below. Candidates are listed under question 4.

| Current id | Current name | Used by |
| --- | --- | --- |
| `incline-row-db` | Incline Row (dumbbell) | Full Body B, Upper Body (legacy) |
| `iso-lat-row` | Iso-Lateral Row (machine) | catalog only |
| `squat-machine` | Squat (machine) | catalog only; Strong `Squat (Machine)` |
| `triceps-press-machine` | Triceps Press (machine) | catalog only; Strong `Triceps Press` |
| `chest-dip` | Chest Dip (assisted) | catalog only |
| `chest-fly-peck-deck` | Chest Fly (machine) | catalog only; Strong bare `Chest Fly` |
| `biceps-curl-machine` | Curl (machine) | catalog only; Strong `Bicep Curl (Machine)` |
| `lateral-raise-machine` | Lateral Raise (machine) | catalog only; Strong `Lateral Raise (Machine)` |
| `face-pull` | Face Pull (cable) | catalog only; RepDB id `face-pull` exists, and its `mechanic` is `compound` |

## Open questions

Implementation waits on these.

1. **Which exercises ship?** All 601, or a subset (for example strength only, 491)? Stretching, cardio, olympic lifts, and plyometrics are in the same file.

2. **Which RepDB fields does the app use in this change?** The domain today needs id, name, a muscle group, and an equipment value. Also in the file: images, instructions, tips, secondary muscles, `mechanic`, `difficulty`, `body_part`, equipment slug, goals, tags, MET, and German/Spanish text. The spec asks for secondary muscle groups. The app is English-only and has no exercise-detail screen.

3. **Images.** Flat art is 16.7 MB in the APK if it ships. Show it in the library (and where else), or leave images out of this change?

4. **Which RepDB exercise replaces each ambiguous Bybon exercise?**
   - Incline Row (dumbbell): `chest-supported-db-row` (Chest-Supported Dumbbell Row), `bent-over-db-row`, or `single-arm-db-row`?
   - Iso-Lateral Row (machine): no plate-loaded iso-lateral row. Nearest are `seated-cable-row` and `t-bar-row`. Drop it from the catalog (it is not on a built-in plan)?
   - Squat (machine): `hack-squat` or `smith-machine-squat`?
   - Triceps Press (machine): `machine-triceps-extension`?
   - Chest Dip (assisted): `assisted-dips` (Machine Assisted Dips, `body_part` upper_arms) or `dips` (Chest Dips, dip station, unassisted)?
   - Chest Fly (machine), which Strong’s bare `Chest Fly` aliases to today: `pec-deck` or `machine-chest-fly`?
   - Curl (machine): `machine-bicep-curl` or `machine-preacher-curl`?
   - Lateral Raise (machine): `plate-loaded-lateral-raise`?
   - Strong names with no exact RepDB row: `Chest Fly (Band)` (no band fly), `Cable Pushdown (rope)` (closest `tricep-pushdown`), `Triceps Extension (Cable)`, `Reverse Lunges` (`reverse-lunge` is dumbbell; bodyweight and barbell variants also exist), `Crunch (Machine)` (closest `machine-seated-crunch`).

5. **Rest.** Use RepDB `mechanic` for the default (compound 2:00, isolation 1:00), or keep a Bybon list? `face-pull` would move from 1:00 to 2:00 if mechanic wins. Cable pushdowns are mixed in RepDB (`tricep-pushdown` isolation, `single-arm-tricep-pushdown` compound).

6. **Strong names that still match nothing.** Today those become new exercises with guessed muscle and equipment. Options: keep creating them; require an alias and skip or fail the row; alias every name from the known backup and keep creating only genuine unknowns. This is the conflict between “Strong import still works” and “RepDB is the source of truth.”

7. **Equipment model.** Progression and the library filters use five Bybon values. RepDB has ~50 equipment slugs (kettlebell, cable, ez_bar, smith_machine, bands, and individual machines). Options: map each slug onto the existing five for increments and filters, and keep the RepDB slug for display; or replace `Equipment` with the RepDB slug and define an increment per slug. Cable is `Machine` today (2.5 kg). Kettlebell has no Bybon value.

8. **Muscle groups.** Map `body_part` onto the current eight (`upper_arms` + `lower_arms` → Arms, `upper_legs` + `lower_legs` → Legs), or group the library by RepDB `body_part` (9) or by `primary_muscles` (~27)? `lat-pulldown` is `back`. `romanian-deadlift` is `upper_legs` (Bybon files RDL under Legs). `face-pull` is `shoulders`. `assisted-dips` is `upper_arms`.

9. **Library UX.** With the coarse groups, Legs is on the order of 170 exercises, and the screen has no search. Is a name search part of this change?

10. **How the files get into the build.** In-app use is allowed. Republishing the dataset from a public repo is not. Bybon is public. Options: download the pinned commit at build time and keep it out of git; commit the JSON and WebPs into this repo anyway. Attribution has to land in the README, an in-app credits surface, or both. There is no credits screen today.

11. **Languages.** Display `name_en` only, and leave `de` / `es` unused in the bundled file?

## Left as they are

Set and rep prescriptions, warm-up counts, plan names, Strong plan matching, session identity, and the in-memory workout repository. Paid-tier animations and `premium-samples/` stay out. Workout Room persistence stays a separate task.
