package dev.sanastasov.bybon.workout.ui.summary

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
import dev.sanastasov.bybon.workout.domain.completedSessionKey
import dev.sanastasov.bybon.workout.domain.estimateOneRmKg
import dev.sanastasov.bybon.workout.domain.exercisesMap
import java.time.LocalDateTime
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutSummaryViewModelTest {

    @Test
    fun `completed session is shown with every completed set`() = runTest {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(
                completedExercise(
                    "rdl-bb",
                    45f to 12,
                    45f to 11,
                ),
                completedExercise(
                    "incline-bench-press-db",
                    20f to 13,
                    20f to 11,
                    20f to 8,
                ),
            ),
        )
        val viewModel = summaryViewModel(session)

        val expected = WorkoutSummaryUiState.Content(
            title = "Full Body B",
            exercises = listOf(
                WorkoutSummaryExerciseUi(
                    id = "rdl-bb",
                    name = "Romanian Deadlift (RDL) (barbell)",
                    sets = listOf(
                        WorkoutSummarySetUi(1, "45", 12, estimateOneRmKg(45f, 12)),
                        WorkoutSummarySetUi(2, "45", 11, estimateOneRmKg(45f, 11)),
                    ),
                ),
                WorkoutSummaryExerciseUi(
                    id = "incline-bench-press-db",
                    name = "Incline Bench Press (dumbbell)",
                    sets = listOf(
                        WorkoutSummarySetUi(1, "20", 13, estimateOneRmKg(20f, 13)),
                        WorkoutSummarySetUi(2, "20", 11, estimateOneRmKg(20f, 11)),
                        WorkoutSummarySetUi(3, "20", 8, estimateOneRmKg(20f, 8)),
                    ),
                ),
            ),
        )
        viewModel.uiState.test {
            assert(awaitItem() == WorkoutSummaryUiState.Loading)
            assert(awaitItem() == expected)
        }
    }

    @Test
    fun `only the matching completed session is shown`() = runTest {
        val matching = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val other = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = LocalDateTime.of(2026, 8, 10, 18, 0),
            exercises = listOf(completedExercise("bench-press-bb", 80f to 8)),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(other, matching))
        val viewModel = WorkoutSummaryViewModel(
            checkNotNull(matching.completedSessionKey()),
            repository,
            backgroundScope,
        )

        viewModel.uiState.test {
            skipItems(1)
            val actual = awaitItem() as WorkoutSummaryUiState.Content
            assert(actual.title == "Full Body B")
            assert(actual.exercises.single().id == "rdl-bb")
        }
    }

    @Test
    fun `incomplete sets are omitted and completed set numbers are preserved`() = runTest {
        val bench = exercisesMap.getValue("bench-press-bb")
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
                        ExerciseSet(bench, Weight.kilograms(80), 7, SetState.InProgress),
                        ExerciseSet(bench, Weight.kilograms(80), 6, SetState.NotStated),
                    ),
                ),
            ),
        )
        val viewModel = summaryViewModel(session)

        viewModel.uiState.test {
            skipItems(1)
            val sets = (awaitItem() as WorkoutSummaryUiState.Content)
                .exercises.single().sets
            assert(sets == listOf(WorkoutSummarySetUi(1, "80", 8, estimateOneRmKg(80f, 8))))
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
        val viewModel = summaryViewModel(session)

        viewModel.uiState.test {
            skipItems(1)
            val exercises = (awaitItem() as WorkoutSummaryUiState.Content).exercises
            assert(exercises.size == 1)
            assert(exercises.single().name == "Bench Press (barbell)")
        }
    }

    @Test
    fun `unknown session key emits not found`() = runTest {
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(session))
        val viewModel = WorkoutSummaryViewModel(
            "missing-session",
            repository,
            backgroundScope,
        )

        viewModel.uiState.test {
            assert(awaitItem() == WorkoutSummaryUiState.Loading)
            assert(awaitItem() == WorkoutSummaryUiState.NotFound)
        }
    }

    @Test
    fun `in-progress sessions are not shown as a summary`() = runTest {
        val inProgress = WorkoutSession(
            planId = WorkoutPlanId("full-body-a"),
            planName = "Full Body A",
            planDescription = null,
            exercises = listOf(completedExercise("bench-press-bb", 80f to 8)),
            state = WorkoutState.InProgress(LocalDateTime.of(2026, 8, 14, 18, 0)),
        )
        val repository = FakeWorkoutsRepository(initialSessions = listOf(inProgress))
        val viewModel = WorkoutSummaryViewModel(
            "full-body-a-2026-08-14T18:00",
            repository,
            backgroundScope,
        )

        viewModel.uiState.test {
            skipItems(1)
            assert(awaitItem() == WorkoutSummaryUiState.NotFound)
        }
    }

    @Test
    fun `ui state updates when the repository emits the session`() = runTest {
        val repository = FakeWorkoutsRepository()
        val session = completedSession(
            planId = "full-body-b",
            planName = "Full Body B",
            startedAt = LocalDateTime.of(2026, 8, 13, 18, 0),
            exercises = listOf(completedExercise("rdl-bb", 45f to 12)),
        )
        val viewModel = WorkoutSummaryViewModel(
            checkNotNull(session.completedSessionKey()),
            repository,
            backgroundScope,
        )
        val expected = WorkoutSummaryUiState.Content(
            title = "Full Body B",
            exercises = listOf(
                WorkoutSummaryExerciseUi(
                    id = "rdl-bb",
                    name = "Romanian Deadlift (RDL) (barbell)",
                    sets = listOf(
                        WorkoutSummarySetUi(1, "45", 12, estimateOneRmKg(45f, 12)),
                    ),
                ),
            ),
        )

        viewModel.uiState.test {
            assert(awaitItem() == WorkoutSummaryUiState.Loading)
            assert(awaitItem() == WorkoutSummaryUiState.NotFound)
            repository.emitSessions(listOf(session))
            assert(awaitItem() == expected)
        }
    }

    private fun TestScope.summaryViewModel(session: WorkoutSession) = WorkoutSummaryViewModel(
        checkNotNull(session.completedSessionKey()),
        FakeWorkoutsRepository(initialSessions = listOf(session)),
        backgroundScope,
    )
}
