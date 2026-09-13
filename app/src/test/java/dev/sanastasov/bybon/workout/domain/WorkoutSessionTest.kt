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
            ),
        )
        assert(
            actual.workoutSets[1] == ExerciseSet(
                session.exercises.first().exerciseDefinition,
                Weight.kilograms(50),
                8,
                SetState.InProgress,
            ),
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

    @Test
    fun `toWorkoutSession attaches previous sets from same plan history`() {
        val previous = fullBodyA.toWorkoutSession().let { session ->
            session.copy(
                exercises = session.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.mapIndexed { index, set ->
                            set.copy(
                                weight = Weight.kilograms(40 + index),
                                reps = 9,
                                setState = SetState.Completed,
                            )
                        },
                    )
                },
                state = WorkoutState.Completed(
                    startedAt = java.time.LocalDateTime.of(2026, 1, 1, 12, 0),
                    duration = kotlin.time.Duration.ZERO,
                ),
            )
        }

        val actual = fullBodyA.toWorkoutSession(previous)

        val firstSet = actual.exercises.first().sets.first()
        assert(
            firstSet.previous == PreviousSetPerformance(Weight.kilograms(40), 9),
        )
        assert(firstSet.weight == Weight.kilograms(40))
        assert(firstSet.reps == 9)
        assert(actual.exercises.first().sets[1].previous?.weight == Weight.kilograms(41))
        assert(actual.exercises.first().sets[1].weight == Weight.kilograms(41))
        assert(actual.exercises.first().sets[1].reps == 9)
    }

    @Test
    fun `update weight preserves previous set reference`() {
        val previous = PreviousSetPerformance(Weight.kilograms(45), 12)
        val session = fullBodyA.toWorkoutSession().let { session ->
            session.copy(
                exercises = session.exercises.mapIndexed { exerciseIndex, exercise ->
                    if (exerciseIndex != 0) {
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

        val actual = session.updateWeight(session.exercises.first(), 0, Weight.kilograms(52.5f))

        assert(actual.exercises.first().sets.first().previous == previous)
    }

    @Test
    fun `toOverviewSession clears in-progress sets and keeps previous performance`() {
        val previous = fullBodyA.toWorkoutSession().let { session ->
            session.copy(
                exercises = session.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.map { set ->
                            set.copy(
                                weight = Weight.kilograms(40),
                                reps = 9,
                                setState = SetState.Completed,
                            )
                        },
                    )
                },
                state = WorkoutState.Completed(
                    startedAt = java.time.LocalDateTime.of(2026, 1, 1, 12, 0),
                    duration = kotlin.time.Duration.ZERO,
                ),
            )
        }

        val actual = fullBodyA.toOverviewSession(previous)

        assert(actual.state == WorkoutState.NotStarted)
        assert(actual.workoutSets.none { it.setState == SetState.InProgress })
        assert(actual.exercises.first().sets.first().weight == Weight.kilograms(40))
        assert(actual.exercises.first().sets.first().reps == 9)
        assert(
            actual.exercises.first().sets.first().previous == PreviousSetPerformance(
                Weight.kilograms(40),
                9,
            ),
        )
    }

    @Test
    fun `startWorkout marks the first set in progress`() {
        val overview = fullBodyA.toOverviewSession()

        val actual = overview.startWorkout()

        assert(actual.exercises.first().sets.first().setState == SetState.InProgress)
        assert(actual.workoutSets.drop(1).all { it.setState == SetState.NotStated })
    }

    @Test
    fun `remove last set on overview draft does not start another set`() {
        val overview = fullBodyA.toOverviewSession()

        val actual = overview.removeLastSet(overview.exercises.first())

        assert(actual.exercises.first().sets.size == 2)
        assert(actual.workoutSets.none { it.setState == SetState.InProgress })
    }

    @Test
    fun `resetTo restores the previous overview draft`() {
        val previous = fullBodyA.toOverviewSession()
        val increased = previous.adjustAll(increase = true)

        val actual = increased.resetTo(previous)

        assert(actual == previous)
    }

    @Test
    fun `resetExercise restores only that exercise from the previous draft`() {
        val previous = fullBodyA.toOverviewSession()
        val increased = previous.adjustAll(increase = true)

        val actual = increased.resetExercise(increased.exercises.first(), previous)

        assert(actual.exercises.first() == previous.exercises.first())
        assert(actual.exercises.drop(1) == increased.exercises.drop(1))
    }
}
