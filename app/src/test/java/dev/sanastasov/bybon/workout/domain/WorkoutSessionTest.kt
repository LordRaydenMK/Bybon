package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class WorkoutSessionTest {

    @Test
    fun `complete first set, exercise with no completed sets, completes first set and makes second in progress`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.completeSet(session.exercises.first(), 0)

        assert(
            actual.workoutSets.first() == ExerciseSet(
                session.exercises.first().exerciseDefinition,
                Weight.kilograms(50),
                8,
                SetState.Completed,
            )
        )
        assert(
            actual.workoutSets[1] == ExerciseSet(
                session.exercises.first().exerciseDefinition,
                Weight.kilograms(50),
                8,
                SetState.InProgress,
            )
        )
        assert(actual.workoutSets.drop(2) == session.workoutSets.drop(2))
    }

    @Test
    fun `update first set weight - first set weight updated`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.updateWeight(session.exercises.first(), 0, Weight.kilograms(100))

        val expected = ExerciseSet(
            session.exercises.first().exerciseDefinition,
            Weight.kilograms(100),
            8,
            SetState.InProgress,
        )
        assert(actual.exercises.first().sets.first() == expected)
        assert(actual.exercises.drop(1) == session.exercises.drop(1))
    }

    @Test
    fun `update fourth set weight - fourth set weight updated`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.updateWeight(session.exercises[1], 0, Weight.kilograms(100))

        val expected = ExerciseSet(
            session.exercises[1].exerciseDefinition,
            Weight.kilograms(100),
            8,
            SetState.NotStated,
        )
        assert(actual.workoutSets[3] == expected)
        assert(actual.workoutSets.take(3) == session.workoutSets.take(3))
        assert(actual.exercises.drop(4) == session.exercises.drop(4))
    }

    @Test
    fun `update first set reps - first exercise - reps updated`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.updateReps(session.exercises.first(), 0, 10)

        val expected = ExerciseSet(
            session.exercises.first().exerciseDefinition,
            Weight.kilograms(50),
            10,
            SetState.InProgress,
        )
        assert(actual.exercises.first().sets.first() == expected)
        assert(actual.exercises.drop(1) == session.exercises.drop(1))
    }

    @Test
    fun `remove last set when it is in progress - another set becomes in progress`() {
        val initial = fullBodyA.toWorkoutSession()
        val session = initial
            .completeSet(initial.exercises.first(), 0)
            .let { it.completeSet(it.exercises.first(), 1) }
        // First exercise: [Completed, Completed, InProgress]

        val actual = session.removeLastSet(session.exercises.first())

        assert(actual.exercises.first().sets.size == 2)
        assert(actual.workoutSets.count { it.setState == SetState.InProgress } == 1)
        assert(actual.exercises[1].sets.first().setState == SetState.InProgress)
    }

    @Test
    fun `Weight kilograms formats whole and decimal values`() {
        assert(Weight.kilograms(50).kilograms == "50")
        assert(Weight.kilograms(52.5f).kilograms == "52.5")
    }
}