package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.WorkoutsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeWorkoutsRepository(
    initialPlans: List<WorkoutPlan> = emptyList(),
    initialSessions: List<WorkoutSession> = emptyList(),
    initialExercises: List<ExerciseDefinition> = emptyList(),
    initialArchivedPlanIds: Set<WorkoutPlanId> = emptySet(),
) : WorkoutsRepository {

    private val exercises = MutableStateFlow(initialExercises)
    private val plans = MutableStateFlow(initialPlans)
    private val archivedPlanIds = MutableStateFlow(initialArchivedPlanIds)
    private val sessions = MutableStateFlow(initialSessions)

    override fun exercises(): Flow<List<ExerciseDefinition>> = exercises

    override fun workoutPlans(): Flow<List<WorkoutPlan>> = plans

    override fun archivedPlanIds(): Flow<Set<WorkoutPlanId>> = archivedPlanIds

    override suspend fun archivePlan(planId: WorkoutPlanId) {
        archivedPlanIds.update { it + planId }
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

    fun emitSessions(value: List<WorkoutSession>) {
        sessions.value = value
    }
}
