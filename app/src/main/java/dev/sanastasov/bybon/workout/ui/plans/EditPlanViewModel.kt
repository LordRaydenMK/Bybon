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
import dev.sanastasov.bybon.workout.domain.requireExercise
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

    private var loadedPlan: WorkoutPlan? = null
    private var confirmDiscard: Boolean = false

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

                    else -> publish(requireDraft().applyEdit(action))
                }
            }
        }
    }

    private fun handleBack() {
        val draft = _uiState.value?.plan
        val baseline = loadedPlan
        if (draft != null && baseline != null && draft != baseline) {
            confirmDiscard = true
            publish(draft)
        } else {
            _effects.trySend(EditPlanEffect.NavigateBack)
        }
    }

    private suspend fun handleSave() {
        val draft = requireDraft()
        val baseline = loadedPlan
        if (baseline != null && draft != baseline) {
            val saved = draft.copy(id = planId)
            repository.updatePlan(planId) { _ -> saved }
            loadedPlan = saved
            confirmDiscard = false
            publish(saved)
        }
    }

    private fun dismissDiscard() {
        confirmDiscard = false
        _uiState.value?.plan?.let(::publish)
    }

    private suspend fun openLibrary() {
        val plan = requireDraft()
        _effects.trySend(
            EditPlanEffect.OpenExerciseLibrary(plan.sets.map { it.exercise.id }),
        )
    }

    private suspend fun addPickedExercise(exerciseId: String) {
        val exercise = repository.requireExercise(exerciseId)
        publish(requireDraft().addExercise(exercise))
    }

    private suspend fun ensureLoaded() {
        if (loadedPlan == null) {
            val plan = repository.workoutPlans().first().firstOrNull { it.id == planId }
            if (plan != null && loadedPlan == null) {
                loadedPlan = plan
                publish(plan)
            }
        }
    }

    private suspend fun requireDraft(): WorkoutPlan {
        ensureLoaded()
        return checkNotNull(_uiState.value?.plan) {
            "Plan $planId is not in the repository"
        }
    }

    private fun publish(draft: WorkoutPlan) {
        val baseline = checkNotNull(loadedPlan)
        _uiState.value = EditPlanUiState(
            plan = draft,
            canSave = draft != baseline,
            confirmDiscard = confirmDiscard,
        )
    }
}

private fun WorkoutPlan.applyEdit(action: EditPlanAction): WorkoutPlan = when (action) {
    is EditPlanAction.OnNameChanged -> copy(name = action.name)

    is EditPlanAction.OnNoteChanged -> copy(description = action.note.ifEmpty { null })

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
