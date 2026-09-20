package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.Test

class WorkoutSessionStateTest {

    @Test
    fun `startWorkout marks the first warmup in progress`() {
        val overview = fullBodyA.toOverviewSession()

        val actual = overview.startWorkout()
        val bench = actual.exercises.first()

        val warmupSets = checkNotNull(bench.warmupSets)
        assert(warmupSets.first().setState == SetState.InProgress)
        assert(warmupSets.drop(1).all { it.setState == SetState.NotStated })
        assert(bench.sets.all { it.setState == SetState.NotStated })
        assert(actual.workoutSets.drop(1).all { it.setState == SetState.NotStated })
        assert(actual.state == WorkoutState.InProgress)
        assert(actual.isResumable)
    }

    @Test
    fun `startWorkout is idempotent when a set is already in progress`() {
        val started = fullBodyA.toOverviewSession().startWorkout()

        val actual = started.startWorkout()

        assert(actual.state == WorkoutState.InProgress)
        assert(actual.workoutSets.count { it.setState == SetState.InProgress } == 1)
        assert(actual.exercises.first().warmupSets!!.first().setState == SetState.InProgress)
    }

    @Test
    fun `startWorkout restores an in-progress set when none is active`() {
        val started = fullBodyA.toWorkoutSession()
        val paused = started.copy(
            exercises = started.exercises.mapIndexed { exerciseIndex, exercise ->
                if (exerciseIndex != 0) {
                    exercise
                } else {
                    exercise.copy(
                        warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
                            set.copy(
                                setState = if (index == 0) {
                                    SetState.Completed
                                } else {
                                    SetState.NotStated
                                },
                            )
                        },
                    )
                }
            },
        )
        assert(paused.workoutSets.none { it.setState == SetState.InProgress })

        val actual = paused.startWorkout()

        assert(actual.state == WorkoutState.InProgress)
        assert(actual.exercises.first().warmupSets!![0].setState == SetState.Completed)
        assert(actual.exercises.first().warmupSets!![1].setState == SetState.InProgress)
        assert(actual.isResumable)
    }

    @Test
    fun `session id is plan id plus started at for every state`() {
        val startedAt = LocalDateTime.of(2026, 8, 13, 18, 0)
        val inProgress = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val expectedId = WorkoutSessionId(fullBodyA.id, startedAt)

        assert(inProgress.id == expectedId)
        assert(inProgress.state is WorkoutState.InProgress)

        val completed = inProgress.copy(
            exercises = inProgress.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.map { it.copy(setState = SetState.Completed) },
                    warmupSets = exercise.warmupSets?.map {
                        it.copy(setState = SetState.Completed)
                    },
                )
            },
            duration = Duration.ZERO,
        )
        assert(completed.id == expectedId)
        assert(completed.state == WorkoutState.Completed(Duration.ZERO))
    }

    @Test
    fun `exercise state is derived from set status`() {
        val overview = fullBodyA.toOverviewSession()
        assert(overview.exercises.all { it.state == ExerciseState.NotStarted })
        assert(overview.state == WorkoutState.NotStarted)

        val started = overview.startWorkout()
        assert(started.exercises.first().state == ExerciseState.InProgress)
        assert(started.exercises.drop(1).all { it.state == ExerciseState.NotStarted })
        assert(started.state == WorkoutState.InProgress)

        val firstCompleted = started.copy(
            exercises = started.exercises.mapIndexed { index, exercise ->
                if (index != 0) {
                    exercise
                } else {
                    exercise.copy(
                        warmupSets = exercise.warmupSets?.map {
                            it.copy(setState = SetState.Completed)
                        },
                        sets = exercise.sets.map { it.copy(setState = SetState.Completed) },
                    )
                }
            },
        )
        assert(firstCompleted.exercises.first().state == ExerciseState.Completed)
        assert(firstCompleted.exercises[1].state == ExerciseState.NotStarted)
        assert(firstCompleted.state == WorkoutState.InProgress)
    }

    @Test
    fun `empty session is illegal`() {
        val error = assertFailsWith<IllegalArgumentException> {
            WorkoutSession(
                planId = WorkoutPlanId("empty"),
                planName = "Empty",
                planDescription = null,
                exercises = emptyList(),
                startedAt = LocalDateTime.of(2026, 1, 1, 12, 0),
            )
        }
        assert(error.message == "Session must contain at least one exercise")
    }

    @Test
    fun `exercise without work sets is illegal`() {
        val bench = catalogExercise("bench-press-bb")
        val error = assertFailsWith<IllegalArgumentException> {
            WorkoutExercise(
                bench,
                8..10,
                sets = emptyList(),
            )
        }
        assert(error.message == "sets must contain at least one set")
    }

    @Test
    fun `warmup sets cannot be an empty list`() {
        val bench = catalogExercise("bench-press-bb")
        val error = assertFailsWith<IllegalArgumentException> {
            WorkoutExercise(
                exerciseDefinition = bench,
                repRange = 8..10,
                warmupSets = emptyList(),
                sets = listOf(
                    ExerciseSet(bench, Weight.kilograms(50), 8, SetState.NotStated),
                ),
            )
        }
        assert(error.message == "warmupSets must be null or contain at least one set")
    }

    @Test
    fun `warmup set count cannot be negative`() {
        val bench = catalogExercise("bench-press-bb")
        val error = assertFailsWith<IllegalArgumentException> {
            PlanedExercise(
                exercise = bench,
                warmupSets = -1,
                sets = 3,
                repRange = 8..10,
            )
        }
        assert(error.message == "warmupSets must be >= 0")
    }

    @Test
    fun `reps must be positive`() {
        val bench = catalogExercise("bench-press-bb")
        val error = assertFailsWith<IllegalArgumentException> {
            ExerciseSet(bench, Weight.kilograms(50), 0, SetState.NotStated)
        }
        assert(error.message == "reps must be positive")
    }

    @Test
    fun `completing every set completes the workout`() {
        val startedAt = LocalDateTime.of(2026, 1, 1, 12, 0)
        val now = startedAt.plusMinutes(47)
        var session = fullBodyA.toWorkoutSession(startedAt = startedAt)
        session.exercises.indices.forEach { exerciseIndex ->
            val warmupCount = session.exercises[exerciseIndex].warmupSets?.size ?: 0
            (0 until warmupCount).forEach { index ->
                session = session.completeSet(
                    session.exercises[exerciseIndex],
                    index,
                    isWarmup = true,
                    now = now,
                )
            }
            session.exercises[exerciseIndex].sets.indices.forEach { index ->
                session = session.completeSet(session.exercises[exerciseIndex], index, now = now)
            }
        }

        assert(session.exercises.all { it.state == ExerciseState.Completed })
        assert(session.workoutSets.all { it.setState == SetState.Completed })
        assert(session.state == WorkoutState.Completed(47.minutes))
    }
}
