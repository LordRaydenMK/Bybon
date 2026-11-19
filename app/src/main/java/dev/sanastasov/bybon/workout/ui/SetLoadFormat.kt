package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Equipment
import dev.sanastasov.bybon.workout.domain.OneRmChange
import dev.sanastasov.bybon.workout.domain.Weight
import dev.sanastasov.bybon.workout.domain.isAssisted
import dev.sanastasov.bybon.workout.domain.oneRmChange
import java.util.*

const val MISSING_LOAD_LABEL = "-"

fun formatOneRmKg(kg: Float): String = "%.2f".format(Locale.US, kg).trimEnd('0').trimEnd('.')

fun Weight.toLoadLabel(equipment: Equipment? = null): String =
    if (equipment?.isAssisted == true) "-$kilograms" else kilograms

fun Weight.toContentLoadLabel(equipment: Equipment? = null): String = "${toLoadLabel(equipment)} kg"

fun Weight?.toOneRmColumnLabel(): String =
    this?.let { formatOneRmKg(it.kilogramsValue) } ?: MISSING_LOAD_LABEL

fun Weight?.toOneRmLabel(): String =
    this?.let { "@ ${formatOneRmKg(it.kilogramsValue)} kg 1RM" } ?: MISSING_LOAD_LABEL

fun formatOneRmDelta(current: Weight?, previous: Weight?): String {
    val currentWeight = current ?: return MISSING_LOAD_LABEL
    return when (val change = oneRmChange(currentWeight, previous)) {
        is OneRmChange.Improved -> "+${change.delta.kilograms}kg"
        is OneRmChange.Decreased -> "-${change.delta.kilograms}kg"
        OneRmChange.Unchanged, null -> MISSING_LOAD_LABEL
    }
}

fun formatLoadedSet(weight: Weight?, reps: Int, equipment: Equipment? = null): String {
    val load = weight?.toContentLoadLabel(equipment)
        ?: if (equipment?.isAssisted == true) "0 kg" else MISSING_LOAD_LABEL
    return "$load x $reps"
}

fun formatLoadedSet(weightKg: String, reps: Int): String = "${weightKg.toDisplayLoad()} x $reps"

fun formatSetPerformance(
    weight: Weight?,
    reps: Int,
    oneRm: Weight?,
    equipment: Equipment? = null,
): String {
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "${formatLoadedSet(weight, reps, equipment)} @ $rm"
}

fun formatSetPerformance(weightKg: String, reps: Int, oneRm: Weight?): String {
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "${formatLoadedSet(weightKg, reps)} @ $rm"
}

private fun String.toDisplayLoad(): String =
    if (this == MISSING_LOAD_LABEL) MISSING_LOAD_LABEL else "$this kg"
