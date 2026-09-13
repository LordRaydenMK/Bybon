package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class WorkoutProgressionTest {

    @Test
    fun `increase within rep range adds one rep and keeps weight`() {
        val set = ExerciseSet(
            exercisesMap["bench-press-bb"]!!,
            Weight.kilograms(50),
            8,
            SetState.NotStated,
        )

        val actual = set.adjust(8..10, Equipment.Barbell.weightIncrement, increase = true)

        assert(actual.weight == Weight.kilograms(50))
        assert(actual.reps == 9)
    }

    @Test
    fun `increase at top of range adds barbell increment and resets reps to lower bound`() {
        val set = ExerciseSet(
            exercisesMap["bench-press-bb"]!!,
            Weight.kilograms(50),
            10,
            SetState.NotStated,
        )

        val actual = set.adjust(8..10, Equipment.Barbell.weightIncrement, increase = true)

        assert(actual.weight == Weight.kilograms(52.5f))
        assert(actual.reps == 8)
    }

    @Test
    fun `increase at top of range adds dumbbell increment`() {
        val set = ExerciseSet(
            exercisesMap["upright-row-db"]!!,
            Weight.kilograms(20),
            14,
            SetState.NotStated,
        )

        val actual = set.adjust(10..14, Equipment.Dumbbell.weightIncrement, increase = true)

        assert(actual.weight == Weight.kilograms(22))
        assert(actual.reps == 10)
    }

    @Test
    fun `decrease above lower bound subtracts one rep`() {
        val set = ExerciseSet(
            exercisesMap["bench-press-bb"]!!,
            Weight.kilograms(50),
            10,
            SetState.NotStated,
        )

        val actual = set.adjust(8..10, Equipment.Barbell.weightIncrement, increase = false)

        assert(actual.weight == Weight.kilograms(50))
        assert(actual.reps == 9)
    }

    @Test
    fun `decrease at lower bound drops weight and sets reps to upper bound`() {
        val set = ExerciseSet(
            exercisesMap["bench-press-bb"]!!,
            Weight.kilograms(50),
            8,
            SetState.NotStated,
        )

        val actual = set.adjust(8..10, Equipment.Barbell.weightIncrement, increase = false)

        assert(actual.weight == Weight.kilograms(47.5f))
        assert(actual.reps == 10)
    }

    @Test
    fun `bodyweight increase only changes reps`() {
        val definition = ExerciseDefinition(
            "push-up",
            "Push Up",
            MuscleGroup.Chest,
            Equipment.Bodyweight,
        )
        val set = ExerciseSet(definition, Weight.kilograms(0), 10, SetState.NotStated)

        val atTop = set.adjust(8..10, Equipment.Bodyweight.weightIncrement, increase = true)
        val within = set.copy(reps = 8).adjust(8..10, Equipment.Bodyweight.weightIncrement, increase = true)

        assert(atTop == set)
        assert(within.weight == Weight.kilograms(0))
        assert(within.reps == 9)
    }

    @Test
    fun `adjustExercise applies independently per set`() {
        val session = fullBodyA.toOverviewSession().let { overview ->
            overview.updateReps(overview.exercises.first(), 0, 10)
        }
        val bench = session.exercises.first()

        val actual = session.adjustExercise(bench, increase = true)
        val sets = actual.exercises.first().sets

        assert(sets[0].weight == Weight.kilograms(52.5f))
        assert(sets[0].reps == 8)
        assert(sets[1].weight == Weight.kilograms(50))
        assert(sets[1].reps == 9)
        assert(actual.exercises.drop(1) == session.exercises.drop(1))
    }

    @Test
    fun `adjustAll uses each exercise equipment increment`() {
        val session = fullBodyA.toOverviewSession().let { overview ->
            overview.copy(
                exercises = overview.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.map { set ->
                            set.copy(reps = exercise.repRange.last)
                        }
                    )
                }
            )
        }

        val actual = session.adjustAll(increase = true)
        val barbell = actual.exercises.first { it.id == "bench-press-bb" }.sets.first()
        val dumbbell = actual.exercises.first { it.id == "upright-row-db" }.sets.first()
        val machine = actual.exercises.first { it.id == "leg-curl" }.sets.first()

        assert(barbell.weight == Weight.kilograms(52.5f))
        assert(barbell.reps == 8)
        assert(dumbbell.weight == Weight.kilograms(52))
        assert(dumbbell.reps == 10)
        assert(machine.weight == Weight.kilograms(52.5f))
        assert(machine.reps == 12)
    }

    @Test
    fun `adjust preserves previous performance`() {
        val previous = PreviousSetPerformance(Weight.kilograms(45), 12)
        val session = fullBodyA.toOverviewSession().let { overview ->
            overview.copy(
                exercises = overview.exercises.mapIndexed { index, exercise ->
                    if (index != 0) exercise
                    else exercise.copy(
                        sets = exercise.sets.mapIndexed { setIndex, set ->
                            if (setIndex != 0) set else set.copy(previous = previous)
                        }
                    )
                }
            )
        }

        val actual = session.adjustExercise(session.exercises.first(), increase = true)

        assert(actual.exercises.first().sets.first().previous == previous)
    }

    @Test
    fun `weight increment is 2_5kg for barbell and 2kg for dumbbell`() {
        assert(Equipment.Barbell.weightIncrement == Weight.kilograms(2.5f))
        assert(Equipment.Dumbbell.weightIncrement == Weight.kilograms(2f))
        assert(Equipment.Machine.weightIncrement == Weight.kilograms(2.5f))
        assert(Equipment.AssistedBodyWeight.weightIncrement == Weight.kilograms(2.5f))
        assert(Equipment.Bodyweight.weightIncrement == Weight.kilograms(0))
    }
}
