package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WorkoutPlansViewModel(
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<WorkoutPlanEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutPlanEffect> = _effects.receiveAsFlow()

    private val showArchived = MutableStateFlow(false)

    val uiState = combine(
        repository.workoutPlans(WorkoutPlansFilter.AllPlans),
        repository.workoutSessions(),
        showArchived,
    ) { plans, sessions, includeArchived ->
        WorkoutPlansUiState(
            plans = plans.map { it.toUi(sessions) },
            showArchived = includeArchived,
        )
    }.stateInWhileInForeground(coroutineScope, WorkoutPlansUiState())

    fun onAction(action: WorkoutPlansAction) {
        when (action) {
            is WorkoutPlansAction.OnStartPlan -> {
                val isResume = uiState.value.unarchivedPlans.any {
                    it.plan.id == action.plan.id && it.isActive
                }
                val effect = if (isResume) {
                    WorkoutPlanEffect.OpenSession(action.plan)
                } else {
                    WorkoutPlanEffect.OpenOverview(action.plan)
                }
                _effects.trySend(effect)
            }

            is WorkoutPlansAction.OnEditPlan -> {
                _effects.trySend(WorkoutPlanEffect.OpenEditPlan(action.plan))
            }

            is WorkoutPlansAction.OnArchivePlan -> coroutineScope.launch {
                repository.archivePlan(action.plan.id, archived = true)
            }

            is WorkoutPlansAction.OnUnarchivePlan -> coroutineScope.launch {
                repository.archivePlan(action.plan.id, archived = false)
            }

            WorkoutPlansAction.OnShowArchivedPlans -> {
                showArchived.value = true
            }

            WorkoutPlansAction.OnHideArchivedPlans -> {
                showArchived.value = false
            }
        }
    }
}
