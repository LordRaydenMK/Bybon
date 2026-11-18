package dev.sanastasov.bybon.workout.ui.summary

import dev.sanastasov.bybon.workout.data.completedAssistedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.catalogExercise
import java.time.LocalDateTime
import org.junit.Test

class WorkoutSummaryAssistedLoadTest {

    @Test
    fun `summary uses zero kilograms when an assisted set has no load`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                WorkoutExercise(
                    exerciseDefinition = catalogExercise("assisted-pull-ups"),
                    repRange = 6..10,
                    sets = listOf(
                        ExerciseSet(
                            catalogExercise("assisted-pull-ups"),
                            null,
                            8,
                            SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )

        val content = session.toSummaryUi()
        val exercise = content.exercises.single()
        val set = exercise.sets.single()
        assert(set.weightKg == "0")
        assert(set.effectiveLoadKg == null)
        assert(exercise.bodyWeightLabel == null)
        assert(exercise.showEffectiveLoad)
        assert(set.reps == 8)
        assert(set.oneRm == null)
        assert(set.previousOneRm == null)
        assert(exercise.volumeLoadLabel == "0 kg")
        assert(content.sessionSummary.volumeLoadLabel == "0 kg")
        assert(content.sessionSummary.setPerformance == null)
        assert(content.sessionSummary.exercisePerformance.isEmpty())
    }

    @Test
    fun `summary assisted work sets expose body weight and effective load`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(
                completedAssistedExercise(
                    "assisted-pull-ups",
                    80f,
                    25f to 8,
                    null to 6,
                ),
            ),
        )

        val exercise = session.toSummaryUi().exercises.single()
        assert(exercise.bodyWeightLabel == "80 kg")
        assert(exercise.showEffectiveLoad)
        assert(exercise.sets[0].weightKg == "-25")
        assert(exercise.sets[0].effectiveLoadKg == "55")
        assert(exercise.sets[1].weightKg == "0")
        assert(exercise.sets[1].effectiveLoadKg == "80")
    }

    @Test
    fun `summary assisted warmup omits effective load`() {
        val definition = catalogExercise("assisted-pull-ups")
        val bodyWeight = Weight.kilograms(80)
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(
                WorkoutExercise(
                    exerciseDefinition = definition,
                    repRange = 6..10,
                    warmupSets = listOf(
                        ExerciseSet(
                            definition,
                            Weight.kilograms(40),
                            5,
                            SetState.Completed(),
                            bodyWeight = bodyWeight,
                        ),
                    ),
                    sets = listOf(
                        ExerciseSet(
                            definition,
                            Weight.kilograms(20),
                            8,
                            SetState.Completed(),
                            bodyWeight = bodyWeight,
                        ),
                    ),
                ),
            ),
        )

        val exercise = session.toSummaryUi().exercises.single()
        val warmup = exercise.sets.single { it.isWarmup }
        val work = exercise.sets.single { !it.isWarmup }
        assert(exercise.bodyWeightLabel == "80 kg")
        assert(exercise.showEffectiveLoad)
        assert(warmup.weightKg == "-40")
        assert(warmup.effectiveLoadKg == null)
        assert(work.weightKg == "-20")
        assert(work.effectiveLoadKg == "60")
    }

    @Test
    fun `summary still uses dash when a barbell set has no load`() {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(
                WorkoutExercise(
                    exerciseDefinition = catalogExercise("romanian-deadlift"),
                    repRange = 8..12,
                    sets = listOf(
                        ExerciseSet(
                            catalogExercise("romanian-deadlift"),
                            null,
                            8,
                            SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )

        val exercise = session.toSummaryUi().exercises.single()
        val set = exercise.sets.single()
        assert(set.weightKg == null)
        assert(set.effectiveLoadKg == null)
        assert(exercise.bodyWeightLabel == null)
        assert(!exercise.showEffectiveLoad)
    }
}
