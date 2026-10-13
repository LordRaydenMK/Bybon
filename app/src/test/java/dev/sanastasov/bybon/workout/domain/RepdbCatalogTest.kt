package dev.sanastasov.bybon.workout.domain

import kotlin.time.Duration.Companion.minutes
import okhttp3.HttpUrl.Companion.toHttpUrl
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

    @Test
    fun `stores force difficulty and tags`() {
        val bench = catalogExercise("bench-press")
        assert(bench.forceType == ForceType.Push)
        assert(bench.difficulty == Difficulty.Intermediate)
        assert(
            bench.tags == listOf(
                "powerlifting",
                "push_day",
                "big_three",
                "knee_safe",
                "no_axial_load",
                "lower_back_safe",
                "shoulder_safe",
                "requires_bench",
            ),
        )
        assert(catalogExercise("lat-pulldown").forceType == ForceType.Pull)
        assert(catalogExercise("lat-pulldown").difficulty == Difficulty.Beginner)
        assert(catalogExercise("high-plank").forceType == ForceType.Static)
        assert(catalogExercise("thruster").forceType == ForceType.Dynamic)
        assert(catalogExercise("smith-machine-squat").tags.isEmpty())

        val isoLatRow = catalogExercise("iso-lat-row")
        assert(isoLatRow.forceType == ForceType.Pull)
        assert(isoLatRow.difficulty == Difficulty.Intermediate)
        val squatMachine = catalogExercise("squat-machine")
        assert(squatMachine.forceType == ForceType.Push)
        assert(squatMachine.difficulty == Difficulty.Intermediate)
        assert(squatMachine.tags == listOf("leg_day"))
        val tricepsPress = catalogExercise("triceps-press-machine")
        assert(tricepsPress.forceType == ForceType.Push)
        assert(tricepsPress.difficulty == Difficulty.Beginner)
        val lateralRaise = catalogExercise("lateral-raise-machine")
        assert(lateralRaise.forceType == ForceType.Push)
        assert(lateralRaise.difficulty == Difficulty.Beginner)
    }

    @Test
    fun `stores english description instructions and tips`() {
        val bench = catalogExercise("bench-press")
        assert(
            bench.description ==
                "A compound pushing movement that targets the chest, shoulders, and triceps.",
        )
        assert(
            bench.instructions == listOf(
                "Lie flat on a bench with your eyes under the bar.",
                "Grip the bar slightly wider than shoulder-width.",
                "Unrack the bar and lower it to your mid-chest.",
                "Press the bar back up to full arm extension.",
                "Repeat for the desired number of reps.",
            ),
        )
        assert(
            bench.tips == listOf(
                "Keep your feet flat on the floor.",
                "Maintain a slight arch in your lower back.",
                "Drive through your legs for stability.",
            ),
        )

        val isoLatRow = catalogExercise("iso-lat-row")
        assert(
            isoLatRow.description == "A pin-loaded row that loads the lats and upper back.",
        )
        assert(isoLatRow.instructions == null)
        assert(isoLatRow.tips == null)
        assert(catalogExercise("squat-machine").description == "A plate-loaded squat on a sled.")
        assert(
            catalogExercise("triceps-press-machine").description ==
                "A pin-loaded press that isolates the triceps.",
        )
        assert(
            catalogExercise("lateral-raise-machine").description ==
                "A pin-loaded raise that lifts the arms out to the sides.",
        )
        assert(catalogExercise("triceps-press-machine").instructions == null)
        assert(catalogExercise("lateral-raise-machine").tips == null)
    }

    @Test
    fun `stores pinned image urls and leaves bybon rows without images`() {
        val commit = "9ed9357f09c7566ea0256c57ebd6374ebb8b575e"
        val prefix =
            "https://raw.githubusercontent.com/RepDB/exercise-dataset/$commit/images/flat"
        val bench = catalogExercise("bench-press")
        assert(
            bench.images == ExerciseImages.StartAndPeak(
                "$prefix/bench-press-start.webp".toHttpUrl(),
                "$prefix/bench-press-peak.webp".toHttpUrl(),
            ),
        )
        assert(bench.images?.libraryUrl == "$prefix/bench-press-peak.webp".toHttpUrl())
        assert(
            catalogExercise("high-plank").images == ExerciseImages.Main(
                "$prefix/high-plank-main.webp".toHttpUrl(),
            ),
        )
        assert(catalogExercise("iso-lat-row").images == null)
        assert(catalogExercise("squat-machine").images == null)
    }
}
