package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import java.time.LocalDateTime
import org.junit.Test

class ExercisePerformanceTest {

    private val now = LocalDateTime.of(2026, 10, 22, 18, 0)

    @Test
    fun `top set is the heaviest working set even when another set has a higher 1RM`() {
        val exercise = completedExercise("bench-press", 50f to 5, 45f to 12)

        assert(exercise.topSet.weight == Weight.kilograms(50f))
        assert(exercise.topSet.reps == 5)
        assert(exercise.topSet.oneRm == Weight.kilograms(estimateOneRmKg(50f, 5)))
    }

    @Test
    fun `top set ignores warmup sets`() {
        val bench = completedExercise("bench-press", 80f to 8).copy(
            warmupSets = listOf(
                ExerciseSet(
                    exerciseDefinition = catalogExercise("bench-press"),
                    weight = Weight.kilograms(200),
                    reps = 1,
                    setState = SetState.Completed,
                ),
            ),
        )

        assert(bench.topSet.weight == Weight.kilograms(80))
        assert(bench.topSet.reps == 8)
    }

    @Test
    fun `PR is the highest working-set 1RM across completed sessions`() {
        val olderBetter = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 6, 1, 18, 0),
            exercises = listOf(completedExercise("bench-press", 80f to 8)),
        )
        val recent = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 10, 10, 18, 0),
            exercises = listOf(completedExercise("bench-press", 60f to 8)),
        )

        val performance = listOf(olderBetter, recent).exercisePerformance("bench-press", now)

        assert(performance.personalRecordOneRm == Weight.kilograms(estimateOneRmKg(80f, 8)))
        assert(performance.recentSessions.single().startedAt == recent.startedAt)
        assert(
            performance.recentSessions.single().topSetOneRm ==
                Weight.kilograms(estimateOneRmKg(60f, 8)),
        )
    }

    @Test
    fun `PR ignores warmup sets and null load`() {
        val bench = completedExercise("bench-press", 50f to 5).copy(
            warmupSets = listOf(
                ExerciseSet(
                    exerciseDefinition = catalogExercise("bench-press"),
                    weight = Weight.kilograms(200),
                    reps = 1,
                    setState = SetState.Completed,
                ),
            ),
        )
        val pullUp = WorkoutExercise(
            exerciseDefinition = catalogExercise("assisted-pull-ups"),
            repRange = 6..10,
            sets = listOf(
                ExerciseSet(
                    exerciseDefinition = catalogExercise("assisted-pull-ups"),
                    weight = null,
                    reps = 8,
                    setState = SetState.Completed,
                ),
            ),
        )
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = now.minusDays(1),
            exercises = listOf(bench, pullUp),
        )

        val benchPerformance = listOf(session).exercisePerformance("bench-press", now)
        val pullUpPerformance = listOf(session).exercisePerformance("assisted-pull-ups", now)

        assert(benchPerformance.personalRecordOneRm == Weight.kilograms(estimateOneRmKg(50f, 5)))
        assert(pullUpPerformance.personalRecordOneRm == null)
        assert(pullUpPerformance.recentSessions.single().topSetOneRm == null)
        assert(pullUpPerformance.recentSessions.single().workSets.single().weight == null)
    }

    @Test
    fun `recent history is limited to three months and newest first`() {
        val included = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = now.minusMonths(3),
            exercises = listOf(completedExercise("bench-press", 55f to 8)),
        )
        val excluded = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = now.minusMonths(3).minusDays(1),
            exercises = listOf(completedExercise("bench-press", 90f to 5)),
        )
        val newest = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = now.minusDays(2),
            exercises = listOf(completedExercise("bench-press", 60f to 8, 60f to 7)),
        )

        val performance = listOf(included, excluded, newest)
            .exercisePerformance("bench-press", now)

        assert(performance.personalRecordOneRm == Weight.kilograms(estimateOneRmKg(90f, 5)))
        assert(
            performance.recentSessions.map { it.startedAt } ==
                listOf(newest.startedAt, included.startedAt),
        )
        assert(performance.recentSessions.first().planName == "Full Body B")
        assert(performance.recentSessions.first().workSets.map { it.reps } == listOf(8, 7))
    }

    @Test
    fun `in-progress sessions and other exercises are ignored`() {
        val inProgress = fullBodyA.toWorkoutSession(startedAt = now.minusDays(1))
        val otherLift = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = now.minusDays(3),
            exercises = listOf(completedExercise("squat", 100f to 5)),
        )
        val matching = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = now.minusDays(4),
            exercises = listOf(completedExercise("bench-press", 70f to 8)),
        )

        val performance = listOf(inProgress, otherLift, matching)
            .exercisePerformance("bench-press", now)

        assert(performance.personalRecordOneRm == Weight.kilograms(estimateOneRmKg(70f, 8)))
        assert(performance.recentSessions.single().startedAt == matching.startedAt)
    }
}
