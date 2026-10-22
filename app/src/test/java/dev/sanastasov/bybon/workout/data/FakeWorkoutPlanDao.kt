package dev.sanastasov.bybon.workout.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWorkoutPlanDao : WorkoutPlanDao {

    private val plans = MutableStateFlow<List<WorkoutPlanEntity>>(emptyList())
    private val exercises = MutableStateFlow<List<WorkoutPlanExerciseEntity>>(emptyList())
    private var nextExerciseId = 1L

    override fun observePlans(): Flow<List<WorkoutPlanEntity>> = plans

    override fun observePlanExercises(): Flow<List<WorkoutPlanExerciseEntity>> = exercises

    override suspend fun ids(): List<String> = plans.value.map { it.id }

    override suspend fun planById(planId: String): WorkoutPlanEntity? =
        plans.value.firstOrNull { it.id == planId }

    override suspend fun exercisesForPlan(planId: String): List<WorkoutPlanExerciseEntity> =
        exercises.value.filter { it.planId == planId }.sortedBy { it.position }

    override suspend fun setArchived(planId: String, archived: Boolean) {
        val current = plans.value
        val index = current.indexOfFirst { it.id == planId }
        if (index < 0) return
        plans.value = current.toMutableList().apply {
            set(index, current[index].copy(isArchived = archived))
        }
    }

    override suspend fun insertPlans(plans: List<WorkoutPlanEntity>) {
        if (plans.isEmpty()) return
        val existing = this.plans.value.map { it.id }.toSet()
        val added = plans.filter { it.id !in existing }
        if (added.isEmpty()) return
        this.plans.value = this.plans.value + added
    }

    override suspend fun insertPlanExercises(exercises: List<WorkoutPlanExerciseEntity>) {
        if (exercises.isEmpty()) return
        this.exercises.value = this.exercises.value + exercises.map { entity ->
            if (entity.id != 0L) {
                entity
            } else {
                entity.copy(id = nextExerciseId++)
            }
        }
    }

    override suspend fun upsertPlan(plan: WorkoutPlanEntity) {
        val current = plans.value.toMutableList()
        val index = current.indexOfFirst { it.id == plan.id }
        if (index >= 0) {
            current[index] = plan
        } else {
            current += plan
        }
        plans.value = current
    }

    override suspend fun deleteExercisesForPlan(planId: String) {
        exercises.value = exercises.value.filterNot { it.planId == planId }
    }
}
