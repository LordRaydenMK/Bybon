package dev.sanastasov.bybon.workout.ui.details

import app.cash.turbine.test
import dev.sanastasov.bybon.test.BackgroundFailures
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.data.completedExercise
import dev.sanastasov.bybon.workout.data.completedSession
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.exercisePerformance
import java.time.LocalDateTime
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExerciseDetailsViewModelTest {

    @Test
    fun `loads catalog details for the exercise id`() = runTest {
        val viewModel = ExerciseDetailsViewModel(
            "bench-press",
            FakeWorkoutsRepository(initialExercises = catalogExercises),
            backgroundScope,
        )

        viewModel.uiState.test {
            assert(awaitItem() == ExerciseDetailsUiState.Loading)
            assert(awaitItem() == catalogExercise("bench-press").toDetailsUi())
        }
    }

    @Test
    fun `attaches all-time PR and recent session history`() = runTest {
        val now = LocalDateTime.of(2026, 10, 22, 18, 0)
        val recent = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = now.minusDays(5),
            exercises = listOf(completedExercise("bench-press", 70f to 8)),
        )
        val olderPr = completedSession(
            planId = "full-body-a",
            planName = "Full Body A",
            startedAt = now.minusMonths(5),
            exercises = listOf(completedExercise("bench-press", 90f to 5)),
        )
        val viewModel = ExerciseDetailsViewModel(
            "bench-press",
            FakeWorkoutsRepository(
                initialExercises = catalogExercises,
                initialSessions = listOf(recent, olderPr),
            ),
            backgroundScope,
            now = { now },
        )

        viewModel.uiState.test {
            assert(awaitItem() == ExerciseDetailsUiState.Loading)
            assert(
                awaitItem() == catalogExercise("bench-press").toDetailsUi(
                    listOf(recent, olderPr).exercisePerformance("bench-press", now),
                ),
            )
        }
    }

    @Test
    fun `unknown exercise id is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = ExerciseDetailsViewModel(
            "missing",
            FakeWorkoutsRepository(initialExercises = catalogExercises),
            failures.scope,
        )
        failures.expectFailure("Exercise missing is not in the repository") {
            backgroundScope.launch { viewModel.uiState.collect {} }
        }
    }
}
