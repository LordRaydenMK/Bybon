package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.ExerciseState
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import org.junit.Test

class WorkoutPlanUiTest {

    @Test
    fun `last executed is the latest completed session date`() {
        val older = completedFullBodyA(LocalDateTime.of(2026, 8, 10, 18, 0))
        val newer = completedFullBodyA(LocalDateTime.of(2026, 10, 1, 18, 0))

        val ui = fullBodyA.toUi(listOf(older, newer))

        assert(ui.lastExecutedLabel == "1 Oct")
        assert(!ui.isActive)
        assert(ui.activeExerciseId == null)
    }

    @Test
    fun `in-progress sessions are not last executed`() {
        val completed = completedFullBodyA(LocalDateTime.of(2026, 8, 10, 18, 0))
        val inProgress = fullBodyA.toWorkoutSession(
            startedAt = LocalDateTime.of(2026, 10, 23, 18, 0),
        )

        val ui = fullBodyA.toUi(listOf(completed, inProgress))

        assert(ui.lastExecutedLabel == "10 Aug")
        assert(ui.isActive)
        assert(ui.activeExerciseId == fullBodyA.sets.first().exercise.id)
    }

    @Test
    fun `never completed omits last executed`() {
        val ui = fullBodyA.toUi(emptyList())

        assert(ui.lastExecutedLabel == null)
        assert(!ui.isActive)
        assert(ui.activeExerciseId == null)
    }

    @Test
    fun `active exercise is the first not completed in a resumable session`() {
        val started = fullBodyA.toWorkoutSession(
            startedAt = LocalDateTime.of(2026, 10, 23, 18, 0),
        )
        val firstDone = started.withExerciseCompleted(0)

        assert(firstDone.exercises.first().state == ExerciseState.Completed)
        assert(firstDone.exercises[1].state == ExerciseState.NotStarted)

        val ui = fullBodyA.toUi(listOf(firstDone))

        assert(ui.isActive)
        assert(ui.activeExerciseId == fullBodyA.sets[1].exercise.id)
    }

    @Test
    fun `session-only current exercise is not highlighted`() {
        val started = fullBodyA.toWorkoutSession(
            startedAt = LocalDateTime.of(2026, 10, 23, 18, 0),
        )
        val extra = fullBodyB.toWorkoutSession(
            startedAt = LocalDateTime.of(2026, 10, 23, 18, 0),
        ).exercises.first()
        val resetPlanExercises = started.exercises.map { exercise ->
            exercise.copy(
                warmupSets = exercise.warmupSets?.map { it.copy(setState = SetState.NotStated) },
                sets = exercise.sets.map { it.copy(setState = SetState.NotStated) },
            )
        }
        val session = started.copy(exercises = listOf(extra) + resetPlanExercises)

        val ui = fullBodyA.toUi(listOf(session))

        assert(ui.isActive)
        assert(ui.activeExerciseId == null)
    }

    @Test
    fun `active plan is listed first among unarchived plans`() {
        val state = WorkoutPlansUiState(
            plans = listOf(
                WorkoutPlanUi(fullBodyA, isActive = false),
                WorkoutPlanUi(fullBodyB, isActive = true),
            ),
        )

        assert(state.unarchivedPlans.map { it.plan } == listOf(fullBodyB, fullBodyA))
    }
}

private fun completedFullBodyA(startedAt: LocalDateTime) = completedSession(
    planId = fullBodyA.id.id,
    planName = fullBodyA.name,
    startedAt = startedAt,
    exercises = listOf(completedExercise("bench-press", 50f to 8)),
)

private fun WorkoutSession.withExerciseCompleted(index: Int) = copy(
    exercises = exercises.mapIndexed { exerciseIndex, exercise ->
        if (exerciseIndex != index) {
            exercise
        } else {
            exercise.copy(
                warmupSets = exercise.warmupSets?.map { it.copy(setState = SetState.Completed) },
                sets = exercise.sets.map { it.copy(setState = SetState.Completed) },
            )
        }
    },
)
