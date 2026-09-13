package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import org.junit.Test

class WorkoutSessionTest {

    @Test
    fun `complete first warmup completes it and makes the next warmup in progress`() {
        val session = fullBodyA.toWorkoutSession()
        val bench = session.exercises.first()

        val actual = session.completeSet(bench, 0, isWarmup = true)
        val actualBench = actual.exercises.first()

        assert(actualBench.warmupSets[0].setState == SetState.Completed)
        assert(actualBench.warmupSets[1].setState == SetState.InProgress)
        assert(actualBench.warmupSets[2].setState == SetState.NotStated)
        assert(actualBench.sets.all { it.setState == SetState.NotStated })
    }

    @Test
    fun `completing last warmup starts the first work set`() {
        val session = fullBodyA.toWorkoutSession().completeWarmups(0)

        val actual = session.completeSet(session.exercises.first(), 2, isWarmup = true)
        val bench = actual.exercises.first()

        assert(bench.warmupSets.all { it.setState == SetState.Completed })
        assert(bench.sets.first().setState == SetState.InProgress)
    }

    @Test
    fun `completing last work set starts the next exercise warmup`() {
        val session = fullBodyA.toWorkoutSession()
            .completeWarmups(0)
            .completeWorkSets(0, count = 2)

        val actual = session.completeSet(session.exercises.first(), 2)
        val squat = actual.exercises[1]

        assert(actual.exercises.first().sets.all { it.setState == SetState.Completed })
        assert(squat.warmupSets.first().setState == SetState.InProgress)
        assert(squat.sets.all { it.setState == SetState.NotStated })
    }

    @Test
    fun `update first work set weight - first set weight updated`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.updateWeight(session.exercises.first(), 0, Weight.kilograms(100))

        val expected = ExerciseSet(
            session.exercises.first().exerciseDefinition,
            Weight.kilograms(100),
            8,
            SetState.NotStated,
        )
        assert(actual.exercises.first().sets.first() == expected)
        assert(actual.exercises.drop(1) == session.exercises.drop(1))
    }

    @Test
    fun `update squat first work set weight`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.updateWeight(session.exercises[1], 0, Weight.kilograms(100))

        val expected = ExerciseSet(
            session.exercises[1].exerciseDefinition,
            Weight.kilograms(100),
            8,
            SetState.NotStated,
        )
        assert(actual.exercises[1].sets.first() == expected)
        assert(actual.exercises.first() == session.exercises.first())
        assert(actual.exercises.drop(2) == session.exercises.drop(2))
    }

    @Test
    fun `update first work set reps - first exercise - reps updated`() {
        val session = fullBodyA.toWorkoutSession()

        val actual = session.updateReps(session.exercises.first(), 0, 10)

        val expected = ExerciseSet(
            session.exercises.first().exerciseDefinition,
            Weight.kilograms(50),
            10,
            SetState.NotStated,
        )
        assert(actual.exercises.first().sets.first() == expected)
        assert(actual.exercises.drop(1) == session.exercises.drop(1))
    }

    @Test
    fun `remove last not completed work set promotes the next exercise warmup`() {
        val session = fullBodyA.toWorkoutSession()
            .completeWarmups(0)
            .completeWorkSets(0, count = 2)
        // Bench work sets: [Completed, Completed, InProgress]

        val actual = session.removeLastSet(session.exercises.first())

        assert(actual.exercises.first().sets.size == 2)
        assert(actual.workoutSets.count { it.setState == SetState.InProgress } == 1)
        assert(actual.exercises[1].warmupSets.first().setState == SetState.InProgress)
    }

    @Test
    fun `Weight kilograms formats whole and decimal values`() {
        assert(Weight.kilograms(50).kilograms == "50")
        assert(Weight.kilograms(52.5f).kilograms == "52.5")
    }

    @Test
    fun `toWorkoutSession attaches previous work and warmup sets from same plan history`() {
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
                        warmupSets = exercise.warmupSets.mapIndexed { index, set ->
                            set.copy(
                                weight = Weight.kilograms(22 + index),
                                reps = 6,
                                setState = SetState.Completed,
                            )
                        },
                    )
                },
                state = WorkoutState.Completed(kotlin.time.Duration.ZERO),
                startedAt = java.time.LocalDateTime.of(2026, 1, 1, 12, 0),
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
        val firstWarmup = actual.exercises.first().warmupSets.first()
        assert(firstWarmup.weight == Weight.kilograms(22))
        assert(firstWarmup.reps == 6)
        assert(firstWarmup.previous == PreviousSetPerformance(Weight.kilograms(22), 6))
        assert(actual.exercises.first().warmupSets[1].weight == Weight.kilograms(23))
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
                        warmupSets = exercise.warmupSets.map { set ->
                            set.copy(setState = SetState.Completed)
                        },
                    )
                },
                state = WorkoutState.Completed(kotlin.time.Duration.ZERO),
                startedAt = java.time.LocalDateTime.of(2026, 1, 1, 12, 0),
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
                9
            )
        )
    }

    @Test
    fun `startWorkout marks the first warmup in progress`() {
        val overview = fullBodyA.toOverviewSession()

        val actual = overview.startWorkout()
        val bench = actual.exercises.first()

        assert(bench.warmupSets.first().setState == SetState.InProgress)
        assert(bench.warmupSets.drop(1).all { it.setState == SetState.NotStated })
        assert(bench.sets.all { it.setState == SetState.NotStated })
        assert(actual.workoutSets.drop(1).all { it.setState == SetState.NotStated })
    }

    @Test
    fun `remove last set on overview draft does not start another set`() {
        val overview = fullBodyA.toOverviewSession()

        val actual = overview.removeLastSet(overview.exercises.first())

        assert(actual.exercises.first().sets.size == 2)
        assert(actual.exercises.first().warmupSets.size == 3)
        assert(actual.workoutSets.none { it.setState == SetState.InProgress })
    }

    @Test
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

    @Test
    fun `session id is plan id plus started at for every state`() {
        val startedAt = LocalDateTime.of(2026, 8, 13, 18, 0)
        val notStarted = fullBodyA.toWorkoutSession(startedAt = startedAt)
        val expectedId = WorkoutSessionId(fullBodyA.id, startedAt)

        assert(notStarted.id == expectedId)
        assert(notStarted.copy(state = WorkoutState.InProgress).id == expectedId)

        val completed = notStarted.copy(
            exercises = notStarted.exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.map { it.copy(setState = SetState.Completed) },
                    warmupSets = exercise.warmupSets.map { it.copy(setState = SetState.Completed) },
                )
            },
            state = WorkoutState.Completed(Duration.ZERO),
        )
        assert(completed.id == expectedId)
    }

    @Test
    fun `completed session cannot have incomplete sets`() {
        val session = fullBodyA.toWorkoutSession(startedAt = LocalDateTime.of(2026, 1, 1, 12, 0))

        val error = assertFailsWith<IllegalArgumentException> {
            session.copy(state = WorkoutState.Completed(Duration.ZERO))
        }
        assert(error.message!!.contains("incomplete sets"))
    }

    @Test
    fun `weight formats whole kilos one decimal and two decimal values`() {
        assert(Weight.kilograms(50).kilograms == "50")
        assert(Weight.kilograms(52.5f).kilograms == "52.5")
        assert(Weight.kilograms(74.48f).kilograms == "74.48")
    }

    @Test
    fun `estimated one RM is a Weight`() {
        val set = ExerciseSet(
            exercisesMap.getValue("bench-press-bb"),
            Weight.kilograms(60),
            8,
            SetState.Completed,
        )
        assert(set.oneRm == Weight.kilograms(estimateOneRmKg(60f, 8)))
        assert(set.oneRm!!.kilograms == "74.48")
    }

    @Test
    fun `default plans include warmup sets from the strong sample`() {
        assert(
            fullBodyA.sets.map { it.exercise.id to it.warmupSets.size } == listOf(
            "bench-press-bb" to 3,
            "squat-bb" to 3,
            "pullup-assisted" to 2,
            "leg-curl" to 0,
            "upright-row-db" to 0,
            "skullcrusher-db" to 0,
        )
        )
        assert(
            fullBodyB.sets.map { it.exercise.id to it.warmupSets.size } == listOf(
            "rdl-bb" to 2,
            "incline-bench-press-db" to 2,
            "split-squat-db" to 1,
            "incline-row-db" to 0,
            "lateral-raise-db" to 0,
            "incline-curl-db" to 0,
        )
        )
        assert(fullBodyA.sets[0].warmupSets.all { it.weight == Weight.kilograms(20) })
        assert(fullBodyA.sets[0].warmupSets.map { it.reps } == listOf(8, 4, 3))
        assert(fullBodyB.sets[1].warmupSets.all { it.weight == Weight.kilograms(10) })
        assert(fullBodyB.sets[2].warmupSets.single().weight == Weight.kilograms(10))
    }

    @Test
    fun `converting set one to warmup prepends it before work sets`() {
        val session = fullBodyA.toOverviewSession()
        val bench = session.exercises.first()

        val actual = session.convertFirstWorkSetToWarmup(bench)
        val updated = actual.exercises.first()

        assert(updated.warmupSets.size == 4)
        assert(updated.sets.size == 2)
        assert(updated.warmupSets.last().weight == bench.sets.first().weight)
        assert(updated.warmupSets.last().reps == bench.sets.first().reps)
        assert(updated.sets.first() == bench.sets[1])
    }

    @Test
    fun `converting last warmup restores it as work set one`() {
        val session = fullBodyA.toOverviewSession()
        val converted = session.convertFirstWorkSetToWarmup(session.exercises.first())

        val actual = converted.convertLastWarmupToWorkSet(converted.exercises.first())
        val bench = actual.exercises.first()

        assert(bench.warmupSets.size == 3)
        assert(bench.sets.size == 3)
        assert(bench.sets.first() == session.exercises.first().sets.first())
    }

    @Test
    fun `add set still adds a work set after warmups`() {
        val session = fullBodyA.toOverviewSession()

        val actual = session.addSet(session.exercises.first())
        val bench = actual.exercises.first()

        assert(bench.warmupSets.size == 3)
        assert(bench.sets.size == 4)
    }

    @Test
    fun `toWorkoutSession seeds planned warmup weights`() {
        val session = fullBodyA.toWorkoutSession()
        val bench = session.exercises.first()

        assert(bench.warmupSets.size == 3)
        assert(
            bench.warmupSets.map { it.weight } == listOf(
            Weight.kilograms(20),
            Weight.kilograms(20),
            Weight.kilograms(20),
        )
        )
        assert(bench.warmupSets.map { it.reps } == listOf(8, 4, 3))
        assert(bench.warmupSets.first().setState == SetState.InProgress)
        assert(bench.sets.all { it.setState == SetState.NotStated })
    }
}

private fun WorkoutSession.completeWarmups(exerciseIndex: Int): WorkoutSession {
    val warmupCount = exercises[exerciseIndex].warmupSets.size
    return (0 until warmupCount).fold(this) { session, index ->
        session.completeSet(session.exercises[exerciseIndex], index, isWarmup = true)
    }
}

private fun WorkoutSession.completeWorkSets(exerciseIndex: Int, count: Int): WorkoutSession =
    (0 until count).fold(this) { session, index ->
        session.completeSet(session.exercises[exerciseIndex], index)
    }
