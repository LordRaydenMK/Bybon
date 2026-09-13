package dev.sanastasov.bybon.workout.ui.overview

import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.addSet
import dev.sanastasov.bybon.workout.domain.adjustAll
import dev.sanastasov.bybon.workout.domain.adjustExercise
import dev.sanastasov.bybon.workout.domain.removeLastSet
import dev.sanastasov.bybon.workout.domain.startWorkout
import dev.sanastasov.bybon.workout.domain.toOverviewSession
import dev.sanastasov.bybon.workout.domain.updateReps
import dev.sanastasov.bybon.workout.domain.updateWeight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkoutOverviewViewModel(
    val planId: WorkoutPlanId,
    val repository: WorkoutsRepository,
    val coroutineScope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow<WorkoutSession?>(null)
    val uiState: StateFlow<WorkoutSession?> = _uiState.asStateFlow()

    private val _effects = Channel<WorkoutOverviewEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutOverviewEffect> = _effects.receiveAsFlow()

    init {
        coroutineScope.launch {
            val plan = repository.workoutPlans().first().first { it.id == planId }
            val previousSession = repository.workoutSessions().first()
                .filter { it.planId == planId && it.state is WorkoutState.Completed }
                .maxByOrNull { (it.state as WorkoutState.Completed).startedAt }
            _uiState.value = plan.toOverviewSession(previousSession)
        }
    }

    fun onAction(action: WorkoutOverviewAction) {
        when (action) {
            WorkoutOverviewAction.OnIncreaseWorkout -> updateDraft { it.adjustAll(increase = true) }
            WorkoutOverviewAction.OnDecreaseWorkout -> updateDraft { it.adjustAll(increase = false) }
            is WorkoutOverviewAction.OnIncreaseExercise -> updateDraft {
                it.adjustExercise(action.exercise, increase = true)
            }

            is WorkoutOverviewAction.OnDecreaseExercise -> updateDraft {
                it.adjustExercise(action.exercise, increase = false)
            }

            is WorkoutOverviewAction.OnWeightUpdated -> updateDraft { session ->
                action.newWeight.toFloatOrNull()?.let { weight ->
                    session.updateWeight(action.exercise, action.index, Weight.kilograms(weight))
                } ?: session
            }

            is WorkoutOverviewAction.OnRepsUpdated -> updateDraft { session ->
                action.newReps.toIntOrNull()?.let { reps ->
                    session.updateReps(action.exercise, action.index, reps)
                } ?: session
            }

            is WorkoutOverviewAction.OnAddSet -> updateDraft { it.addSet(action.exercise) }
            is WorkoutOverviewAction.RemoveLastSet -> updateDraft { it.removeLastSet(action.exercise) }
            WorkoutOverviewAction.OnStartWorkout -> coroutineScope.launch {
                val session = _uiState.value ?: return@launch
                repository.updateWorkout(session.startWorkout())
                _effects.trySend(WorkoutOverviewEffect.NavigateToSession)
            }
        }
    }

    private fun updateDraft(transform: (WorkoutSession) -> WorkoutSession) {
        _uiState.update { session -> session?.let(transform) }
    }
}
