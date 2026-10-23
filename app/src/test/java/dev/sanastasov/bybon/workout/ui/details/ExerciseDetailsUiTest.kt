package dev.sanastasov.bybon.workout.ui.details

import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseImages
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.domain.exercisePerformance
import java.time.LocalDateTime
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

    @Test
    fun `maps PR chart points and working-set history`() {
        val older = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(completedExercise("bench-press", 50f to 5, 45f to 12)),
        )
        val newer = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 10, 1, 18, 0),
            exercises = listOf(
                completedExercise("bench-press", 80f to 8).copy(
                    warmupSets = listOf(
                        ExerciseSet(
                            exerciseDefinition = catalogExercise("bench-press"),
                            weight = Weight.kilograms(40),
                            reps = 5,
                            setState = SetState.Completed,
                        ),
                    ),
                ),
            ),
        )
        val now = LocalDateTime.of(2026, 10, 22, 18, 0)
        val ui = catalogExercise("bench-press").toDetailsUi(
            listOf(older, newer).exercisePerformance("bench-press", now),
        )

        val olderOneRm = Weight.kilograms(estimateOneRmKg(45f, 12))
        val newerOneRm = Weight.kilograms(estimateOneRmKg(80f, 8))
        assert(ui.personalRecord == PersonalRecordUi(newerOneRm, "1 Oct 2026"))
        assert(
            ui.oneRmTrend == listOf(
                ExerciseOneRmPointUi("10 Aug", olderOneRm.kilogramsValue, olderOneRm, "45", 12),
                ExerciseOneRmPointUi("1 Oct", newerOneRm.kilogramsValue, newerOneRm, "80", 8),
            ),
        )
        assert(oneRmTrendPopupText(ui.oneRmTrend, 1) == "1 Oct  80 kg x 8")
        assert(ui.recentSessions.map { it.planName } == listOf("Full Body B", "Full Body A"))
        assert(ui.recentSessions.first().dateLabel == "1 Oct 2026")
        assert(
            ui.recentSessions.first().sets == listOf(
                ExerciseDetailsSetUi(1, "80", 8, Weight.kilograms(estimateOneRmKg(80f, 8))),
            ),
        )
        assert(ui.recentSessions.last().sets.size == 2)
        val firstSet = ui.recentSessions.first().sets.single()
        assert(historySetLoadLabel(firstSet) == "1. 80 kg x 8")
        assert(historySetOneRmLabel(firstSet) == "${newerOneRm.kilograms} kg 1RM")
    }
}
