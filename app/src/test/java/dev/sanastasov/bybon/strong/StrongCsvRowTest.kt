package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.Mechanic
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.upperBodyA
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class StrongCsvRowTest {

    private val bench = ExerciseDefinition(
        "bench-press-bb",
        "Bench Press (barbell)",
        MuscleGroup.Chest,
        Equipment.Barbell,
        Mechanic.Compound,
    )
    private val squat = ExerciseDefinition(
        "squat-bb",
        "Squat (barbell)",
        MuscleGroup.Legs,
        Equipment.Barbell,
        Mechanic.Compound,
    )
    private val pullUp = ExerciseDefinition(
        "pullup-assisted",
        "Pull Up (assisted)",
        MuscleGroup.Back,
        Equipment.AssistedBodyWeight,
        Mechanic.Compound,
    )
    private val catalog = listOf(bench, squat, pullUp)
    private val fullBodyPlan = WorkoutPlan(
        id = WorkoutPlanId("full-body-a"),
        name = "Full Body A",
        description = "Bybon full body A",
        sets = listOf(
            PlanedExercise(bench, sets = 3, repRange = 8..10),
            PlanedExercise(squat, sets = 3, repRange = 8..10),
            PlanedExercise(pullUp, sets = 3, repRange = 6..10),
        ),
    )

    @Test
    fun `rejects a file that is not a strong csv export`() {
        val error = assertFailsWith<IllegalArgumentException> {
            StrongCsvParser.parse("not a strong csv")
        }
        assert(error.message == "Not a Strong CSV export")
    }

    @Test
    fun `reads strong backup sample into dto rows`() {
        val rows = StrongCsvParser.parse(readStrongBackupSample(javaClass.classLoader))

        assert(rows.size == 2053)
    }

    @Test
    fun `imports completed session history from the strong backup sample`() {
        val result = StrongCsvParser.parse(
            readStrongBackupSample(javaClass.classLoader),
        ).toStrongImport(
            plans = listOf(fullBodyA, fullBodyB),
            exerciseCatalog = catalogExercises,
        )

        assert(result.sessionHistory.size == 52)
        assert(result.sessionHistory.all { it.state is WorkoutState.Completed })
        assert(
            result.sessionHistory.map { it.planId }.toSet() == setOf(
                fullBodyA.id,
                fullBodyB.id,
                WorkoutPlanId("upper-body-a"),
                WorkoutPlanId("upper-body-b"),
            ),
        )
        assert(result.sessionHistory.count { it.planId == fullBodyA.id } == 23)
        assert(result.sessionHistory.count { it.planId == fullBodyB.id } == 23)
        val firstRdl = result.sessionHistory.first { it.planId == fullBodyB.id }.exercises.first()
        assert(firstRdl.id == "romanian-deadlift")
        assert(firstRdl.warmupSets?.size == 2)
        assert(
            firstRdl.warmupSets?.map { it.weight to it.reps } == listOf(
                Weight.kilograms(20f) to 8,
                Weight.kilograms(35f) to 4,
            ),
        )
        assert(firstRdl.sets.size == 3)
        assert(firstRdl.restAfterWorkSet == 120.seconds)
        val firstSplitSquat = result.sessionHistory
            .first { it.planId == fullBodyB.id }
            .exercises
            .first { it.id == "bulgarian-split-squat" }
        assert(firstSplitSquat.warmupSets?.map { it.weight to it.reps } == listOf(null to 6))
        val firstFullBodyA = result.sessionHistory.first { it.planId == fullBodyA.id }
        assert(
            firstFullBodyA.exercises.first { it.id == "bench-press" }.restAfterWorkSet ==
                120.seconds,
        )
        assert(
            firstFullBodyA.exercises.first {
                it.id == "seated-leg-curl"
            }.restAfterWorkSet ==
                90.seconds,
        )
        assert(
            firstFullBodyA.exercises.first { it.id == "db-skull-crusher" }.restAfterWorkSet ==
                60.seconds,
        )
        val crunch = result.sessionHistory
            .flatMap { it.exercises }
            .first { it.id == "machine-seated-crunch" }
        assert(crunch.restAfterWorkSet == 120.seconds)
    }

    @Test
    fun `imports only exercises that do not already exist in Bybon`() {
        val result = StrongCsvParser.parse(
            readStrongBackupSample(javaClass.classLoader),
        ).toStrongImport(
            plans = listOf(fullBodyA, fullBodyB),
            exerciseCatalog = catalogExercises,
        )

        assert(result.exercises.isEmpty())
        val crunch = result.sessionHistory.flatMap { it.exercises }
            .first { it.id == "machine-seated-crunch" }
        assert(crunch.exerciseDefinition.name == "Machine Seated Crunch")
        assert(crunch.exerciseDefinition.primaryMuscleGroup == MuscleGroup.Core)
        assert(crunch.exerciseDefinition.equipment == Equipment.Machine)
    }

    @Test
    fun `imports only plans that do not match existing Bybon plans`() {
        val result = StrongCsvParser.parse(
            readStrongBackupSample(javaClass.classLoader),
        ).toStrongImport(
            plans = listOf(fullBodyA, fullBodyB),
            exerciseCatalog = catalogExercises,
        )

        assert(result.plans.map { it.name } == listOf("Upper body A", "Upper body B"))
        assert(result.plans.all { it.isArchived })
        assert(
            result.plans.first { it.name == "Upper body A" }.sets.map { it.exercise.id } == listOf(
                "bench-press",
                "assisted-pull-ups",
                "dumbbell-upright-row",
                "db-skull-crusher",
                "machine-seated-crunch",
            ),
        )
        assert(
            result.plans.first { it.name == "Upper body B" }.sets.map { it.exercise.id } == listOf(
                "incline-db-press",
                "chest-supported-db-row",
                "lateral-raise",
                "incline-db-curl",
                "machine-seated-crunch",
            ),
        )
    }

    @Test
    fun `does not import an exercise that already exists in the catalog`() {
        val rows = workout(
            number = 1,
            name = "Full body A",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assert(result.exercises == emptyList<ExerciseDefinition>())
        assert(
            result.sessionHistory.single().exercises.map { it.id } == listOf(
                "bench-press-bb",
                "squat-bb",
            ),
        )
    }

    @Test
    fun `leaves an exercise that is missing from the catalog out of the import`() {
        val rows = workout(
            number = 1,
            name = "Upper body A",
            exercises = listOf("Bench Press (Barbell)", "Crunch (Machine)"),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        assert(result.exercises.isEmpty())
        assert(result.sessionHistory.single().exercises.map { it.id } == listOf("bench-press-bb"))
        assert(result.plans.single().sets.map { it.exercise.id } == listOf("bench-press-bb"))
    }

    @Test
    fun `does not import a plan that matches an existing plan by similar name and exercises`() {
        val rows = workout(
            number = 1,
            name = "Full body A",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)", "Pull Up (Assisted)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assert(result.plans == emptyList<WorkoutPlan>())
        assert(result.sessionHistory.single().planId == fullBodyPlan.id)
        assert(result.sessionHistory.single().planName == fullBodyPlan.name)
    }

    @Test
    fun `matches plans when exercise order differs`() {
        val rows = workout(
            number = 1,
            name = "Full body A",
            exercises = listOf("Pull Up (Assisted)", "Squat (Barbell)", "Bench Press (Barbell)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assert(result.plans == emptyList<WorkoutPlan>())
        assert(result.sessionHistory.single().planId == fullBodyPlan.id)
    }

    @Test
    fun `matches plans when some exercises are substituted`() {
        val crunch = ExerciseDefinition(
            "machine-seated-crunch",
            "Machine Seated Crunch",
            MuscleGroup.Core,
            Equipment.Machine,
            Mechanic.Isolation,
        )
        val rows = workout(
            number = 1,
            name = "Full Body A Workout",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)", "Crunch (Machine)"),
        )

        val result = rows.toStrongImport(
            plans = listOf(fullBodyPlan),
            exerciseCatalog = catalog + crunch,
        )

        assert(result.plans == emptyList<WorkoutPlan>())
        assert(result.sessionHistory.single().planId == fullBodyPlan.id)
        assert(result.exercises.isEmpty())
    }

    @Test
    fun `imports a plan that does not match existing plans`() {
        val rows = workout(
            number = 1,
            name = "Upper body A",
            exercises = listOf("Bench Press (Barbell)", "Crunch (Machine)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assert(result.plans.map { it.name } == listOf("Upper body A"))
        assert(result.plans.single().isArchived)
        assert(result.sessionHistory.single().planId == WorkoutPlanId("upper-body-a"))
        assert(result.plans.single().sets.map { it.exercise.id } == listOf("bench-press-bb"))
        assert(result.plans.single().sets.map { it.sets } == listOf(3))
    }

    @Test
    fun `archives unmatched plans executed fewer than five times and keeps busier ones`() {
        val rare = (1..4).flatMap { number ->
            workout(
                number = number,
                name = "Upper body A",
                exercises = listOf("Bench Press (Barbell)"),
                date = "2026-01-0$number 12:00:00",
            )
        }
        val frequent = (1..5).flatMap { number ->
            workout(
                number = number + 10,
                name = "Chest day",
                exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)"),
                date = "2026-02-0$number 12:00:00",
            )
        }
        val matched = workout(
            number = 99,
            name = "Full body A",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)", "Pull Up (Assisted)"),
            date = "2026-03-01 12:00:00",
        )

        val result = (rare + frequent + matched).toStrongImport(
            plans = listOf(fullBodyPlan),
            exerciseCatalog = catalog,
        )

        assert(result.plans.single { it.name == "Upper body A" }.isArchived)
        assert(!result.plans.single { it.name == "Chest day" }.isArchived)
        assert(result.plans.none { it.id == fullBodyPlan.id })
        assert(result.sessionHistory.count { it.planId == WorkoutPlanId("upper-body-a") } == 4)
        assert(result.sessionHistory.count { it.planId == WorkoutPlanId("chest-day") } == 5)
        assert(
            result.sessionHistory.single { it.planName == fullBodyPlan.name }.planId ==
                fullBodyPlan.id,
        )
    }

    @Test
    fun `does not match Full body A against Full Body B despite similar names`() {
        val rows = workout(
            number = 1,
            name = "Full body A",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)", "Pull Up (Assisted)"),
        )
        val fullBodyBPlan = fullBodyPlan.copy(
            id = WorkoutPlanId("full-body-b"),
            name = "Full Body B",
            sets = listOf(
                PlanedExercise(
                    ExerciseDefinition(
                        "rdl-bb",
                        "Romanian Deadlift (barbell)",
                        MuscleGroup.Legs,
                        Equipment.Barbell,
                        Mechanic.Compound,
                    ),
                    sets = 3,
                    repRange = 8..10,
                ),
            ),
        )

        val result = rows.toStrongImport(
            plans = listOf(fullBodyBPlan),
            exerciseCatalog = catalog,
        )

        assert(result.plans.map { it.name } == listOf("Full body A"))
        assert(result.sessionHistory.single().planId == WorkoutPlanId("full-body-a"))
    }

    @Test
    fun `maps strong names onto existing catalog exercises`() {
        val rows = workout(
            number = 1,
            name = "Catalog mapping",
            exercises = listOf(
                "Lat Pulldown (Cable)",
                "Leg Press",
                "Seated Leg Curl (Machine)",
                "Lying Leg Curl (Machine)",
                "Dumbbell lateral raises ",
                "Chest Fly",
            ),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalogExercises)

        assert(result.exercises.isEmpty())
        val imported = result.sessionHistory.single().exercises
        assert(
            imported.map { it.id } == listOf(
                "lat-pulldown",
                "leg-press",
                "seated-leg-curl",
                "leg-curl",
                "lateral-raise",
                "pec-deck",
            ),
        )
        val chestFly = imported.last().exerciseDefinition
        assert(chestFly.name == "Pec Deck")
        assert(chestFly.equipment == Equipment.Machine)
        assert(
            catalogExercises.first { it.id == "lateral-raise-machine" }.equipment ==
                Equipment.Machine,
        )
    }

    @Test
    fun `trims exercise display names from the csv`() {
        val header = listOf(
            "Workout #",
            "Date",
            "Workout Name",
            "Duration (sec)",
            "Exercise Name",
            "Set Order",
            "Weight (kg)",
            "Reps",
            "RPE",
            "Distance (meters)",
            "Seconds",
            "Notes",
            "Workout Notes",
        ).joinToString(";") { "\"$it\"" }
        val row = listOf(
            "1",
            "2026-01-01 12:00:00",
            "Solo",
            "60",
            "Reverse Lunges ",
            "1",
            "20.0",
            "8",
            "",
            "",
            "",
            "",
            "",
        ).joinToString(";") { "\"$it\"" }

        val rows = StrongCsvParser.parse("$header\n$row")
        assert(rows.single().exerciseName == "Reverse Lunges")

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = emptyList())
        assert(result.exercises.isEmpty())
        assert(result.sessionHistory.isEmpty())
    }

    @Test
    fun `does not attach Strong Upper body A to the renamed seed plan`() {
        val result = StrongCsvParser.parse(
            readStrongBackupSample(javaClass.classLoader),
        ).toStrongImport(
            plans = listOf(fullBodyA, fullBodyB, upperBodyA),
            exerciseCatalog = catalogExercises,
        )

        assert(upperBodyA.id == WorkoutPlanId("upper-body-legacy"))
        assert(
            result.plans.map { it.id } == listOf(
                WorkoutPlanId("upper-body-a"),
                WorkoutPlanId("upper-body-b"),
            ),
        )
        assert(result.sessionHistory.none { it.planId == upperBodyA.id })
        assert(result.sessionHistory.count { it.planId == WorkoutPlanId("upper-body-a") } == 3)
    }

    @Test
    fun `imports zero-load work sets and warmups from the strong backup sample`() {
        val result = StrongCsvParser.parse(
            readStrongBackupSample(javaClass.classLoader),
        ).toStrongImport(
            plans = listOf(fullBodyA, fullBodyB),
            exerciseCatalog = catalogExercises,
        )

        assert(
            result.sessionHistory.sumOf { session ->
                session.exercises.sumOf { exercise -> exercise.sets.count { it.weight == null } }
            } == 29,
        )
        assert(
            result.sessionHistory.sumOf { session ->
                session.exercises.sumOf { exercise ->
                    exercise.warmupSets.orEmpty().count { it.weight == null }
                }
            } == 15,
        )

        val mixedPullUp = result.sessionHistory
            .first { it.startedAt == LocalDateTime.of(2026, 4, 7, 17, 29, 3) }
            .exercises
            .first { it.id == "assisted-pull-ups" }
        assert(
            mixedPullUp.sets.map { it.weight to it.reps } == listOf(
                null to 8,
                Weight.kilograms(12.5f) to 9,
                Weight.kilograms(12.5f) to 8,
            ),
        )

        val unassisted = result.sessionHistory
            .first { it.startedAt == LocalDateTime.of(2026, 4, 28, 17, 15, 54) }
            .exercises
            .first { it.id == "assisted-pull-ups" }
        assert(unassisted.sets.map { it.weight to it.reps } == listOf(null to 8, null to 6))
        assert(unassisted.sets.all { it.oneRm == null })
    }

    @Test
    fun `keeps an exercise when every work set is zero load`() {
        val rows = listOf(
            setRow("Pull Up (Assisted)", "1", 0.0, 8),
            setRow("Pull Up (Assisted)", "2", 0.0, 6),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        val pullUp = result.sessionHistory.single().exercises.single()
        assert(pullUp.id == "pullup-assisted")
        assert(pullUp.sets.map { it.weight to it.reps } == listOf(null to 8, null to 6))
    }

    @Test
    fun `keeps a zero-load warmup on a loaded exercise`() {
        val rows = listOf(
            setRow("Bulgarian Split Squat", "W", 0.0, 6),
            setRow("Bulgarian Split Squat", "1", 14.0, 8),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalogExercises)

        val splitSquat = result.sessionHistory.single().exercises.single()
        assert(splitSquat.id == "bulgarian-split-squat")
        assert(splitSquat.warmupSets?.map { it.weight to it.reps } == listOf(null to 6))
        assert(splitSquat.sets.map { it.weight to it.reps } == listOf(Weight.kilograms(14f) to 8))
    }

    @Test
    fun `still drops set rows without positive reps`() {
        val rows = listOf(
            setRow("Bench Press (Barbell)", "1", 0.0, 0),
            setRow("Bench Press (Barbell)", "2", 50.0, 8),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        assert(
            result.sessionHistory.single().exercises.single().sets.map { it.weight to it.reps } ==
                listOf(Weight.kilograms(50) to 8),
        )
    }

    private fun setRow(
        exerciseName: String,
        setOrder: String,
        weightKg: Double?,
        reps: Int?,
    ): StrongCsvRow = StrongCsvRow(
        workoutNumber = 1,
        date = "2026-01-01 12:00:00",
        workoutName = "Zero load",
        durationSec = 1800,
        exerciseName = exerciseName,
        setOrder = setOrder,
        weightKg = weightKg,
        reps = reps,
        rpe = null,
        distanceMeters = null,
        seconds = null,
        notes = null,
        workoutNotes = null,
    )

    private fun workout(
        number: Int,
        name: String,
        exercises: List<String>,
        date: String = "2026-01-01 12:00:00",
        durationSec: Int = 1800,
        workoutNotes: String? = null,
        exerciseNotes: Map<String, List<String>> = emptyMap(),
    ): List<StrongCsvRow> = exercises.flatMap { exerciseName ->
        val noteRows = exerciseNotes[exerciseName].orEmpty().map { note ->
            StrongCsvRow(
                workoutNumber = number,
                date = date,
                workoutName = name,
                durationSec = durationSec,
                exerciseName = exerciseName,
                setOrder = "Note",
                weightKg = null,
                reps = null,
                rpe = null,
                distanceMeters = null,
                seconds = null,
                notes = note,
                workoutNotes = workoutNotes,
            )
        }
        val setRows = (1..3).map { setNumber ->
            StrongCsvRow(
                workoutNumber = number,
                date = date,
                workoutName = name,
                durationSec = durationSec,
                exerciseName = exerciseName,
                setOrder = setNumber.toString(),
                weightKg = 20.0,
                reps = 8,
                rpe = null,
                distanceMeters = null,
                seconds = null,
                notes = null,
                workoutNotes = workoutNotes,
            )
        }
        noteRows + setRows
    }
}
