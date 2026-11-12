package dev.sanastasov.bybon.workout.domain

import java.time.LocalDateTime

data class PreviousSetPerformance(
    val weight: Weight?,
    val reps: Int,
    val bodyWeight: Weight? = null,
) {
    init {
        require(reps > 0) { "reps must be positive" }
    }

    fun oneRm(equipment: Equipment): Weight? = oneRmFor(
        equipment = equipment,
        loggedWeight = weight,
        reps = reps,
        bodyWeight = bodyWeight,
        blankIsUnassisted = true,
    )
}

data class ExerciseSet(
    val exerciseDefinition: ExerciseDefinition,
    val weight: Weight?,
    val reps: Int,
    val setState: SetState,
    val previous: PreviousSetPerformance? = null,
    val bodyWeight: Weight? = null,
) {
    init {
        require(reps > 0) { "reps must be positive" }
    }

    val completedAt: LocalDateTime?
        get() = (setState as? SetState.Completed)?.at

    val oneRm: Weight?
        get() = oneRmFor(weight, reps, blankIsUnassisted = setState is SetState.Completed)

    val previousOneRm: Weight?
        get() {
            val previous = previous ?: return null
            val equipment = exerciseDefinition.equipment ?: return previous.weight?.let {
                estimatedOneRm(it, previous.reps)
            }
            return previous.oneRm(equipment)
        }

    internal fun oneRmFor(
        loggedWeight: Weight?,
        reps: Int,
        blankIsUnassisted: Boolean = true,
    ): Weight? {
        val equipment = exerciseDefinition.equipment ?: return loggedWeight?.let {
            estimatedOneRm(it, reps)
        }
        return oneRmFor(equipment, loggedWeight, reps, bodyWeight, blankIsUnassisted)
    }
}

internal fun oneRmFor(
    equipment: Equipment,
    loggedWeight: Weight?,
    reps: Int,
    bodyWeight: Weight?,
    blankIsUnassisted: Boolean,
): Weight? = if (equipment.isAssisted) {
    val knownBodyWeight = bodyWeight ?: return null
    estimatedAssistedOneRm(knownBodyWeight, loggedWeight, reps, blankIsUnassisted)
} else {
    loggedWeight?.let { estimatedOneRm(it, reps) }
}

internal fun List<ExerciseSet>.volumeLoadKg(): Float = sumOf { set ->
    (set.weight?.kilogramsValue ?: 0f).toDouble() * set.reps
}.toFloat()
