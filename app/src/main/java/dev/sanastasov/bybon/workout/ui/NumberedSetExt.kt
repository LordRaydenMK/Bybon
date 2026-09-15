package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.NumberedSet
import dev.sanastasov.bybon.workout.domain.SetState
import java.util.Locale

val NumberedSet.oneRmLabel: String
    get() = "@ ${formatOneRmKg(set.oneRm.kilogramsValue)} kg 1RM"

val NumberedSet.previousLabel: String?
    get() = set.previous?.let { previous ->
        "${previous.weight.kilograms} x ${previous.reps}"
    }

val NumberedSet.weightRepsLabel: String
    get() = "${set.weight.kilograms} kg x ${set.reps}"

val NumberedSet.setTitle: String
    get() = if (isWarmup) "Warmup set" else "Set $workSetNumber"

val NumberedSet.overviewContentDescription: String
    get() = buildString {
        append("$setTitle, ${set.weight.kilograms} kg by ${set.reps}")
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
        append(", ${set.weight.kilograms} kg by ${set.reps}")
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
            append(weightRepsLabel)
            append(" $oneRmLabel")
        }
    }

val NumberedSet.uncompleteContentDescription: String
    get() = "$completedContentDescription. Double tap to undo complete."

fun formatOneRmKg(kg: Float): String = "%.2f".format(Locale.US, kg).trimEnd('0').trimEnd('.')
