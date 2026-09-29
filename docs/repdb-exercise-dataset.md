# RepDB exercise dataset — proposal

Status: proposal only. No implementation in this change.

Snapshot pin: [RepDB/exercise-dataset](https://github.com/RepDB/exercise-dataset) `9ed9357f09c7566ea0256c57ebd6374ebb8b575e` (2026-09-16). `exercises.json` schema 3, 601 exercises in the full file.

## Decisions

- Ship a **subset**, built at compile time from that pinned file. The file is downloaded by Gradle and filtered into a build asset. It is not committed.
- English only (`name_en`, `description_en`, `instructions_en`, `tips_en`). German and Spanish are dropped at build time.
- Images are **URLs**, loaded at runtime. The APK does not contain the WebPs.
- Exercise detail screen, library search, and the about screen come later.
- README gets the credit line `Exercise data by RepDB (repdb.co)` when the data is wired in. The free-tier license asks for that credit on any in-app use; the README line covers it.
- Strong import matches a name onto a shipped RepDB exercise when one exists. Aliases cover the names in the backup. A Strong name with no shipped exercise becomes an active exercise only after 5 workouts in the file. Below that, the sets stay in history on an archived exercise, and the library does not list it.
- Kettlebell, `loop_band`, and `resistance_band` are excluded. Other equipment stays in the catalog. Splitting new equipment values (cable, EZ-bar, Smith, and the rest) waits for a later pass.
- Yoga and Pilates are excluded by English name. See the catalog filter.
- An exercise stores `bodyPart` and the muscle lists. `bodyPart` is RepDB's nine regions and is what the library groups by. `primaryMuscles` and `secondaryMuscles` stay as the more specific list. One is not derived from the other.
- `force_type`, `difficulty`, and `tags` are stored on the exercise. `met` is not.
- The library is three sources: the RepDB subset, a few exercises Bybon ships because RepDB has no match, and exercises the user creates. Bybon-shipped and user-created exercises use the same definition as a RepDB row, with RepDB-only fields empty (images, instructions, tips, tags, and the rest).
- Bybon ships these exercises because RepDB has no match. Strong names for them alias onto these ids.
  - `iso-lat-row`, Iso-Lateral Row (machine). Pin-loaded row, one arm or both. Not Seated Cable Row.
  - `squat-machine`, Squat (machine). Plate-loaded squat. Not Hack Squat or Smith Machine Squat.
  - `triceps-press-machine`, Triceps Press (machine). Not Machine Triceps Extension or a cable pushdown.
  - `lateral-raise-machine`, Lateral Raise (machine). Pin-loaded. Not Plate-Loaded Lateral Raise. Strong `Lateral Raise (Machine)` aliases here.

## Catalog filter

An exercise is included when all of these hold:

1. `category` is `strength`.
2. `goals` contains `hypertrophy` or `strength` (many list both).
3. `equipment` is not `kettlebell`, `loop_band`, or `resistance_band`.
4. The English name does not mark the exercise as yoga or Pilates. A name matches when it contains `Pilates`, `Pose`, `Warrior`, `Downward Dog`, `Upward Dog`, `Crescent Lunge`, or `Shoulderstand`.

That is **382** exercises. The name rule removes 28: 23 yoga asanas and 5 Pilates exercises. Bird Dog stays. Four more Pilates exercises (`Pilates Roll Down`, `Pilates Saw`, `Pilates Spine Stretch Forward`, `Pilates Spine Twist`) are stretches with a mobility goal, so the category and goal rules already drop them.

Yoga names removed: Boat Pose, Bow Pose, Chair Pose, Crescent Lunge, Crow Pose, Dancer Pose, Dolphin Pose, Downward Dog Knee Tuck, Downward Dog to Knee Drive, Downward Dog to Plank, Downward Dog to Upward Dog, Eagle Pose, Extended Side Angle Pose, Half Moon Pose, Locust Pose, Revolved Chair Pose, Revolved Crescent Lunge, Supported Shoulderstand, Three-Legged Downward Dog, Tree Pose, Warrior I, Warrior II, Warrior III.

Pilates names removed: Pilates Kneeling Side Kick, Pilates Leg Pull Back, Pilates Leg Pull Front, Pilates Roll Over, Pilates Side Bend.

`category` and `goals` are different fields. `category` is the kind of movement (`strength`, `stretching`, `cardio`, `olympic`, `plyometrics`). `goals` is why someone would do it, and an exercise can list several.

This filter drops:

- Stretching (76), plyometrics (4), and cardio except as noted below.
- All 14 olympic lifts. They are `category: olympic` with goals `power` + `strength` (Clean, Snatch, Push Jerk, and so on). Nine of them are barbell or dumbbell; five are kettlebell and would be dropped by the equipment rule anyway.
- Air Bike, the one cardio exercise tagged hypertrophy and strength.
- Eight `category: strength` exercises whose goals are endurance, power, rehab, or mobility only: Bear Crawl, Bird-Dog, Clap Push-Ups, Dead Bug, Dead Bug Hold, Heel-to-Toe Walk, Medicine Ball Slam, One-Arm Dumbbell Swing.
- 56 kettlebell exercises in the strength category, 15 `loop_band`, 2 `resistance_band` (Band Assisted Pull Ups, Band Pull Apart, Banded Squat, Banded Hip Thrust, and the rest of that list).

## What we store

Parsed into the exercise model from the pinned file:

| Field | Use |
| --- | --- |
| `id` | Stable id. Replaces Bybon slugs such as `bench-press-bb`. |
| `name_en` | Display name. `Barbell Bench Press`, not `Bench Press (barbell)`. |
| `description_en` | One-line summary for the detail screen. |
| `instructions_en`, `tips_en` | Steps and form cues for the detail screen. Stored now so that screen does not need another import. |
| `images` | Sealed: `StartAndPeak` or `Main`. Pinned URLs. A row is one shape, not both. |
| `bodyPart` | RepDB `body_part`. Replaces `MuscleGroup`. Library group. |
| `primaryMuscles`, `secondaryMuscles` | Anatomical muscles. 191 of the 382 have more than one primary. 27 have no secondary list. |
| `equipment` | RepDB slug, kept even when the load class is one of the current five. |
| `mechanic` | `compound` or `isolation`. Default rest. |
| `force_type` | `push`, `pull`, `static`, or `dynamic`. |
| `difficulty` | `beginner`, `intermediate`, or `advanced`. |
| `tags` | The slug list as published. Seven included exercises have an empty list. |

Used only while filtering, not stored on the exercise: `category`, `goals`.

Not stored: `met`, `is_unilateral`, `is_bodyweight` (same fact as a missing `equipment`), and every German and Spanish field. This change stores the new fields and does not add library filters for them. The detail screen can show them later.

## Force, difficulty, tags

Counts inside the 382.

`force_type`: push 187, pull 155, static 31, dynamic 9.

- Push: Barbell Bench Press, Dumbbell Lateral Raise, Cable Tricep Pushdown.
- Pull: Barbell Deadlift, Romanian Deadlift, Lat Pulldown, Incline Dumbbell Curl.
- Static: a hold. High Plank, Dead Hang, Wall Sit, Dumbbell Farmer's Walk, L-Sit.
- Dynamic: the body moves through a cycle. Thruster, Barbell Ab Rollout, Jackknife Sit-Up.

`difficulty`: intermediate 213, beginner 138, advanced 31. Barbell Bench Press and Barbell Back Squat are intermediate. Dumbbell Lateral Raise, Lat Pulldown, and Cable Face Pull are beginner.

`tags` has 23 slugs. The common ones are safety and setup labels, then day labels. `no_axial_load` means the load is not stacked through the spine. `big_three` is squat, bench, and deadlift.

| Tag | Count |
| --- | --- |
| `knee_safe` | 265 |
| `lower_back_safe` | 242 |
| `no_axial_load` | 239 |
| `shoulder_safe` | 239 |
| `calisthenics` | 47 |
| `requires_bench` | 62 |
| `leg_day` | 61 |
| `push_day` | 57 |
| `pull_day` | 48 |
| `arm_day` | 43 |
| `core` | 28 |
| `core_focus`, `glute_focus`, `shoulder_focus`, `grip_focus`, `shoulder_stability`, `calf_focus`, `chest_focus`, `back_focus` | 11 or fewer each |
| `powerlifting` | 5 |
| `big_three` | 3 |
| `full_body` | 3 |
| `back_day` | 1 |

Barbell Bench Press is tagged `powerlifting`, `push_day`, `big_three`, `knee_safe`, `no_axial_load`, `lower_back_safe`, `shoulder_safe`, `requires_bench`. The slugs are stored as strings. Friendly labels can wait for the screen that shows them.

## Images

Each exercise points at repo-relative WebP paths, for example `images/flat/bench-press-start.webp` and `images/flat/bench-press-peak.webp`. Holds use `main` instead of the pair.

Image shape is a sealed type: `StartAndPeak(start, peak)` or `Main(main)`. A record is one of those, not a bag of optional URLs.

The build writes absolute URLs pinned to the same commit:

`https://raw.githubusercontent.com/RepDB/exercise-dataset/9ed9357f09c7566ea0256c57ebd6374ebb8b575e/images/flat/<file>.webp`

That URL returns `image/webp` for the bench-press start pose. `https://exercise-dataset.com/images/flat/...` also serves the files, and it tracks the live site, so a later RepDB commit could change a picture without changing our JSON. The raw URL with the SHA stays on this snapshot.

The app has no image loader and no `INTERNET` permission. This change adds both. Coil is the loader, since the UI is Compose and nothing in the project loads remote images today. Names, muscles, and prescriptions work with no network. A row shows the `peak` image, or `main` when there is no pair. `start` is kept for the detail screen. Coil caches files it has already fetched.

## Mechanic and rest

`mechanic` is RepDB’s label for how many joints the lift uses.

- **Compound**: more than one joint, several muscles share the work.
- **Isolation**: one joint, one muscle does the work.

It is not the same thing as `force_type`. A compound lift can be a push or a pull.

| Exercise | `mechanic` | `force_type` | Muscles RepDB lists |
| --- | --- | --- | --- |
| Barbell Bench Press | compound | push | chest primary; front delt and triceps secondary |
| Barbell Back Squat | compound | push | glutes and quads primary; spinal erectors and hamstrings secondary |
| Romanian Deadlift | compound | pull | glutes and hamstrings primary; spinal erectors secondary |
| Lat Pulldown | compound | pull | lats primary; biceps, rear delt, rhomboids secondary |
| Dumbbell Lateral Raise | isolation | push | side delt primary; front delt secondary |
| Incline Dumbbell Curl | isolation | pull | biceps |
| Cable Tricep Pushdown | isolation | push | triceps |
| Single Arm Tricep Pushdown | compound | push | triceps primary, plus shoulder |
| Cable Face Pull | compound | pull | rear delt and rhomboids primary; traps secondary |
| Back Extension | isolation | pull | spinal erectors |

Bybon’s default rest is a hardcoded id list: 2:00 compound, 1:00 isolation, 1:30 for anything not on either list. The proposal replaces that list with `mechanic`:

- compound → 2:00
- isolation → 1:00

Bench, squat, lat pulldown, and lateral raise stay on the same clock they have today. Face pull moves from 1:00 (it is on Bybon’s isolation list) to 2:00, because RepDB marks it compound. Back Extension, which is not in the current catalog, would be 1:00 instead of the 1:30 fallback. The two pushdown rows above show the label is per exercise, not per muscle: the two-arm cable pushdown is isolation and the single-arm variation is compound.

## Muscles

Both layers are stored. `bodyPart` is the library group. The muscle lists are the specific muscles. They are stored separately because the list does not determine the region: the same primary muscle shows up under more than one RepDB `body_part`, and 191 exercises have more than one primary.

```kotlin
enum class BodyPart {
    UpperLegs,   // was Legs, together with LowerLegs
    Back,
    UpperArms,   // was Arms, together with LowerArms
    Chest,
    Shoulders,
    Core,
    LowerLegs,
    LowerArms,
    FullBody,
    // removed: Arms, Legs, Other
}
```

Counts inside the 382:

| `body_part` | Count |
| --- | --- |
| `upper_legs` | 81 |
| `back` | 77 |
| `upper_arms` | 54 |
| `chest` | 51 |
| `shoulders` | 50 |
| `core` | 45 |
| `lower_legs` | 11 |
| `lower_arms` | 8 |
| `full_body` | 5 |

Barbell Bench Press is `Chest`, primary `pectoralis_major`, secondary `anterior_deltoid` and `triceps_brachii`. Fractional sets use the muscle lists later.

Bybon-shipped rows pick a `bodyPart` directly: Iso-Lateral Row is `Back`, Squat (machine) is `UpperLegs`, Triceps Press (machine) is `UpperArms`, Lateral Raise (machine) is `Shoulders`. A guessed Strong exercise picks one of these nine. There is no `Other`.

Primary slugs that show up in the 382, most common first: `gluteus_maximus`, `pectoralis_major`, `latissimus_dorsi`, `quadriceps`, `triceps_brachii`, `rectus_abdominis`, `anterior_deltoid`, `biceps_brachii`, `lateral_deltoid`, `rhomboids`, `hamstrings`, `trapezius`, `erector_spinae`, `obliques`, `hip_flexors`, `gastrocnemius`, `forearm_flexors`, `posterior_deltoid`, `brachialis`, `gluteus_medius`, `transverse_abdominis`, `forearm_extensors`, `brachioradialis`, `abductors`, `soleus`, `adductors`. Secondary-only slugs: `serratus_anterior`, `quadratus_lumborum`, `forearms`, `supraspinatus`.

## Equipment

Excluded now: `kettlebell`, `loop_band`, `resistance_band`.

The RepDB slug is stored on every included exercise. Progression still needs a load class. Until the later equipment pass, the class is:

| RepDB `equipment` | Load class | Increment |
| --- | --- | --- |
| `barbell`, `trap_bar` | Barbell | 2.5 kg |
| `dumbbell` | Dumbbell | 2 kg |
| absent (bodyweight) | Bodyweight | none |
| `assisted_pullup_machine`, `dip_machine` | Assisted | 2.5 kg |
| everything else that we kept | Machine | 2.5 kg |

“Everything else” is the list to split later. Counts inside the 382:

| Slug | Count | Examples |
| --- | --- | --- |
| `cable` | 27 | Lat Pulldown, Cable Fly, Cable Lateral Raise |
| `ez_bar` | 21 | EZ-bar curls and skull crushers |
| `pull_up_bar` | 21 | Pull-Up, Chin-Up |
| `smith_machine` | 19 | Smith Machine Squat |
| `suspension_trainer` | 12 | TRX rows and presses |
| `stability_ball` | 7 | Stability Ball Leg Curl |
| `rings` | 7 | Ring Dips |
| `leg_press` | 6 | Leg Press |
| `plates` | 4 | plate raises |
| named machines | 1–3 each | leg curl, hack squat, pec deck, chest press, calf raise, and the other single-machine slugs |
| `ab_wheel`, `sled`, `climbing_rope`, `wrist_roller` | 1 each | |

`pull_up_bar` is in the Machine bucket only as a stand-in. Unassisted pull-ups are bodyweight work; say if that slug should be Bodyweight when we do the equipment pass.

## Strong import

Resolution order:

1. Alias from the backup’s `Exercise Name` to a shipped RepDB id.
2. Case-insensitive match on `name_en`.
3. Otherwise the name is not in the shipped catalog.

A name that resolves in step 1 or 2 is imported for every workout it appears in. That exercise already exists, so it is not a new duplicate.

A name that falls through to step 3 is counted across the whole file. One workout counts once, when that exercise has at least one set with reps greater than 0.

**Five or more workouts.** The name becomes an active exercise: slug id, guessed muscle and equipment. Every workout is imported, not only the fifth onward. It shows in the library.

**Fewer than five workouts.** No active exercise is created. The sets stay on the imported session, so history still shows the Strong name, weight, and reps. The session already stores that name on each exercise. The definition is also saved as **archived**, because a few lookups resolve an exercise by id from the repository (`requireExercise`). Archived exercises are left out of the library and out of the exercise picker. An imported plan template is built only from active exercises, so a one-off name does not become something you schedule. The historical session still contains it.

This is aimed at one-off Strong names that are usually a second spelling of an exercise already in the catalog. A name that clears 5 workouts and still matches nothing is its own active exercise. Aliases are how a frequent Strong spelling attaches to RepDB instead of becoming that extra row.

Step 2 fails for most current Strong strings, because RepDB word order differs (`Bench Press (Barbell)` vs `Barbell Bench Press`). The alias table is how those attach. It will be written from the names in `docs/strong-import.md` and the sample CSV. Names that only match an excluded exercise (kettlebell, band, yoga, Pilates) fall through to step 3. `Chest Fly (Band)` is in that group: the dataset has no band fly, and band equipment is excluded. In the full backup it appears in 11 workouts, so it would still become an active exercise. Created names with 4 workouts or fewer stay in history as archived exercises.

Plan fuzzy-matching, session identity, and the in-memory repository stay.

## Plans

Set counts, rep ranges, warm-up counts, and plan names stay. Each planned exercise uses the RepDB id in the table below. Full Body B and Upper Body (legacy) use `chest-supported-db-row` for the old incline dumbbell row.

Already clear:

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
| `face-pull` | `face-pull` | Cable Face Pull |
| `incline-row-db` | `chest-supported-db-row` | Chest-Supported Dumbbell Row |
| `chest-dip` | `assisted-dips` | Machine Assisted Dips |
| `chest-fly-peck-deck` | `pec-deck` | Pec Deck |
| `biceps-curl-machine` | `machine-bicep-curl` | Machine Bicep Curl |

Bybon-shipped, not a RepDB id:

| Current id | Name | Why it stays |
| --- | --- | --- |
| `iso-lat-row` | Iso-Lateral Row (machine) | Pin-loaded row, one arm or both |
| `squat-machine` | Squat (machine) | Plate-loaded squat, not a hack squat |
| `triceps-press-machine` | Triceps Press (machine) | Not the seated extension machine or a pushdown |
| `lateral-raise-machine` | Lateral Raise (machine) | Pin-loaded, not the plate-loaded raise |

Strong bare `Chest Fly` aliases to `pec-deck`. Strong `Bicep Curl (Machine)` aliases to `machine-bicep-curl`. Strong `Lateral Raise (Machine)` aliases to `lateral-raise-machine`. Strong `Squat (Machine)` aliases to `squat-machine`. Strong `Triceps Press` aliases to `triceps-press-machine`. Strong `Reverse Lunges` aliases to `reverse-lunge` (Reverse Lunge, dumbbells). Strong `Crunch (Machine)` aliases to `machine-seated-crunch` (Machine Seated Crunch).

`Chest Fly (Band)`, `Cable Pushdown (rope)`, and `Triceps Extension (Cable)` have no alias. They follow the import rule. Five or more workouts become an active exercise. Fewer stay in history as an archived exercise. `Chest Fly (Band)` is in 11 workouts in the full backup, so it becomes active. There is no shipped band fly to attach it to.

## Left as they are

Plan names, set and rep prescriptions, warm-up counts, Strong plan matching, session identity, and the in-memory workout repository. Paid-tier animations stay out. Workout Room persistence stays a separate task. Library search, the detail screen, fractional sets, and the about screen stay later. The data for the detail screen and for fractional sets is stored in this change.
