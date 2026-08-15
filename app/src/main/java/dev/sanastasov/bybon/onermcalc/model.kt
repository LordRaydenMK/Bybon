package dev.sanastasov.bybon.onermcalc

data class OneRmEntry(val weight: Float, val reps: Int) {
    init {
        require(weight > 0) { "Weight must be positive. Found `$weight`" }
        require(reps > 0) { "Reps must be positive. Found `$reps`" }
    }
}

fun OneRmEntry.calculate1Rm(): Float = if (reps < 10) {
    // Brzycki
    weight * 36f / (37 - reps)
} else {
    // Epley
    weight * (1 + reps / 30f)
}

sealed class OneRmCalcAction {
    data class OnWeightChanged(val newWeight: String) : OneRmCalcAction()
    data class OnRepsChanged(val newReps: String) : OneRmCalcAction()
    data class OnUpdateWeight(val amount: Float) : OneRmCalcAction()
    data class OnUpdateReps(val amount: Int) : OneRmCalcAction()
}

