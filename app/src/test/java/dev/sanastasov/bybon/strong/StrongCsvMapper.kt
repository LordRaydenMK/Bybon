package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import dev.sanastasov.bybon.workout.domain.exercises
import dev.sanastasov.bybon.workout.domain.fullBodyA
import dev.sanastasov.bybon.workout.domain.fullBodyB
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.seconds

private val strongDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/** Strong display names that do not case-insensitively match a Bybon catalog name. */
private val strongExerciseAliases = mapOf(
    "romanian deadlift (barbell)" to "rdl-bb",
    "bulgarian split squat" to "split-squat-db",
    "bicep curl (machine)" to "biceps-curl-machine",
    "seated leg curl (machine)" to "leg-curl",
    "triceps press" to "triceps-press-machine",
)

fun List<StrongCsvRow>.toWorkoutSessions(
    plans: List<WorkoutPlan> = listOf(fullBodyA, fullBodyB),
    exerciseCatalog: List<ExerciseDefinition> = exercises,
): List<WorkoutSession> {
    val plansByName = plans.associateBy { it.name.trim().lowercase() }
    val exercisesByNormalizedName = exerciseCatalog.associateBy { it.name.normalizedExerciseName() }
    val exercisesById = exerciseCatalog.associateBy { it.id }

    return groupBy { it.workoutNumber }
        .toSortedMap()
        .map { (_, workoutRows) ->
            workoutRows.toWorkoutSession(plansByName, exercisesByNormalizedName, exercisesById)
        }
}

private fun List<StrongCsvRow>.toWorkoutSession(
    plansByName: Map<String, WorkoutPlan>,
    exercisesByNormalizedName: Map<String, ExerciseDefinition>,
    exercisesById: Map<String, ExerciseDefinition>,
): WorkoutSession {
    val first = first()
    val planName = first.workoutName.trim()
    val matchedPlan = plansByName[planName.lowercase()]
    val planId = matchedPlan?.id ?: WorkoutPlanId(planName.slugify())

    val exercises = groupByExerciseOrder().map { exerciseRows ->
        exerciseRows.toWorkoutExercise(exercisesByNormalizedName, exercisesById)
    }

    return WorkoutSession(
        planId = planId,
        planName = matchedPlan?.name ?: planName,
        planDescription = first.workoutNotes ?: matchedPlan?.description,
        exercises = exercises,
        state = WorkoutState.Completed(
            startedAt = LocalDateTime.parse(first.date, strongDateTime),
            duration = first.durationSec.seconds,
        ),
    )
}

private fun List<StrongCsvRow>.groupByExerciseOrder(): List<List<StrongCsvRow>> {
    val blocks = mutableListOf<MutableList<StrongCsvRow>>()
    for (row in this) {
        val current = blocks.lastOrNull()
        if (current == null || current.first().exerciseName != row.exerciseName) {
            blocks += mutableListOf(row)
        } else {
            current += row
        }
    }
    return blocks
}

private fun List<StrongCsvRow>.toWorkoutExercise(
    exercisesByNormalizedName: Map<String, ExerciseDefinition>,
    exercisesById: Map<String, ExerciseDefinition>,
): WorkoutExercise {
    val strongName = first().exerciseName
    val definition = resolveExercise(strongName, exercisesByNormalizedName, exercisesById)
    val workingRows = filter { it.setOrder.toIntOrNull() != null }
    val reps = workingRows.mapNotNull { it.reps }
    val repRange = if (reps.isEmpty()) 0..0 else reps.min()..reps.max()

    return WorkoutExercise(
        exerciseDefinition = definition,
        repRange = repRange,
        sets = workingRows.map { row ->
            ExerciseSet(
                exerciseDefinition = definition,
                weight = Weight.kilograms((row.weightKg ?: 0.0).toFloat()),
                reps = row.reps ?: 0,
                setState = SetState.Completed,
            )
        },
    )
}

private fun resolveExercise(
    strongName: String,
    exercisesByNormalizedName: Map<String, ExerciseDefinition>,
    exercisesById: Map<String, ExerciseDefinition>,
): ExerciseDefinition {
    val normalized = strongName.normalizedExerciseName()
    strongExerciseAliases[normalized]?.let { id -> exercisesById[id] }?.let { return it }
    exercisesByNormalizedName[normalized]?.let { return it }

    return ExerciseDefinition(
        id = strongName.slugify(),
        name = strongName,
        primaryMuscleGroup = MuscleGroup.Other,
        equipment = inferEquipment(strongName),
    )
}

private fun inferEquipment(name: String): Equipment {
    val lower = name.lowercase()
    return when {
        "barbell" in lower -> Equipment.Barbell
        "dumbbell" in lower -> Equipment.Dumbbell
        "assisted" in lower -> Equipment.AssistedBodyWeight
        "machine" in lower || "cable" in lower -> Equipment.Machine
        else -> Equipment.Bodyweight
    }
}

private fun String.normalizedExerciseName(): String =
    lowercase()
        .replace("(rdl)", "")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun String.slugify(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
