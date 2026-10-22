package dev.sanastasov.bybon.workout.ui.details

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseImages
import dev.sanastasov.bybon.workout.domain.catalogExercise
import org.junit.Test

class ExerciseDetailsUiTest {

    @Test
    fun `maps a full catalog exercise`() {
        val bench = catalogExercise("bench-press").toDetailsUi()

        assert(bench.name == "Barbell Bench Press")
        assert(bench.images is ExerciseImages.StartAndPeak)
        assert(
            bench.facts == listOf(
                "Chest",
                "Barbell",
                "Compound",
                "Push",
                "Intermediate",
            ),
        )
        assert(bench.primaryMuscles == listOf("Pectoralis Major"))
        assert(bench.secondaryMuscles == listOf("Anterior Deltoid", "Triceps Brachii"))
        assert(
            bench.description ==
                "A compound pushing movement that targets the chest, shoulders, and triceps.",
        )
        assert(bench.instructions.size == 5)
        assert(bench.tips.size == 3)
        assert(bench.tags.contains("No Axial Load"))
        assert(bench.tags.contains("Push Day"))
    }

    @Test
    fun `maps a bybon exercise without instructions images or tips`() {
        val row = catalogExercise("iso-lat-row").toDetailsUi()

        assert(row.name == "Iso-Lateral Row (machine)")
        assert(row.images == null)
        assert(row.facts == listOf("Back", "Machine", "Compound", "Pull", "Intermediate"))
        assert(row.primaryMuscles == listOf("Latissimus Dorsi", "Rhomboids"))
        assert(row.secondaryMuscles == listOf("Biceps Brachii", "Posterior Deltoid"))
        assert(row.description == "A pin-loaded row that loads the lats and upper back.")
        assert(row.instructions.isEmpty())
        assert(row.tips.isEmpty())
        assert(row.tags.contains("Knee Safe"))
    }

    @Test
    fun `hides missing catalog fields on a sparse import`() {
        val sparse = ExerciseDefinition(
            id = "custom-curl",
            name = "Custom Curl",
        ).toDetailsUi()

        assert(sparse.name == "Custom Curl")
        assert(sparse.images == null)
        assert(sparse.facts.isEmpty())
        assert(sparse.primaryMuscles.isEmpty())
        assert(sparse.secondaryMuscles.isEmpty())
        assert(sparse.description == null)
        assert(sparse.instructions.isEmpty())
        assert(sparse.tips.isEmpty())
        assert(sparse.tags.isEmpty())
    }

    @Test
    fun `assisted equipment uses the short label`() {
        val pullUp = catalogExercise("assisted-pull-ups").toDetailsUi()

        assert(pullUp.facts.contains("Assisted"))
        assert(!pullUp.facts.contains("AssistedBodyWeight"))
    }

    @Test
    fun `maps a single main image`() {
        val plank = catalogExercise("high-plank").toDetailsUi()

        assert(plank.images is ExerciseImages.Main)
        assert(plank.facts.contains("Static"))
    }
}
