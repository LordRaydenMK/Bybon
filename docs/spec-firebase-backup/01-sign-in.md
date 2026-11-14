# Step 1 — Google sign-in and sign-out

Implemented.

Firebase Auth only. This step writes no user data to the cloud. It does not restore, upload, or ask
about conflicts.

## Account

- Sign-in is optional. The app is fully usable while signed out.
- The main overflow menu has an **Account** item that opens the Account screen.
- Signed out: a Google sign-in button. The button opens the Google account chooser.
- Cancel and failure leave the user signed out, show a short message, and leave the rest of the app
  usable.
- Signed in: the account email and sign-out.
- Tapping sign-out asks "Sign out?" first. Confirming ends the Firebase Auth session. Dismissing
  leaves the session as it is. Local data stays on the phone.
- The user stays signed in across process death and reboot until they sign out.
- Sign-in uses Credential Manager with Google Identity Services.

## Firebase projects

- Debug builds use `bybon-stg`, from `app/src/debug/google-services.json`, for
  `dev.sanastasov.bybon.debug`.
- Release builds use `bybon-f343b`, from `app/src/release/google-services.json`, for
  `dev.sanastasov.bybon`.
- After install, the app talks to the project it was built with. It does not show that project
  name.
- Auth and Crashlytics use that same project.

## Signing

Debug builds, local and CI, sign with the committed `keystore/debug.keystore`. The `CI=true` branch
in `app/build.gradle.kts` goes away, so a local debug install and a CI debug install share one
certificate.

That certificate is registered on the debug app (`dev.sanastasov.bybon.debug`) in `bybon-stg`.

Release builds sign with `keystore.jks`, as they do today. That certificate is registered on the
release app (`dev.sanastasov.bybon`) in `bybon-f343b`.

Switching local debug onto `keystore/debug.keystore` changes the signing certificate. An existing
debug install signed with the machine debug keystore has to be uninstalled before this build can
replace it.

## Console setup

Each Firebase project enables the Google sign-in provider. Each Android app has the SHA-1 of the
certificate that signs it. The committed `google-services.json` files have a non-empty OAuth client
list.

## Done when

- Overflow → Account, on a debug build and a release build.
- The Google account chooser appears. Signing in shows that account's email.
- The session is still there after the process is killed.
- Sign-out asks first. Dismissing keeps the session. Confirming returns to the signed-out screen and
  leaves local workouts and weight entries in place.
- Canceling the chooser, or a failed sign-in, shows a short message and stays signed out. The other
  tabs still work.
- A local debug build and a CI debug build report the same signing certificate.
- Debug processes `app/src/debug/google-services.json`. Release processes
  `app/src/release/google-services.json`.
- Nothing in this step writes user data to Firebase.
