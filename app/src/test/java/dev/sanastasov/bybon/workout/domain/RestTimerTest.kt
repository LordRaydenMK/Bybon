package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class RestTimerTest {

    private val startedAt = LocalDateTime.of(2026, 10, 24, 12, 0, 0)

    @Test
    fun `completeSet writes completedAt`() {
        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val actual = session.completeSet(
            session.exercises.first(),
            0,
            isWarmup = true,
            now = startedAt,
        )

        assert(actual.exercises.first().warmupSets!![0].setState == SetState.Completed(startedAt))
        assert(actual.exercises.first().warmupSets!![1].setState == SetState.InProgress)
    }

    @Test
    fun `a non-last warmup does not start rest`() {
        val started = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val afterFirstWarmup = started.completeSet(
            started.exercises.first(),
            0,
            isWarmup = true,
            now = startedAt,
        )

        assert(afterFirstWarmup.restTimer() == null)
    }

    @Test
    fun `last warmup starts rest before the first work set`() {
        val completedAt = startedAt.plusMinutes(4)
        val session = completeWarmups(now = completedAt)

        assert(session.state is WorkoutState.Resting)
        assert(session.exercises.first().sets.first().setState == SetState.NotStated)
        assert(session.restTimer() == RestTimer(completedAt, 2.minutes))
        assert(session.restTimer()!!.progress(completedAt) == 0f)
        assert(session.restTimer()!!.remaining(completedAt) == 2.minutes)
        assert(session.restTimer()!!.progress(completedAt.plusMinutes(1)) == 0.5f)
        assert(session.restTimer()!!.remaining(completedAt.plusMinutes(1)) == 1.minutes)
        assert(session.restTimer()!!.progress(completedAt.plusMinutes(2)) == null)

        val afterRest = session.startWorkout(now = completedAt.plusMinutes(2))
        assert(afterRest.state is WorkoutState.InProgress)
        assert(afterRest.exercises.first().sets.first().setState == SetState.InProgress)
        assert(afterRest.restTimer() == null)
    }

    @Test
    fun `a work set starts rest including the last set of an exercise`() {
        val warmupAt = startedAt.plusMinutes(3)
        val workAt = startedAt.plusMinutes(6)
        val afterFirstWork = completeWarmups(now = warmupAt)
            .let { session ->
                session.completeSet(session.exercises.first(), 0, now = workAt)
            }
        assert(afterFirstWork.state is WorkoutState.Resting)
        assert(afterFirstWork.exercises.first().sets[1].setState == SetState.NotStated)
        assert(afterFirstWork.restTimer() == RestTimer(workAt, 2.minutes))

        val afterLastWork = (0..2).fold(completeWarmups(now = warmupAt)) { session, index ->
            session.completeSet(session.exercises.first(), index, now = workAt)
        }
        assert(afterLastWork.exercises.first().state == ExerciseState.Completed)
        assert(afterLastWork.state is WorkoutState.Resting)
        assert(afterLastWork.exercises[1].warmupSets!!.first().setState == SetState.NotStated)
        assert(afterLastWork.restTimer() == RestTimer(workAt, 2.minutes))

        val afterRest = afterLastWork.startWorkout(now = workAt.plusMinutes(2))
        assert(afterRest.state is WorkoutState.InProgress)
        assert(afterRest.exercises[1].warmupSets!!.first().setState == SetState.InProgress)
        assert(afterRest.restTimer() == null)
    }

    @Test
    fun `the last set of the workout does not start rest`() {
        val now = startedAt.plusMinutes(40)
        val session = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val completed = session.exercises.indices.fold(session) { current, exerciseIndex ->
            completeAllSets(current, exerciseIndex, now)
        }

        assert(completed.state is WorkoutState.Completed)
        assert(completed.restTimer() == null)
    }

    @Test
    fun `completed sets without completedAt do not start rest`() {
        val session = completeWarmups(now = startedAt).let { current ->
            current.updateExercise(current.exercises.first().id) { exercise ->
                exercise.copy(
                    warmupSets = exercise.warmupSets?.map {
                        it.copy(setState = SetState.Completed())
                    },
                )
            }
        }

        assert(session.exercises.first().warmupSets!!.all { it.setState is SetState.Completed })
        assert(session.restTimer() == null)
    }

    @Test
    fun `asOverviewDraft clears completedAt`() {
        val session = completeWarmups(now = startedAt)
        val draft = session.asOverviewDraft()

        assert(draft.workoutSets.all { it.completedAt == null })
        assert(draft.workoutSets.all { it.setState == SetState.NotStated })
    }
}

private fun completeWarmups(now: LocalDateTime): WorkoutSession {
    val started = fullBodyA.toWorkoutSession(startedAt = now)
    return completeAllWarmups(started, exerciseIndex = 0, now = now)
}

private fun completeAllWarmups(
    session: WorkoutSession,
    exerciseIndex: Int,
    now: LocalDateTime,
): WorkoutSession {
    val warmupCount = session.exercises[exerciseIndex].warmupSets?.size ?: 0
    return (0 until warmupCount).fold(session) { current, index ->
        current.completeSet(current.exercises[exerciseIndex], index, isWarmup = true, now = now)
    }
}

private fun completeAllSets(
    session: WorkoutSession,
    exerciseIndex: Int,
    now: LocalDateTime,
): WorkoutSession {
    val withWarmups = completeAllWarmups(session, exerciseIndex, now)
    val workCount = withWarmups.exercises[exerciseIndex].sets.size
    return (0 until workCount).fold(withWarmups) { current, index ->
        current.completeSet(current.exercises[exerciseIndex], index, now = now)
    }
}
