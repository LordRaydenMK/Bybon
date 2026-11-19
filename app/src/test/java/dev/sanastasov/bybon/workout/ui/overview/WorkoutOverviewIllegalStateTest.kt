package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.test.BackgroundFailures
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.fullBodyA
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test

class WorkoutOverviewIllegalStateTest {

    @Test
    fun `missing plan is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = overviewViewModel(
            WorkoutPlanId("missing"),
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            failures.scope,
        )
        failures.expectFailure("Plan missing does not exist") {
            failures.scope.launch { viewModel.uiState.first { it != null } }
        }
    }

    @Test
    fun `edits before overview loads are rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = overviewViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            failures.scope,
        )
        failures.expectFailure("No overview session") {
            viewModel.onAction(WorkoutOverviewAction.OnIncreaseWorkout)
        }
    }
}
