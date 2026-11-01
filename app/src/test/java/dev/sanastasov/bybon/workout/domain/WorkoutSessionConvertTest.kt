package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import org.junit.Test

class WorkoutSessionConvertTest {

    @Test
    fun `converting the last remaining work set to warmup is a no-op`() {
        val session = fullBodyA.toOverviewSession().let { draft ->
            draft.updateExercise(draft.exercises.first().id) { exercise ->
                exercise.copy(sets = listOf(exercise.sets.first()))
            }
        }

        val actual = session.convertFirstWorkSetToWarmup(session.exercises.first())

        assert(actual.exercises.first().sets.size == 1)
        assert(actual.exercises.first().warmupSets?.size == 3)
    }

    @Test
    fun `converting last warmup restores it as work set one`() {
        val session = fullBodyA.toOverviewSession()
        val converted = session.convertFirstWorkSetToWarmup(session.exercises.first())

        val actual = converted.convertLastWarmupToWorkSet(converted.exercises.first())
        val bench = actual.exercises.first()

        assert(bench.warmupSets?.size == 3)
        assert(bench.sets.size == 3)
        assert(bench.sets.first() == session.exercises.first().sets.first())
    }

    @Test
    fun `converting last warmup keeps work set previous by work index`() {
        val previous = completedBenchWithDistinctSets()
        val previousBench = previous.exercises.first()
        val overview = fullBodyA.toOverviewSession(previous)
        val converted = overview.convertLastWarmupToWorkSet(overview.exercises.first())
            .updateExercise(previousBench.id) { it.withPreviousFrom(previousBench) }
        val actual = converted.removeLastSet(converted.exercises.first())
            .updateExercise(previousBench.id) { it.withPreviousFrom(previousBench) }
        val bench = actual.exercises.first()

        assert(bench.warmupSets?.size == 2)
        assert(bench.sets.size == 3)
        assert(bench.sets.map { it.previous } == previousWorkPerformance())
        assert(
            checkNotNull(bench.warmupSets).map { it.previous } ==
                previousWarmupPerformance().take(2),
        )
    }

    @Test
    fun `converting first work set to warmup keeps work previous by work index`() {
        val previous = completedBenchWithDistinctSets()
        val previousBench = previous.exercises.first()
        val overview = fullBodyA.toOverviewSession(previous)

        val actual = overview.convertFirstWorkSetToWarmup(overview.exercises.first())
            .updateExercise(previousBench.id) { it.withPreviousFrom(previousBench) }
        val bench = actual.exercises.first()

        assert(bench.warmupSets?.size == 4)
        assert(bench.sets.size == 2)
        assert(bench.sets.map { it.previous } == previousWorkPerformance().take(2))
        assert(checkNotNull(bench.warmupSets).last().previous == null)
        assert(
            checkNotNull(bench.warmupSets).dropLast(1).map { it.previous } ==
                previousWarmupPerformance(),
        )
    }

    @Test
    fun `converting last warmup pairs a fourth previous work set`() {
        val previous = completedBenchWithDistinctSets().let { session ->
            session.updateExercise(session.exercises.first().id) { bench ->
                bench.copy(
                    sets = bench.sets + ExerciseSet(
                        bench.exerciseDefinition,
                        Weight.kilograms(83),
                        11,
                        SetState.Completed(),
                    ),
                )
            }
        }
        val previousBench = previous.exercises.first()
        val overview = fullBodyA.toOverviewSession(previous)
        val actual = overview.convertLastWarmupToWorkSet(overview.exercises.first())
            .updateExercise(previousBench.id) { it.withPreviousFrom(previousBench) }
        val bench = actual.exercises.first()

        assert(bench.warmupSets?.size == 2)
        assert(bench.sets.size == 4)
        assert(
            bench.sets.map { it.previous } == previousWorkPerformance() +
                PreviousSetPerformance(Weight.kilograms(83), 11),
        )
    }

    @Test
    fun `toOverviewSession matches work previous when warmup count differs`() {
        val previous = completedBenchWithDistinctSets()
        val plan = fullBodyA.copy(
            sets = fullBodyA.sets.map { planned ->
                if (planned.exercise.id == "bench-press") {
                    planned.copy(warmupSets = 2)
                } else {
                    planned
                }
            },
        )

        val actual = plan.toOverviewSession(previous)
        val bench = actual.exercises.first { it.id == "bench-press" }

        assert(bench.warmupSets?.size == 2)
        assert(bench.sets.map { it.previous } == previousWorkPerformance())
        assert(
            checkNotNull(bench.warmupSets).map { it.previous } ==
                previousWarmupPerformance().take(2),
        )
        assert(
            bench.sets.map { it.weight to it.reps } == listOf(
                Weight.kilograms(80) to 8,
                Weight.kilograms(81) to 9,
                Weight.kilograms(82) to 10,
            ),
        )
    }

    @Test
    fun `converting the last remaining warmup leaves warmup sets null`() {
        val session = fullBodyB.toOverviewSession()
        val splitSquat = session.exercises.first { it.id == "bulgarian-split-squat" }
        assert(splitSquat.warmupSets?.size == 1)

        val actual = session.convertLastWarmupToWorkSet(splitSquat)
        val updated = actual.exercises.first { it.id == "bulgarian-split-squat" }

        assert(updated.warmupSets == null)
        assert(updated.sets.size == 4)
    }
}

private fun previousWorkPerformance() = listOf(
    PreviousSetPerformance(Weight.kilograms(80), 8),
    PreviousSetPerformance(Weight.kilograms(81), 9),
    PreviousSetPerformance(Weight.kilograms(82), 10),
)

private fun previousWarmupPerformance() = listOf(
    PreviousSetPerformance(Weight.kilograms(20), 8),
    PreviousSetPerformance(Weight.kilograms(21), 8),
    PreviousSetPerformance(Weight.kilograms(22), 8),
)

private fun completedBenchWithDistinctSets(): WorkoutSession =
    fullBodyA.toWorkoutSession().let { session ->
        session.copy(
            exercises = session.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(80 + index),
                            reps = 8 + index,
                            setState = SetState.Completed(),
                        )
                    },
                    warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                        set.copy(
                            weight = Weight.kilograms(20 + index),
                            reps = 8,
                            setState = SetState.Completed(),
                        )
                    },
                )
            },
            duration = kotlin.time.Duration.ZERO,
            startedAt = LocalDateTime.of(2026, 10, 1, 12, 0),
        )
    }
