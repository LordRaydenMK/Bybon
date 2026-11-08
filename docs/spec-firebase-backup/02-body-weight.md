# Step 2 — Body-weight entries, one device

Backup and restore of `weight_entry` only. One row per calendar date.

Not specified yet. The sign-in restore rule below was agreed early and applies once this step has
cloud data. Step 1 does not implement it. Cloud writes go to the Firestore database named by this
build's environment, `stg` or `prd`, in `bybon-f343b`.

## Sign-in restore

Restore runs automatically after a successful sign-in.

- The app tries to restore.
- When there is no conflict, it restores and shows a summary of what was imported.
- When there is a conflict, it asks the user.
- Until multi-device, accepting that restore deletes local user data, including slices that have no
  cloud backup yet, then writes the cloud snapshot.
- When the cloud has nothing to restore, sign-in leaves local data as it is. It does not ask, and it
  does not clear the phone.
- Sign-out does not restore and does not delete local data.

## Local user data

The shipped RepDB catalog is seeded on first use, along with three plans: Full Body A
(`full-body-a`), Full Body B (`full-body-b`), and Upper Body (legacy) (`upper-body-legacy`,
archived). Those seeded rows are not user data by themselves.

Local user data is any of:

- a weight entry
- an exercise with source `user` or `import`
- a completed workout session
- a plan whose id is not one of the three built-in ids
- a built-in plan the user has edited

There is no plan-delete action. Plans can be edited and archived. Upper Body (legacy) is archived in
the seed, so unarchiving it is an edit.

## Open

- What a conflict is.
- What the conflict question offers besides accepting.
- What the import summary contains, and whether an empty import shows one.
- How an edited built-in plan is detected.
- Whether accepting restore deletes in-progress sessions and diet phases.
- Whether sign-in uploads.
- Signing in with a different Google account while local user data is on the phone.
