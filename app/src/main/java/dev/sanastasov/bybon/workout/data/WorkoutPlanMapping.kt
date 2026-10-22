package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import kotlin.time.Duration.Companion.seconds

internal fun WorkoutPlan.toEntity(): WorkoutPlanEntity = WorkoutPlanEntity(
    id = id.id,
    name = name,
    description = description,
    isArchived = isArchived,
)

internal fun WorkoutPlan.toExerciseEntities(): List<WorkoutPlanExerciseEntity> =
    sets.mapIndexed { position, planned ->
        WorkoutPlanExerciseEntity(
            planId = id.id,
            exerciseId = planned.exercise.id,
            position = position,
            warmupSets = planned.warmupSets,
            workSets = planned.sets,
            repMin = planned.repRange.first,
            repMax = planned.repRange.last,
            restAfterWorkSetSeconds = planned.restAfterWorkSet.inWholeSeconds.toInt(),
            notes = planned.notes.toJsonArray(),
        )
    }

internal fun List<WorkoutPlanEntity>.toDomainPlans(
    lines: List<WorkoutPlanExerciseEntity>,
    exercises: List<ExerciseEntity>,
): List<WorkoutPlan> {
    val definitions = exercises.associate { it.id to it.toDefinition() }
    val linesByPlan = lines.groupBy { it.planId }
    return mapNotNull { plan ->
        plan.toDomain(linesByPlan[plan.id].orEmpty(), definitions)
    }
}

internal fun WorkoutPlanEntity.toDomain(
    lines: List<WorkoutPlanExerciseEntity>,
    exercises: Map<String, ExerciseDefinition>,
): WorkoutPlan? {
    val planned = lines.sortedBy { it.position }.mapNotNull { line ->
        exercises[line.exerciseId]?.let { line.toPlannedExercise(it) }
    }
    if (planned.isEmpty() || planned.size != lines.size) return null
    return WorkoutPlan(
        id = WorkoutPlanId(id),
        name = name,
        description = description,
        sets = planned,
        isArchived = isArchived,
    )
}

internal fun WorkoutPlanExerciseEntity.toPlannedExercise(
    exercise: ExerciseDefinition,
): PlanedExercise {
    require(warmupSets >= 0) { "warmup_sets must be >= 0" }
    require(workSets >= 1) { "work_sets must be >= 1" }
    require(repMin > 0 && repMax >= repMin) {
        "rep_min must be > 0 and rep_max must be >= rep_min"
    }
    return PlanedExercise(
        exercise = exercise,
        warmupSets = warmupSets,
        sets = workSets,
        repRange = repMin..repMax,
        restAfterWorkSet = restAfterWorkSetSeconds.seconds,
        notes = notes.toStringList(),
    )
}
