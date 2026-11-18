package dev.sanastasov.bybon.workout.ui

import dev.sanastasov.bybon.workout.domain.Weight

fun formatAssistedAdjustedLoadLabel(bodyWeight: Weight?, effectiveLoad: Weight?): String? {
    val body = bodyWeight?.kilograms ?: return null
    val lifted = effectiveLoad?.kilograms
    return if (lifted != null) "$body → $lifted kg" else "$body kg"
}

fun formatLoggedLoadWithAdjusted(weightKg: String?, adjustedLoadLabel: String?): String {
    val logged = weightKg ?: return adjustedLoadLabel ?: MISSING_LOAD_LABEL
    return if (adjustedLoadLabel != null) "$logged · $adjustedLoadLabel" else logged
}
