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

    @Test
    fun `removeLastSet of a reopened exercise resumes the later exercise`() {
        val reopened = fullBodyA.toWorkoutSession()
            .completeExerciseAt(0)
            .let { session -> session.addSet(session.exercises.first()) }

        val actual = reopened.removeLastSet(reopened.exercises.first())

        assert(actual.exercises.first().sets.size == 3)
        assert(actual.exercises.first().state == ExerciseState.Completed)
        actual.assertOnlyInProgress("squat", index = 0, isWarmup = true)
    }

    @Test
    fun `addSet on a finished workout starts the new set`() {
        val finished = fullBodyA.sets.indices.fold(fullBodyA.toWorkoutSession()) { session, index ->
            session.completeExerciseAt(index)
        }
        assert(finished.state is WorkoutState.Completed)

        val actual = finished.addSet(finished.exercises.last())
        val last = actual.exercises.last()

        assert(last.sets.size == finished.exercises.last().sets.size + 1)
        assert(last.sets.dropLast(1).all { it.setState is SetState.Completed })
        assert(actual.state is WorkoutState.InProgress)
        assert(actual.duration == null)
        actual.assertOnlyInProgress(last.id, index = last.sets.lastIndex, isWarmup = false)
    }

    @Test
    fun `addSet on the active exercise keeps the current set in progress`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.addSet(session.exercises.first())

        assert(actual.exercises.first().sets.last().setState == SetState.NotStated)
        actual.assertOnlyInProgress("bench-press", index = 0, isWarmup = true)
    }

    @Test
    fun `addSet while the last work set is in progress keeps that set active`() {
        val session = fullBodyA.toWorkoutSession().withLastWorkSetInProgress(0)
        val bench = session.exercises.first()
        session.assertOnlyInProgress("bench-press", bench.sets.lastIndex, isWarmup = false)

        val actual = session.addSet(bench)

        assert(actual.exercises.first().sets.last().setState == SetState.NotStated)
        actual.assertOnlyInProgress("bench-press", bench.sets.lastIndex, isWarmup = false)
    }

    @Test
    fun `addSet on a later exercise keeps the current set in progress`() {
        val session = fullBodyA.toWorkoutSession()
        val later = session.exercises.first { it.id == "seated-leg-curl" }

        val actual = session.addSet(later)
        val added = actual.exercises.first { it.id == "seated-leg-curl" }

        assert(added.sets.size == later.sets.size + 1)
        assert(added.sets.last().setState == SetState.NotStated)
        actual.assertOnlyInProgress("bench-press", index = 0, isWarmup = true)
    }

    @Test
    fun `removeLastSet of a pending set keeps the current set in progress`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.removeLastSet(session.exercises.first())

        assert(actual.exercises.first().sets.size == 2)
        actual.assertOnlyInProgress("bench-press", index = 0, isWarmup = true)
    }
}
