package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class RepdbCatalogTest {

    @Test
    fun `ships filtered repdb names plus the exercises repdb does not have`() {
        assert(repdbCatalogExercises.size == 382)
        assert(
            bybonCatalogExercises.map { it.id } == listOf(
                "iso-lat-row",
                "squat-machine",
                "triceps-press-machine",
                "lateral-raise-machine",
            ),
        )
        assert(catalogExercises.map { it.id }.toSet().size == catalogExercises.size)

        val bench = catalogExercise("bench-press")
        assert(bench.name == "Barbell Bench Press")
        assert(bench.primaryMuscleGroup == MuscleGroup.Chest)
        assert(bench.equipment == Equipment.Barbell)
        assert(catalogExercise("assisted-pull-ups").equipment == Equipment.AssistedBodyWeight)
        assert(catalogExercise("archer-push-ups").equipment == Equipment.Bodyweight)
        assert(catalogExercise("lat-pulldown").primaryMuscleGroup == MuscleGroup.Back)
        assert(catalogExercises.none { "Pose" in it.name || "Pilates" in it.name })
        assert(catalogExercise("bench-press").defaultRest == 2.minutes)
        assert(catalogExercise("face-pull").defaultRest == 1.minutes)
        assert(catalogExercise("seated-leg-curl").defaultRest == 90.seconds)
    }
}
