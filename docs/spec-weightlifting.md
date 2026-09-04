# Bybon — App Spec (v1)

Personal gym/fitness companion Android app. Single-user, local-first.

## Core Concept

Bybon lets the user plan **mesocycles** (Dr. Mike terminology), track workouts against them, and
auto-suggest progression based on estimated 1RM. It is not a generic exercise logger — the
mesocycle/progression model is the core of the app.

## Data & Accounts

- Single local user, no login required.
- All data stored on-device to start.
- Cloud backup (Google Drive or Firebase) is a planned future feature, out of scope for v1.

## Program Structure

- **Microcycle**: a repeating block of training days (e.g. Mon/Wed/Fri full body, or labeled A/B/C).
    - Days can be labeled by weekday (Mon/Wed/Fri) **or** by generic label (A/B/C).
    - With generic labels, the user picks which day to do each time, and the order/spacing can
      differ per microcycle instance.
- **Mesocycle**: a microcycle repeated for N weeks.
    - Default length: 6 weeks, the user can enter a custom length when planning/creating. The user
      can end early or extend while active.
    - Includes an explicit **deload/off period** (days) — used for planned time off or vacations,
      tracked as part of the mesocycle timeline rather than an auto-generated reduced-volume week.
- Exercises can be freely swapped, added, or removed mid-mesocycle (no locking, no deviation
  tracking for v1).
- Superset/circuit grouping is **not in v1**, but the data model should not preclude adding it
  later.

## Exercise Library

- Built-in curated library **plus** user-added custom exercises.
- Each exercise stores: name, primary muscle group, secondary groups, equipment type.
- Each exercise in a program carries:
    - A target rep range (e.g. 8–12).
    - A configurable progression increment (e.g. barbell +2.5 kg, dumbbell +2 kg, or a rep
      increment).
    - A configurable rest duration (e.g. longer for compounds, shorter for isolation).

## Workout Session Logging

Per set, log:

- Weight
- Reps
- RIR (reps in reserve) (optional)

Additional session features:

- Sets are flagged as **warm-up** or **working** — warm-up sets do not count toward progression.
- Live countdown rest timer auto-starts after logging a set, using the exercise's configured rest
  duration.
- Superset grouping: not in v1 (see above).

## Progression Logic

- The **top set** = the heaviest working set of an exercise (typically, but not necessarily, the
  first set logged).
- Progression is driven by **estimated 1RM** of the top set (formula already implemented elsewhere
  in the WIP project).
- Progression direction: increase weight or reps on the top set, as long as reps stay within the
  exercise's target rep range.
- **Back-off sets** use the same weight as the top set, allowed to hit fewer reps.
- **Before each session**, the app proactively suggests the next top-set target (weight/reps) based
  on the prior session's estimated 1RM — the user can accept or override.
- **On failure** (top set falls short of the rep range): no automatic adjustment — the app just logs
  the result as-is; no forced deload/reset logic in v1.

## Templates

- No built-in split library needed (PPL/5x5/etc. not required).
- User will create their own templates directly: 2× full body + 2× upper body sessions, to start.

## Analytics / Progress Tracking

- Strength trends: estimated 1RM / weight over time, per exercise.
- Volume tracking: sets × reps × weight over time.
- Body metrics: **out of scope here** — user already has a separate spec for body weight tracking.

## History

- v1: simple chronological list of past sessions.
- Calendar view: planned for later, not v1.

## Explicitly Out of Scope (v1 / later)

- Plate calculator (plate math per side) — later.
- Cloud sync/backup — later.
- Calendar-based history view — later.
- Superset/circuit support — later (design shouldn't block it).
- Body weight/metrics tracking — separate spec already exists.
- Tech stack — out of scope for this spec (app is already WIP).
