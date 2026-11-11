package dev.sanastasov.bybon.workout.ui.session

import dev.sanastasov.bybon.workout.domain.ExerciseState
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import org.junit.Test

class SessionUiTest {

    @Test
    fun `exerciseOverflow disables move up on the first incomplete card`() {
        val overflow = fullBodyA.toWorkoutSession().exerciseOverflow(0) {}!!

        assert(!overflow.canMoveUp)
        assert(overflow.canMoveDown)
    }

    @Test
    fun `exerciseOverflow disables move down on the last card`() {
        val session = fullBodyA.toWorkoutSession()
        val overflow = session.exerciseOverflow(session.exercises.lastIndex) {}!!

        assert(overflow.canMoveUp)
        assert(!overflow.canMoveDown)
    }

    @Test
    fun `exerciseOverflow is hidden for completed exercises`() {
        val session = fullBodyA.toWorkoutSession().withExerciseCompleted("bench-press")

        assert(session.exerciseOverflow(0) {} == null)
    }

    @Test
    fun `exerciseOverflow cannot move up past a completed exercise`() {
        val session = fullBodyA.toWorkoutSession().withExerciseCompleted("bench-press")
        val squatIndex = session.exercises.indexOfFirst { it.id == "squat" }
        val overflow = session.exerciseOverflow(squatIndex) {}!!

        assert(session.exercises[squatIndex].state != ExerciseState.Completed)
        assert(!overflow.canMoveUp)
        assert(overflow.canMoveDown)
    }

    @Test
    fun `exerciseOverflow is hidden when only one incomplete exercise remains`() {
        val session = fullBodyA.toWorkoutSession().let { started ->
            started.copy(
                exercises = started.exercises.mapIndexed { index, exercise ->
                    if (index == started.exercises.lastIndex) {
                        exercise
                    } else {
                        exercise.completed()
                    }
                },
            )
        }

        assert(session.exerciseOverflow(session.exercises.lastIndex) {} == null)
    }
}

private fun WorkoutSession.withExerciseCompleted(exerciseId: String): WorkoutSession = copy(
    exercises = exercises.map { exercise ->
        if (exercise.id != exerciseId) exercise else exercise.completed()
    },
)

private fun WorkoutExercise.completed() = copy(
    warmupSets = warmupSets?.map { it.copy(setState = SetState.Completed()) },
    sets = sets.map { it.copy(setState = SetState.Completed()) },
)
