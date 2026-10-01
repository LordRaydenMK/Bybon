@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.strong

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.Mechanic
import dev.sanastasov.bybon.workout.domain.MuscleGroup
import dev.sanastasov.bybon.workout.domain.PlanedExercise
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlan
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.defaultRest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val strongDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

private val planNameFillers = setOf("workout", "session", "training", "routine", "day")

/**
 * Strong display name → catalog id, when the name is that exercise.
 * Mechanic is the RepDB value, used only when this catalog does not contain the id.
 */
private data class StrongExercise(
    val catalogId: String? = null,
    val mechanic: Mechanic,
    val muscleGroup: MuscleGroup? = null,
    val equipment: Equipment? = null,
)

private val strongExercises = mapOf(
    "bench press (barbell)" to StrongExercise("bench-press", Mechanic.Compound),
    "squat (barbell)" to StrongExercise("squat", Mechanic.Compound),
    "squat (machine)" to StrongExercise("squat-machine", Mechanic.Compound),
    "pull up (assisted)" to StrongExercise("assisted-pull-ups", Mechanic.Compound),
    "incline bench press (dumbbell)" to StrongExercise("incline-db-press", Mechanic.Compound),
    "incline row (dumbbell)" to StrongExercise("chest-supported-db-row", Mechanic.Compound),
    "incline curl (dumbbell)" to StrongExercise("incline-db-curl", Mechanic.Isolation),
    "lateral raise (dumbbell)" to StrongExercise("lateral-raise", Mechanic.Isolation),
    "lateral raise (machine)" to StrongExercise("lateral-raise-machine", Mechanic.Isolation),
    "skullcrusher (dumbbell)" to StrongExercise("db-skull-crusher", Mechanic.Isolation),
    "skullcrusher (barbell)" to StrongExercise("skull-crusher", Mechanic.Isolation),
    "upright row (dumbbell)" to StrongExercise("dumbbell-upright-row", Mechanic.Compound),
    "leg extension (machine)" to StrongExercise("leg-extension", Mechanic.Isolation),
    "romanian deadlift (barbell)" to StrongExercise("romanian-deadlift", Mechanic.Compound),
    "bulgarian split squat" to StrongExercise("bulgarian-split-squat", Mechanic.Compound),
    "bicep curl (machine)" to StrongExercise("machine-bicep-curl", Mechanic.Isolation),
    "seated leg curl (machine)" to StrongExercise("seated-leg-curl", Mechanic.Isolation),
    "lying leg curl (machine)" to StrongExercise("leg-curl", Mechanic.Isolation),
    "triceps press" to StrongExercise("triceps-press-machine", Mechanic.Isolation),
    "leg press" to StrongExercise("leg-press", Mechanic.Compound),
    "dumbbell lateral raises" to StrongExercise("lateral-raise", Mechanic.Isolation),
    "chest fly" to StrongExercise("pec-deck", Mechanic.Isolation),
    "chest fly (cable)" to StrongExercise("cable-fly", Mechanic.Isolation),
    "chest fly (band)" to StrongExercise(
        mechanic = Mechanic.Isolation,
        muscleGroup = MuscleGroup.Chest,
        equipment = Equipment.Bodyweight,
    ),
    "lat pulldown (cable)" to StrongExercise("lat-pulldown", Mechanic.Compound),
    "reverse lunges" to StrongExercise("reverse-lunge", Mechanic.Compound),
    "crunch (machine)" to StrongExercise("machine-seated-crunch", Mechanic.Isolation),
    "standing calf raise (barbell)" to StrongExercise("barbell-calf-raise", Mechanic.Isolation),
    "hip thrust (barbell)" to StrongExercise("hip-thrust", Mechanic.Compound),
    "cable pushdown (rope)" to StrongExercise(
        mechanic = Mechanic.Isolation,
        muscleGroup = MuscleGroup.Arms,
        equipment = Equipment.Machine,
    ),
    "triceps extension (cable)" to StrongExercise(
        mechanic = Mechanic.Isolation,
        muscleGroup = MuscleGroup.Arms,
        equipment = Equipment.Machine,
    ),
    "iso-lateral chest press" to StrongExercise(
        mechanic = Mechanic.Compound,
        muscleGroup = MuscleGroup.Chest,
        equipment = Equipment.Machine,
    ),
    "back extension" to StrongExercise("back-extension", Mechanic.Isolation),
)

data class StrongImportResult(
    val sessionHistory: List<WorkoutSession>,
    val exercises: List<ExerciseDefinition>,
    val plans: List<WorkoutPlan>,
)

fun List<StrongCsvRow>.toWorkoutSessions(
    plans: List<WorkoutPlan>,
    exerciseCatalog: List<ExerciseDefinition>,
): List<WorkoutSession> = toStrongImport(plans, exerciseCatalog).sessionHistory

fun List<StrongCsvRow>.toStrongImport(
    plans: List<WorkoutPlan>,
    exerciseCatalog: List<ExerciseDefinition>,
): StrongImportResult {
    val catalogByNormalizedName = exerciseCatalog.associateBy { it.name.normalizedExerciseName() }
    val catalogById = exerciseCatalog.associateBy { it.id }

    val parsedWorkouts = groupBy { it.workoutNumber }
        .toSortedMap()
        .map { (_, workoutRows) ->
            workoutRows.toParsedWorkout(catalogByNormalizedName, catalogById)
        }
        .filter { it.exercises.isNotEmpty() }

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
                val imported = representative.toWorkoutPlan(
                    description = workoutsForPlan.templateNote(),
                    isArchived = workoutsForPlan.size < PLAN_ARCHIVE_BELOW_EXECUTIONS,
                )
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
        exercises = groupByExerciseOrder().mapNotNull { exerciseRows ->
            exerciseRows.toWorkoutExercise(exercisesByNormalizedName, exercisesById)
        },
    )
}

private fun ParsedWorkout.toWorkoutSession(plan: WorkoutPlan): WorkoutSession = WorkoutSession(
    planId = plan.id,
    planName = plan.name,
    note = workoutNotes,
    exercises = exercises,
    startedAt = startedAt,
    duration = duration,
)

private fun List<ParsedWorkout>.templateNote(): String? {
    val notes = mapNotNull { it.workoutNotes?.trim()?.takeIf { note -> note.isNotEmpty() } }
    if (notes.isEmpty()) return null
    val counts = notes.groupingBy { it }.eachCount()
    val maxCount = counts.maxOf { it.value }
    return notes.last { counts.getValue(it) == maxCount }
}

private fun ParsedWorkout.toWorkoutPlan(description: String?, isArchived: Boolean): WorkoutPlan =
    WorkoutPlan(
        id = WorkoutPlanId(planName.slugify()),
        name = planName,
        description = description,
        sets = exercises.map { exercise ->
            PlanedExercise(
                exercise = exercise.exerciseDefinition,
                warmupSets = exercise.warmupSets?.size ?: 0,
                sets = exercise.sets.size,
                repRange = exercise.repRange,
                restAfterWorkSet = exercise.restAfterWorkSet,
                notes = exercise.notes,
            )
        },
        isArchived = isArchived,
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

private const val PLAN_ARCHIVE_BELOW_EXECUTIONS = 5

private fun combinedPlanScore(nameScore: Double, exerciseScore: Double): Double =
    0.55 * nameScore + 0.45 * exerciseScore

@Suppress("ReturnCount")
private fun nameSimilarity(left: String, right: String): Double {
    val a = left.normalizedPlanName()
    val b = right.normalizedPlanName()
    if (a.isEmpty() && b.isEmpty()) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    if (a == b) return 1.0
    return 1.0 - levenshtein(a, b).toDouble() / max(a.length, b.length)
}

@Suppress("ReturnCount")
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
): WorkoutExercise? {
    val definition = resolveExercise(first().exerciseName, exercisesByNormalizedName, exercisesById)
    return toResolvedWorkoutExercise(definition)
}

private fun List<StrongCsvRow>.toResolvedWorkoutExercise(
    definition: ExerciseDefinition,
): WorkoutExercise? {
    val warmupRows = filter { it.setOrder.equals("W", ignoreCase = true) }
    val workingRows = filter { it.setOrder.toIntOrNull() != null }
    val workSets = workingRows.mapNotNull { it.toCompletedSet(definition) }
    if (workSets.isEmpty()) return null
    val reps = workSets.map { it.reps }
    val repRange = reps.min()..reps.max()

    val restAfterWorkSet = filter { it.setOrder.equals("Rest Timer", ignoreCase = true) }
        .mapNotNull { it.seconds }
        .firstOrNull()
        ?.toLong()
        ?.seconds
        ?: definition.defaultRest

    return WorkoutExercise(
        exerciseDefinition = definition,
        repRange = repRange,
        warmupSets = warmupRows
            .mapNotNull { it.toCompletedSet(definition) }
            .takeIf { it.isNotEmpty() },
        sets = workSets,
        restAfterWorkSet = restAfterWorkSet,
        notes = exerciseNotes(),
    )
}

private fun List<StrongCsvRow>.exerciseNotes(): List<String> =
    filter { it.setOrder.equals("Note", ignoreCase = true) }
        .mapNotNull { it.notes?.trim()?.takeIf { note -> note.isNotEmpty() } }

private fun StrongCsvRow.toCompletedSet(definition: ExerciseDefinition): ExerciseSet? {
    val reps = reps?.takeIf { it > 0 } ?: return null
    val weight = weightKg?.toFloat()?.let { Weight.kilogramsOrNull(it) }
    return ExerciseSet(
        exerciseDefinition = definition,
        weight = weight,
        reps = reps,
        setState = SetState.Completed,
    )
}

@Suppress("ReturnCount")
private fun resolveExercise(
    strongName: String,
    exercisesByNormalizedName: Map<String, ExerciseDefinition>,
    exercisesById: Map<String, ExerciseDefinition>,
): ExerciseDefinition {
    val normalized = strongName.normalizedExerciseName()
    val mapped = strongExercises[normalized]
    mapped?.catalogId?.let { exercisesById[it] }?.let { return it }
    exercisesByNormalizedName[normalized]?.let { return it }

    val trimmedName = strongName.trim()
    return ExerciseDefinition(
        id = trimmedName.slugify(),
        name = trimmedName,
        primaryMuscleGroup = mapped?.muscleGroup ?: inferMuscleGroup(trimmedName),
        equipment = mapped?.equipment ?: inferEquipment(trimmedName),
        mechanic = mapped?.mechanic,
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

private fun String.normalizedExerciseName(): String = lowercase()
    .replace("(rdl)", "")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun String.normalizedPlanName(): String = lowercase()
    .replace(Regex("[^a-z0-9]+"), " ")
    .split(" ")
    .filter { it.isNotEmpty() && it !in planNameFillers }
    .joinToString(" ")

private fun String.slugify(): String = lowercase()
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
