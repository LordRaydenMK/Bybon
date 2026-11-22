package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.workout.data.completedAssistedExercise
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import java.time.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class LastSevenDaysSummaryTest {

    private val today = LocalDate.of(2026, 11, 11)

    @Test
    fun `window is today minus six days through today`() {
        val inWindow = sessionOn(today.minusDays(6), completedExercise("bench-press", 60f to 8))
        val outside = sessionOn(today.minusDays(7), completedExercise("squat", 80f to 5))

        val summary = listOf(inWindow, outside).lastSevenDaysSummary(today)

        assert(summary.startDate == LocalDate.of(2026, 11, 5))
        assert(summary.endDate == today)
        assert(summary.workoutCount == 1)
    }

    @Test
    fun `counts workouts working sets weight and training time in the window`() {
        val first = completedSession(
            planId = "upper-body-a",
            planName = "Upper body A",
            startedAt = today.minusDays(2).atTime(18, 0),
            exercises = listOf(
                completedExercise("bench-press", 60f to 8, 60f to 7),
                completedExercise("lateral-raise-machine", 20f to 12),
            ),
        ).copy(duration = 1.hours + 10.minutes)
        val second = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = today.atTime(17, 0),
            exercises = listOf(
                completedExercise("seated-leg-curl", 40f to 10, 40f to 10, 40f to 8),
            ),
        ).copy(duration = 48.minutes + 20.seconds)

        val summary = listOf(first, second).lastSevenDaysSummary(today)

        assert(summary.workoutCount == 2)
        assert(summary.workingSetCount == 6)
        assert(summary.volumeLoad == Weight.kilograms(60f * 15 + 20f * 12 + 40f * 28))
        assert(summary.trainingTime == 1.hours + 58.minutes + 20.seconds)
    }

    @Test
    fun `working set totals ignore warmup sets`() {
        val bench = completedExercise("bench-press", 80f to 8).copy(
            warmupSets = listOf(
                ExerciseSet(
                    exerciseDefinition = catalogExercise("bench-press"),
                    weight = Weight.kilograms(40),
                    reps = 10,
                    setState = SetState.Completed(),
                ),
            ),
        )
        val session = sessionOn(today, bench)

        val summary = listOf(session).lastSevenDaysSummary(today)

        assert(summary.workingSetCount == 1)
        assert(summary.volumeLoad == Weight.kilograms(80f * 8))
    }

    @Test
    fun `draft and in-progress sessions are ignored`() {
        val completed = sessionOn(today, completedExercise("bench-press", 60f to 8))
        val draft = fullBodyA.toOverviewSession()
        val inProgress = completed.copy(
            exercises = completed.exercises.map { exercise ->
                exercise.copy(sets = exercise.sets.map { it.copy(setState = SetState.InProgress) })
            },
        )

        val summary = listOf(completed, draft, inProgress).lastSevenDaysSummary(today)

        assert(summary.workoutCount == 1)
    }

    @Test
    fun `compares each exercise to its latest session before the window`() {
        val previousBench = sessionOn(
            today.minusDays(20),
            completedExercise("bench-press", 70f to 8),
        )
        val olderBetterSquat = sessionOn(
            today.minusDays(30),
            completedExercise("squat", 120f to 5),
        )
        val latestSquatBefore = sessionOn(
            today.minusDays(10),
            completedExercise("squat", 100f to 5),
        )
        val previousCurl = sessionOn(
            today.minusDays(14),
            completedExercise("incline-db-curl", 10f to 12),
        )
        val current = sessionOn(
            today,
            completedExercise("bench-press", 80f to 8),
            completedExercise("squat", 110f to 5),
            completedExercise("incline-db-curl", 10f to 12),
            completedExercise("seated-leg-curl", 40f to 12),
        )

        val summary = listOf(
            previousBench,
            olderBetterSquat,
            latestSquatBefore,
            previousCurl,
            current,
        ).lastSevenDaysSummary(today)

        assert(summary.performance.improved == 2)
        assert(summary.performance.maintained == 1)
        assert(summary.performance.declined == 0)
        assert(summary.performance.comparableCount == 3)
    }

    @Test
    fun `uses the best top-set 1RM in the window`() {
        val previous = sessionOn(
            today.minusDays(10),
            completedExercise("bench-press", 70f to 8),
        )
        val weaker = sessionOn(
            today.minusDays(2),
            completedExercise("bench-press", 60f to 8),
        )
        val stronger = sessionOn(
            today,
            completedExercise("bench-press", 80f to 8),
        )

        val summary = listOf(previous, weaker, stronger).lastSevenDaysSummary(today)

        assert(summary.performance.improved == 1)
        assert(summary.performance.maintained == 0)
        assert(summary.performance.declined == 0)
    }

    @Test
    fun `skips exercises with no prior 1RM`() {
        val missingLoad = completedAssistedExercise(
            "assisted-pull-ups",
            bodyWeightKg = null,
            null to 8,
        )
        val firstTime = sessionOn(today, completedExercise("bench-press", 60f to 8), missingLoad)

        val summary = listOf(firstTime).lastSevenDaysSummary(today)

        assert(summary.performance == ExercisePerformanceChange(0, 0, 0))
        assert(summary.volumeLoad == Weight.kilograms(60f * 8))
    }

    @Test
    fun `assisted volume is body weight minus assistance`() {
        val session = sessionOn(
            today,
            completedAssistedExercise("assisted-pull-ups", 80f, 25f to 8, 20f to 6),
            completedAssistedExercise("assisted-dips", 80f, 15f to 10),
            completedExercise("bench-press", 60f to 5),
        )

        val summary = listOf(session).lastSevenDaysSummary(today)

        assert(summary.volumeLoad == Weight.kilograms(55f * 8 + 60f * 6 + 65f * 10 + 60f * 5))
    }

    @Test
    fun `assisted performance improves when assistance drops`() {
        val previous = sessionOn(
            today.minusDays(10),
            completedAssistedExercise("assisted-pull-ups", 80f, 25f to 8),
            completedAssistedExercise("assisted-dips", 80f, 20f to 8),
        )
        val current = sessionOn(
            today,
            completedAssistedExercise("assisted-pull-ups", 80f, 20f to 8),
            completedAssistedExercise("assisted-dips", 80f, 20f to 8),
        )

        val summary = listOf(previous, current).lastSevenDaysSummary(today)

        assert(summary.performance.improved == 1)
        assert(summary.performance.maintained == 1)
        assert(summary.performance.declined == 0)
        assert(summary.volumeLoad == Weight.kilograms(60f * 8 + 60f * 8))
    }

    @Test
    fun `assisted performance improves without body weight when assistance drops`() {
        val previous = sessionOn(
            today.minusDays(10),
            completedAssistedExercise("assisted-pull-ups", null, 25f to 8),
        )
        val harder = sessionOn(
            today.minusDays(2),
            completedAssistedExercise("assisted-pull-ups", null, 20f to 8),
        )
        val easier = sessionOn(
            today.minusDays(1),
            completedAssistedExercise("assisted-pull-ups", null, 30f to 8),
        )

        val summary = listOf(previous, easier, harder).lastSevenDaysSummary(today)

        assert(summary.performance.improved == 1)
        assert(summary.performance.maintained == 0)
        assert(summary.performance.declined == 0)
    }

    @Test
    fun `assisted performance counts reps and extra assistance without body weight`() {
        val previous = sessionOn(
            today.minusDays(10),
            completedAssistedExercise("assisted-pull-ups", null, 20f to 6),
            completedAssistedExercise("assisted-dips", null, 15f to 8),
        )
        val current = sessionOn(
            today,
            completedAssistedExercise("assisted-pull-ups", null, 20f to 8),
            completedAssistedExercise("assisted-dips", null, 20f to 8),
        )

        val summary = listOf(previous, current).lastSevenDaysSummary(today)

        assert(summary.performance.improved == 1)
        assert(summary.performance.declined == 1)
        assert(summary.performance.maintained == 0)
    }

    @Test
    fun `assisted performance is maintained when assistance and reps match`() {
        val previous = sessionOn(
            today.minusDays(10),
            completedAssistedExercise("assisted-pull-ups", null, 20f to 8),
        )
        val current = sessionOn(
            today,
            completedAssistedExercise("assisted-pull-ups", null, 20f to 8),
        )

        val summary = listOf(previous, current).lastSevenDaysSummary(today)

        assert(summary.performance == ExercisePerformanceChange(0, 1, 0))
    }

    @Test
    fun `marks a decline against the previous top set`() {
        val previous = sessionOn(
            today.minusDays(10),
            completedExercise("bench-press", 80f to 8),
        )
        val current = sessionOn(today, completedExercise("bench-press", 70f to 8))

        val summary = listOf(previous, current).lastSevenDaysSummary(today)

        assert(summary.performance.declined == 1)
        assert(summary.performance.improved == 0)
        assert(summary.performance.maintained == 0)
    }

    @Test
    fun `empty history yields zeros`() {
        val summary = emptyList<WorkoutSession>().lastSevenDaysSummary(today)

        assert(summary.workoutCount == 0)
        assert(summary.workingSetCount == 0)
        assert(summary.volumeLoad == null)
        assert(summary.trainingTime == Duration.ZERO)
        assert(summary.performance == ExercisePerformanceChange(0, 0, 0))
        assert(summary.daysTrained.weeks.flatMap { it.days }.none { it.trained })
    }

    private fun sessionOn(date: LocalDate, vararg exercises: WorkoutExercise) = completedSession(
        planId = "plan",
        planName = "Plan",
        startedAt = date.atTime(18, 0),
        exercises = exercises.toList(),
    )
}
