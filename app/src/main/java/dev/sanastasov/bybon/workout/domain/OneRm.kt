package dev.sanastasov.bybon.workout.domain

/** Brzycki when reps &lt; 10, otherwise Epley. */
fun estimateOneRmKg(weightKg: Float, reps: Int): Float = if (reps < 10) {
    weightKg * 36f / (37 - reps)
} else {
    weightKg * (1 + reps / 30f)
}

internal fun oneRmOrNull(weight: Weight, reps: Int): Weight? {
    val kg = weight.kilogramsValue
    if (kg <= 0f || reps <= 0) return null
    return Weight.kilograms(estimateOneRmKg(kg, reps))
}
