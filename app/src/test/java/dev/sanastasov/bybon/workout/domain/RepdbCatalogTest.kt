package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration.Companion.minutes
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
        assert(bench.bodyPart == BodyPart.Chest)
        assert(bench.primaryMuscles == listOf("pectoralis_major"))
        assert(bench.secondaryMuscles == listOf("anterior_deltoid", "triceps_brachii"))
        assert(bench.equipment == Equipment.Barbell)
        assert(catalogExercise("assisted-pull-ups").equipment == Equipment.AssistedBodyWeight)
        assert(catalogExercise("archer-push-ups").equipment == Equipment.Bodyweight)
        assert(catalogExercise("lat-pulldown").bodyPart == BodyPart.Back)
        assert(catalogExercise("squat").bodyPart == BodyPart.UpperLegs)
        assert(catalogExercise("standing-calf-raise").bodyPart == BodyPart.LowerLegs)
        assert(catalogExercise("barbell-wrist-curl").bodyPart == BodyPart.LowerArms)
        assert(catalogExercise("back-lever").bodyPart == BodyPart.FullBody)
        assert(catalogExercise("iso-lat-row").bodyPart == BodyPart.Back)
        assert(catalogExercise("iso-lat-row").primaryMuscles.isEmpty())
        assert(catalogExercises.none { "Pose" in it.name || "Pilates" in it.name })
        assert(bench.mechanic == Mechanic.Compound)
        assert(catalogExercise("face-pull").mechanic == Mechanic.Compound)
        assert(catalogExercise("seated-leg-curl").mechanic == Mechanic.Isolation)
        assert(catalogExercise("bench-press").defaultRest == 2.minutes)
        assert(catalogExercise("face-pull").defaultRest == 2.minutes)
        assert(catalogExercise("seated-leg-curl").defaultRest == 1.minutes)
        assert(catalogExercise("iso-lat-row").defaultRest == 2.minutes)
        assert(catalogExercise("lateral-raise-machine").defaultRest == 1.minutes)
    }
}
