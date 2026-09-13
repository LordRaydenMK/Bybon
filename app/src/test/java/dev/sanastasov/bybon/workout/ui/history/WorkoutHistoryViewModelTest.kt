package dev.sanastasov.bybon.workout.ui.history

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.domain.exercisesMap
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WorkoutHistoryViewModelTest {

    @Test
    fun `empty repository emits empty history`() = runTest {
        val repository = FakeWorkoutsRepository()
        val viewModel = WorkoutHistoryViewModel(repository, backgroundScope)

        viewModel.uiState.test {
            assert(awaitItem() == null)
            assert(awaitItem() == emptyList<WorkoutSessionHistoryUi>())
        }
    }

    @Test
    fun `completed sessions are shown newest first with top set performance`() = runTest {
        val older = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                completedExercise(
                    "bench-press-bb",
                    80f to 8,
                    80f to 7,
                    80f to 6,
                ),
                completedExercise(
                    "squat-bb",
                    100f to 5,
                    90f to 8,
                ),
            ),
        )
        val newer = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(
                completedExercise(
                    "rdl-bb",
                    45f to 12,
                    45f to 12,
                ),
            ),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(older, newer))
        val viewModel = WorkoutHistoryViewModel(repository, backgroundScope)

        val expected = listOf(
            WorkoutSessionHistoryUi(
                key = "full-body-b-2026-08-13T18:00",
                planName = "Full Body B",
                date = LocalDate.of(2026, 8, 13),
                exercises = listOf(
                    ExerciseTopSetUi(
                        name = "Romanian Deadlift (RDL) (barbell)",
                        weightKg = "45",
                        reps = 12,
                        estimatedOneRmKg = estimateOneRmKg(45f, 12),
                    ),
                ),
            ),
            WorkoutSessionHistoryUi(
                key = "full-body-a-2026-08-10T18:00",
                planName = "Full Body A",
                date = LocalDate.of(2026, 8, 10),
                exercises = listOf(
                    ExerciseTopSetUi(
                        name = "Bench Press (barbell)",
                        weightKg = "80",
                        reps = 8,
                        estimatedOneRmKg = estimateOneRmKg(80f, 8),
                    ),
                    ExerciseTopSetUi(
                        name = "Squat (barbell)",
                        weightKg = "100",
                        reps = 5,
                        estimatedOneRmKg = estimateOneRmKg(100f, 5),
                    ),
                ),
            ),
        )
        viewModel.uiState.test {
            assert(awaitItem() == null)
            assert(awaitItem() == expected)
        }
    }

    @Test
    fun `in-progress sessions are excluded from history`() = runTest {
        val completed = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val inProgress = WorkoutSession(
            planId = WorkoutPlanId("full-body-a"),
            planName = "Full Body A",
            planDescription = null,
            exercises = listOf(completedExercise("bench-press-bb", 80f to 8)),
            state = WorkoutState.InProgress(LocalDateTime.of(2026, 8, 14, 18, 0)),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(inProgress, completed))
        val viewModel = WorkoutHistoryViewModel(repository, backgroundScope)

        viewModel.uiState.test {
            skipItems(1)
            val actual = awaitItem()
            assert(actual?.size == 1)
            assert(actual?.single()?.planName == "Full Body B")
            assert(actual?.single()?.date == LocalDate.of(2026, 8, 13))
        }
    }

    @Test
    fun `top set is the heaviest completed set even when another set has a higher 1RM`() = runTest {
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                completedExercise(
                    "bench-press-bb",
                    50f to 5,
                    45f to 12,
                ),
            ),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(session))
        val viewModel = WorkoutHistoryViewModel(repository, backgroundScope)

        viewModel.uiState.test {
            skipItems(1)
            val topSet = awaitItem()?.single()?.exercises?.single()
            assert(topSet?.weightKg == "50")
            assert(topSet?.reps == 5)
            assert(topSet?.estimatedOneRmKg == estimateOneRmKg(50f, 5))
        }
    }

    @Test
    fun `exercises without completed sets are omitted`() = runTest {
        val bench = exercisesMap.getValue("bench-press-bb")
        val squat = exercisesMap.getValue("squat-bb")
        val session = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(
                WorkoutExercise(
                    bench,
                    8..10,
                    listOf(
                        ExerciseSet(bench, Weight.kilograms(80), 8, SetState.Completed),
                    ),
                ),
                WorkoutExercise(
                    squat,
                    8..10,
                    listOf(
                        ExerciseSet(squat, Weight.kilograms(100), 8, SetState.NotStated),
                    ),
                ),
            ),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(session))
        val viewModel = WorkoutHistoryViewModel(repository, backgroundScope)

        viewModel.uiState.test {
            skipItems(1)
            val exercises = awaitItem()?.single()?.exercises
            assert(exercises?.size == 1)
            assert(exercises?.single()?.name == "Bench Press (barbell)")
        }
    }

    @Test
    fun `ui state updates when the repository emits new sessions`() = runTest {
        val repository = FakeWorkoutsRepository()
        val viewModel = WorkoutHistoryViewModel(repository, backgroundScope)
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val expected = listOf(
            WorkoutSessionHistoryUi(
                key = "full-body-b-2026-08-13T18:00",
                planName = "Full Body B",
                date = LocalDate.of(2026, 8, 13),
                exercises = listOf(
                    ExerciseTopSetUi(
                        name = "Romanian Deadlift (RDL) (barbell)",
                        weightKg = "45",
                        reps = 12,
                        estimatedOneRmKg = estimateOneRmKg(45f, 12),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            assert(awaitItem() == null)
            assert(awaitItem() == emptyList<WorkoutSessionHistoryUi>())
            repository.emitSessions(listOf(session))
            assert(awaitItem() == expected)
        }
    }
}
