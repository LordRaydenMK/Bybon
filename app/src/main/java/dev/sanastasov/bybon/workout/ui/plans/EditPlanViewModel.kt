package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addExercise
import dev.sanastasov.bybon.workout.domain.addWarmupSet
import dev.sanastasov.bybon.workout.domain.addWorkSet
import dev.sanastasov.bybon.workout.domain.moveExerciseDown
import dev.sanastasov.bybon.workout.domain.moveExerciseUp
import dev.sanastasov.bybon.workout.domain.removeExercise
import dev.sanastasov.bybon.workout.domain.removeLastWarmupSet
import dev.sanastasov.bybon.workout.domain.removeLastWorkSet
import dev.sanastasov.bybon.workout.domain.rename
import dev.sanastasov.bybon.workout.domain.requireExercise
import dev.sanastasov.bybon.workout.domain.withNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EditPlanViewModel(
    private val planId: WorkoutPlanId,
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val edits = Mutex()
    private val _effects = Channel<EditPlanEffect>(Channel.BUFFERED)
    val effects: Flow<EditPlanEffect> = _effects.receiveAsFlow()

    private val _uiState = MutableStateFlow<EditPlanUiState?>(null)
    val uiState: StateFlow<EditPlanUiState?> = _uiState

    init {
        coroutineScope.launch {
            edits.withLock { ensureLoaded() }
        }
    }

    fun onAction(action: EditPlanAction) {
        coroutineScope.launch {
            edits.withLock {
                when (action) {
                    EditPlanAction.OnBack -> handleBack()

                    EditPlanAction.OnSave -> handleSave()

                    EditPlanAction.OnDiscardConfirmed ->
                        _effects.trySend(EditPlanEffect.NavigateBack)

                    EditPlanAction.OnDiscardDismissed -> dismissDiscard()

                    EditPlanAction.OnArchivePlan -> {
                        repository.archivePlan(planId, archived = true)
                        _effects.trySend(EditPlanEffect.NavigateBack)
                    }

                    EditPlanAction.OnAddExercise -> openLibrary()

                    is EditPlanAction.OnExercisePicked -> addPickedExercise(action.exerciseId)

                    else -> {
                        val state = requireState()
                        publish(
                            state.plan.applyEdit(action),
                            state.loadedPlan,
                            state.confirmDiscard,
                        )
                    }
                }
            }
        }
    }

    private fun handleBack() {
        val state = _uiState.value
        if (state != null && state.plan != state.loadedPlan) {
            publish(state.plan, state.loadedPlan, confirmDiscard = true)
        } else {
            _effects.trySend(EditPlanEffect.NavigateBack)
        }
    }

    private suspend fun handleSave() {
        val state = requireState()
        if (state.plan != state.loadedPlan) {
            val saved = state.plan.copy(id = planId)
            repository.updatePlan(planId) { _ -> saved }
            publish(saved, saved, confirmDiscard = false)
        }
    }

    private fun dismissDiscard() {
        _uiState.value?.let { state ->
            publish(state.plan, state.loadedPlan, confirmDiscard = false)
        }
    }

    private suspend fun openLibrary() {
        val plan = requireState().plan
        _effects.trySend(
            EditPlanEffect.OpenExerciseLibrary(plan.sets.map { it.exercise.id }),
        )
    }

    private suspend fun addPickedExercise(exerciseId: String) {
        val exercise = repository.requireExercise(exerciseId)
        val state = requireState()
        publish(
            state.plan.addExercise(exercise),
            state.loadedPlan,
            state.confirmDiscard,
        )
    }

    private suspend fun ensureLoaded() {
        if (_uiState.value == null) {
            val plan = repository.workoutPlans().first().firstOrNull { it.id == planId }
            if (plan != null && _uiState.value == null) {
                publish(plan, plan, confirmDiscard = false)
            }
        }
    }

    private suspend fun requireState(): EditPlanUiState {
        ensureLoaded()
        return checkNotNull(_uiState.value) {
            "Plan $planId is not in the repository"
        }
    }

    private fun publish(draft: WorkoutPlan, loadedPlan: WorkoutPlan, confirmDiscard: Boolean) {
        _uiState.value = EditPlanUiState(
            plan = draft,
            loadedPlan = loadedPlan,
            confirmDiscard = confirmDiscard,
        )
    }
}

private fun WorkoutPlan.applyEdit(action: EditPlanAction): WorkoutPlan = when (action) {
    is EditPlanAction.OnRenamePlan -> rename(action.name)

    is EditPlanAction.OnUpdateNote -> withNote(action.note)

    is EditPlanAction.OnAddSet -> addWorkSet(action.exerciseId)

    is EditPlanAction.OnRemoveLastSet -> removeLastWorkSet(action.exerciseId)

    is EditPlanAction.OnAddWarmupSet -> addWarmupSet(action.exerciseId)

    is EditPlanAction.OnRemoveLastWarmupSet -> removeLastWarmupSet(action.exerciseId)

    is EditPlanAction.OnRemoveExercise -> removeExercise(action.exerciseId)

    is EditPlanAction.OnMoveExerciseUp -> moveExerciseUp(action.exerciseId)

    is EditPlanAction.OnMoveExerciseDown -> moveExerciseDown(action.exerciseId)

    EditPlanAction.OnAddExercise,
    EditPlanAction.OnArchivePlan,
    EditPlanAction.OnBack,
    EditPlanAction.OnDiscardConfirmed,
    EditPlanAction.OnDiscardDismissed,
    is EditPlanAction.OnExercisePicked,
    EditPlanAction.OnSave,
    -> error("Not a draft edit: $action")
}
