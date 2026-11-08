# Firebase backup

Sign in with a Google account, then back up body-weight entries and workout data to Firebase. The
app stays local-first. Each slice is backed up on its own.

A single-device step backs up and restores that slice. A conflict in a step changes that slice
only. Two phones do not edit the same slice until that slice's multi-device step.

The app uses one Firebase project, `bybon-f343b`, for Auth and Crashlytics. Later Firestore steps
use `stg` and `prd` databases in that project. Two Firebase projects are not used: Google OAuth
clients require the Android package name plus SHA-1 to be globally unique.

## Steps

1. [Google sign-in and sign-out](spec-firebase-backup/01-sign-in.md) — implemented
2. [Body-weight entries, one device](spec-firebase-backup/02-body-weight.md)
3. [Custom and imported exercise definitions, one device](spec-firebase-backup/03-exercises.md)
4. [Workout plans, one device](spec-firebase-backup/04-plans.md)
5. [Completed workout sessions, one device](spec-firebase-backup/05-sessions.md)
6. [Multi-device body weight](spec-firebase-backup/06-multi-device-body-weight.md)
7. [Multi-device custom and imported exercises](spec-firebase-backup/07-multi-device-exercises.md)
8. [Multi-device plans](spec-firebase-backup/08-multi-device-plans.md)
9. [Multi-device completed sessions](spec-firebase-backup/09-multi-device-sessions.md)

Stable ids for Room-generated `Long`s are an open question. Revisit them at the start of
multi-device work, before step 6.

How sign-in restore behaves once multi-device exists is revisited at step 6.

## Parked

- **Diet phases.** The feature has no UI yet.
- **In-progress workouts.** An open workout is not part of backup.
- Shipped RepDB catalog, exercise image files, the 1RM calculator, and Android Auto Backup.
