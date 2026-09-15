package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class WorkoutProgressionTest {

    private val bench = catalogExercise("bench-press-bb")
    private val lateralRaise = catalogExercise("lateral-raise-db")
    private val benchRange = 8..10
    private val lateralRange = 10..16

    @Test
    fun `bench press plus from 50kg x 8 increases reps then weight without dropping 1RM`() {
        val start = benchSet(50f, 8)
        val after1 = start.increase()
        val after2 = after1.increase()
        val after3 = after2.increase()

        assert(after1.weight == Weight.kilograms(50))
        assert(after1.reps == 9)
        assert(after2.weight == Weight.kilograms(50))
        assert(after2.reps == 10)
        assert(Weight.kilograms(estimateOneRmKg(52.5f, 8)) < after2.oneRm)
        assert(after3.weight == Weight.kilograms(52.5f))
        assert(after3.reps == 9)
        assert(after1.oneRm > start.oneRm)
        assert(after2.oneRm > after1.oneRm)
        assert(after3.oneRm > after2.oneRm)
    }

    @Test
    fun `bench press plus from 50kg keeps increasing estimated 1RM`() {
        val steps = generateSequence(benchSet(50f, 8)) { it.increase() }.take(12).toList()

        steps.zipWithNext().forEach { (previous, next) ->
            assert(next.oneRm > previous.oneRm)
        }
        assert(
            steps.map { it.weight to it.reps } == listOf(
                Weight.kilograms(50) to 8,
                Weight.kilograms(50) to 9,
                Weight.kilograms(50) to 10,
                Weight.kilograms(52.5f) to 9,
                Weight.kilograms(52.5f) to 10,
                Weight.kilograms(55) to 9,
                Weight.kilograms(55) to 10,
                Weight.kilograms(57.5f) to 9,
                Weight.kilograms(57.5f) to 10,
                Weight.kilograms(60) to 9,
                Weight.kilograms(60) to 10,
                Weight.kilograms(62.5f) to 9,
            ),
        )
    }

    @Test
    fun `bench press minus from 50kg x 10 decreases reps then weight without raising 1RM`() {
        val start = benchSet(50f, 10)
        val after1 = start.decrease()
        val after2 = after1.decrease()
        val after3 = after2.decrease()

        assert(after1.weight == Weight.kilograms(50))
        assert(after1.reps == 9)
        assert(after2.weight == Weight.kilograms(50))
        assert(after2.reps == 8)
        assert(Weight.kilograms(estimateOneRmKg(47.5f, 10)) > after2.oneRm)
        assert(after3.weight == Weight.kilograms(47.5f))
        assert(after3.reps == 9)
        assert(after1.oneRm < start.oneRm)
        assert(after2.oneRm < after1.oneRm)
        assert(after3.oneRm < after2.oneRm)
    }

    @Test
    fun `bench press minus from 50kg x 8 keeps decreasing estimated 1RM`() {
        val steps = generateSequence(benchSet(50f, 8)) { it.decrease() }.take(8).toList()

        steps.zipWithNext().forEach { (previous, next) ->
            assert(next.oneRm < previous.oneRm)
        }
    }

    @Test
    fun `lateral raise plus from 10kg x 10 walks the 10-16 range then adds 2kg`() {
        val start = lateralSet(10f, 10)
        val steps = generateSequence(start) { it.increaseLateral() }.take(8).toList()

        assert(
            steps.map { it.weight to it.reps } == listOf(
                Weight.kilograms(10) to 10,
                Weight.kilograms(10) to 11,
                Weight.kilograms(10) to 12,
                Weight.kilograms(10) to 13,
                Weight.kilograms(10) to 14,
                Weight.kilograms(10) to 15,
                Weight.kilograms(10) to 16,
                Weight.kilograms(12) to 10,
            ),
        )
        steps.zipWithNext().forEach { (previous, next) ->
            assert(next.oneRm > previous.oneRm)
        }
    }

    @Test
    fun `lateral raise plus from 10kg keeps increasing estimated 1RM`() {
        val steps = generateSequence(lateralSet(10f, 10)) { it.increaseLateral() }.take(20).toList()

        steps.zipWithNext().forEach { (previous, next) ->
            assert(next.oneRm > previous.oneRm)
        }
    }

    @Test
    fun `lateral raise minus from 12kg x 10 drops to 10kg at a lower 1RM`() {
        val start = lateralSet(12f, 10)
        val decreased = start.decreaseLateral()

        assert(decreased.weight == Weight.kilograms(10))
        assert(decreased.reps == 16)
        assert(decreased.oneRm < start.oneRm)
    }

    @Test
    fun `lateral raise minus from 10kg x 16 keeps decreasing estimated 1RM`() {
        val steps = generateSequence(lateralSet(10f, 16)) { it.decreaseLateral() }.take(7).toList()

        assert(
            steps.map { it.weight to it.reps } == listOf(
                Weight.kilograms(10) to 16,
                Weight.kilograms(10) to 15,
                Weight.kilograms(10) to 14,
                Weight.kilograms(10) to 13,
                Weight.kilograms(10) to 12,
                Weight.kilograms(10) to 11,
                Weight.kilograms(10) to 10,
            ),
        )
        steps.zipWithNext().forEach { (previous, next) ->
            assert(next.oneRm < previous.oneRm)
        }
    }

    @Test
    fun `bodyweight increase only changes reps`() {
        val definition = ExerciseDefinition(
            "push-up",
            "Push Up",
            MuscleGroup.Chest,
            Equipment.Bodyweight,
        )
        val set = ExerciseSet(definition, Weight.kilograms(70), 10, SetState.NotStated)
        val range = 8..10

        val atTop = set.adjust(range, Equipment.Bodyweight.weightIncrement, increase = true)
        val within = set.copy(
            reps = 8,
        ).adjust(range, Equipment.Bodyweight.weightIncrement, increase = true)

        assert(atTop == set)
        assert(within.weight == Weight.kilograms(70))
        assert(within.reps == 9)
    }

    @Test
    fun `adjustExercise copies first work set weight to other sets`() {
        val session = fullBodyA.toOverviewSession().let { overview ->
            overview.updateReps(overview.exercises.first(), 0, 10)
        }
        val benchExercise = session.exercises.first()

        val actual = session.adjustExercise(benchExercise, increase = true)
        val sets = actual.exercises.first().sets

        assert(sets[0].weight == Weight.kilograms(52.5f))
        assert(sets[0].reps == 9)
        assert(sets[0].reps in benchRange)
        assert(sets.all { it.weight == sets[0].weight })
        assert(sets[1].reps == 7)
        assert(sets[1].reps !in benchRange)
        assert(actual.exercises.drop(1) == session.exercises.drop(1))
        assert(sets[0].oneRm > benchExercise.sets[0].oneRm)
        assert(sets[1].oneRm > benchExercise.sets[1].oneRm)
        assert(actual.exercises.first().warmupSets == benchExercise.warmupSets)
    }

    @Test
    fun `adjustExercise keeps matching weights when first set only adds a rep`() {
        val session = fullBodyA.toOverviewSession()
        val benchExercise = session.exercises.first()

        val actual = session.adjustExercise(benchExercise, increase = true)
        val sets = actual.exercises.first().sets

        assert(sets.all { it.weight == Weight.kilograms(50) })
        assert(sets.all { it.reps == 9 })
        assert(sets[0].oneRm > benchExercise.sets[0].oneRm)
    }

    @Test
    fun `first set can leave the range when needed to increase 1RM`() {
        val session = fullBodyA.toOverviewSession().let { overview ->
            overview.updateReps(overview.exercises.first(), 0, 12)
        }
        val start = session.exercises.first().sets.first()

        val actual = session.adjustExercise(session.exercises.first(), increase = true)
        val first = actual.exercises.first().sets.first()

        assert(first.weight == Weight.kilograms(52.5f))
        assert(first.reps == 11)
        assert(first.reps !in benchRange)
        assert(first.oneRm > start.oneRm)
        assert(actual.exercises.first().sets.all { it.weight == first.weight })
    }

    @Test
    fun `adjustExercise decrease keeps shared weight and can leave later sets outside range`() {
        val session = fullBodyA.toOverviewSession().let { overview ->
            overview.updateReps(overview.exercises.first(), 1, 10)
        }
        val benchExercise = session.exercises.first()

        val actual = session.adjustExercise(benchExercise, increase = false)
        val sets = actual.exercises.first().sets

        assert(sets[0].weight == Weight.kilograms(47.5f))
        assert(sets[0].reps == 9)
        assert(sets[0].reps in benchRange)
        assert(sets.all { it.weight == sets[0].weight })
        assert(sets[1].reps == 12)
        assert(sets[1].reps !in benchRange)
        assert(sets[0].oneRm < benchExercise.sets[0].oneRm)
        assert(sets[1].oneRm < benchExercise.sets[1].oneRm)
    }

    @Test
    fun `adjust preserves previous performance`() {
        val previous = PreviousSetPerformance(Weight.kilograms(45), 12)
        val session = fullBodyA.toOverviewSession().let { overview ->
            overview.copy(
                exercises = overview.exercises.mapIndexed { index, exercise ->
                    if (index != 0) {
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

        val actual = session.adjustExercise(session.exercises.first(), increase = true)

        assert(actual.exercises.first().sets.first().previous == previous)
        assert(actual.exercises.first().sets[1].previous == null)
    }

    private fun benchSet(kg: Float, reps: Int) = ExerciseSet(
        bench,
        Weight.kilograms(kg),
        reps,
        SetState.NotStated,
    )

    private fun lateralSet(kg: Float, reps: Int) = ExerciseSet(
        lateralRaise,
        Weight.kilograms(kg),
        reps,
        SetState.NotStated,
    )

    private fun ExerciseSet.increase() =
        adjust(benchRange, Equipment.Barbell.weightIncrement, increase = true)

    private fun ExerciseSet.decrease() =
        adjust(benchRange, Equipment.Barbell.weightIncrement, increase = false)

    private fun ExerciseSet.increaseLateral() =
        adjust(lateralRange, Equipment.Dumbbell.weightIncrement, increase = true)

    private fun ExerciseSet.decreaseLateral() =
        adjust(lateralRange, Equipment.Dumbbell.weightIncrement, increase = false)
}
