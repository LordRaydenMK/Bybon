package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class WorkoutSessionTest {

    @Test
    fun `complete first warmup completes it and makes the next warmup in progress`() {
        val session = fullBodyA.toWorkoutSession()
        val bench = session.exercises.first()

        val actual = session.completeSet(bench, 0, isWarmup = true)
        val actualBench = actual.exercises.first()
        val warmupSets = checkNotNull(actualBench.warmupSets)

        assert(warmupSets[0].setState == SetState.Completed)
        assert(warmupSets[1].setState == SetState.InProgress)
        assert(warmupSets[2].setState == SetState.NotStated)
        assert(actualBench.sets.all { it.setState == SetState.NotStated })
    }

    @Test
    fun `completing last warmup starts the first work set`() {
        val session = fullBodyA.toWorkoutSession().completeWarmups(0)

        val actual = session.completeSet(session.exercises.first(), 2, isWarmup = true)
        val bench = actual.exercises.first()

        assert(checkNotNull(bench.warmupSets).all { it.setState == SetState.Completed })
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
        assert(checkNotNull(squat.warmupSets).first().setState == SetState.InProgress)
        assert(squat.sets.all { it.setState == SetState.NotStated })
        assert(actual.inProgressExerciseIndex() == 1)
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
        assert(checkNotNull(actual.exercises[1].warmupSets).first().setState == SetState.InProgress)
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
                        warmupSets = exercise.warmupSets?.mapIndexed { index, set ->
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
        val firstWarmup = checkNotNull(actual.exercises.first().warmupSets).first()
        assert(firstWarmup.weight == Weight.kilograms(22))
        assert(firstWarmup.reps == 6)
        assert(firstWarmup.previous == PreviousSetPerformance(Weight.kilograms(22), 6))
        assert(checkNotNull(actual.exercises.first().warmupSets)[1].weight == Weight.kilograms(23))
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
                        warmupSets = exercise.warmupSets?.map { set ->
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
                9,
            ),
        )
    }

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
    }

    @Test
    fun `remove last set on overview draft does not start another set`() {
        val overview = fullBodyA.toOverviewSession()

        val actual = overview.removeLastSet(overview.exercises.first())

        assert(actual.exercises.first().sets.size == 2)
        assert(actual.exercises.first().warmupSets?.size == 3)
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

    @Test
    fun `resetSetToPrevious restores weight and reps from previous performance`() {
        val session = fullBodyA.toOverviewSession().let { draft ->
            draft.copy(
                exercises = draft.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.mapIndexed { index, set ->
                            set.copy(
                                weight = Weight.kilograms(60),
                                reps = 10,
                                previous = PreviousSetPerformance(Weight.kilograms(40 + index), 9),
                            )
                        },
                    )
                },
            )
        }

        val actual = session.resetSetToPrevious(session.exercises.first(), 0, isWarmup = false)

        assert(actual.exercises.first().sets.first().weight == Weight.kilograms(40))
        assert(actual.exercises.first().sets.first().reps == 9)
        assert(actual.exercises.first().sets[1].weight == Weight.kilograms(60))
    }

    @Test
    fun `resetExerciseToPrevious restores all sets with previous performance`() {
        val session = fullBodyA.toOverviewSession().let { draft ->
            draft.copy(
                exercises = draft.exercises.map { exercise ->
                    exercise.copy(
                        sets = exercise.sets.map { set ->
                            set.copy(
                                weight = Weight.kilograms(60),
                                reps = 10,
                                previous = PreviousSetPerformance(Weight.kilograms(40), 9),
                            )
                        },
                    )
                },
            )
        }

        val actual = session.resetExerciseToPrevious(session.exercises.first())

        assert(
            actual.exercises.first().sets.all { it.weight == Weight.kilograms(40) && it.reps == 9 },
        )
        assert(actual.exercises[1].sets.first().weight == Weight.kilograms(60))
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
                    warmupSets = exercise.warmupSets?.map {
                        it.copy(setState = SetState.Completed)
                    },
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
        assert(Weight.kilograms(50.50f) == Weight.kilograms(50.5f))
        assert(Weight.kilograms(50.50f).kilograms == "50.5")
        assert(Weight.parseString("50.50").kilograms == "50.5")
        assert(Weight.kilograms(50.25f).kilograms == "50.25")
        assert(Weight.kilograms(74.48f).kilograms == "74.48")
        assertFailsWith<IllegalArgumentException> { Weight.kilograms(0) }
        assertFailsWith<IllegalArgumentException> { Weight.kilograms(0f) }
        assertFailsWith<IllegalArgumentException> { Weight.kilograms(-2.5f) }
        assert(Weight.kilogramsOrNull(0f) == null)
        assertFailsWith<IllegalArgumentException> {
            Weight.kilograms(2.5f) - Weight.kilograms(2.5f)
        }
    }

    @Test
    fun `estimated one RM is a Weight`() {
        val set = ExerciseSet(
            catalogExercise("bench-press-bb"),
            Weight.kilograms(60),
            8,
            SetState.Completed,
        )
        assert(set.oneRm == Weight.kilograms(estimateOneRmKg(60f, 8)))
        assert(set.oneRm.kilograms == "74.48")
    }

    @Test
    fun `converting last warmup restores it as work set one`() {
        val session = fullBodyA.toOverviewSession()
        val converted = session.convertFirstWorkSetToWarmup(session.exercises.first())

        val actual = converted.convertLastWarmupToWorkSet(converted.exercises.first())
        val bench = actual.exercises.first()

        assert(bench.warmupSets?.size == 3)
        assert(bench.sets.size == 3)
        assert(bench.sets.first() == session.exercises.first().sets.first())
    }

    @Test
    fun `converting the last remaining warmup leaves warmup sets null`() {
        val session = fullBodyB.toOverviewSession()
        val splitSquat = session.exercises.first { it.id == "split-squat-db" }
        assert(splitSquat.warmupSets?.size == 1)

        val actual = session.convertLastWarmupToWorkSet(splitSquat)
        val updated = actual.exercises.first { it.id == "split-squat-db" }

        assert(updated.warmupSets == null)
        assert(updated.sets.size == 4)
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
    fun `add set still adds a work set after warmups`() {
        val session = fullBodyA.toOverviewSession()

        val actual = session.addSet(session.exercises.first())
        val bench = actual.exercises.first()

        assert(bench.warmupSets?.size == 3)
        assert(bench.sets.size == 4)
    }

    @Test
    fun `toWorkoutSession seeds planned warmup weights`() {
        val session = fullBodyA.toWorkoutSession()
        val bench = session.exercises.first()

        val warmupSets = checkNotNull(bench.warmupSets)
        assert(warmupSets.size == 3)
        assert(
            warmupSets.map { it.weight } == listOf(
                Weight.kilograms(20),
                Weight.kilograms(20),
                Weight.kilograms(20),
            ),
        )
        assert(warmupSets.map { it.reps } == listOf(8, 4, 3))
        assert(warmupSets.first().setState == SetState.InProgress)
        assert(bench.sets.all { it.setState == SetState.NotStated })
        assert(session.exercises.first { it.id == "leg-curl" }.warmupSets == null)
    }

    @Test
    fun `numbered sets expose warmup and work indexes`() {
        val bench = fullBodyA.toWorkoutSession().exercises.first()
        val legCurl = fullBodyA.toWorkoutSession().exercises.first { it.id == "leg-curl" }

        assert(
            checkNotNull(bench.numberedWarmupSets).map { it.isWarmup to it.index } ==
                listOf(true to 0, true to 1, true to 2),
        )
        assert(bench.numberedWorkSets.map { it.workSetNumber } == listOf(1, 2, 3))
        assert(legCurl.numberedWarmupSets == null)
        assert(legCurl.numberedWorkSets.size == legCurl.sets.size)
    }

    @Test
    fun `default rest is 2 minutes for compounds, 1 for isolation, 1_5 otherwise`() {
        assert(catalogExercise("bench-press-bb").defaultRest == 2.minutes)
        assert(catalogExercise("squat-bb").defaultRest == 2.minutes)
        assert(catalogExercise("rdl-bb").defaultRest == 2.minutes)
        assert(catalogExercise("skullcrusher-db").defaultRest == 1.minutes)
        assert(catalogExercise("incline-curl-db").defaultRest == 1.minutes)
        assert(catalogExercise("leg-curl").defaultRest == 90.seconds)
        assert(catalogExercise("leg-extension").defaultRest == 90.seconds)
        assert(2.minutes.formatRestClock() == "2:00")
        assert(90.seconds.formatRestClock() == "1:30")
        assert(1.minutes.formatRestClock() == "1:00")
    }

    @Test
    fun `toWorkoutSession copies plan rest onto each exercise`() {
        val session = fullBodyA.toWorkoutSession()

        assert(
            session.exercises.map { it.id to it.restAfterWorkSet } == listOf(
                "bench-press-bb" to 2.minutes,
                "squat-bb" to 2.minutes,
                "pullup-assisted" to 2.minutes,
                "leg-curl" to 90.seconds,
                "upright-row-db" to 1.minutes,
                "skullcrusher-db" to 1.minutes,
            ),
        )
    }
}

private fun WorkoutSession.completeWarmups(exerciseIndex: Int): WorkoutSession {
    val warmupCount = exercises[exerciseIndex].warmupSets?.size ?: 0
    return (0 until warmupCount).fold(this) { session, index ->
        session.completeSet(session.exercises[exerciseIndex], index, isWarmup = true)
    }
}

private fun WorkoutSession.completeWorkSets(exerciseIndex: Int, count: Int): WorkoutSession =
    (0 until count).fold(this) { session, index ->
        session.completeSet(session.exercises[exerciseIndex], index)
    }
