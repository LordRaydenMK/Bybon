package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.Mechanic
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import org.junit.Test

class StrongCsvNotesTest {

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
    fun `maps workout notes to the session and note rows to exercises`() {
        val rows = workout(
            number = 1,
            name = "Upper body A",
            exercises = listOf("Bench Press (Barbell)", "Crunch (Machine)"),
            workoutNotes = "Full body B without legs",
            exerciseNotes = mapOf("Bench Press (Barbell)" to listOf("Rep range 11-15")),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        val session = result.sessionHistory.single()
        assert(session.note == "Full body B without legs")
        assert(
            session.exercises.first { it.id == "bench-press-bb" }.notes ==
                listOf("Rep range 11-15"),
        )
        assert(session.exercises.first { it.id == "crunch-machine" }.notes.isEmpty())
        assert(result.plans.single().description == "Full body B without legs")
        assert(
            result.plans.single().sets.first { it.exercise.id == "bench-press-bb" }.notes ==
                listOf("Rep range 11-15"),
        )
        assert(
            result.plans.single().sets.first { it.exercise.id == "crunch-machine" }.notes.isEmpty(),
        )
    }

    @Test
    fun `keeps multiple note rows as a list on the exercise`() {
        val rows = workout(
            number = 1,
            name = "Upper body A",
            exercises = listOf("Bench Press (Barbell)"),
            exerciseNotes = mapOf(
                "Bench Press (Barbell)" to listOf("Rep range 11-15", "Pause at the bottom"),
            ),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        assert(
            result.sessionHistory.single().exercises.single().notes ==
                listOf("Rep range 11-15", "Pause at the bottom"),
        )
        assert(
            result.plans.single().sets.single().notes ==
                listOf("Rep range 11-15", "Pause at the bottom"),
        )
    }

    @Test
    fun `sets a new plan description from the most common workout notes`() {
        val rows = listOf(
            Triple(1, "2026-01-01 12:00:00", "Cue A"),
            Triple(2, "2026-01-08 12:00:00", "Cue B"),
            Triple(3, "2026-01-15 12:00:00", "Cue A"),
        ).flatMap { (number, date, notes) ->
            workout(
                number = number,
                name = "Upper body A",
                date = date,
                exercises = listOf("Bench Press (Barbell)"),
                workoutNotes = notes,
            )
        }

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        assert(result.plans.single().description == "Cue A")
        assert(result.sessionHistory.map { it.note } == listOf("Cue A", "Cue B", "Cue A"))
    }

    @Test
    fun `uses the last workout notes when frequencies tie`() {
        val rows = workout(
            number = 1,
            name = "Upper body A",
            date = "2026-01-01 12:00:00",
            exercises = listOf("Bench Press (Barbell)"),
            workoutNotes = "Cue A",
        ) + workout(
            number = 2,
            name = "Upper body A",
            date = "2026-01-08 12:00:00",
            exercises = listOf("Bench Press (Barbell)"),
            workoutNotes = "Cue B",
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalog)

        assert(result.plans.single().description == "Cue B")
    }

    @Test
    fun `does not copy exercise notes onto an existing matching plan`() {
        val rows = workout(
            number = 1,
            name = "Full body A",
            exercises = listOf("Bench Press (Barbell)", "Squat (Barbell)", "Pull Up (Assisted)"),
            workoutNotes = "Monday full body workout",
            exerciseNotes = mapOf("Squat (Barbell)" to listOf("Right knee slight pain")),
        )

        val result = rows.toStrongImport(plans = listOf(fullBodyPlan), exerciseCatalog = catalog)

        assert(result.plans == emptyList<WorkoutPlan>())
        val session = result.sessionHistory.single()
        assert(session.note == "Monday full body workout")
        assert(
            session.exercises.first { it.id == "squat-bb" }.notes ==
                listOf("Right knee slight pain"),
        )
        assert(fullBodyPlan.sets.all { it.notes.isEmpty() })
    }

    @Test
    fun `imports session and exercise notes from the strong backup sample`() {
        val result = StrongCsvParser.parse(
            readStrongBackupSample(javaClass.classLoader),
        ).toStrongImport(
            plans = listOf(fullBodyA, fullBodyB),
            exerciseCatalog = catalogExercises,
        )

        val firstFullBodyB = result.sessionHistory.first { it.planId == fullBodyB.id }
        assert(firstFullBodyB.note == "Friday full body workout")
        assert(
            firstFullBodyB.exercises.first { it.id == "incline-db-press" }.notes ==
                listOf("Rep range 11-15"),
        )

        val upperBodyBPlan = result.plans.first { it.name == "Upper body B" }
        assert(upperBodyBPlan.description == "Full body B without legs")
        assert(
            upperBodyBPlan.sets.first { it.exercise.id == "incline-db-press" }.notes ==
                listOf("Rep range 11-15"),
        )

        val lastUpperBodyB = result.sessionHistory.last {
            it.planId == WorkoutPlanId("upper-body-b")
        }
        assert(lastUpperBodyB.note == "Full body B without legs")
        assert(
            lastUpperBodyB.exercises.first { it.id == "incline-db-press" }.notes ==
                listOf("Rep range 11-15"),
        )

        val legCurlNoteSession = result.sessionHistory.first { session ->
            session.exercises.any {
                it.id == "seated-leg-curl" && it.notes == listOf("Rep range: 12-14")
            }
        }
        assert(legCurlNoteSession.planId == fullBodyA.id)
    }

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
