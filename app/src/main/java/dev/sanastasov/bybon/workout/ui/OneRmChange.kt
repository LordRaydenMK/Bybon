package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Weight

enum class OneRmChange {
    Improved,
    Decreased,
    Unchanged,
}

fun oneRmChange(current: Weight?, previous: Weight?): OneRmChange {
    if (current == null || previous == null) return OneRmChange.Unchanged
    return when {
        current > previous -> OneRmChange.Improved
        current < previous -> OneRmChange.Decreased
        else -> OneRmChange.Unchanged
    }
}
