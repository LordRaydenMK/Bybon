package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.catalogExercises
import org.junit.Test

class StrongCsvCatalogMappingTest {

    @Test
    fun `maps remaining Strong backup names onto shipped RepDB exercises`() {
        val rows = workout(
            exercises = listOf(
                "Lateral Raise (Cable)",
                "Chest Press (Machine)",
                "Deadlift (Barbell)",
                "Overhead Press (Barbell)",
                "Standing Calf Raise (Machine)",
                "Face Pull (Cable)",
                "Cable Pushdown (rope)",
                "Incline Dumbbell Overhead Extension",
                "Cable Crossover",
            ),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalogExercises)

        assert(result.exercises.isEmpty())
        assert(
            result.sessionHistory.single().exercises.map { it.id } == listOf(
                "cable-lateral-raise",
                "chest-press-machine",
                "deadlift",
                "ohp",
                "standing-calf-raise",
                "face-pull",
                "tricep-pushdown",
                "overhead-tricep-extension",
                "cable-fly",
            ),
        )
    }

    @Test
    fun `does not alias Strong names that are not the same RepDB exercise`() {
        val rows = workout(
            exercises = listOf(
                "Chest Fly (Band)",
                "Triceps Extension (Cable)",
                "Iso-Lateral Chest Press (Machine)",
            ),
        )

        val result = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalogExercises)

        assert(
            result.exercises.map { it.id } == listOf(
                "chest-fly-band",
                "triceps-extension-cable",
                "iso-lateral-chest-press-machine",
            ),
        )
        assert(result.sessionHistory.single().exercises.none { it.id == "chest-press-machine" })
        assert(result.sessionHistory.single().exercises.none { it.id == "tricep-pushdown" })
    }

    private fun workout(exercises: List<String>): List<StrongCsvRow> = exercises.flatMap { name ->
        (1..3).map { setNumber ->
            StrongCsvRow(
                workoutNumber = 1,
                date = "2026-01-01 12:00:00",
                workoutName = "Catalog mapping",
                durationSec = 1800,
                exerciseName = name,
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
