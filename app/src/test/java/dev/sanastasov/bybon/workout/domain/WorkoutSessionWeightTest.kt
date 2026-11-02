package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class WorkoutSessionWeightTest {

    @Test
    fun `update first work set weight - first set weight updated`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.updateWeight(session.exercises.first(), 0, Weight.kilograms(100))

        val expected = ExerciseSet(
            session.exercises.first().exerciseDefinition,
            Weight.kilograms(100),
            8,
            SetState.NotStated,
        )
        assert(actual.exercises.first().sets.all { it == expected })
        assert(actual.exercises.first().warmupSets == session.exercises.first().warmupSets)
        assert(actual.exercises.drop(1) == session.exercises.drop(1))
    }

    @Test
    fun `update squat first work set weight`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.updateWeight(session.exercises[1], 0, Weight.kilograms(100))

        val expected = ExerciseSet(
            session.exercises[1].exerciseDefinition,
            Weight.kilograms(100),
            8,
            SetState.NotStated,
        )
        assert(actual.exercises[1].sets.all { it == expected })
        assert(actual.exercises[1].warmupSets == session.exercises[1].warmupSets)
        assert(actual.exercises.first() == session.exercises.first())
        assert(actual.exercises.drop(2) == session.exercises.drop(2))
    }

    @Test
    fun `update later work set weight leaves earlier work sets and warmups unchanged`() {
        val session = fullBodyA.toWorkoutSession().let { started ->
            started.updateExercise(started.exercises.first().id) { exercise ->
                exercise.copy(
                    sets = exercise.sets.mapIndexed { index, set ->
                        set.copy(weight = Weight.kilograms(50 + index * 5f))
                    },
                )
            }
        }
        val bench = session.exercises.first()
        val originalWarmups = checkNotNull(bench.warmupSets)

        val actual = session.updateWeight(bench, 1, Weight.kilograms(80))
        val actualBench = actual.exercises.first()

        assert(
            actualBench.sets.map { it.weight } == listOf(
                Weight.kilograms(50),
                Weight.kilograms(80),
                Weight.kilograms(80),
            ),
        )
        assert(actualBench.warmupSets == originalWarmups)
    }

    @Test
    fun `clearing a work set weight does not change later work sets`() {
        val session = fullBodyA.toWorkoutSession()
        val bench = session.exercises.first()
        val laterWeights = bench.sets.drop(1).map { it.weight }

        val actual = session.updateWeight(bench, 0, null)
        val actualBench = actual.exercises.first()

        assert(actualBench.sets.first().weight == null)
        assert(actualBench.sets.drop(1).map { it.weight } == laterWeights)
    }

    @Test
    fun `update warmup weight does not change work sets`() {
        val session = fullBodyA.toWorkoutSession()
        val bench = session.exercises.first()
        val originalWorkSets = bench.sets
        val originalWarmups = checkNotNull(bench.warmupSets)

        val actual = session.updateWeight(bench, 1, Weight.kilograms(30), isWarmup = true)
        val warmupSets = checkNotNull(actual.exercises.first().warmupSets)

        assert(warmupSets[0].weight == originalWarmups[0].weight)
        assert(warmupSets[1].weight == Weight.kilograms(30))
        assert(warmupSets[2].weight == originalWarmups[2].weight)
        assert(actual.exercises.first().sets == originalWorkSets)
    }

    @Test
    fun `update weight preserves previous set reference`() {
        val previous = PreviousSetPerformance(Weight.kilograms(45), 12)
        val session = fullBodyA.toWorkoutSession().let { session ->
            session.copy(
                exercises = session.exercises.mapIndexed { exerciseIndex, exercise ->
                    if (exerciseIndex != 0) {
                        exercise
                    } else {
                        exercise.copy(
                            sets = exercise.sets.mapIndexed { setIndex, set ->
                                if (setIndex != 0) set else set.copy(previous = previous)
                            },
                        )
                    }
                },
            )
        }

        val actual = session.updateWeight(session.exercises.first(), 0, Weight.kilograms(52.5f))

        assert(actual.exercises.first().sets.first().previous == previous)
        assert(actual.exercises.first().sets.all { it.weight == Weight.kilograms(52.5f) })
    }

    @Test
    fun `updateWeightFromField treats blank and zero as no load`() {
        val session = fullBodyA.toOverviewSession()
        val bench = session.exercises.first()

        val originalLater = bench.sets.drop(1).map { it.weight }
        val cleared = session.updateWeightFromField(bench, 0, "")
        assert(cleared.exercises.first().sets.first().weight == null)
        assert(cleared.exercises.first().sets.drop(1).map { it.weight } == originalLater)

        val loaded = session.updateWeightFromField(bench, 0, "80")
        assert(loaded.exercises.first().sets.all { it.weight == Weight.kilograms(80) })

        val zeroed = loaded.updateWeightFromField(loaded.exercises.first(), 0, "0")
        assert(zeroed.exercises.first().sets.first().weight == null)
        assert(
            zeroed.exercises.first().sets.drop(1).all { it.weight == Weight.kilograms(80) },
        )

        val ignored = loaded.updateWeightFromField(loaded.exercises.first(), 0, "nope")
        assert(ignored.exercises.first().sets.first().weight == Weight.kilograms(80))
    }
}
