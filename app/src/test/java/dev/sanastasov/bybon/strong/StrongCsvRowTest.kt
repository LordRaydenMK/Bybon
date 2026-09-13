package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class StrongCsvRowTest {

    private val bench = ExerciseDefinition(
        "bench-press-bb",
        "Bench Press (barbell)",
        MuscleGroup.Chest,
        Equipment.Barbell,
    )
    private val squat = ExerciseDefinition(
        "squat-bb",
        "Squat (barbell)",
        MuscleGroup.Legs,
        Equipment.Barbell,
    )
    private val pullUp = ExerciseDefinition(
        "pullup-assisted",
        "Pull Up (assisted)",
        MuscleGroup.Back,
        Equipment.AssistedBodyWeight,
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
        ).toStrongImport()

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
        assert(firstRdl.id == "rdl-bb")
        assert(firstRdl.warmupSets?.size == 2)
        assert(
            firstRdl.warmupSets?.map { it.weight to it.reps } == listOf(
                Weight.kilograms(20f) to 8,
                Weight.kilograms(35f) to 4,
            ),
        )
        assert(firstRdl.sets.size == 3)
        assert(firstRdl.restAfterWorkSet == 120.seconds)
        val firstFullBodyA = result.sessionHistory.first { it.planId == fullBodyA.id }
        assert(
            firstFullBodyA.exercises.first { it.id == "bench-press-bb" }.restAfterWorkSet ==
                120.seconds,
        )
        assert(
            firstFullBodyA.exercises.first { it.id == "leg-curl" }.restAfterWorkSet == 90.seconds,
        )
        assert(
            firstFullBodyA.exercises.first { it.id == "skullcrusher-db" }.restAfterWorkSet ==
                60.seconds,
        )
        val crunch = result.sessionHistory
            .flatMap { it.exercises }
            .first { it.id == "crunch-machine" }
        assert(crunch.restAfterWorkSet == 120.seconds)
    }

    @Test
    fun `imports only exercises that do not already exist in Bybon`() {
        val result = StrongCsvParser.parse(
            readStrongBackupSample(javaClass.classLoader),
        ).toStrongImport()

        assert(result.exercises.map { it.name } == listOf("Crunch (Machine)"))
        assert(result.exercises.single().id == "crunch-machine")
        assert(result.exercises.single().primaryMuscleGroup == MuscleGroup.Core)
        assert(result.exercises.single().equipment == Equipment.Machine)
    }

    @Test
    fun `imports only plans that do not match existing Bybon plans`() {
        val result = StrongCsvParser.parse(
            readStrongBackupSample(javaClass.classLoader),
        ).toStrongImport()

        assert(result.plans.map { it.name } == listOf("Upper body A", "Upper body B"))
        assert(
            result.plans.first { it.name == "Upper body A" }.sets.map { it.exercise.id } == listOf(
                "bench-press-bb",
                "pullup-assisted",
                "upright-row-db",
                "skullcrusher-db",
                "crunch-machine",
            ),
        )
        assert(
            result.plans.first { it.name == "Upper body B" }.sets.map { it.exercise.id } == listOf(
                "incline-bench-press-db",
                "incline-row-db",
                "lateral-raise-db",
                "incline-curl-db",
                "crunch-machine",
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
    fun `imports an exercise that is missing from the catalog`() {
        val rows = workout(
            number = 1,
            name = "Upper body A",
            exercises = listOf("Bench Press (Barbell)", "Crunch (Machine)"),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        assert(result.exercises.map { it.name } == listOf("Crunch (Machine)"))
        assert(result.sessionHistory.single().exercises.last().id == "crunch-machine")
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
        val rows = workout(
            number = 1,
            name = "Full Body A Workout",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)", "Crunch (Machine)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assert(result.plans == emptyList<WorkoutPlan>())
        assert(result.sessionHistory.single().planId == fullBodyPlan.id)
        assert(result.exercises.map { it.name } == listOf("Crunch (Machine)"))
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
        assert(result.sessionHistory.single().planId == WorkoutPlanId("upper-body-a"))
        assert(
            result.plans.single().sets.map { it.exercise.id } == listOf(
                "bench-press-bb",
                "crunch-machine",
            ),
        )
        assert(result.plans.single().sets.map { it.sets } == listOf(3, 3))
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

    private fun workout(
        number: Int,
        name: String,
        exercises: List<String>,
        date: String = "2026-01-01 12:00:00",
        durationSec: Int = 1800,
    ): List<StrongCsvRow> = exercises.flatMap { exerciseName ->
        (1..3).map { setNumber ->
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
                workoutNotes = null,
            )
        }
    }
}
