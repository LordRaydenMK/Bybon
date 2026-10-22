package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.BodyPart
import dev.sanastasov.bybon.workout.domain.Difficulty
import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseImages
import dev.sanastasov.bybon.workout.domain.ForceType
import dev.sanastasov.bybon.workout.domain.Mechanic
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class ExerciseMappingTest {

    @Test
    fun `round trip keeps domain fields and stores enums lists and images as columns`() {
        val definition = ExerciseDefinition(
            id = "bench-press",
            name = "Barbell Bench Press",
            bodyPart = BodyPart.Chest,
            equipment = Equipment.Barbell,
            mechanic = Mechanic.Compound,
            primaryMuscles = listOf("pectoralis_major"),
            secondaryMuscles = listOf("anterior_deltoid", "triceps_brachii"),
            equipmentSlug = "barbell",
            forceType = ForceType.Push,
            difficulty = Difficulty.Intermediate,
            tags = listOf("push_day"),
            description = "A press.",
            instructions = listOf("Lie down.", "Press the \"bar\" up."),
            tips = listOf("Don't bounce."),
            images = ExerciseImages.StartAndPeak(
                start = "https://example.test/bench-start.webp".toHttpUrl(),
                peak = "https://example.test/bench-peak.webp".toHttpUrl(),
            ),
        )

        val entity = definition.toEntity(source = ExerciseSource.SHIPPED)

        assert(entity.toDefinition() == definition)
        assert(entity.bodyPart == "Chest")
        assert(entity.equipment == "Barbell")
        assert(entity.mechanic == "Compound")
        assert(entity.forceType == "Push")
        assert(entity.difficulty == "Intermediate")
        assert(entity.primaryMuscles == """["pectoralis_major"]""")
        assert(entity.secondaryMuscles == """["anterior_deltoid","triceps_brachii"]""")
        assert(entity.tags == """["push_day"]""")
        assert(entity.instructions == """["Lie down.","Press the \"bar\" up."]""")
        assert(entity.tips == """["Don't bounce."]""")
        assert(entity.imageStart == "https://example.test/bench-start.webp")
        assert(entity.imagePeak == "https://example.test/bench-peak.webp")
        assert(entity.imageMain == null)
        assert(entity.source == ExerciseSource.SHIPPED)
        assert(entity.isArchived == false)
    }

    @Test
    fun `main image and null lists stay mutually exclusive`() {
        val definition = ExerciseDefinition(
            id = "high-plank",
            name = "High Plank",
            instructions = null,
            tips = null,
            images = ExerciseImages.Main("https://example.test/plank-main.webp".toHttpUrl()),
        )

        val entity = definition.toEntity(source = ExerciseSource.IMPORT, isArchived = true)

        assert(entity.toDefinition() == definition)
        assert(entity.imageStart == null)
        assert(entity.imagePeak == null)
        assert(entity.imageMain == "https://example.test/plank-main.webp")
        assert(entity.instructions == null)
        assert(entity.tips == null)
        assert(entity.primaryMuscles == "[]")
        assert(entity.source == ExerciseSource.IMPORT)
        assert(entity.isArchived)
    }
}
