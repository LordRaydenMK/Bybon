package dev.sanastasov.bybon.workout.data

import dev.sanastasov.bybon.workout.domain.ExerciseDefinition
import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.PreviousSetPerformance
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.WorkoutExercise
import dev.sanastasov.bybon.workout.domain.WorkoutPlanId
import dev.sanastasov.bybon.workout.domain.WorkoutSession
import dev.sanastasov.bybon.workout.domain.WorkoutState
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

data class WorkoutSessionWrite(
    val header: WorkoutSessionEntity,
    val exercises: List<WorkoutSessionExerciseWrite>,
)

data class WorkoutSessionExerciseWrite(
    val entity: WorkoutSessionExerciseEntity,
    val sets: List<WorkoutSetEntity>,
)

internal fun WorkoutSession.toWrite(sessionId: Long = 0): WorkoutSessionWrite {
    val durationSeconds = duration?.inWholeSeconds?.toInt()
        ?: if (state is WorkoutState.Completed) 0 else null
    val header = WorkoutSessionEntity(
        id = sessionId,
        planId = planId.id,
        planName = planName,
        note = note,
        startedAt = startedAt,
        durationSeconds = durationSeconds,
    )
    val exercises = exercises.mapIndexed { position, exercise ->
        val entity = WorkoutSessionExerciseEntity(
            sessionId = sessionId,
            exerciseId = exercise.exerciseDefinition.id,
            exerciseName = exercise.exerciseDefinition.name,
            position = position,
            repMin = exercise.repRange.first,
            repMax = exercise.repRange.last,
            restAfterWorkSetSeconds = exercise.restAfterWorkSet.inWholeSeconds.toInt(),
            notes = exercise.notes.toJsonArray(),
        )
        val warmupCount = exercise.warmupSets?.size ?: 0
        val sets = exercise.orderedSets.mapIndexed { setPosition, set ->
            require(set.reps > 0) { "reps must be > 0" }
            val previousWeight = set.previous?.weight?.toHundredths()
            val previousReps = set.previous?.reps
            require(previousReps != null || previousWeight == null) {
                "previous_weight requires previous_reps"
            }
            WorkoutSetEntity(
                sessionExerciseId = 0,
                position = setPosition,
                isWarmup = setPosition < warmupCount,
                weight = set.weight?.toHundredths(),
                reps = set.reps,
                setState = set.setState.name,
                previousWeight = previousWeight,
                previousReps = previousReps,
            )
        }
        WorkoutSessionExerciseWrite(entity, sets)
    }
    return WorkoutSessionWrite(header, exercises)
}

internal fun List<WorkoutSessionEntity>.toDomainSessions(
    lines: List<WorkoutSessionExerciseEntity>,
    sets: List<WorkoutSetEntity>,
    exercises: List<ExerciseEntity>,
): List<WorkoutSession> {
    val definitions = exercises.associate { it.id to it.toDefinition() }
    val linesBySession = lines.groupBy { it.sessionId }
    val setsByExercise = sets.groupBy { it.sessionExerciseId }
    return map { session ->
        session.toDomain(
            linesBySession[session.id].orEmpty(),
            setsByExercise,
            definitions,
        )
    }
}

internal fun WorkoutSessionEntity.toDomain(
    lines: List<WorkoutSessionExerciseEntity>,
    setsByExercise: Map<Long, List<WorkoutSetEntity>>,
    exercises: Map<String, ExerciseDefinition>,
): WorkoutSession {
    check(lines.isNotEmpty()) { "Session $id has no exercises" }
    val mapped = lines.sortedBy { it.position }.map { line ->
        val definition = checkNotNull(exercises[line.exerciseId]) {
            "Session $id references missing exercise ${line.exerciseId}"
        }
        line.toWorkoutExercise(setsByExercise[line.id].orEmpty(), definition)
    }
    return WorkoutSession(
        planId = WorkoutPlanId(planId),
        planName = planName,
        note = note,
        exercises = mapped,
        startedAt = startedAt,
        duration = durationSeconds?.seconds,
    )
}

internal fun WorkoutSessionExerciseEntity.toWorkoutExercise(
    setRows: List<WorkoutSetEntity>,
    definition: ExerciseDefinition,
): WorkoutExercise {
    require(repMin > 0 && repMax >= repMin) {
        "rep_min must be > 0 and rep_max must be >= rep_min"
    }
    val ordered = setRows.sortedBy { it.position }
    require(ordered.isNotEmpty()) { "Session exercise $id has no sets" }
    val warmupRows = ordered.filter { it.isWarmup }
    val workRows = ordered.filter { !it.isWarmup }
    require(workRows.isNotEmpty()) { "sets must contain at least one set" }
    return WorkoutExercise(
        exerciseDefinition = definition,
        repRange = repMin..repMax,
        warmupSets = warmupRows.takeIf { it.isNotEmpty() }?.map { it.toExerciseSet(definition) },
        sets = workRows.map { it.toExerciseSet(definition) },
        restAfterWorkSet = restAfterWorkSetSeconds.seconds,
        notes = notes.toStringList(),
    )
}

internal fun WorkoutSetEntity.toExerciseSet(definition: ExerciseDefinition): ExerciseSet {
    require(reps > 0) { "reps must be > 0" }
    require(previousReps != null || previousWeight == null) {
        "previous_weight and previous_reps must both be null, or previous_reps must be set"
    }
    return ExerciseSet(
        exerciseDefinition = definition,
        weight = weight.toWeightOrNull(),
        reps = reps,
        setState = SetState.valueOf(setState),
        previous = previousReps?.let { repsLogged ->
            PreviousSetPerformance(previousWeight.toWeightOrNull(), repsLogged)
        },
    )
}

internal fun Weight.toHundredths(): Int {
    val formatted = "%.2f".format(Locale.US, kilogramsValue)
    val dot = formatted.indexOf('.')
    val whole = formatted.substring(0, dot).toInt()
    val fraction = formatted.substring(dot + 1).padEnd(2, '0').take(2)
    return whole * 100 + fraction.toInt()
}

internal fun Int?.toWeightOrNull(): Weight? = this?.let { hundredths ->
    if (hundredths > 0) Weight.kilograms(hundredths / 100f) else null
}
