package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.NumberedSet
import dev.sanastasov.bybon.workout.domain.SetState

val NumberedSet.oneRmLabel: String
    get() = set.oneRm.toOneRmLabel()

val NumberedSet.previousLabel: String?
    get() = set.previous?.let { previous ->
        "${previous.weight.toLoadLabel()} x ${previous.reps}"
    }

val NumberedSet.weightRepsLabel: String
    get() = formatLoadedSet(set.weight, set.reps)

val NumberedSet.notificationText: String
    get() = buildString {
        if (isWarmup) append("[w] ")
        append("${set.exerciseDefinition.name} $weightRepsLabel")
    }

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
    }

val NumberedSet.completeSetContentDescription: String
    get() = "Mark $setTitle complete"

val NumberedSet.weightFieldContentDescription: String
    get() = "$setTitle weight"

val NumberedSet.repsFieldContentDescription: String
    get() = "$setTitle reps"

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
