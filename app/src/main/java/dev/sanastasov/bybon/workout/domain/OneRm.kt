package dev.sanastasov.bybon.workout.domain

/** Brzycki when reps &lt; 10, otherwise Epley. */
fun estimateOneRmKg(weightKg: Float, reps: Int): Float = if (reps < 10) {
    weightKg * 36f / (37 - reps)
} else {
    weightKg * (1 + reps / 30f)
}

fun estimatedOneRm(weight: Weight, reps: Int): Weight =
    Weight.kilograms(estimateOneRmKg(weight.kilogramsValue, reps))
