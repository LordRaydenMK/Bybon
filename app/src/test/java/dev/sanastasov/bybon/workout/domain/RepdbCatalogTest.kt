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
        assert(bench.equipmentSlug == "barbell")
        assert(catalogExercise("assisted-pull-ups").equipment == Equipment.AssistedBodyWeight)
        assert(catalogExercise("assisted-pull-ups").equipmentSlug == "assisted_pullup_machine")
        assert(catalogExercise("archer-push-ups").equipment == Equipment.Bodyweight)
        assert(catalogExercise("archer-push-ups").equipmentSlug == null)
        assert(catalogExercise("pull-up").equipment == Equipment.Machine)
        assert(catalogExercise("pull-up").equipmentSlug == "pull_up_bar")
        assert(catalogExercise("ez-bar-curl").equipment == Equipment.Machine)
        assert(catalogExercise("ez-bar-curl").equipmentSlug == "ez_bar")
        assert(catalogExercise("smith-machine-squat").equipmentSlug == "smith_machine")
        assert(catalogExercise("iso-lat-row").equipment == Equipment.Machine)
        assert(catalogExercise("iso-lat-row").equipmentSlug == "iso_lateral_row_machine")
        assert(catalogExercise("squat-machine").equipmentSlug == "squat_machine")
        assert(catalogExercise("triceps-press-machine").equipmentSlug == "tricep_press_machine")
        assert(catalogExercise("lateral-raise-machine").equipmentSlug == "lateral_raise_machine")
        assert(catalogExercise("lat-pulldown").bodyPart == BodyPart.Back)
        assert(catalogExercise("squat").bodyPart == BodyPart.UpperLegs)
        assert(catalogExercise("standing-calf-raise").bodyPart == BodyPart.LowerLegs)
        assert(catalogExercise("barbell-wrist-curl").bodyPart == BodyPart.LowerArms)
        assert(catalogExercise("back-lever").bodyPart == BodyPart.FullBody)
        val isoLatRow = catalogExercise("iso-lat-row")
        assert(isoLatRow.bodyPart == BodyPart.Back)
        assert(isoLatRow.primaryMuscles == listOf("latissimus_dorsi", "rhomboids"))
        assert(isoLatRow.secondaryMuscles == listOf("biceps_brachii", "posterior_deltoid"))
        val squatMachine = catalogExercise("squat-machine")
        assert(squatMachine.primaryMuscles == listOf("gluteus_maximus", "quadriceps"))
        assert(squatMachine.secondaryMuscles == listOf("erector_spinae", "hamstrings"))
        val tricepsPress = catalogExercise("triceps-press-machine")
        assert(tricepsPress.primaryMuscles == listOf("triceps_brachii"))
        assert(tricepsPress.secondaryMuscles.isEmpty())
        val lateralRaise = catalogExercise("lateral-raise-machine")
        assert(lateralRaise.primaryMuscles == listOf("lateral_deltoid"))
        assert(lateralRaise.secondaryMuscles == listOf("anterior_deltoid", "trapezius"))
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
