package dev.sanastasov.bybon.workout.domain

import org.junit.Test

class WorkoutSessionEditingTest {

    @Test
    fun `addSet on a completed exercise becomes the only in-progress set`() {
        val session = fullBodyA.toWorkoutSession().completeExerciseAt(0)
        assert(session.exercises[1].id == "squat")
        session.assertOnlyInProgress("squat", index = 0, isWarmup = true)

        val actual = session.addSet(session.exercises.first())
        val bench = actual.exercises.first()

        assert(bench.sets.size == 4)
        assert(bench.sets.dropLast(1).all { it.setState is SetState.Completed })
        actual.assertOnlyInProgress("bench-press", index = 3, isWarmup = false)
        assert(
            checkNotNull(actual.exercises.first { it.id == "squat" }.warmupSets)
                .all { it.setState == SetState.NotStated },
        )
    }
}
