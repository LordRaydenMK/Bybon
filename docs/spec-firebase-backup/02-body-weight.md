# Step 2 — Body-weight entries, one device

Backup and restore of `weight_entry` only. One row per calendar date. This step does not change
exercises, plans, sessions, diet phases, or an in-progress workout.

Cloud writes go to the Firestore database named by this build's environment, `stg` or `prd`, in
`bybon-f343b`.

## Domain

`BodyWeightEntry` stays `date` + `weight`. `weight` is the `Int` Room stores (0.01 kg units).
`updatedAt` and `deleted` are not fields on that type and not columns on `weight_entry`.

A separate local sync row, keyed by date, remembers a pending add, edit, or delete until that
Firestore write succeeds. After it succeeds, the sync row goes away.

Saving a weight for a date that already exists replaces that local row. The current insert aborts
on an existing date.

## Firestore

```
users (collection)
  └── {uid} (document)
        └── weights (subcollection)
              └── 2026-11-08 (document)
                    weight: 6545
                    updatedAt: <server timestamp>
                    deleted: false
```

The document ID is the calendar date, `YYYY-MM-DD`. `updatedAt` is the time of the write. A delete
keeps the document and sets `deleted: true` with a new `updatedAt`.

While signed in, adding, changing, or deleting a date writes that one document. The whole history
is not rewritten.

Only that signed-in user can read and write their `users/{uid}` tree.

## Sync

The phone stores, per signed-in user and per database (`stg` or `prd`), the latest `updatedAt` it
has applied.

A full read of `weights` runs only when that cursor is missing, and when sign-in finds no
non-deleted documents. Every other sync uploads pending dates and queries documents whose
`updatedAt` is greater than the cursor.

Sign-in, each add, edit, or delete, and a daily WorkManager job all run that same sync. The job
requires a network. The app has no WorkManager use today. The daily job retries a write after the
process dies and pulls newer documents. It does not start a second full backup. A signed-out user
runs no sync.

When a sync would delete or change a phone weight while the app is in the background, both sides
stay as they are for that conflict. The three-way question is shown the next time the app is open.
Dates that only need to be added are still added. The summary is shown when the app is open.

## After sign-in

A successful sign-in runs the sync above.

- The phone has no weight entries and the cloud has at least one non-deleted document: restore
  those entries and show a summary.
- The cloud has no non-deleted documents: upload every weight on the phone, one document at a time.
  Do not ask.
- A cloud date the phone does not have, when every shared date already matches: add it and show the
  summary.
- A cloud value that would delete a phone weight or change one: ask.
- The summary, when a restore imported at least one entry, shows how many and the earliest and
  latest dates. A restore that imports nothing shows no summary.

While that question is unanswered, sync does not upload and does not apply the cloud change.

When that comparison asks, the choices are:

1. **Keep this phone.** Leave the cloud unchanged. Keep the phone's weights. End the Firebase
   session, so a later sync does not write those weights. Pending sync rows are not uploaded.
2. **Use the backup.** Remove the phone's weights and restore the non-deleted cloud weights. Stay
   signed in. Then show the summary.
3. **Replace the backup.** Write the phone's weights over the cloud and stay signed in. Each phone
   date is written with `deleted: false`. A live cloud date the phone does not have is kept as a
   document with `deleted: true` and a new `updatedAt`.

The Account screen shows the time of the last sync that finished with nothing pending, and that a
backup is pending while sync rows are waiting. A retry stays pending. There is no error dialog.

The phone remembers the user id that last synced. The first sign-in, with an empty cloud, still
uploads the phone's weights. A different user, while the phone has weights, always gets this
question, including when that user's cloud is empty. “Use the backup” then replaces the phone with
that user's weights, and clears the phone when that cloud is empty. “Replace the backup” uploads
the phone's weights into that account. “Keep this phone” uploads nothing.

Sign-out from the Account screen does not restore and does not delete local weights.
