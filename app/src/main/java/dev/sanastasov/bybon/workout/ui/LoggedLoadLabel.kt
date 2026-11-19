package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.ExerciseSet
import dev.sanastasov.bybon.workout.domain.isAssisted

fun ExerciseSet.loggedLoadLabel(): String {
    val equipment = exerciseDefinition.equipment
    return weight?.toLoadLabel(equipment)
        ?: if (equipment?.isAssisted == true) "0" else MISSING_LOAD_LABEL
}

fun ExerciseSet.contentLoadLabel(): String {
    val label = loggedLoadLabel()
    return if (label == MISSING_LOAD_LABEL) MISSING_LOAD_LABEL else "$label kg"
}
