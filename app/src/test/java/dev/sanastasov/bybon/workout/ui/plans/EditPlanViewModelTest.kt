package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.test.BackgroundFailures
import dev.sanastasov.bybon.workout.data.FakeWorkoutsRepository
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.addWarmupSet
import dev.sanastasov.bybon.workout.domain.addWorkSet
import dev.sanastasov.bybon.workout.domain.catalogExercise
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.toWorkoutSession
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test

class EditPlanViewModelTest {

    @Test
    fun `loads the matching plan once`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        val state = viewModel.uiState.first { it != null }!!
        assert(state.plan == fullBodyA)
        assert(!state.canSave)
        assert(!state.confirmDiscard)

        repository.updatePlan(fullBodyA.id) { it.addWorkSet("bench-press") }
        yield()

        assert(viewModel.uiState.value?.plan == fullBodyA)
        assert(viewModel.uiState.value?.canSave == false)
    }

    @Test
    fun `archiving the plan hides it and navigates back`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press"))
        viewModel.uiState.first { it?.canSave == true }
        viewModel.onAction(EditPlanAction.OnArchivePlan)

        assert(viewModel.effects.first() == EditPlanEffect.NavigateBack)
        assert(repository.workoutPlans().first() == listOf(fullBodyB))
        val archived = repository.workoutPlans(WorkoutPlansFilter.AllPlans).first()
            .first { it.id == fullBodyA.id }
        assert(archived.isArchived)
        assert(archived.sets == fullBodyA.sets)
    }

    @Test
    fun `adding a set changes the working copy and not the repository`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press"))

        val updated = viewModel.uiState.first { it?.plan?.sets?.first()?.sets == 4 }!!
        assert(updated.canSave)
        assert(updated.plan.sets.first().warmupSets == 3)
        assert(repository.workoutPlans().first() == listOf(fullBodyA, fullBodyB))
    }

    @Test
    fun `removing the last set updates the working copy`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnRemoveLastSet("seated-leg-curl"))

        val updated = viewModel.uiState.first { state ->
            state?.plan?.sets?.first { it.exercise.id == "seated-leg-curl" }?.sets == 2
        }!!
        assert(updated.canSave)
        assert(updated.plan.sets.first { it.exercise.id == "seated-leg-curl" }.sets == 2)
        assert(updated.plan.sets.first().sets == 3)
        assert(repository.workoutPlans().first().single() == fullBodyA)
    }

    @Test
    fun `adding a warmup updates the working copy`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddWarmupSet("seated-leg-curl"))

        val updated = viewModel.uiState.first { state ->
            state?.plan?.sets?.first { it.exercise.id == "seated-leg-curl" }?.warmupSets == 1
        }!!
        val curl = updated.plan.sets.first { it.exercise.id == "seated-leg-curl" }
        assert(updated.canSave)
        assert(curl.warmupSets == 1)
        assert(curl.sets == 3)
        assert(updated.plan.sets.first().warmupSets == 3)
        assert(repository.workoutPlans().first() == listOf(fullBodyA, fullBodyB))
    }

    @Test
    fun `removing the last warmup updates the working copy`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnRemoveLastWarmupSet("bench-press"))

        val updated = viewModel.uiState.first { state ->
            state?.plan?.sets?.first()?.warmupSets == 2
        }!!
        assert(updated.canSave)
        assert(updated.plan.sets.first().warmupSets == 2)
        assert(updated.plan.sets.first().sets == 3)
        assert(repository.workoutPlans().first().single() == fullBodyA)
    }

    @Test
    fun `removing a warmup when none remain is rejected`() = runTest {
        val failures = BackgroundFailures(this)
        val viewModel = EditPlanViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            failures.scope,
        )
        viewModel.uiState.first { it != null }
        failures.expectFailure(
            "Cannot remove last warmup set from seated-leg-curl; none remain",
        ) {
            viewModel.onAction(EditPlanAction.OnRemoveLastWarmupSet("seated-leg-curl"))
        }
    }

    @Test
    fun `removing an exercise updates the working copy`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnRemoveExercise("seated-leg-curl"))

        val updated = viewModel.uiState.first { state ->
            state?.plan?.sets?.none { it.exercise.id == "seated-leg-curl" } == true
        }!!
        assert(updated.canSave)
        assert(updated.plan.sets.none { it.exercise.id == "seated-leg-curl" })
        assert(updated.plan.sets.size == fullBodyA.sets.size - 1)
        assert(repository.workoutPlans().first().single() == fullBodyA)
    }

    @Test
    fun `moving an exercise down updates the working copy`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnMoveExerciseDown("bench-press"))

        val updated = viewModel.uiState.first { state ->
            state?.plan?.sets?.map { it.exercise.id }?.take(2) == listOf("squat", "bench-press")
        }!!
        assert(updated.canSave)
        assert(updated.plan.sets.map { it.exercise.id }.take(2) == listOf("squat", "bench-press"))
        assert(updated.plan.sets.drop(2) == fullBodyA.sets.drop(2))
        assert(repository.workoutPlans().first() == listOf(fullBodyA, fullBodyB))
    }

    @Test
    fun `moving an exercise up updates the working copy`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)

        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnMoveExerciseUp("squat"))

        val updated = viewModel.uiState.first { state ->
            state?.plan?.sets?.first()?.exercise?.id == "squat"
        }!!
        assert(updated.canSave)
        assert(updated.plan.sets.map { it.exercise.id }.take(2) == listOf("squat", "bench-press"))
        assert(repository.workoutPlans().first().single() == fullBodyA)
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
    fun `add exercise offers ids from the unsaved working copy`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnExercisePicked("incline-db-curl"))
        viewModel.uiState.first { it?.plan?.sets?.last()?.exercise?.id == "incline-db-curl" }

        viewModel.onAction(EditPlanAction.OnAddExercise)

        assert(
            viewModel.effects.first() == EditPlanEffect.OpenExerciseLibrary(
                fullBodyA.sets.map { it.exercise.id } + "incline-db-curl",
            ),
        )
        assert(repository.workoutPlans().first().single() == fullBodyA)
    }

    @Test
    fun `picked exercise is added to the working copy`() = runTest {
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialExercises = catalogExercises,
        )
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }

        viewModel.onAction(EditPlanAction.OnExercisePicked("incline-db-curl"))

        val added = viewModel.uiState.first { state ->
            state?.plan?.sets?.last()?.exercise?.id == "incline-db-curl"
        }!!.plan.sets.last()
        assert(added.exercise == catalogExercise("incline-db-curl"))
        assert(added.sets == 3)
        assert(added.warmupSets == 0)
        assert(added.repRange == 8..12)
        assert(added.restAfterWorkSet == 1.minutes)
        assert(repository.workoutPlans().first().single() == fullBodyA)
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
        failures.expectFailure("Exercise bench-press is already on the plan") {
            viewModel.onAction(EditPlanAction.OnExercisePicked("bench-press"))
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
            viewModel.onAction(EditPlanAction.OnRemoveExercise("bench-press"))
        }
    }

    @Test
    fun `name and note edits stay in the working copy until save`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }

        viewModel.onAction(EditPlanAction.OnNameChanged("Push A"))
        viewModel.uiState.first { it?.plan?.name == "Push A" }
        viewModel.onAction(EditPlanAction.OnNameChanged(fullBodyA.name))
        val restored = viewModel.uiState.first { it?.plan?.name == fullBodyA.name }!!
        assert(!restored.canSave)
        assert(restored.plan == fullBodyA)
        assert(repository.workoutPlans().first().first() == fullBodyA)

        viewModel.onAction(EditPlanAction.OnNameChanged("Push A"))
        viewModel.onAction(EditPlanAction.OnNoteChanged("Heavier bench"))

        val edited = viewModel.uiState.first { state ->
            val plan = state?.plan
            plan != null && plan.name == "Push A" && plan.description == "Heavier bench"
        }!!
        assert(edited.canSave)
        assert(repository.workoutPlans().first() == listOf(fullBodyA, fullBodyB))

        viewModel.onAction(EditPlanAction.OnSave)

        val saved = viewModel.uiState.first { it?.canSave == false }!!
        assert(saved.plan.id == fullBodyA.id)
        assert(saved.plan.name == "Push A")
        assert(saved.plan.description == "Heavier bench")
        assert(saved.plan.sets == fullBodyA.sets)
        assert(repository.workoutPlans().first().first() == saved.plan)
        assert(repository.workoutPlans().first()[1] == fullBodyB)
    }

    @Test
    fun `clearing the note stores null`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }

        viewModel.onAction(EditPlanAction.OnNoteChanged(""))

        val cleared = viewModel.uiState.first { it?.plan?.description == null }!!
        assert(cleared.canSave)
        assert(repository.workoutPlans().first().single() == fullBodyA)

        viewModel.onAction(EditPlanAction.OnSave)

        val saved = viewModel.uiState.first { it?.canSave == false }!!
        assert(saved.plan.description == null)
        assert(repository.workoutPlans().first().single().description == null)
        assert(repository.workoutPlans().first().single().name == fullBodyA.name)
    }

    @Test
    fun `save writes the whole working copy on the same id and disables save`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press"))
        viewModel.onAction(EditPlanAction.OnNameChanged("Push A"))
        viewModel.uiState.first { state ->
            val plan = state?.plan
            plan != null && plan.name == "Push A" && plan.sets.first().sets == 4
        }
        repository.updatePlan(fullBodyA.id) { it.addWarmupSet("bench-press") }

        viewModel.onAction(EditPlanAction.OnSave)

        val saved = viewModel.uiState.first { it?.canSave == false }!!
        val stored = repository.workoutPlans().first()
        assert(saved.plan.id == fullBodyA.id)
        assert(stored.first() == saved.plan)
        assert(stored.first().sets.first().sets == 4)
        assert(stored.first().sets.first().warmupSets == 3)
        assert(stored[1] == fullBodyB)

        viewModel.onAction(EditPlanAction.OnAddSet("bench-press"))
        val editedAgain = viewModel.uiState.first { it?.plan?.sets?.first()?.sets == 5 }!!
        assert(editedAgain.canSave)
        assert(repository.workoutPlans().first().first().sets.first().sets == 4)
    }

    @Test
    fun `save does not rewrite an in-progress workout`() = runTest {
        val startedAt = LocalDateTime.of(2026, 3, 1, 9, 0)
        val inProgress = fullBodyA.toWorkoutSession(startedAt = startedAt)
        assert(inProgress.state is WorkoutState.InProgress)
        val repository = FakeWorkoutsRepository(
            initialPlans = listOf(fullBodyA),
            initialSessions = listOf(inProgress),
        )
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press"))
        viewModel.uiState.first { it?.canSave == true }

        viewModel.onAction(EditPlanAction.OnSave)
        viewModel.uiState.first { it?.canSave == false }

        assert(repository.workoutPlans().first().single().sets.first().sets == 4)
        assert(repository.workoutSessions().first() == listOf(inProgress))
        assert(repository.activeWorkoutSession().first() == inProgress)
    }

    @Test
    fun `back with no edits leaves immediately`() = runTest {
        val viewModel = EditPlanViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            backgroundScope,
        )
        viewModel.uiState.first { it != null }

        viewModel.onAction(EditPlanAction.OnBack)

        assert(viewModel.effects.first() == EditPlanEffect.NavigateBack)
        assert(viewModel.uiState.value?.confirmDiscard == false)
    }

    @Test
    fun `back with unsaved edits asks to discard and can keep them`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press"))
        viewModel.uiState.first { it?.canSave == true }

        viewModel.onAction(EditPlanAction.OnBack)

        val asking = viewModel.uiState.first { it?.confirmDiscard == true }!!
        assert(asking.canSave)
        assert(asking.plan.sets.first().sets == 4)
        assert(repository.workoutPlans().first().single() == fullBodyA)

        viewModel.onAction(EditPlanAction.OnDiscardDismissed)

        val kept = viewModel.uiState.first { it?.confirmDiscard == false }!!
        assert(kept.canSave)
        assert(kept.plan.sets.first().sets == 4)
        assert(repository.workoutPlans().first().single() == fullBodyA)
    }

    @Test
    fun `confirming discard leaves without writing`() = runTest {
        val repository = FakeWorkoutsRepository(initialPlans = listOf(fullBodyA, fullBodyB))
        val viewModel = EditPlanViewModel(fullBodyA.id, repository, backgroundScope)
        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press"))
        viewModel.uiState.first { it?.canSave == true }
        viewModel.onAction(EditPlanAction.OnBack)
        viewModel.uiState.first { it?.confirmDiscard == true }

        viewModel.onAction(EditPlanAction.OnDiscardConfirmed)

        assert(viewModel.effects.first() == EditPlanEffect.NavigateBack)
        assert(repository.workoutPlans().first() == listOf(fullBodyA, fullBodyB))
    }

    @Test
    fun `back after save leaves without asking to discard`() = runTest {
        val viewModel = EditPlanViewModel(
            fullBodyA.id,
            FakeWorkoutsRepository(initialPlans = listOf(fullBodyA)),
            backgroundScope,
        )
        viewModel.uiState.first { it != null }
        viewModel.onAction(EditPlanAction.OnAddSet("bench-press"))
        viewModel.uiState.first { it?.canSave == true }
        viewModel.onAction(EditPlanAction.OnSave)
        viewModel.uiState.first { it?.canSave == false }

        viewModel.onAction(EditPlanAction.OnBack)

        assert(viewModel.effects.first() == EditPlanEffect.NavigateBack)
        assert(viewModel.uiState.value?.confirmDiscard == false)
    }
}
