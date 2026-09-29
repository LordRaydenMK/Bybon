package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Weight
import java.util.Locale

const val MISSING_LOAD_LABEL = "-"

fun formatOneRmKg(kg: Float): String = "%.2f".format(Locale.US, kg).trimEnd('0').trimEnd('.')

fun Weight?.toLoadLabel(): String = this?.kilograms ?: MISSING_LOAD_LABEL

fun Weight?.toContentLoadLabel(): String = this?.let { "${it.kilograms} kg" } ?: MISSING_LOAD_LABEL

fun Weight?.toOneRmColumnLabel(): String =
    this?.let { formatOneRmKg(it.kilogramsValue) } ?: MISSING_LOAD_LABEL

fun Weight?.toOneRmLabel(): String =
    this?.let { "@ ${formatOneRmKg(it.kilogramsValue)} kg 1RM" } ?: MISSING_LOAD_LABEL

fun formatLoadedSet(weight: Weight?, reps: Int): String = "${weight.toContentLoadLabel()} x $reps"

fun formatLoadedSet(weightKg: String, reps: Int): String = "${weightKg.toDisplayLoad()} x $reps"

fun formatSetPerformance(weight: Weight?, reps: Int, oneRm: Weight?): String {
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "${formatLoadedSet(weight, reps)} @ $rm"
}

fun formatSetPerformance(weightKg: String, reps: Int, oneRm: Weight?): String {
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "${formatLoadedSet(weightKg, reps)} @ $rm"
}

private fun String.toDisplayLoad(): String =
    if (this == MISSING_LOAD_LABEL) MISSING_LOAD_LABEL else "$this kg"
