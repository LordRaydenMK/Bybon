package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.ui.stateInWhileInForeground
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class EditPlanViewModel(
    private val planId: WorkoutPlanId,
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<EditPlanEffect>(Channel.BUFFERED)
    val effects: Flow<EditPlanEffect> = _effects.receiveAsFlow()

    val uiState: StateFlow<WorkoutPlan?> = repository.workoutPlans()
        .map { plans -> plans.firstOrNull { it.id == planId } }
        .stateInWhileInForeground(coroutineScope, null)

    fun onAction(action: EditPlanAction) {
        coroutineScope.launch {
            when (action) {
                EditPlanAction.OnArchivePlan -> {
                    repository.archivePlan(planId, archived = true)
                    _effects.trySend(EditPlanEffect.NavigateBack)
                }

                is EditPlanAction.OnRenamePlan ->
                    repository.updatePlan(planId) { it.rename(action.name) }

                is EditPlanAction.OnUpdateNote ->
                    repository.updatePlan(planId) { it.withNote(action.note) }

                is EditPlanAction.OnAddSet ->
                    repository.updatePlan(planId) { it.addWorkSet(action.exerciseId) }

                is EditPlanAction.OnRemoveLastSet ->
                    repository.updatePlan(planId) { it.removeLastWorkSet(action.exerciseId) }

                is EditPlanAction.OnAddWarmupSet ->
                    repository.updatePlan(planId) { it.addWarmupSet(action.exerciseId) }

                is EditPlanAction.OnRemoveLastWarmupSet ->
                    repository.updatePlan(planId) { it.removeLastWarmupSet(action.exerciseId) }

                is EditPlanAction.OnRemoveExercise ->
                    repository.updatePlan(planId) { it.removeExercise(action.exerciseId) }

                is EditPlanAction.OnMoveExerciseUp ->
                    repository.updatePlan(planId) { it.moveExerciseUp(action.exerciseId) }

                is EditPlanAction.OnMoveExerciseDown ->
                    repository.updatePlan(planId) { it.moveExerciseDown(action.exerciseId) }

                EditPlanAction.OnAddExercise -> {
                    val plan = checkNotNull(
                        repository.workoutPlans().first().firstOrNull { it.id == planId },
                    ) { "Plan $planId is not in the repository" }
                    _effects.trySend(
                        EditPlanEffect.OpenExerciseLibrary(plan.sets.map { it.exercise.id }),
                    )
                }

                is EditPlanAction.OnExercisePicked -> {
                    val exercise = repository.requireExercise(action.exerciseId)
                    repository.updatePlan(planId) { it.addExercise(exercise) }
                }
            }
        }
    }
}
