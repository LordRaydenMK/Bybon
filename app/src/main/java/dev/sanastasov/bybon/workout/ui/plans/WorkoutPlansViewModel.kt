package dev.sanastasov.bybon.workout.ui.plans

import dev.sanastasov.bybon.ui.stateInWhileInForeground
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class WorkoutPlansViewModel(
    private val repository: WorkoutsRepository,
    private val coroutineScope: CoroutineScope,
) {

    private val _effects = Channel<WorkoutPlanEffect>(Channel.BUFFERED)
    val effects: Flow<WorkoutPlanEffect> = _effects.receiveAsFlow()

    private val showArchived = MutableStateFlow(false)

    private val hasArchivedPlans = combine(
        repository.workoutPlans(),
        repository.workoutPlans(WorkoutPlansFilter.AllPlans),
    ) { activePlans, allPlans -> allPlans.size > activePlans.size }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = showArchived
        .map { includeArchived ->
            if (includeArchived) {
                WorkoutPlansFilter.AllPlans
            } else {
                WorkoutPlansFilter.ActivePlans
            }
        }
        .flatMapLatest { filter ->
            combine(
                repository.workoutPlans(filter),
                repository.workoutSessions(),
                hasArchivedPlans,
            ) { allPlans, sessions, hasArchived ->
                WorkoutPlansUiState(
                    plans = allPlans.map { it.toUi(sessions) },
                    showArchived = filter == WorkoutPlansFilter.AllPlans,
                    hasArchivedPlans = hasArchived,
                )
            }
        }
        .stateInWhileInForeground(coroutineScope, WorkoutPlansUiState())

    fun onAction(action: WorkoutPlansAction) {
        when (action) {
            is WorkoutPlansAction.OnStartPlan -> {
                val isResume = uiState.value.activePlans.any {
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
