# RepDB exercise dataset — proposal

Status: proposal only. No implementation in this change.

Snapshot pin: [RepDB/exercise-dataset](https://github.com/RepDB/exercise-dataset) `9ed9357f09c7566ea0256c57ebd6374ebb8b575e` (2026-09-16). `exercises.json` schema 3, 601 exercises in the full file.

## Decisions

- Ship a **subset**, built at compile time from that pinned file. The file is downloaded by Gradle and filtered into a build asset. It is not committed.
- English only (`name_en`, `description_en`, `instructions_en`, `tips_en`). German and Spanish are dropped at build time.
- Images are **URLs**, loaded at runtime. The APK does not contain the WebPs.
- Exercise detail screen, library search, and the about screen come later.
- README gets the credit line `Exercise data by RepDB (repdb.co)` when the data is wired in. The free-tier license asks for that credit on any in-app use; the README line covers it.
- Strong import matches a name onto a shipped RepDB exercise when one exists. Aliases cover the names in the backup. A Strong name with no shipped exercise is still created, as today.
- Kettlebell, `loop_band`, and `resistance_band` were the first exclusions. Every RepDB equipment slug still needs an explicit in-or-out and a load class. See [Equipment](#equipment).
- General muscle group comes from RepDB `body_part` (nine groups). Primary and secondary muscles are stored on the exercise for fractional-set tracking later.
- `force_type`, `difficulty`, and `tags` are stored on the exercise. `met` is not.
- Ambiguous Bybon → RepDB exercise picks are decided one at a time. See the end of this doc.

## Catalog filter

An exercise is included when all of these hold:

1. `category` is `strength`.
2. `goals` contains `hypertrophy` or `strength` (many list both).
3. `equipment` is not `kettlebell`, `loop_band`, or `resistance_band`.

That is **410** exercises.

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
| `images.flat` | `start` + `peak`, or a single `main`. Turned into pinned URLs at build time. |
| `body_part` | General muscle group. |
| `primary_muscles`, `secondary_muscles` | Specific muscles. Fractional sets later. 27 included exercises have no secondary list. |
| `equipment` | RepDB slug, kept even when the load class is one of the current five. |
| `mechanic` | `compound` or `isolation`. Default rest. |
| `force_type` | `push`, `pull`, `static`, or `dynamic`. |
| `difficulty` | `beginner`, `intermediate`, or `advanced`. |
| `tags` | The slug list as published. Seven included exercises have an empty list. |

Used only while filtering, not stored on the exercise: `category`, `goals`.

Not stored: `met`, `is_unilateral`, `is_bodyweight` (same fact as a missing `equipment`), and every German and Spanish field. This change stores the new fields and does not add library filters for them. The detail screen can show them later.

## Force, difficulty, tags

Counts inside the 410.

`force_type`: push 187, pull 155, static 50, dynamic 18.

- Push: Barbell Bench Press, Dumbbell Lateral Raise, Cable Tricep Pushdown.
- Pull: Barbell Deadlift, Romanian Deadlift, Lat Pulldown, Incline Dumbbell Curl.
- Static: a hold. High Plank, Dead Hang, Wall Sit, Dumbbell Farmer's Walk, L-Sit, and also yoga poses that passed the strength filter (Warrior II, Tree Pose, Crow Pose, Chair Pose).
- Dynamic: the body moves through a cycle. Thruster, Barbell Ab Rollout, Jackknife Sit-Up, and several Pilates and downward-dog variations.

`difficulty`: intermediate 235, beginner 140, advanced 35. Barbell Bench Press and Barbell Back Squat are intermediate. Dumbbell Lateral Raise, Lat Pulldown, and Cable Face Pull are beginner.

`tags` has 23 slugs. The common ones are safety and setup labels, then day labels. `no_axial_load` means the load is not stacked through the spine. `big_three` is squat, bench, and deadlift.

| Tag | Count |
| --- | --- |
| `knee_safe` | 265 |
| `lower_back_safe` | 242 |
| `no_axial_load` | 239 |
| `shoulder_safe` | 239 |
| `calisthenics` | 75 |
| `requires_bench` | 62 |
| `leg_day` | 61 |
| `push_day` | 57 |
| `pull_day` | 48 |
| `arm_day` | 43 |
| `core` | 39 |
| `core_focus`, `glute_focus`, `shoulder_focus`, `grip_focus`, `shoulder_stability`, `calf_focus`, `chest_focus`, `back_focus` | 11 or fewer each |
| `powerlifting` | 5 |
| `big_three` | 3 |
| `full_body` | 3 |
| `back_day` | 1 |

Barbell Bench Press is tagged `powerlifting`, `push_day`, `big_three`, `knee_safe`, `no_axial_load`, `lower_back_safe`, `shoulder_safe`, `requires_bench`. The slugs are stored as strings. Friendly labels can wait for the screen that shows them.

## Images

Each exercise points at repo-relative WebP paths, for example `images/flat/bench-press-start.webp` and `images/flat/bench-press-peak.webp`. Holds use `main` instead of the pair.

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

Two layers, both stored.

**General group** is `body_part`. Counts inside the 410:

| `body_part` | Count | Bybon group today |
| --- | --- | --- |
| `upper_legs` | 91 | Legs |
| `back` | 79 | Back |
| `core` | 56 | Core |
| `upper_arms` | 55 | Arms |
| `shoulders` | 54 | Shoulders |
| `chest` | 51 | Chest |
| `lower_legs` | 11 | Legs |
| `lower_arms` | 8 | Arms |
| `full_body` | 5 | FullBody |

The library groups by these nine. That splits today’s Arms into upper arms and lower arms, and today’s Legs into upper legs and lower legs.

**Specific muscles** are the anatomical slugs. Barbell Bench Press is the shape you described: general group chest, primary `pectoralis_major`, secondary `anterior_deltoid` and `triceps_brachii`. Fractional sets (a set of bench counting toward chest, front delt, and triceps) use this list later. This change only stores it.

Primary slugs that show up in the 410, most common first: `gluteus_maximus`, `pectoralis_major`, `quadriceps`, `latissimus_dorsi`, `rectus_abdominis`, `triceps_brachii`, `anterior_deltoid`, `biceps_brachii`, `lateral_deltoid`, `hamstrings`, `rhomboids`, `erector_spinae`, `trapezius`, `obliques`, `hip_flexors`, `gluteus_medius`, `gastrocnemius`, `forearm_flexors`, `posterior_deltoid`, `brachialis`, `transverse_abdominis`, `forearm_extensors`, `brachioradialis`, `abductors`, `soleus`, `adductors`. Secondary-only slugs: `serratus_anterior`, `quadratus_lumborum`, `forearms`, `supraspinatus`.

## Equipment

RepDB has 56 equipment values, counting exercises with no `equipment` field as bodyweight. The slug is stored on the exercise. A separate **load class** drives the progression increment, the default work and warm-up weight, and the library filter chips. Bybon has five load classes today:

| Load class | Increment | Default work | Default warm-up |
| --- | --- | --- | --- |
| Barbell | 2.5 kg | 50 kg | 20 kg |
| Dumbbell | 2 kg | 50 kg | 10 kg |
| Machine | 2.5 kg | 50 kg | 20 kg |
| Assisted | 2.5 kg | none | none |
| Bodyweight | none | none | none |

A new load class needs its own increment and defaults. One chip is shown per load class. Giving every slug its own class adds one chip per slug.

Counts below are exercises with `category: strength` and a `hypertrophy` or `strength` goal. That is the set this decision changes. The same slug on a stretch, cardio piece, or olympic lift is already outside the catalog.

| Slug | Exercises | Examples |
| --- | --- | --- |
| *(none — bodyweight)* | 109 | Push-Up, Back Extension, Plank, Bodyweight Squat, Lunge |
| `dumbbell` | 77 | Dumbbell Bench Press, Dumbbell Lateral Raise, Bulgarian Split Squat |
| `barbell` | 60 | Barbell Bench Press, Barbell Back Squat, Romanian Deadlift |
| `kettlebell` | 54 | Goblet Squat, Kettlebell Deadlift, Kettlebell Turkish Get Ups |
| `cable` | 27 | Lat Pulldown, Cable Fly, Cable Tricep Pushdown |
| `ez_bar` | 21 | EZ-Bar Curl, EZ-Bar Lying Triceps Extension, Preacher Curl |
| `pull_up_bar` | 21 | Pull-Up, Chin-Ups, Weighted Pull-Up, Hanging Leg Raise |
| `smith_machine` | 19 | Smith Machine Squat, Smith Machine Bench Press, Smith Machine Romanian Deadlift |
| `loop_band` | 15 | Banded Squat, Banded Hip Thrust, Banded Romanian Deadlift |
| `suspension_trainer` | 12 | TRX Row, TRX Chest Press, TRX Hamstring Curl |
| `rings` | 7 | Ring Dips, Ring Row, Ring Muscle-Up |
| `stability_ball` | 7 | Stability Ball Leg Curl, Stability Ball Push-Up, Ball Pike |
| `leg_press` | 6 | Leg Press, Close-Stance Leg Press, Single Leg Press |
| `plates` | 4 | Plate Pinch, Svend Press, Plate Pullover, Weighted Wall Crunch |
| `dip_station` | 3 | Chest Dips, Weighted Dips, L Sit |
| `leg_curl` | 3 | Lying Leg Curl, Seated Leg Curl, Single Leg Lying Leg Curl |
| `resistance_band` | 2 | Band Assisted Pull Ups, Band Pull Apart |
| `hack_squat` | 2 | Hack Squat, Hack Squat Calf Raise |
| `lat_pulldown_machine` | 2 | V-Bar Lat Pulldown, Behind-the-Neck Lat Pulldown |
| `leg_extension` | 2 | Leg Extension, Single Leg Extension |
| `plate_loaded_lateral_raise_machine` | 2 | Plate-Loaded Lateral Raise, Single-Arm Plate-Loaded Lateral Raise |
| `shoulder_press_machine` | 2 | Machine Shoulder Press, Single-Arm Machine Shoulder Press |
| `standing_calf_raise_machine` | 2 | Standing Calf Raise, Machine Calf Raise |
| `ab_crunch_machine` | 1 | Machine Seated Crunch |
| `ab_wheel` | 1 | Ab Wheel Rollout |
| `assisted_pullup_machine` | 1 | Assisted Pull Ups |
| `back_extension_machine` | 1 | Machine Back Extension |
| `bicep_curl_machine` | 1 | Machine Bicep Curl |
| `chest_fly_machine` | 1 | Machine Chest Fly |
| `chest_press_machine` | 1 | Machine Chest Press |
| `climbing_rope` | 1 | Rope Climb |
| `dip_machine` | 1 | Machine Assisted Dips |
| `donkey_calf_raise_machine` | 1 | Plate-Loaded Donkey Calf Raise |
| `glute_ham_developer` | 1 | Nordic Hamstring Curl |
| `hip_abduction_machine` | 1 | Machine Hip Abduction |
| `hip_adduction_machine` | 1 | Hip Adduction |
| `hip_thrust_machine` | 1 | Plate-Loaded Glute Drive |
| `pec_deck` | 1 | Pec Deck |
| `preacher_curl_machine` | 1 | Machine Preacher Curl |
| `seated_calf_raise_machine` | 1 | Seated Calf Raise |
| `shrug_machine` | 1 | Plate-Loaded Shrug |
| `sled` | 1 | Sled Row |
| `trap_bar` | 1 | Hex Bar Deadlift |
| `tricep_extension_machine` | 1 | Machine Triceps Extension |
| `wrist_roller` | 1 | Wrist Roller |

For each row: include the exercises, or drop them, and if they stay, which load class. `kettlebell` (54), `loop_band` (15), and `resistance_band` (2) were the earlier exclusions. Eleven more `resistance_band` exercises are stretches and are already out through `category`.

These slugs have no exercise in that set, so the category and goal filter already removes them: `flat_bench` (10 stretches), `battle_rope`, `treadmill`, `air_bike`, `elliptical`, `jump_rope`, `rower`, `stair_climber`, `stationary_bike`, `plyo_box`, `slam_ball` (Medicine Ball Slam, goals are power and endurance).

## Strong import

Resolution order:

1. Alias from the backup’s `Exercise Name` to a shipped RepDB id.
2. Case-insensitive match on `name_en`.
3. If the shipped catalog has no exercise for that name, create one, same as today (slug id, guessed muscle and equipment).

Step 2 fails for most current Strong strings, because RepDB word order differs (`Bench Press (Barbell)` vs `Barbell Bench Press`). The alias table is how those attach. It will be written from the names in `docs/strong-import.md` and the sample CSV. Names that only match an excluded exercise (kettlebell, band) fall through to step 3. `Chest Fly (Band)` is in that group: the dataset has no band fly, and band equipment is excluded.

Plan fuzzy-matching, session identity, and the in-memory repository stay.

## Plans

Set counts, rep ranges, warm-up counts, and plan names stay. Each planned exercise id changes to the RepDB id once that row is confirmed.

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

Open, besides the exercise picks: the strength filter still includes yoga poses and Pilates variations (`Warrior II`, `Tree Pose`, `Crow Pose`, downward-dog flows). They are `category: strength` and carry a hypertrophy or strength goal, so they are in the 410. Say if those should be excluded.

Still to pick, one at a time:

| Current id | Current name | Where it matters |
| --- | --- | --- |
| `incline-row-db` | Incline Row (dumbbell) | Full Body B, Upper Body (legacy) |
| `iso-lat-row` | Iso-Lateral Row (machine) | catalog only |
| `squat-machine` | Squat (machine) | catalog; Strong `Squat (Machine)` |
| `triceps-press-machine` | Triceps Press (machine) | catalog; Strong `Triceps Press` |
| `chest-dip` | Chest Dip (assisted) | catalog only |
| `chest-fly-peck-deck` | Chest Fly (machine) | catalog; Strong bare `Chest Fly` |
| `biceps-curl-machine` | Curl (machine) | catalog; Strong `Bicep Curl (Machine)` |
| `lateral-raise-machine` | Lateral Raise (machine) | catalog; Strong `Lateral Raise (Machine)` |

Strong strings with no exact included exercise, after the picks above: `Chest Fly (Band)`, `Cable Pushdown (rope)`, `Triceps Extension (Cable)`, `Reverse Lunges`, `Crunch (Machine)`.

## Left as they are

Plan names, set and rep prescriptions, warm-up counts, Strong plan matching, session identity, and the in-memory workout repository. Paid-tier animations stay out. Workout Room persistence stays a separate task. Library search, the detail screen, fractional sets, and the about screen stay later. The data for the detail screen and for fractional sets is stored in this change.
