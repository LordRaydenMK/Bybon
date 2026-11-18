Offline first exercise / fitness app for Android built for personal use.
Features: Weight tracking, Exercise plans, Workout sessions, Workout history etc...

Tech stack:

- Kotlin
- Jetpack Compose
- Manual DI
- Room for local storage
- Firebase Firestore as a remote data storage
- retain for retaining ViewModel across config changes
- Kotlinx Serialization
- Jetpack Navigation 3
- Coil
- Power Assert + Turbine for tests
- Paparazzi for Compose screenshot tests
- Maestro for e2e tests

Architecture:
Clean architecture. The domain layer contains the logic. Presentation and data depend on domain.

Rules:

- The domain layer MUST NOT depend on data/presentation layer
- Use/create domain type where it makes sense e.g. BodyWeight for weight tracking
- Date/Time: NEVER use java.utilDate or long for time. Use LocalDate/LocalDateTime/Instant etc...
- NEVER suppress detekt issues without asking first
- Always use `RetainedEntry.defaultScope` when creating ViewModel. It is the retained viewModelScope
  on `Dispatchers.Default`, cancelled when the screen is cleared. Do not pass `it.coroutineScope`.
  Unit tests keep injecting a test scope.
- Specify return types for public functions

Important tasks:

- Use `app:assembleDebug` to build the debug apk
- Use `testDebutUnitTest` to run the unit tests
- Use `verifyPaparazziDebug` to run screenshot tests; `recordPaparazziDebug` to write goldens
- Snapshot PNGs under `app/src/test/snapshots` are stored in Git LFS
- CI copies `scripts/pre-receive` into git hooks after checkout and runs it, then `git lfs pull`
- Use `detekt` to run detekt checks, add `--auto-correct` to fix issues automatically
- Use `lintDebug` for running lint checks

Maestro tests

- The maestro tests do NOT run in the cloud agent env. They run automatically on CI, use that.