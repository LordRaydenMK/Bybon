package dev.sanastasov.bybon.weight.input

sealed class WeightInputAction {
    data class OnWeightChanged(val weight: String) : WeightInputAction()
    data class OnSaveWeight(val weight: String) : WeightInputAction()
}