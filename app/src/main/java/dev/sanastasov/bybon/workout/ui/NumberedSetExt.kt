@file:Suppress("TooManyFunctions")

package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.NumberedSet
import dev.sanastasov.bybon.workout.domain.SetState
import dev.sanastasov.bybon.workout.domain.Weight
import java.util.Locale

const val MISSING_LOAD_LABEL = "-"

val NumberedSet.oneRmLabel: String
    get() = set.oneRm.toOneRmLabel()

val NumberedSet.previousLabel: String?
    get() = set.previous?.let { previous ->
        "${previous.weight.toLoadLabel()} x ${previous.reps}"
    }

val NumberedSet.weightRepsLabel: String
    get() = formatLoadedSet(set.weight, set.reps)

val NumberedSet.setTitle: String
    get() = if (isWarmup) "Warmup set" else "Set $workSetNumber"

val NumberedSet.overviewContentDescription: String
    get() = buildString {
        append("$setTitle, ${set.weight.toContentLoadLabel()} by ${set.reps}")
        if (!isWarmup) {
            append(", $oneRmLabel")
        }
        previousLabel?.let { append(". Previous: $it") }
    }

val NumberedSet.sessionContentDescription: String
    get() = buildString {
        append(setTitle)
        if (set.setState == SetState.InProgress) {
            append(" in progress")
        }
        append(", ${set.weight.toContentLoadLabel()} by ${set.reps}")
        if (!isWarmup) {
            append(", $oneRmLabel")
        }
        previousLabel?.let { append(". Previous: $it") }
        if (set.setState == SetState.InProgress) {
            append(". Double tap to mark complete.")
        }
    }

val NumberedSet.completedContentDescription: String
    get() = buildString {
        if (isWarmup) {
            append("Warmup set, ")
            append(weightRepsLabel)
        } else {
            append("$workSetNumber. ")
            append(formatSetPerformance(set.weight, set.reps, set.oneRm))
        }
    }

fun formatOneRmKg(kg: Float): String = "%.2f".format(Locale.US, kg).trimEnd('0').trimEnd('.')

fun Weight?.toLoadLabel(): String = this?.kilograms ?: MISSING_LOAD_LABEL

fun Weight?.toContentLoadLabel(): String = this?.let { "${it.kilograms} kg" } ?: MISSING_LOAD_LABEL

fun Weight?.toOneRmColumnLabel(): String =
    this?.let { formatOneRmKg(it.kilogramsValue) } ?: MISSING_LOAD_LABEL

fun Weight?.toOneRmLabel(): String =
    this?.let { "@ ${formatOneRmKg(it.kilogramsValue)} kg 1RM" } ?: MISSING_LOAD_LABEL

fun formatLoadedSet(weight: Weight?, reps: Int): String = "${weight.toContentLoadLabel()} x $reps"

fun formatSetPerformance(weight: Weight?, reps: Int, oneRm: Weight?): String {
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "${formatLoadedSet(weight, reps)} @ $rm"
}

fun formatSetPerformance(weightKg: String, reps: Int, oneRm: Weight?): String {
    val load = if (weightKg == MISSING_LOAD_LABEL) MISSING_LOAD_LABEL else "$weightKg kg"
    val rm = oneRm?.let { "${it.kilograms} kg 1RM" } ?: MISSING_LOAD_LABEL
    return "$load x $reps @ $rm"
}
