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

    val effectiveLoad: Weight?
        get() {
            val equipment = exerciseDefinition.equipment ?: return weight
            return if (equipment.isAssisted) {
                bodyWeight?.let {
                    assistedEffectiveLoad(
                        bodyWeight = it,
                        assistance = weight,
                        blankIsUnassisted = setState is SetState.Completed,
                    )
                }
            } else {
                weight
            }
        }

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
): Weight? {
    val load = if (equipment.isAssisted) {
        bodyWeight?.let { assistedEffectiveLoad(it, loggedWeight, blankIsUnassisted) }
    } else {
        loggedWeight
    } ?: return null
    return estimatedOneRm(load, reps)
}

internal fun List<ExerciseSet>.volumeLoad(): Weight? {
    val kilograms = sumOf { set ->
        (set.effectiveLoad?.kilogramsValue ?: 0f).toDouble() * set.reps
    }.toFloat()
    return Weight.kilogramsOrNull(kilograms)
}
