package dev.sanastasov.bybon.onermcalc

fun calculate1Rm(weight: Float, reps: Int): Float {
    require(weight > 0) { "Weight must be positive. Found `$weight`" }
    require(reps > 0) { "Reps must be positive. Found `$reps`" }

    return if (reps < 10) {
        // Brzycki
        weight * 36f / (37 - reps)
    } else {
        // Epley
        weight * (1 + reps / 30f)
    }
}

