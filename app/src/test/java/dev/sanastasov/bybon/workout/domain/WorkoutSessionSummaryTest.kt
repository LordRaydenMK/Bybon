package dev.sanastasov.bybon.workout.domain

import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import java.time.LocalDateTime
import org.junit.Test

class WorkoutSessionSummaryTest {

    private val startedAt = LocalDateTime.of(2026, 11, 11, 18, 0)

    @Test
    fun `counts working sets reps volume and exercises`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = startedAt,
            exercises = listOf(
                completedExercise("incline-db-press", 17.5f to 15, 17.5f to 13, 17.5f to 11),
                completedExercise("romanian-deadlift", 45f to 12, 45f to 12),
            ),
        )

        val summary = session.workoutSessionSummary(listOf(session))

        assert(summary.exerciseCount == 2)
        assert(summary.workingSetCount == 5)
        assert(summary.totalReps == 15 + 13 + 11 + 12 + 12)
        assert(summary.volumeLoadKg == 17.5f * 39 + 45f * 24)
        assert(summary.setPerformance == ExercisePerformanceChange(0, 0, 0))
        assert(summary.exercisePerformance.isEmpty())
    }

    @Test
    fun `working set totals ignore warmup sets`() {
        val bench = completedExercise("bench-press", 80f to 8, 80f to 7).copy(
            warmupSets = listOf(
                ExerciseSet(
                    exerciseDefinition = catalogExercise("bench-press"),
                    weight = Weight.kilograms(40),
                    reps = 10,
                    setState = SetState.Completed(),
                ),
            ),
        )
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = startedAt,
            exercises = listOf(bench),
        )

        val summary = session.workoutSessionSummary(listOf(session))

        assert(summary.workingSetCount == 2)
        assert(summary.totalReps == 15)
        assert(summary.volumeLoadKg == 80f * 8 + 80f * 7)
    }

    @Test
    fun `set performance compares matched working sets to the previous session`() {
        val previous = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = startedAt.minusDays(4),
            exercises = listOf(
                completedExercise("incline-db-press", 17.5f to 14, 17.5f to 12, 17.5f to 10),
                completedExercise("romanian-deadlift", 45f to 12),
                completedExercise("squat-machine", 80f to 8),
            ),
        )
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = startedAt,
            exercises = listOf(
                completedExercise("incline-db-press", 17.5f to 15, 17.5f to 13, 17.5f to 11),
                completedExercise("romanian-deadlift", 45f to 12),
                completedExercise("squat-machine", 75f to 8),
                completedExercise("lateral-raise-machine", 20f to 12),
            ),
        )

        val summary = session.workoutSessionSummary(listOf(previous, session))

        assert(
            summary.setPerformance == ExercisePerformanceChange(
                improved = 3,
                maintained = 1,
                declined = 1,
            ),
        )
        assert(summary.setPerformance.comparableCount == 5)
        assert(
            summary.exercisePerformance.map { it.exerciseId } == listOf(
                "incline-db-press",
                "romanian-deadlift",
                "squat-machine",
            ),
        )
        val incline = summary.exercisePerformance[0]
        val rdl = summary.exercisePerformance[1]
        val squat = summary.exercisePerformance[2]
        assert(incline.currentAverage > incline.previousAverage)
        assert(rdl.currentAverage == rdl.previousAverage)
        assert(squat.currentAverage < squat.previousAverage)
    }

    @Test
    fun `skips sets and exercises with no previous 1RM`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = startedAt,
            exercises = listOf(
                completedExercise("bench-press", 80f to 8),
                WorkoutExercise(
                    exerciseDefinition = catalogExercise("assisted-pull-ups"),
                    repRange = 6..10,
                    sets = listOf(
                        ExerciseSet(
                            catalogExercise("assisted-pull-ups"),
                            null,
                            8,
                            SetState.Completed(),
                        ),
                    ),
                ),
            ),
        )

        val summary = session.workoutSessionSummary(listOf(session))

        assert(summary.setPerformance == ExercisePerformanceChange(0, 0, 0))
        assert(summary.exercisePerformance.isEmpty())
    }

    @Test
    fun `muscles trained use primary muscles in display order`() {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = startedAt,
            exercises = listOf(
                completedExercise("bench-press", 60f to 8),
                completedExercise("iso-lat-row", 50f to 10),
                completedExercise("squat-machine", 80f to 8),
                completedExercise("seated-leg-curl", 40f to 12),
                completedExercise("lateral-raise-machine", 20f to 12),
                completedExercise("incline-db-curl", 10f to 12),
                completedExercise("triceps-press-machine", 40f to 12),
            ),
        )

        val summary = session.workoutSessionSummary(listOf(session))

        assert(
            summary.musclesTrained == listOf(
                TrainedMuscle.Chest,
                TrainedMuscle.Back,
                TrainedMuscle.Quads,
                TrainedMuscle.Hamstrings,
                TrainedMuscle.Glutes,
                TrainedMuscle.Shoulders,
                TrainedMuscle.Biceps,
                TrainedMuscle.Triceps,
            ),
        )
    }
}
