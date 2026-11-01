package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import org.junit.Test

class WorkoutSessionPreviousTest {

    @Test
    fun `withPreviousFrom pairs work and warmup sets by index`() {
        val previous = completedBench(
            work = listOf(80f to 8, 81f to 9, 82f to 10),
            warmups = listOf(20f to 8, 21f to 8, 22f to 8),
        )
        val current = benchExercise(
            work = listOf(85f to 8, 85f to 7),
            warmups = listOf(25f to 6),
        )

        val actual = current.withPreviousFrom(previous)

        assert(
            actual.sets.map { it.previous } == listOf(
                PreviousSetPerformance(Weight.kilograms(80), 8),
                PreviousSetPerformance(Weight.kilograms(81), 9),
            ),
        )
        assert(
            checkNotNull(actual.warmupSets).map { it.previous } == listOf(
                PreviousSetPerformance(Weight.kilograms(20), 8),
            ),
        )
        assert(
            actual.sets.map { it.weight to it.reps } ==
                current.sets.map { it.weight to it.reps },
        )
    }

    @Test
    fun `withPreviousFrom ignores warmup count when pairing work sets`() {
        val previous = completedBench(
            work = listOf(80f to 8, 81f to 9, 82f to 10),
            warmups = listOf(20f to 8, 21f to 8),
        )
        val current = benchExercise(
            work = listOf(85f to 8, 85f to 7, 85f to 6),
            warmups = listOf(25f to 6, 30f to 4, 35f to 3),
        )

        val actual = current.withPreviousFrom(previous)

        assert(actual.sets.map { it.previous } == previousWorkPerformance())
        assert(
            checkNotNull(actual.warmupSets).map { it.previous } == listOf(
                PreviousSetPerformance(Weight.kilograms(20), 8),
                PreviousSetPerformance(Weight.kilograms(21), 8),
                null,
            ),
        )
    }

    @Test
    fun `withPreviousFrom leaves extra current sets unpaired`() {
        val previous = completedBench(
            work = listOf(80f to 8, 81f to 9),
            warmups = listOf(20f to 8),
        )
        val current = benchExercise(
            work = listOf(85f to 8, 85f to 7, 85f to 6),
            warmups = listOf(25f to 6, 30f to 4),
        )

        val actual = current.withPreviousFrom(previous)

        assert(
            actual.sets.map { it.previous } == listOf(
                PreviousSetPerformance(Weight.kilograms(80), 8),
                PreviousSetPerformance(Weight.kilograms(81), 9),
                null,
            ),
        )
        assert(checkNotNull(actual.warmupSets).last().previous == null)
    }

    @Test
    fun `withPreviousFromHistory clears previous when there is no prior exercise`() {
        val current = benchExercise(
            work = listOf(85f to 8),
            warmups = listOf(25f to 6),
        ).copy(
            sets = listOf(
                ExerciseSet(
                    catalogExercise("bench-press"),
                    Weight.kilograms(85),
                    8,
                    SetState.NotStated,
                    PreviousSetPerformance(Weight.kilograms(80), 8),
                ),
            ),
        )
        val session = completedBenchSession(LocalDateTime.of(2026, 10, 1, 12, 0), 85f)
            .copy(exercises = listOf(current))

        val actual = session.withPreviousFromHistory(emptyList()).exercises.single()

        assert(actual.sets.single().previous == null)
        assert(checkNotNull(actual.warmupSets).single().previous == null)
    }

    @Test
    fun `latestCompletedExercise skips sessions at or after the cutoff`() {
        val older = completedBenchSession(LocalDateTime.of(2026, 8, 1, 12, 0), 70f)
        val previous = completedBenchSession(LocalDateTime.of(2026, 9, 1, 12, 0), 80f)
        val current = completedBenchSession(LocalDateTime.of(2026, 10, 1, 12, 0), 90f)
        val history = listOf(older, previous, current)

        val matched = history.latestCompletedExercise(
            "bench-press",
            before = current.startedAt,
        )

        assert(matched?.sets?.first()?.weight == Weight.kilograms(80))
        assert(
            history.latestCompletedExercise("bench-press")?.sets?.first()?.weight ==
                Weight.kilograms(90),
        )
    }

    @Test
    fun `withPreviousFromHistory rematches every exercise against earlier completed work`() {
        val previous = completedBenchSession(LocalDateTime.of(2026, 9, 1, 12, 0), 80f)
        val current = completedBenchSession(LocalDateTime.of(2026, 10, 1, 12, 0), 90f)

        val actual = current.withPreviousFromHistory(listOf(previous, current))

        assert(
            actual.exercises.first().sets.first().previous ==
                PreviousSetPerformance(Weight.kilograms(80), 8),
        )
    }
}

private fun previousWorkPerformance() = listOf(
    PreviousSetPerformance(Weight.kilograms(80), 8),
    PreviousSetPerformance(Weight.kilograms(81), 9),
    PreviousSetPerformance(Weight.kilograms(82), 10),
)

private fun benchExercise(
    work: List<Pair<Float, Int>>,
    warmups: List<Pair<Float, Int>>,
): WorkoutExercise {
    val definition = catalogExercise("bench-press")
    return WorkoutExercise(
        exerciseDefinition = definition,
        repRange = 8..10,
        warmupSets = warmups.map { (kg, reps) ->
            ExerciseSet(definition, Weight.kilograms(kg), reps, SetState.NotStated)
        }.takeIf { it.isNotEmpty() },
        sets = work.map { (kg, reps) ->
            ExerciseSet(definition, Weight.kilograms(kg), reps, SetState.NotStated)
        },
    )
}

private fun completedBench(
    work: List<Pair<Float, Int>>,
    warmups: List<Pair<Float, Int>>,
): WorkoutExercise = benchExercise(work, warmups).copy(
    warmupSets = benchExercise(work, warmups).warmupSets?.map {
        it.copy(setState = SetState.Completed())
    },
    sets = benchExercise(work, warmups).sets.map { it.copy(setState = SetState.Completed()) },
)

private fun completedBenchSession(startedAt: LocalDateTime, kg: Float): WorkoutSession =
    WorkoutSession(
        planId = fullBodyA.id,
        planName = fullBodyA.name,
        note = null,
        exercises = listOf(
            completedBench(work = listOf(kg to 8), warmups = emptyList()),
        ),
        startedAt = startedAt,
        duration = kotlin.time.Duration.ZERO,
    )
