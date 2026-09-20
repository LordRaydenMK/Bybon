package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.test.BackgroundFailures
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EditPlanViewModelTest {

    @Test
    fun `loads the matching plan`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        val state = viewModel.uiState.first { it != null }
        assert(state == fullBodyA)
    }

    @Test
    fun `archiving the plan hides it and navigates back`() = runTest {
        val repository: WorkoutsRepository =
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnArchivePlan)

        assert(viewModel.effects.first() == EditPlanEffect.NavigateBack)
        assert(repository.workoutPlans().first() == listOf(fullBodyB))
    }

    @Test
    fun `adding a set persists a work set on the planned exercise`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press-bb"))

        val updated = viewModel.uiState.first { it?.sets?.first()?.sets == 4 }!!
        assert(updated.sets.first().warmupSets == 3)
        assert(repository.workoutPlans().first().first().sets.first().sets == 4)
        assert(repository.workoutPlans().first()[1] == fullBodyB)
    }

    @Test
    fun `removing the last set persists one fewer work set`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnRemoveLastSet("leg-curl"))

        val updated = viewModel.uiState.first { plan ->
            plan?.sets?.first { it.exercise.id == "leg-curl" }?.sets == 2
        }!!
        assert(updated.sets.first { it.exercise.id == "leg-curl" }.sets == 2)
        assert(updated.sets.first().sets == 3)
    }

    @Test
    fun `removing an exercise persists the plan without it`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnRemoveExercise("leg-curl"))

        val updated = viewModel.uiState.first { plan ->
            plan?.sets?.none { it.exercise.id == "leg-curl" } == true
        }!!
        assert(updated.sets.none { it.exercise.id == "leg-curl" })
        assert(updated.sets.size == fullBodyA.sets.size - 1)
    }

    @Test
    fun `moving an exercise down persists the new order`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnMoveExerciseDown("bench-press-bb"))

        val updated = viewModel.uiState.first { plan ->
            plan?.sets?.map { it.exercise.id }?.take(2) == listOf("squat-bb", "bench-press-bb")
        }!!
        assert(updated.sets.map { it.exercise.id }.take(2) == listOf("squat-bb", "bench-press-bb"))
        assert(updated.sets.drop(2) == fullBodyA.sets.drop(2))
        assert(repository.workoutPlans().first()[1] == fullBodyB)
    }

    @Test
    fun `moving an exercise up persists the new order`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnMoveExerciseUp("squat-bb"))

        val updated = viewModel.uiState.first { plan ->
            plan?.sets?.first()?.exercise?.id == "squat-bb"
        }!!
        assert(updated.sets.map { it.exercise.id }.take(2) == listOf("squat-bb", "bench-press-bb"))
    }

    @Test
    fun `add exercise opens the exercise library with existing ids`() = runTest {
        val viewModel = EditPlanViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            backgroundScope,
        )

        viewModel.onAction(EditPlanAction.OnAddExercise)

        assert(
            viewModel.effects.first() == EditPlanEffect.OpenExerciseLibrary(
                fullBodyA.sets.map { it.exercise.id },
            ),
        )
    }

    @Test
    fun `picked exercise is added to the plan`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }

        viewModel.onAction(EditPlanAction.OnExercisePicked("incline-curl-db"))

        val added = viewModel.uiState.first { plan ->
            plan?.sets?.last()?.exercise?.id == "incline-curl-db"
        }!!.sets.last()
        assert(added.exercise == catalogExercise("incline-curl-db"))
        assert(added.sets == 3)
        assert(added.warmupSets == 0)
        assert(added.repRange == 8..12)
        assert(added.restAfterWorkSet == 1.minutes)
        assert(repository.workoutPlans().first().single().sets.dropLast(1) == fullBodyA.sets)
    }

    @Test
    fun `unknown picked exercise is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = EditPlanViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(
                initialPlans = listOf(fullBodyA),
                initialExercises = catalogExercises,
            ),
            failures.scope,
        )
        viewModel.uiState.first { it != null }
        failures.expectFailure("Exercise missing is not in the repository") {
            viewModel.onAction(EditPlanAction.OnExercisePicked("missing"))
        }
    }

    @Test
    fun `picked exercise already on the plan is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = EditPlanViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(
                initialPlans = listOf(fullBodyA),
                initialExercises = catalogExercises,
            ),
            failures.scope,
        )
        viewModel.uiState.first { it != null }
        failures.expectFailure("Exercise bench-press-bb is already on the plan") {
            viewModel.onAction(EditPlanAction.OnExercisePicked("bench-press-bb"))
        }
    }

    @Test
    fun `removing an unknown exercise is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = EditPlanViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            failures.scope,
        )
        viewModel.uiState.first { it != null }
        failures.expectFailure("Exercise missing is not in the plan") {
            viewModel.onAction(EditPlanAction.OnRemoveExercise("missing"))
        }
    }

    @Test
    fun `removing the last remaining exercise is rejected`() = runTest {
        val single = fullBodyA.copy(sets = listOf(fullBodyA.sets.first()))
        val failures = BackgroundFailures(this)
        val viewModel = EditPlanViewModel(
            single.id,
            FakeWorkoutsRepository(initialPlans = listOf(single)),
            failures.scope,
        )
        viewModel.uiState.first { it != null }
        failures.expectFailure("Cannot remove last exercise from the plan") {
            viewModel.onAction(EditPlanAction.OnRemoveExercise("bench-press-bb"))
        }
    }
}
