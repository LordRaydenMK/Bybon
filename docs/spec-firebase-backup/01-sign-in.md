# Step 1 — Google sign-in and sign-out

Firebase Auth only. This step writes no user data to the cloud. It does not restore, upload, or ask
about conflicts.

## Account

- Sign-in is optional. The app is fully usable while signed out.
- The main overflow menu has an **Account** item that opens the Account screen.
- The Account screen shows the environment name, `stg` or `prd`, signed in and signed out.
- Signed out: a Google sign-in button. The button opens the Google account chooser.
- Cancel and failure leave the user signed out, show a short message, and leave the rest of the app
  usable.
- Signed in: the account email and sign-out.
- Tapping sign-out asks "Sign out?" first. Confirming ends the Firebase Auth session. Dismissing
  leaves the session as it is. Local data stays on the phone.
- The user stays signed in across process death and reboot until they sign out.
- Sign-in uses Credential Manager with Google Identity Services.

## Environments

- Two Firebase projects. `prd` is the existing project `bybon-f343b`. `stg` is a new Firebase
  project. Stojan creates `stg` in the Firebase console when asked, then the downloaded config is
  committed.
- A debug variant uses `stg` unless overridden. A release variant uses `prd` unless overridden.
- With no `-Penv`, a command that builds both variants produces a debug app on `stg` and a release
  app on `prd`.
- `-Penv=stg` or `-Penv=prd` overrides every variant in that command. Any other value fails the
  build.
- There is no in-app environment switch. After install, the app talks to the environment it was
  built with.
- Each project registers both application ids: `dev.sanastasov.bybon.debug` and
  `dev.sanastasov.bybon`.
- The two config files are committed at `firebase/stg/google-services.json` and
  `firebase/prd/google-services.json`. The build gives each variant the file for its environment.
  The current `app/src/google-services.json` is removed once those two files are wired in.
- Crashlytics uses the project in the selected file.

## Signing

Debug builds, local and CI, sign with the committed `keystore/debug.keystore`. The `CI=true` branch
in `app/build.gradle.kts` goes away, so a local debug install and a CI debug install share one
certificate.

That certificate is registered on the debug app (`dev.sanastasov.bybon.debug`) in both Firebase
projects.

Release builds sign with `keystore.jks`, as they do today. That certificate is registered on the
release app (`dev.sanastasov.bybon`) in both Firebase projects.

A debug app pointed at `prd` still uses the debug certificate. A release app pointed at `stg` still
uses the release certificate.

Switching local debug onto `keystore/debug.keystore` changes the signing certificate. An existing
debug install signed with the machine debug keystore has to be uninstalled before this build can
replace it.

## Console setup

Both projects enable the Google sign-in provider. Each Android app has the SHA-1 of the certificate
that signs it. The committed `google-services.json` for each project has a non-empty OAuth client
list. The current `bybon-f343b` file has an empty list, so `prd` is re-downloaded after the SHA-1 is
registered.

## Done when

- Overflow → Account, on a debug build and a release build.
- `stg` or `prd` is visible, and it matches the variant and any `-Penv`.
- The Google account chooser appears. Signing in shows that account's email.
- The session is still there after the process is killed.
- Sign-out asks first. Dismissing keeps the session. Confirming returns to the signed-out screen and
  leaves local workouts and weight entries in place.
- Canceling the chooser, or a failed sign-in, shows a short message and stays signed out. The other
  tabs still work.
- `assembleDebug` is `stg`. `assembleRelease` is `prd`. `assembleDebug -Penv=prd` and
  `assembleRelease -Penv=stg` use the override. `-Penv=dev` fails the build.
- A local debug build and a CI debug build report the same signing certificate.
- Nothing in this step writes user data to Firebase.
