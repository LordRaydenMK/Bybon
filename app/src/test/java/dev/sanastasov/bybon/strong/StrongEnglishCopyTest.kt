package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.catalogExercises
import org.junit.Test

class StrongEnglishCopyTest {

    @Test
    fun `leaves description instructions and tips null on a created Strong exercise`() {
        val rows = listOf(
            StrongCsvRow(
                workoutNumber = 1,
                date = "2026-01-01 12:00:00",
                workoutName = "Upper body A",
                durationSec = 60,
                exerciseName = "Some New Lift",
                setOrder = "1",
                weightKg = 20.0,
                reps = 8,
                rpe = null,
                distanceMeters = null,
                seconds = null,
                notes = null,
                workoutNotes = null,
            ),
        )

        val imported = rows.toStrongImport(plans = emptyList(), exerciseCatalog = catalogExercises)
            .exercises
            .single()

        assert(imported.id == "some-new-lift")
        assert(imported.description == null)
        assert(imported.instructions == null)
        assert(imported.tips == null)
    }
}
