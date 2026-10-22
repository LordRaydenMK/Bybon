package dev.sanastasov.bybon.workout.ui.details

import app.cash.turbine.test
import dev.sanastasov.bybon.test.BackgroundFailures
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
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
