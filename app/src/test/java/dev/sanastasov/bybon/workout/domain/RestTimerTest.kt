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

        assert(session.restTimer() == RestTimer(completedAt, 2.minutes))
        assert(session.restTimer()!!.progress(completedAt) == 0f)
        assert(session.restTimer()!!.remaining(completedAt) == 2.minutes)
        assert(session.restTimer()!!.progress(completedAt.plusMinutes(1)) == 0.5f)
        assert(session.restTimer()!!.remaining(completedAt.plusMinutes(1)) == 1.minutes)
        assert(session.restTimer()!!.progress(completedAt.plusMinutes(2)) == null)
    }

    @Test
    fun `a work set starts rest including the last set of an exercise`() {
        val warmupAt = startedAt.plusMinutes(3)
        val workAt = startedAt.plusMinutes(6)
        val afterFirstWork = completeWarmups(now = warmupAt)
            .let { session ->
                session.completeSet(session.exercises.first(), 0, now = workAt)
            }
        assert(afterFirstWork.restTimer() == RestTimer(workAt, 2.minutes))

        val afterLastWork = (0..2).fold(completeWarmups(now = warmupAt)) { session, index ->
            session.completeSet(session.exercises.first(), index, now = workAt)
        }
        assert(afterLastWork.exercises.first().state == ExerciseState.Completed)
        assert(afterLastWork.restTimer() == RestTimer(workAt, 2.minutes))
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
    fun `rest sits after last warmup work sets and between exercises`() {
        val bench = fullBodyA.sets.first()
        val last = fullBodyA.sets.last()

        assert(!bench.startsRestAfter(isWarmup = true, index = 0, isLastExercise = false))
        assert(bench.startsRestAfter(isWarmup = true, index = 2, isLastExercise = false))
        assert(bench.startsRestAfter(isWarmup = false, index = 0, isLastExercise = false))
        assert(bench.startsRestAfter(isWarmup = false, index = 2, isLastExercise = false))
        assert(bench.restAfter(isWarmup = true, index = 2, isLastExercise = false) == 2.minutes)
        assert(bench.restAfter(isWarmup = true, index = 0, isLastExercise = false) == null)

        assert(
            last.startsRestAfter(
                isWarmup = false,
                index = 0,
                isLastExercise = true,
            ),
        )
        assert(
            !last.startsRestAfter(
                isWarmup = false,
                index = last.sets - 1,
                isLastExercise = true,
            ),
        )
        assert(
            last.restAfter(
                isWarmup = false,
                index = last.sets - 1,
                isLastExercise = true,
            ) == null,
        )
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
