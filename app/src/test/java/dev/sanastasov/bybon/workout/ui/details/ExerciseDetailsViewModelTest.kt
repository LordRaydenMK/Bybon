package dev.sanastasov.bybon.workout.ui.details

import app.cash.turbine.test
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
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
    fun `missing exercise id shows missing state`() = runTest {
        val viewModel = ExerciseDetailsViewModel(
            "missing",
            FakeWorkoutsRepository(initialExercises = catalogExercises),
            backgroundScope,
        )

        viewModel.uiState.test {
            assert(awaitItem() == ExerciseDetailsUiState.Loading)
            assert(awaitItem() == ExerciseDetailsUiState.Missing)
        }
    }
}
