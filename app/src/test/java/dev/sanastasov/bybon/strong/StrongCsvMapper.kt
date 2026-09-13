package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.PlanedExercise
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
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val strongDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private val planNameFillers = setOf("workout", "session", "training", "routine", "day")

/** Strong display names that do not case-insensitively match a Bybon catalog name. */
private val strongExerciseAliases = mapOf(
    "romanian deadlift (barbell)" to "rdl-bb",
    "bulgarian split squat" to "split-squat-db",
    "bicep curl (machine)" to "biceps-curl-machine",
    "seated leg curl (machine)" to "leg-curl",
    "triceps press" to "triceps-press-machine",
)

data class StrongImportResult(
    val sessionHistory: List<WorkoutSession>,
    val exercises: List<ExerciseDefinition>,
    val plans: List<WorkoutPlan>,
)

fun List<StrongCsvRow>.toWorkoutSessions(
    plans: List<WorkoutPlan> = listOf(fullBodyA, fullBodyB),
    exerciseCatalog: List<ExerciseDefinition> = exercises,
): List<WorkoutSession> = toStrongImport(plans, exerciseCatalog).sessionHistory

fun List<StrongCsvRow>.toStrongImport(
    plans: List<WorkoutPlan> = listOf(fullBodyA, fullBodyB),
    exerciseCatalog: List<ExerciseDefinition> = exercises,
): StrongImportResult {
    val catalogByNormalizedName = exerciseCatalog.associateBy { it.name.normalizedExerciseName() }
    val catalogById = exerciseCatalog.associateBy { it.id }

    val parsedWorkouts = groupBy { it.workoutNumber }
        .toSortedMap()
        .map { (_, workoutRows) ->
            workoutRows.toParsedWorkout(catalogByNormalizedName, catalogById)
        }

    val catalogIds = catalogById.keys
    val exercisesToImport = parsedWorkouts
        .asSequence()
        .flatMap { it.exercises }
        .map { it.exerciseDefinition }
        .filter { it.id !in catalogIds }
        .distinctBy { it.id }
        .toList()

    val resolvedPlans = linkedMapOf<String, WorkoutPlan>()
    val plansToImport = mutableListOf<WorkoutPlan>()
    parsedWorkouts
        .groupBy { it.planName }
        .forEach { (planName, workoutsForPlan) ->
            val representative = workoutsForPlan.representative()
            val matched = plans.findMatchingPlan(planName, representative.exerciseIds)
            if (matched != null) {
                resolvedPlans[planName] = matched
            } else {
                val imported = representative.toWorkoutPlan()
                resolvedPlans[planName] = imported
                plansToImport += imported
            }
        }

    val sessions = parsedWorkouts.map { parsed ->
        val plan = resolvedPlans.getValue(parsed.planName)
        parsed.toWorkoutSession(plan)
    }

    return StrongImportResult(
        sessionHistory = sessions,
        exercises = exercisesToImport,
        plans = plansToImport,
    )
}

private data class ParsedWorkout(
    val workoutNumber: Int,
    val planName: String,
    val startedAt: LocalDateTime,
    val duration: Duration,
    val workoutNotes: String?,
    val exercises: List<WorkoutExercise>,
) {
    val exerciseIds: List<String> = exercises.map { it.id }
}

private fun List<ParsedWorkout>.representative(): ParsedWorkout {
    val sequenceCounts = groupingBy { it.exerciseIds }.eachCount()
    val bestSequence = sequenceCounts.maxBy { it.value }.key
    return first { it.exerciseIds == bestSequence }
}

private fun List<StrongCsvRow>.toParsedWorkout(
    exercisesByNormalizedName: Map<String, ExerciseDefinition>,
    exercisesById: Map<String, ExerciseDefinition>,
): ParsedWorkout {
    val first = first()
    return ParsedWorkout(
        workoutNumber = first.workoutNumber,
        planName = first.workoutName.trim(),
        startedAt = LocalDateTime.parse(first.date, strongDateTime),
        duration = first.durationSec.seconds,
        workoutNotes = first.workoutNotes,
        exercises = groupByExerciseOrder().map { exerciseRows ->
            exerciseRows.toWorkoutExercise(exercisesByNormalizedName, exercisesById)
        },
    )
}

private fun ParsedWorkout.toWorkoutSession(plan: WorkoutPlan): WorkoutSession =
    WorkoutSession(
        planId = plan.id,
        planName = plan.name,
        planDescription = workoutNotes ?: plan.description,
        exercises = exercises,
        state = WorkoutState.Completed(
            startedAt = startedAt,
            duration = duration,
        ),
    )

private fun ParsedWorkout.toWorkoutPlan(): WorkoutPlan =
    WorkoutPlan(
        id = WorkoutPlanId(planName.slugify()),
        name = planName,
        description = null,
        sets = exercises.map { exercise ->
            PlanedExercise(
                exercise = exercise.exerciseDefinition,
                sets = exercise.sets.size,
                repRange = exercise.repRange,
            )
        },
    )

private fun List<WorkoutPlan>.findMatchingPlan(
    strongName: String,
    strongExerciseIds: List<String>,
): WorkoutPlan? {
    val scored = map { plan ->
        val nameScore = nameSimilarity(strongName, plan.name)
        val exerciseScore = exerciseSimilarity(strongExerciseIds, plan.sets.map { it.exercise.id })
        plan to combinedPlanScore(nameScore, exerciseScore)
    }
    return scored.maxByOrNull { it.second }
        ?.takeIf { it.second >= PLAN_MATCH_THRESHOLD }
        ?.first
}

private const val PLAN_MATCH_THRESHOLD = 0.70

private fun combinedPlanScore(nameScore: Double, exerciseScore: Double): Double =
    0.55 * nameScore + 0.45 * exerciseScore

private fun nameSimilarity(left: String, right: String): Double {
    val a = left.normalizedPlanName()
    val b = right.normalizedPlanName()
    if (a.isEmpty() && b.isEmpty()) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    if (a == b) return 1.0
    return 1.0 - levenshtein(a, b).toDouble() / max(a.length, b.length)
}

private fun exerciseSimilarity(left: List<String>, right: List<String>): Double {
    if (left.isEmpty() && right.isEmpty()) return 1.0
    if (left.isEmpty() || right.isEmpty()) return 0.0
    val leftSet = left.toSet()
    val rightSet = right.toSet()
    val jaccard = leftSet.intersect(rightSet).size.toDouble() / leftSet.union(rightSet).size
    val order = longestCommonSubsequenceLength(left, right).toDouble() / max(left.size, right.size)
    return 0.8 * jaccard + 0.2 * order
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
        primaryMuscleGroup = inferMuscleGroup(strongName),
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

private fun inferMuscleGroup(name: String): MuscleGroup {
    val lower = name.lowercase()
    return when {
        "crunch" in lower || "plank" in lower || "sit-up" in lower || "sit up" in lower -> MuscleGroup.Core
        else -> MuscleGroup.Other
    }
}

private fun String.normalizedExerciseName(): String =
    lowercase()
        .replace("(rdl)", "")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun String.normalizedPlanName(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), " ")
        .split(" ")
        .filter { it.isNotEmpty() && it !in planNameFillers }
        .joinToString(" ")

private fun String.slugify(): String =
    lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')

private fun levenshtein(left: String, right: String): Int {
    val previous = IntArray(right.length + 1) { it }
    val current = IntArray(right.length + 1)
    for (i in left.indices) {
        current[0] = i + 1
        for (j in right.indices) {
            val substitution = previous[j] + if (left[i] == right[j]) 0 else 1
            current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, substitution)
        }
        for (j in previous.indices) previous[j] = current[j]
    }
    return previous[right.length]
}

private fun longestCommonSubsequenceLength(left: List<String>, right: List<String>): Int {
    val dp = Array(left.size + 1) { IntArray(right.size + 1) }
    for (i in left.indices) {
        for (j in right.indices) {
            dp[i + 1][j + 1] = if (left[i] == right[j]) {
                dp[i][j] + 1
            } else {
                max(dp[i + 1][j], dp[i][j + 1])
            }
        }
    }
    return dp[left.size][right.size]
}
