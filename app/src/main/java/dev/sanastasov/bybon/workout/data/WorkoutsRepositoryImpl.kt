package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutPlansFilter
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import dev.sanastasov.bybon.workout.domain.catalogExercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import dev.sanastasov.bybon.workout.domain.upperBodyA
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class WorkoutsRepositoryImpl : WorkoutsRepository {

    private val exercises = MutableStateFlow(catalogExercises)
    private val plans = MutableStateFlow(listOf(fullBodyA, fullBodyB, upperBodyA))
    private val sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())

    override fun exercises(): Flow<List<ExerciseDefinition>> = exercises

    override fun workoutPlans(filter: WorkoutPlansFilter): Flow<List<WorkoutPlan>> =
        plans.map { allPlans ->
            when (filter) {
                WorkoutPlansFilter.UnarchivedPlans -> allPlans.filter { !it.isArchived }
                WorkoutPlansFilter.AllPlans -> allPlans
            }
        }

    override suspend fun archivePlan(planId: WorkoutPlanId, archived: Boolean) {
        plans.update { list ->
            list.map { plan ->
                if (plan.id == planId) plan.copy(isArchived = archived) else plan
            }
        }
    }

    override suspend fun updatePlan(
        planId: WorkoutPlanId,
        transform: (WorkoutPlan) -> WorkoutPlan,
    ) {
        plans.update { list ->
            list.map { plan ->
                if (plan.id == planId) transform(plan) else plan
            }
        }
    }

    override suspend fun updateWorkout(session: WorkoutSession) {
        sessions.update { list ->
            val index = list.indexOfFirst {
                it.planId == session.planId && it.state !is WorkoutState.Completed
            }
            if (index >= 0) {
                list.toMutableList().apply { set(index, session) }
            } else {
                list + session
            }
        }
    }

    override fun workoutSessions(): Flow<List<WorkoutSession>> = sessions

    override suspend fun importHistory(
        plans: List<WorkoutPlan>,
        sessions: List<WorkoutSession>,
        exercises: List<ExerciseDefinition>,
    ) {
        this.exercises.update { it + exercises }
        this.plans.update { it + plans }
        this.sessions.update { it + sessions }
    }
}
