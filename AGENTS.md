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
- Maestro for e2e tests

Architecture:
Clean architecture. The domain layer contains the logic. Presentation and data depend on domain.

Rules:

- The domain layer MUST NOT depend on data/presentation layer
- Use/create domain type where it makes sense e.g. BodyWeight for weight tracking
- Date/Time: NEVER use java.utilDate or long for time. Use LocalDate/LocalDateTime/Instant etc...
- NEVER suppress detekt issues without asking first

Important tasks:

- Use `app:assembleDebug` to build the debug apk
- Use `testDebutUnitTest` to run the unit tests
- Use `detekt` to run detekt checks, add `--auto-correct` to fix issues automatically
- Use `lintDebug` for running lint checks

Maestro tests

- The maestro tests do NOT run in the cloud agent env. They run automatically on CI, use that.