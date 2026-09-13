package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            PlanedExercise(bench, 3, 8..10),
            PlanedExercise(squat, 3, 8..10),
            PlanedExercise(pullUp, 3, 6..10),
        ),
    )

    @Test
    fun `reads strong backup sample into dto rows`() {
        val rows = StrongCsvParser.readSample(javaClass.classLoader)

        assertEquals(2053, rows.size)
    }

    @Test
    fun `imports completed session history from the strong backup sample`() {
        val result = StrongCsvParser.readSample(javaClass.classLoader).toStrongImport()

        assertEquals(52, result.sessionHistory.size)
        assertTrue(result.sessionHistory.all { it.state is WorkoutState.Completed })
        assertEquals(
            setOf(fullBodyA.id, fullBodyB.id, WorkoutPlanId("upper-body-a"), WorkoutPlanId("upper-body-b")),
            result.sessionHistory.map { it.planId }.toSet(),
        )
        assertEquals(
            23,
            result.sessionHistory.count { it.planId == fullBodyA.id },
        )
        assertEquals(
            23,
            result.sessionHistory.count { it.planId == fullBodyB.id },
        )
    }

    @Test
    fun `imports only exercises that do not already exist in Bybon`() {
        val result = StrongCsvParser.readSample(javaClass.classLoader).toStrongImport()

        assertEquals(listOf("Crunch (Machine)"), result.exercises.map { it.name })
        assertEquals("crunch-machine", result.exercises.single().id)
        assertEquals(MuscleGroup.Core, result.exercises.single().primaryMuscleGroup)
        assertEquals(Equipment.Machine, result.exercises.single().equipment)
    }

    @Test
    fun `imports only plans that do not match existing Bybon plans`() {
        val result = StrongCsvParser.readSample(javaClass.classLoader).toStrongImport()

        assertEquals(listOf("Upper body A", "Upper body B"), result.plans.map { it.name })
        assertEquals(
            listOf("bench-press-bb", "pullup-assisted", "upright-row-db", "skullcrusher-db", "crunch-machine"),
            result.plans.first { it.name == "Upper body A" }.sets.map { it.exercise.id },
        )
        assertEquals(
            listOf(
                "incline-bench-press-db",
                "incline-row-db",
                "lateral-raise-db",
                "incline-curl-db",
                "crunch-machine",
            ),
            result.plans.first { it.name == "Upper body B" }.sets.map { it.exercise.id },
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

        assertEquals(emptyList<ExerciseDefinition>(), result.exercises)
        assertEquals(listOf("bench-press-bb", "squat-bb"), result.sessionHistory.single().exercises.map { it.id })
    }

    @Test
    fun `imports an exercise that is missing from the catalog`() {
        val rows = workout(
            number = 1,
            name = "Upper body A",
            exercises = listOf("Bench Press (Barbell)", "Crunch (Machine)"),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        assertEquals(listOf("Crunch (Machine)"), result.exercises.map { it.name })
        assertEquals("crunch-machine", result.sessionHistory.single().exercises.last().id)
    }

    @Test
    fun `does not import a plan that matches an existing plan by similar name and exercises`() {
        val rows = workout(
            number = 1,
            name = "Full body A",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)", "Pull Up (Assisted)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assertEquals(emptyList<WorkoutPlan>(), result.plans)
        assertEquals(fullBodyPlan.id, result.sessionHistory.single().planId)
        assertEquals(fullBodyPlan.name, result.sessionHistory.single().planName)
    }

    @Test
    fun `matches plans when exercise order differs`() {
        val rows = workout(
            number = 1,
            name = "Full body A",
            exercises = listOf("Pull Up (Assisted)", "Squat (Barbell)", "Bench Press (Barbell)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assertEquals(emptyList<WorkoutPlan>(), result.plans)
        assertEquals(fullBodyPlan.id, result.sessionHistory.single().planId)
    }

    @Test
    fun `matches plans when some exercises are substituted`() {
        val rows = workout(
            number = 1,
            name = "Full Body A Workout",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)", "Crunch (Machine)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assertEquals(emptyList<WorkoutPlan>(), result.plans)
        assertEquals(fullBodyPlan.id, result.sessionHistory.single().planId)
        assertEquals(listOf("Crunch (Machine)"), result.exercises.map { it.name })
    }

    @Test
    fun `imports a plan that does not match existing plans`() {
        val rows = workout(
            number = 1,
            name = "Upper body A",
            exercises = listOf("Bench Press (Barbell)", "Crunch (Machine)"),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assertEquals(listOf("Upper body A"), result.plans.map { it.name })
        assertEquals(WorkoutPlanId("upper-body-a"), result.sessionHistory.single().planId)
        assertEquals(
            listOf("bench-press-bb", "crunch-machine"),
            result.plans.single().sets.map { it.exercise.id },
        )
        assertEquals(listOf(3, 3), result.plans.single().sets.map { it.sets })
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
                    ExerciseDefinition("rdl-bb", "Romanian Deadlift (barbell)", MuscleGroup.Legs, Equipment.Barbell),
                    3,
                    8..10,
                ),
            ),
        )

        val result = rows.toStrongImport(
            plans = listOf(fullBodyBPlan),
            exerciseCatalog = catalog,
        )

        assertEquals(listOf("Full body A"), result.plans.map { it.name })
        assertEquals(WorkoutPlanId("full-body-a"), result.sessionHistory.single().planId)
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
